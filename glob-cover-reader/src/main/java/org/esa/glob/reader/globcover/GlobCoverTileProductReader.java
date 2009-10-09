package org.esa.glob.reader.globcover;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.FileUtils;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;

class GlobCoverTileProductReader extends AbstractGcProductReader {

    private static final String PRODUCT_TYPE_ANNUAL = "GC_TILE_AN";
    private static final String PRODUCT_TYPE_BIMON = "GC_TILE_BI";

    private GCTileFile gcTileFile;

    protected GlobCoverTileProductReader(GlobCoverTileReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        return createProduct();
    }

    private Product createProduct() throws IOException {
        gcTileFile = new GCTileFile(getInputFile());
        int width = gcTileFile.getWidth();
        final File fileLocation = new File(gcTileFile.getFilePath());
        int height = gcTileFile.getHeight();
        final String prodName = FileUtils.getFilenameWithoutExtension(fileLocation);
        final String prodType = getProductType(gcTileFile);

        final GCTileFile refGcFile = gcTileFile;

        return createProduct(refGcFile, prodName, prodType, width, height);
    }

    @SuppressWarnings({"SuspiciousSystemArraycopy"})
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

    @Override
    protected GeoPos getUpperLeftPosition() throws IOException {
        return gcTileFile.getUpperLeftCorner();
    }

    @Override
    protected String getBimonthlyProductType() {
        return PRODUCT_TYPE_BIMON;
    }

    @Override
    protected String getAnnualProductType() {
        return PRODUCT_TYPE_ANNUAL;
    }


    @Override
    public void close() throws IOException {
        if (gcTileFile != null) {
            gcTileFile.close();
            gcTileFile = null;
        }
        super.close();
    }


    private File getInputFile() throws IOException {
        final Object input = getInput();

        if (!(input instanceof String || input instanceof File)) {
            throw new IOException("Input object must either be a string or a file.");
        }
        return new File(String.valueOf(input));
    }

}
