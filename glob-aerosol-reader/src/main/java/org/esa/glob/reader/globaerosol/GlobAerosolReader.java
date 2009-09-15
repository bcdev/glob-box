package org.esa.glob.reader.globaerosol;

import com.bc.ceres.core.ProgressMonitor;

import org.esa.beam.dataio.merisl3.ISINGrid;
import org.esa.beam.dataio.netcdf.NcAttributeMap;
import org.esa.beam.dataio.netcdf.NetcdfReaderUtils;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeocentricCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Group;
import ucar.nc2.Attribute;
import ucar.nc2.iosp.hdf4.ODLparser;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// CODE SNIPPET: Reading data
//                try {
//                    final Array array = ncfile.getIosp().readData(variable, shapeAsSection);
//                    final long l = array.getSize();
//                } catch (InvalidRangeException e) {
//                    throw new IOException(e);
//                }

public class GlobAerosolReader extends AbstractProductReader {
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

    private NetcdfFile ncfile;
    private ISINGrid isinGrid;
    private Map<Band, VariableAccessor1D> accessorMap;
    private RowInfo[] rowInfos;
    private Band lonBand;

    private int width;

    private int height;

    protected GlobAerosolReader(GlobAerosolReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        ncfile = getInputNetcdfFile();
        accessorMap = new HashMap<Band, VariableAccessor1D>();
        return createProduct();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        if (sourceStepX != 1 || sourceStepY != 1) {
            throw new IOException("Sub-sampling is not supported by this product reader.");
        }

        if (sourceWidth != destWidth || sourceHeight != destHeight) {
            throw new IllegalStateException("sourceWidth != destWidth || sourceHeight != destHeight");
        }
        
        synchronized (this) {
            if (rowInfos == null) {
                rowInfos = createRowInfos();
            }
        }
        
        pm.beginTask("Reading band '" + destBand.getName() + "'...", sourceHeight);
        try {
            for (int y = sourceOffsetY; y < sourceOffsetY + sourceHeight; y++) {
                if (pm.isCanceled()) {
                    break;
                }
                if (destBand.isNoDataValueUsed()) {
                    double noDataValue = destBand.getNoDataValue();
                    for (int x = sourceOffsetX; x < sourceOffsetX + sourceWidth; x++) {
                        final int rasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);
                        destBuffer.setElemDoubleAt(rasterIndex, noDataValue);
                    }
                }
                final int rowIndex = (height - 1) - y;
                RowInfo rowInfo = rowInfos[rowIndex];
                if (rowInfo != null ) {
                    Array lonData = read(lonBand, rowInfo);
                    Array bandData = read(destBand, rowInfo);
                    int dataSize = (int) bandData.getSize();
                    for (int dataIndex = 0; dataIndex < dataSize; dataIndex++) {
                        double lon = lonData.getDouble(dataIndex) + 180;
                        int colIndex = isinGrid.getColIndex(rowIndex, lon);
                        
                        int rowLength = isinGrid.getRowLength(rowIndex);
                        int x = isinGrid.getRowCount() - (rowLength / 2) + colIndex;
                        
                        if (x >= sourceOffsetX && x < sourceOffsetX + sourceWidth) {
                            final int rasterIndex = sourceWidth * (y - sourceOffsetY) + (x - sourceOffsetX);
                            destBuffer.setElemDoubleAt(rasterIndex,  bandData.getDouble(dataIndex));
                        }
                    }
                } else {
                    // ???
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }
    
    private Array read(Band band, RowInfo rowInfo) throws IOException {
        VariableAccessor1D accessor = accessorMap.get(band);
        try {
            synchronized (ncfile) {
                return accessor.read(rowInfo.offset, rowInfo.length).reduce();
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        accessorMap.clear();
        if (ncfile != null) {
            ncfile.close();
            ncfile = null;
        }
        super.close();
    }

    private Product createProduct() throws IOException {
        isinGrid = new ISINGrid(2004);
        width = isinGrid.getRowCount() * 2;
        height = isinGrid.getRowCount();
        NcAttributeMap globalAttributes = NcAttributeMap.create(ncfile);
        String prodName = globalAttributes.getStringValue("ProductID");
        if (prodName == null) {
            prodName = FileUtils.getFilenameWithoutExtension(ncfile.getLocation());
        }
        final String prodType = "FOOOOOOOOOOOOOOO";
        final Product product = new Product(prodName, prodType, width, height);
        //TODO
//        product.setStartTime(getDate(GA_START_DATE));
//        product.setEndTime(getDate(GA_END_DATE));

        addBands(product);
        // todo - geocoding
        addGeoCoding(product);
        
        NetcdfReaderUtils.transferMetadata(ncfile, product.getMetadataRoot());
        return product;
    }
    
    private void addGeoCoding(Product product) throws IOException {
        DefaultGeographicCRS base = DefaultGeographicCRS.WGS84;
        
        final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
        ParameterValueGroup parameters;
        try {
            parameters = transformFactory.getDefaultParameters("OGC:Sinusoidal");
        } catch (NoSuchIdentifierException e) {
            throw new IOException(e);
        }

        Ellipsoid ellipsoid = base.getDatum().getEllipsoid();
        parameters.parameter("semi_major").setValue(ellipsoid.getSemiMajorAxis());
        parameters.parameter("semi_minor").setValue(ellipsoid.getSemiMinorAxis());

        MathTransform mathTransform;
        try {
            mathTransform = transformFactory.createParameterizedTransform(parameters);
        } catch (Exception e) {
            throw new IOException(e);
        }
        
        CoordinateReferenceSystem modelCrs = new DefaultProjectedCRS("", base, mathTransform, DefaultCartesianCS.PROJECTED);
//        CoordinateReferenceSystem modelCrs = base;
        Rectangle rectangle = new Rectangle(0, 0, width, height);
        AffineTransform i2m = new AffineTransform();
        i2m.translate(-product.getSceneRasterWidth() / 2, -product.getSceneRasterHeight() / 2);
        try {
            CrsGeoCoding geoCoding = new CrsGeoCoding(modelCrs, rectangle, i2m);
            product.setGeoCoding(geoCoding);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void addBands(Product product) throws IOException {
        List<Variable> variableList = ncfile.getRootGroup().getVariables();
        for (Variable variable : variableList) {
            int cellDimemsionIndex = variable.findDimensionIndex("cell");
            if (cellDimemsionIndex != -1) {
                Band band = NetcdfReaderUtils.createBand(variable, width, height);
                accessorMap.put(band, new VariableAccessor1D(variable, "cell"));
                product.addBand(band);
                if (band.getName().equals("lon")) {
                    lonBand = band;
                }
            }
//            Dimension dimension = variable.getDimension(cellDimemsionIndex);
//            List<Range> ranges = variable.getRanges();
//            for (Range range : ranges) {
//                System.out.println(range.getName()+" - "+range.toString());
//            }
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
    
    private RowInfo[] createRowInfos() throws IOException {
        final RowInfo[] binLines = new RowInfo[height];
        final Variable latVariable = ncfile.getRootGroup().findVariable("lat");
        final float[] latValues = (float[]) latVariable.read().getStorage();
        double deltaLat = isinGrid.getDeltaLat();
        double lastLatValue = -91;
        int lastRowIndex = -1;
        int lineOffset = 0;
        int lineLength = 0;
        for (int i = 0; i < latValues.length; i++) {

            final double lat = latValues[i];
            if (lat < lastLatValue) {
                throw new IOException("Unrecognized level-3 format. Bins numbers expected to appear in ascending order.");
            }
            if (lastLatValue == lat) {
                lineLength++;
            } else {
                lastLatValue = lat;
                int rowIndex = (int) Math.round(((lat + 90.0) / deltaLat)+0.5);

                if (rowIndex == lastRowIndex) {
                    throw new IOException("boooo");
                }
                if (lineLength > 0) {
                    lastRowIndex = rowIndex;
                    binLines[lastRowIndex] = new RowInfo(lineOffset, lineLength);
                }
                lineOffset = i;
                lineLength = 1;
            }
        }

        if (lineLength > 0) {
            binLines[lastRowIndex] = new RowInfo(lineOffset, lineLength);
        }

        return binLines;
    }
    
    private static final class RowInfo {

        // offset of row within file
        final int offset;
        // number of bins per row
        final int length;

        public RowInfo(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }
    }

}
