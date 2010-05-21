package org.esa.beam.dataio.igbp.glcc;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.CsvReader;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

class IgbpGlccProductReader extends AbstractProductReader {

    private static final int RASTER_WIDTH = 43200;
    private static final int RASTER_HEIGHT = 21600;
    private static final String PRODUCT_TYPE = "IGBP_GLCC";
    private static final String BAND_NAME = "classes";

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
        final Product product = new Product(productName.toUpperCase(), PRODUCT_TYPE, RASTER_WIDTH, RASTER_HEIGHT);
        final CrsGeoCoding geoCoding = createGeoCoding();
        product.setGeoCoding(geoCoding);
        final IndexCoding indexCoding = createIndexCoding(productName);
        // todo set description to product
        final Band band = product.addBand(BAND_NAME, ProductData.TYPE_INT8);
        product.getIndexCodingGroup().add(indexCoding);
        band.setSampleCoding(indexCoding);
        // todo - band.setSampleCoding();
        band.setSourceImage(getMultiLevelImage(ImageManager.getImageToModelTransform(geoCoding)));

        return product;
    }

    private IndexCoding createIndexCoding(String productName) throws IOException {
        final InputStream stream = this.getClass().getResourceAsStream(productName.toLowerCase() + ".csv");
        final CsvReader csvReader = new CsvReader(new InputStreamReader(stream), new char[]{';'});
        final List<String[]> legendStrings = csvReader.readStringRecords();
        final IndexCoding indexCoding = new IndexCoding(productName + "_classes");
        for (int i = 0; i < legendStrings.size(); i++) {
            String[] legendString = legendStrings.get(i);
            final int value = Integer.parseInt(legendString[0]);
            final String description = legendString[1].trim();
            indexCoding.addIndex("class_" + i, value, description);
        }
        return indexCoding;
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
