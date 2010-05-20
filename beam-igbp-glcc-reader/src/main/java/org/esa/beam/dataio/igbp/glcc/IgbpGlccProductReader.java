package org.esa.beam.dataio.igbp.glcc;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.Debug;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

class IgbpGlccProductReader extends AbstractProductReader {

    private static final int RASTER_WIDTH = 43200;
    private static final int RASTER_HEIGHT = 21600;
    private static final String PRODUCT_TYPE = "IGBP_GLCC";

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected IgbpGlccProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inputFile = getInputFile();
        String inputFileName = inputFile.getName();
        String productName = inputFileName.substring(1, inputFileName.indexOf('2'));
        final Product product = new Product(productName, PRODUCT_TYPE, RASTER_WIDTH, RASTER_HEIGHT);
        final CrsGeoCoding geoCoding = createGeoCoding();
        product.setGeoCoding(geoCoding);
        // todo set description to product
        final Band band = product.addBand(productName.toUpperCase(), ProductData.TYPE_INT8);
        // todo - band.setSampleCoding();
        band.setSourceImage(getMultiLevelImage(ImageManager.getImageToModelTransform(geoCoding)));

        return product;
    }

    private File getInputFile() {
        return new File(getInput().toString());
    }

    private CrsGeoCoding createGeoCoding() {
        AffineTransform i2m = new AffineTransform();
        double scale = 30 / 3600;
        double easting = -647985 / 3600;
        double northing = 323985 / 3600;
        i2m.translate(easting, northing);
        i2m.scale(scale, -scale);
        i2m.translate(-0.5, -0.5);

        try {
            // todo - validate if it is WGS_84
            return new CrsGeoCoding(DefaultGeographicCRS.WGS84, new Rectangle(RASTER_WIDTH, RASTER_HEIGHT), i2m);
        } catch (FactoryException e) {
            Debug.trace(e);
        } catch (TransformException e) {
            Debug.trace(e);
        }
        return null;
    }

    private DefaultMultiLevelImage getMultiLevelImage(AffineTransform i2mTransform) {

        final DefaultMultiLevelModel multiLevelModel = new DefaultMultiLevelModel(i2mTransform,
                                                                                  RASTER_WIDTH, RASTER_HEIGHT);
        return new DefaultMultiLevelImage(new AbstractMultiLevelSource(multiLevelModel) {
            @Override
            protected RenderedImage createImage(int level) {
                try {
                    return new IgbpGlccOpImage((int) multiLevelModel.getModelBounds().getWidth(),
                                               (int) multiLevelModel.getModelBounds().getHeight(),
                                               ResolutionLevel.create(multiLevelModel, level), getInputFile());
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to create image.", e);
                }
            }
        });
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("Nothing to read here.");
    }

}
