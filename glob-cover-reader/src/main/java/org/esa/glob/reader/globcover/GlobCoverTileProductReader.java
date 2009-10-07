package org.esa.glob.reader.globcover;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.netcdf.NetcdfReaderUtils;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.FileUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

class GlobCoverTileProductReader extends AbstractProductReader {

    private static final String PRODUCT_TYPE_ANUUAL = "GC_L3_AN";
    private static final String PRODUCT_TYPE_BIMON = "GC_L3_BI";
    private static final double PIXEL_SIZE_DEG = 1 / 360.0;
    private static final double PIXEL_CENTER = 0.5;

    private GCTileFile gcTileFile;

    protected GlobCoverTileProductReader(GlobCoverTileReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        gcTileFile = new GCTileFile(getInputNetcdfFile());
        return createProduct();
    }

    @Override
    public void close() throws IOException {
        if (gcTileFile != null) {
            gcTileFile.close();
            gcTileFile = null;
        }
        super.close();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        final String bandName = destBand.getName();
        pm.beginTask("Reading band '" + bandName + "'...", 1);
        try {
            final Array array = gcTileFile.readData(bandName,
                                                    sourceOffsetX, sourceOffsetY,
                                                    sourceWidth, sourceHeight,
                                                    sourceStepX, sourceStepY);
            final Object storage = array.getStorage();
            System.arraycopy(storage, 0, destBuffer.getElems(), 0, destWidth * destHeight);
            pm.worked(1);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            pm.done();
        }
    }

    private Product createProduct() throws IOException {
        int width = gcTileFile.getWidth();
        int height = gcTileFile.getHeight();
        final File fileLocation = new File(gcTileFile.getFilePath());
        final String prodName = FileUtils.getFilenameWithoutExtension(fileLocation);
        final String prodType;
        if (gcTileFile.isAnnualFile()) {
            prodType = PRODUCT_TYPE_ANUUAL;
        } else {
            prodType = PRODUCT_TYPE_BIMON;
        }
        final Product product = new Product(prodName, prodType, width, height);
        product.setFileLocation(fileLocation);
        product.setStartTime(gcTileFile.getStartDate());
        product.setEndTime(gcTileFile.getEndDate());

        addBands(product, gcTileFile);
        GCTileFile.addIndexCodingAndBitmasks(product.getBand("SM"));
        final MetadataElement metadataElement = NetcdfReaderUtils.createMetadataElement(gcTileFile.getNetcdfFile());
        product.getMetadataRoot().addElement(metadataElement);
        addGeoCoding(product, gcTileFile);

        return product;
    }

    private void addBands(Product product, final GCTileFile gcTileFile) throws IOException {
        final List<BandDescriptor> bandDescriptorList = gcTileFile.getBandDescriptorList();
        for (BandDescriptor descriptor : bandDescriptorList) {
            final Band band = new Band(descriptor.getName(), descriptor.getDataType(),
                                       descriptor.getWidth(),
                                       descriptor.getHeight());
            band.setScalingFactor(descriptor.getScaleFactor());
            band.setScalingOffset(descriptor.getOffsetValue());
            band.setDescription(descriptor.getDescription());
            band.setUnit(descriptor.getUnit());
            band.setNoDataValueUsed(descriptor.isFillValueUsed());
            band.setNoDataValue(descriptor.getFillValue());
            product.addBand(band);
        }
    }

    private void addGeoCoding(Product product, final GCTileFile gcTileFile) throws IOException {
        GeoPos ulPos = gcTileFile.getUpperLeftCorner();
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
