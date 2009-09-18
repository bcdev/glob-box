package org.esa.glob.reader.globcover;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.iosp.hdf4.ODLparser;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GlobCoverMosaicReader extends AbstractProductReader {

    private static final Object LOCK = new Object();

    private static final String XDIM = "XDim";
    private static final String YDIM = "YDim";
    private static final String GROUP_POSTEL = "POSTEL";
    private static final String GROUP_DATA_FIELDS = "Data Fields";
    private static final String GROUP_GRID_ATTRIBUTES = "Grid Attributes";
    private static final String STRUCT_METADATA_0 = "StructMetadata%2e0";   // StructMetadata.0
    private static final String DF_DIMENSION_X = GROUP_POSTEL + "/" + GROUP_DATA_FIELDS + "/" + XDIM;
    private static final String DF_DIMENSION_Y = GROUP_POSTEL + "/" + GROUP_DATA_FIELDS + "/" + YDIM;
    private static final String GA_START_DATE = GROUP_POSTEL + "/" + GROUP_GRID_ATTRIBUTES + "/Product start date";
    private static final String GA_END_DATE = GROUP_POSTEL + "/" + GROUP_GRID_ATTRIBUTES + "/Product end date";
    private static final String ATTRIB_UNSIGNED = "_Unsigned";
    private static final String ATTRIB_FILL_VALUE = "_FillValue";

    private static final Map<DataType, Integer> dataTypeMap = new EnumMap<DataType, Integer>(DataType.class);

    static {
        dataTypeMap.put(DataType.BYTE, ProductData.TYPE_INT8);
        dataTypeMap.put(DataType.INT, ProductData.TYPE_INT32);
        dataTypeMap.put(DataType.SHORT, ProductData.TYPE_INT16);
        dataTypeMap.put(DataType.FLOAT, ProductData.TYPE_FLOAT32);
        dataTypeMap.put(DataType.DOUBLE, ProductData.TYPE_FLOAT64);
    }

    private static final String PRODUCT_TYPE_ANUUAL = "GC_L3_AN";
    private static final String PRODUCT_TYPE_BIMON = "GC_L3_BI";
    private static final double PIXEL_SIZE_DEG = 1/360.0;
    private static final double PIXEL_CENTER = 0.5;

    private NetcdfFile ncfile;

    protected GlobCoverMosaicReader(GlobCoverMosaicReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        ncfile = getInputNetcdfFile();
        printDebugInfo(false);
        return createProduct();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        final Group group = ncfile.getRootGroup().findGroup(GROUP_POSTEL).findGroup(GROUP_DATA_FIELDS);
        final Variable variable = group.findVariable(destBand.getName());
        if (variable == null) {
            throw new IOException("Unknown band name: " + destBand.getName());
        }
        pm.beginTask("Reading band '" + destBand.getName() + "'...", 1);
        try {
            final int indexDimX = variable.findDimensionIndex(XDIM);
            final int indexDimY = variable.findDimensionIndex(YDIM);
            int[] origin = new int[2];
            int[] size = new int[2];
            int[] stride = new int[2];
            origin[indexDimX] = sourceOffsetX;
            origin[indexDimY] = sourceOffsetY;
            size[indexDimX] = sourceWidth;
            size[indexDimY] = sourceHeight;
            stride[indexDimX] = sourceStepX;
            stride[indexDimY] = sourceStepY;
            final Section section = new Section(origin, size, stride);
            final Array array;
            synchronized (LOCK) {
                array = ncfile.getIosp().readData(variable, section);
            }
            final Object storage = array.getStorage();
            System.arraycopy(storage, 0, destBuffer.getElems(),
                             0, destWidth * destHeight);
            pm.worked(1);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            pm.done();
        }
    }

    @Override
    public void close() throws IOException {
        if (ncfile != null) {
            ncfile.close();
            ncfile = null;
        }
        super.close();
    }

    private Product createProduct() throws IOException {
        int width = ncfile.findDimension(DF_DIMENSION_X).getLength();
        int height = ncfile.findDimension(DF_DIMENSION_Y).getLength();
        final File fileLocation = new File(ncfile.getLocation());
        final String prodName = FileUtils.getFilenameWithoutExtension(fileLocation);
        final String prodType;
        if (prodName.toUpperCase().contains("ANNUAL")) {
            prodType = PRODUCT_TYPE_ANUUAL;
        } else {
            prodType = PRODUCT_TYPE_BIMON;
        }
        final Product product = new Product(prodName, prodType, width, height);
        product.setFileLocation(fileLocation);
        product.setStartTime(getDate(GA_START_DATE, prodType));
        product.setEndTime(getDate(GA_END_DATE, prodType));

        addBands(product);
        addIndexCoding(product.getBand("SM"));
        addMetadata(product.getMetadataRoot(), ncfile.findGroup(GROUP_POSTEL).getGroups());
        addGeoCoding(product);

        return product;
    }

    private void addBands(Product product) throws IOException {
        final String gridAttribPrefix = String.format("%s/%s/", GROUP_POSTEL, GROUP_GRID_ATTRIBUTES);
        final Group group = ncfile.getRootGroup().findGroup(GROUP_POSTEL).findGroup(GROUP_DATA_FIELDS);
        final List<Variable> variableList = group.getVariables();
        for (Variable variable : variableList) {
            final String name = variable.getShortName();
            final Integer dataType = getMappedDataType(variable);
            final int width = variable.getDimension(variable.findDimensionIndex(XDIM)).getLength();
            final int height = variable.getDimension(variable.findDimensionIndex(YDIM)).getLength();
            final Band band = new Band(name, dataType, width, height);

            final String offsetName = String.format("%s%s%s", gridAttribPrefix, "Offset ", name);
            final Number offset = ncfile.findGlobalAttributeIgnoreCase(offsetName).getNumericValue();
            final String scaleName = String.format("%s%s%s", gridAttribPrefix, "Scale ", name);
            final Number scale = ncfile.findGlobalAttributeIgnoreCase(scaleName).getNumericValue();
            // Product Description Manual - GlobCover: p. 17
            // The physical value FDphys is given by the following formula:
            // FDphys = (FD - OffsetFD) / ScaleFD
            // that's why we have to convert the values
            final double scaleFactor = 1 / scale.doubleValue();
            double offsetValue = 0.0;
            if (offset.doubleValue() != 0.0) {
                offsetValue = -offset.doubleValue() / scale.doubleValue();
            }
            band.setScalingFactor(scaleFactor);
            band.setScalingOffset(offsetValue);
            band.setDescription(variable.getDescription());
            band.setUnit(variable.getUnitsString());
            Attribute fillAttrib = variable.findAttribute(ATTRIB_FILL_VALUE);
            if (fillAttrib != null) {
                band.setNoDataValueUsed(true);
                band.setNoDataValue(fillAttrib.getNumericValue().doubleValue());
            }
            product.addBand(band);
        }
    }

    private void addIndexCoding(Band band) {
        final IndexCoding coding = new IndexCoding("SM_coding");
        final MetadataAttribute land = coding.addSample("LAND", 0, "Not cloud, shadow or edge AND land");
        final MetadataAttribute flooded = coding.addSample("FLOODED", 1,"Not land and not cloud, shadow or edge");
        final MetadataAttribute suspect = coding.addSample("SUSPECT", 2, "Cloud shadow or cloud edge");
        final MetadataAttribute cloud = coding.addSample("CLOUD", 3, "Cloud");
        final MetadataAttribute water = coding.addSample("WATER", 4, "Not land");
        final MetadataAttribute snow = coding.addSample("SNOW", 5, "Snow");
        final MetadataAttribute invalid = coding.addSample("INVALID", 6, "Invalid");
        final Product product = band.getProduct();
        product.getIndexCodingGroup().add(coding);
        band.setSampleCoding(coding);
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(0, Color.GREEN.darker(), "land"),
                new ColorPaletteDef.Point(1, Color.BLUE, "flooded"),
                new ColorPaletteDef.Point(2, Color.ORANGE, "suspect"),
                new ColorPaletteDef.Point(3, Color.GRAY, "cloud"),
                new ColorPaletteDef.Point(4, Color.BLUE.darker(), "water"),
                new ColorPaletteDef.Point(5, Color.LIGHT_GRAY, "snow"),
                new ColorPaletteDef.Point(6, Color.RED, "invalid")
        };
        band.setImageInfo(new ImageInfo(new ColorPaletteDef(points)));
        product.addBitmaskDef(new BitmaskDef("land", land.getDescription(), "SM == 0", Color.GREEN.darker(), 0.5f));
        product.addBitmaskDef(
                new BitmaskDef("flooded", flooded.getDescription(), "SM == 1", Color.BLUE, 0.5f));
        product.addBitmaskDef(new BitmaskDef("suspect", suspect.getDescription(), "SM == 2", Color.ORANGE, 0.5f));
        product.addBitmaskDef(new BitmaskDef("cloud", cloud.getDescription(), "SM == 3", Color.GRAY, 0.5f));
        product.addBitmaskDef(new BitmaskDef("water", water.getDescription(), "SM == 4", Color.BLUE.darker(), 0.5f));
        product.addBitmaskDef(new BitmaskDef("snow", snow.getDescription(), "SM == 5", Color.LIGHT_GRAY, 0.5f));
        product.addBitmaskDef(new BitmaskDef("invalid", invalid.getDescription(), "SM == 6", Color.RED, 0.5f));
    }
    
    private void addGeoCoding(Product product) throws IOException {
        GeoPos ulPos = getUpperLeftCornerFromStructMetadata();
        final Rectangle2D.Double rect = new Rectangle2D.Double(0, 0,
                                                               product.getSceneRasterWidth(),
                                                               product.getSceneRasterHeight());
        AffineTransform transform = new AffineTransform();
        transform.translate(ulPos.getLon(), ulPos.getLat());
        transform.scale(PIXEL_SIZE_DEG, -PIXEL_SIZE_DEG);
        transform.translate(-PIXEL_CENTER, -PIXEL_CENTER);

        try {
            final CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, rect, transform);
            product.setGeoCoding(geoCoding);
        } catch (Exception e) {
            throw new IOException("Can not create GeoCoding: ", e);
        }

    }

    private GeoPos getUpperLeftCornerFromStructMetadata() throws IOException {
        final Variable structMetadata0 = ncfile.findVariable(STRUCT_METADATA_0);
        final Array array = structMetadata0.read();
        final String structMetadata0Text = new String(array.getDataAsByteBuffer().array());
        final Element element = new ODLparser().parseFromString(structMetadata0Text);
        final Element grid = element.getChild("GridStructure").getChild("GRID_1");
        final Element ulPointDegElem = grid.getChild("UpperLeftPointMtrs");
        List ulValueElements = ulPointDegElem.getChildren("value");
        String ulPointLon = ((Element) ulValueElements.get(0)).getText();
        String ulPointLat = ((Element) ulValueElements.get(1)).getText();
        return createGeoPos(ulPointLon, ulPointLat);
    }

    static GeoPos createGeoPos(String lonString, String latString) {
        float lon = dgmToDec(lonString);
        float lat = dgmToDec(latString);
        return new GeoPos(lat, lon);
    }

    private static float dgmToDec(String dgmString) {
        final int dotIndex = dgmString.indexOf('.');
        final int secondsIndex = Math.max(0, dotIndex - 3);
        final float seconds = Float.parseFloat(dgmString.substring(secondsIndex));
        final int minutes;
        final int minutesIndex = Math.max(0, secondsIndex - 3);
        if(secondsIndex > 0) {
            minutes = Integer.parseInt(dgmString.substring(minutesIndex, secondsIndex));
        }else {
            minutes = 0;
        }
        final int degrees;
        if(minutesIndex > 0) {
            degrees = Integer.parseInt(dgmString.substring(0, minutesIndex));
        }else {
            degrees = 0;
        }
        return degrees + minutes / 60.0f + seconds / 3600.0f;
    }

    private void addMetadata(MetadataElement root, final List<Group> groups) {
        for (Group group : groups) {
            final MetadataElement subElement = new MetadataElement(group.getShortName());
            final List<Attribute> attributeList = group.getAttributes();
            if(attributeList.isEmpty()) {
                continue;
            }
            for (Attribute attribute : attributeList) {
                final ProductData data;
                if(attribute.isArray()) {
                    final Array array = attribute.getValues();
                    data = ProductData.createInstance(Arrays.toString((Object[])array.getStorage()));
                }else {
                    data = ProductData.createInstance(String.valueOf(attribute.getValue(0)));
                }
                final MetadataAttribute metaAttrib = new MetadataAttribute(attribute.getName(), data, true);
                
                subElement.addAttribute(metaAttrib);
            }
            addMetadata(subElement, group.getGroups());
            root.addElement(subElement);

        }
    }

    private Integer getMappedDataType(Variable variable) throws IOException {
        Integer type = dataTypeMap.get(variable.getDataType());
        if (type == null) {
            throw new IOException(String.format("Can not determine data type of variable '%s'", variable.getShortName()));
        }
        Attribute unsignedAttrib = variable.findAttribute(ATTRIB_UNSIGNED);
        if (unsignedAttrib != null && Boolean.parseBoolean(unsignedAttrib.getStringValue())) {
            type += 10;            // differece between signed and unsigned data type is 10
        }
        return type;
    }

    private ProductData.UTC getDate(String attribName, String productType) {
        final String dateString = ncfile.findGlobalAttributeIgnoreCase(attribName).getStringValue();
        try {
            // This is the date pattern as it is defined in the documentation
            String datePattern = "yyyy/MM/dd HH:mm:ss";
            if (PRODUCT_TYPE_ANUUAL.equals(productType)) {
                // for the annual mosaic the date pattern differs from the documentation
                datePattern = "yyyy/dd/MM HH:mm:ss";
            }
            return ProductData.UTC.parse(dateString, datePattern);
        } catch (ParseException e) {
            Debug.trace(String.format("Can not parse date of attriubte '%s': %s", attribName, e.getMessage()));
        }
        return ProductData.UTC.create(new Date(), 0);
    }

    private void printDebugInfo(boolean debug) {
        if (debug) {
            System.out.println(ncfile);
            // geocoding information is contained in variable StructMetadata.0, which is a string
            final Variable structMetadata0 = ncfile.findVariable(STRUCT_METADATA_0);
            try {
                final Array array = structMetadata0.read();
                final String structMetadata0Text = new String(array.getDataAsByteBuffer().array());
                final Element structMetadata0Elem = new ODLparser().parseFromString(structMetadata0Text);
                structMetadata0Elem.setName("StructMetadata.0");
                new XMLOutputter(Format.getPrettyFormat()).output(structMetadata0Elem, System.out);
            } catch (IOException e) {
                System.err.printf("Unable to print '%s': %s%n", STRUCT_METADATA_0, e.getMessage());
            }
        }
    }

    private NetcdfFile getInputNetcdfFile() throws IOException {
        final Object input = getInput();

        if (!(input instanceof String || input instanceof File)) {
            throw new IOException("Input object must either be a string or a file.");
        }
        final String path;
        if (input instanceof String) {
            path = (String) input;
        } else {
            path = ((File) input).getPath();
        }

        return NetcdfFile.open(path);
    }

}
