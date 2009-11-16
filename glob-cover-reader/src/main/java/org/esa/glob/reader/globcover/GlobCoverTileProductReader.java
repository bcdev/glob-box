package org.esa.glob.reader.globcover;

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.math.MathUtils;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
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

    @Override
    protected GeoPos getUpperLeftPosition() throws IOException {
        return gcTileFile.getUpperLeftCorner();
    }

    @Override
    protected MultiLevelImage getMultiLevelImage(final Band band) {
        AffineTransform i2mTransform = ImageManager.getImageToModelTransform(band.getGeoCoding());
        int width = band.getSceneRasterWidth();
        int height = band.getSceneRasterHeight();
        DefaultMultiLevelModel model = new DefaultMultiLevelModel(i2mTransform, width, height);
        AbstractMultiLevelSource levelSource = new AbstractMultiLevelSource(model) {
            @Override
            protected RenderedImage createImage(int level) {
                int scale = MathUtils.ceilInt(getModel().getScale(level));
                int bufferType = ImageManager.getDataBufferType(band.getDataType());
                return new GCTileImage(gcTileFile, band.getName(), bufferType, scale);
            }
        };
        return new DefaultMultiLevelImage(levelSource);
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
