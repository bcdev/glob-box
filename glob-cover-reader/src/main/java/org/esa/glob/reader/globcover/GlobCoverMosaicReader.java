package org.esa.glob.reader.globcover;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Group;
import ucar.nc2.Attribute;
import ucar.nc2.iosp.hdf4.ODLparser;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// CODE SNIPPET: Reading data
//                try {
//                    final Array array = ncfile.getIosp().readData(variable, shapeAsSection);
//                    final long l = array.getSize();
//                } catch (InvalidRangeException e) {
//                    throw new IOException(e);
//                }

public class GlobCoverMosaicReader extends AbstractProductReader {
    private static final boolean DEBUG = true;

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

    private NetcdfFile ncfile;

    protected GlobCoverMosaicReader(GlobCoverMosaicReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        try {
            ncfile = getInputNetcdfFile();
            return createProduct();
        } finally {
            close();
        }
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        // todo - read data
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
        printDebugInfo(DEBUG);
        int width = ncfile.findDimension(DF_DIMENSION_X).getLength();
        int height = ncfile.findDimension(DF_DIMENSION_Y).getLength();
        final String fileName = FileUtils.getFileNameFromPath(ncfile.getLocation());
        final String prodName = FileUtils.getFilenameWithoutExtension(fileName);
        final String prodType;
        if(fileName.toUpperCase().contains("ANNUAL")) {
            prodType = PRODUCT_TYPE_ANUUAL;
        }else {
            prodType = PRODUCT_TYPE_BIMON;
        }
        final Product product = new Product(prodName, prodType, width, height);
        product.setStartTime(getDate(GA_START_DATE));
        product.setEndTime(getDate(GA_END_DATE));

        addBands(product);
        // todo - geocoding
        // todo - meta data
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
            final Band band = new Band(name, dataType,  width, height);

            Attribute fillAttrib = variable.findAttribute(ATTRIB_FILL_VALUE);
            if(fillAttrib != null) {
                band.setNoDataValueUsed(true);
                band.setNoDataValue(fillAttrib.getNumericValue().doubleValue());
            }
            // Product Description Manual - GlobCover: p. 17
            // The physical value FDphys is given by the following formula:
            // FDphys = (FD - OffsetFD) / ScaleFD  TODO - this is different to the normal BEAM scale
            final String offsetName = String.format("%s%s%s", gridAttribPrefix, "Offset ",name);
            final Number offset = ncfile.findGlobalAttributeIgnoreCase(offsetName).getNumericValue();
            band.setScalingOffset(offset.doubleValue());
            final String scaleName = String.format("%s%s%s", gridAttribPrefix, "Scale ",name);
            final Number scale = ncfile.findGlobalAttributeIgnoreCase(scaleName).getNumericValue();
            band.setScalingOffset(scale.doubleValue());
            product.addBand(band);
        }
    }

    private Integer getMappedDataType(Variable variable) throws IOException {
        Integer type = dataTypeMap.get(variable.getDataType());
        if(type == null) {
            throw new IOException("Can not determine data type of variable '"+variable.getShortName() + "'");
        }
        Attribute unsignedAttrib = variable.findAttribute(ATTRIB_UNSIGNED);
        if(unsignedAttrib != null && Boolean.parseBoolean(unsignedAttrib.getStringValue())) {
            type += 10;            // differece between signed and unsigned data type is 10
        }
        return type;
    }

    private ProductData.UTC getDate(String attribName) {
        final String dateString = ncfile.findGlobalAttributeIgnoreCase(attribName).getStringValue();
        try {
            return ProductData.UTC.parse(dateString, "yyyy/dd/MM HH:mm:ss");
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
