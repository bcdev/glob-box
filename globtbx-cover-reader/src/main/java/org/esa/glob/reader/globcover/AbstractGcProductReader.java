package org.esa.glob.reader.globcover;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
abstract class AbstractGcProductReader extends AbstractProductReader {

    private static final double PIXEL_SIZE_DEG = 1 / 360.0;
    private static final double PIXEL_CENTER = 0.5;

    protected AbstractGcProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    protected Product createProduct(GCTileFile refGcFile, String prodName, String prodType,
                                    int width, int height) throws IOException {
        final Product product = new Product(prodName, prodType, width, height);
        product.setFileLocation(new File(refGcFile.getFilePath()));
        product.setStartTime(refGcFile.getStartDate());
        product.setEndTime(refGcFile.getEndDate());

        addGeoCoding(product);
        addBands(product, refGcFile);
        addIndexCodingAndBitmasks(product.getBand("SM"));
        product.getMetadataRoot().addElement(refGcFile.getMetadata());
        return product;
    }

    protected void addBands(Product product, GCTileFile gcTileFile) {
        final List<BandDescriptor> bandDescriptorList = gcTileFile.getBandDescriptorList();
        for (BandDescriptor descriptor : bandDescriptorList) {
            final Band band = new Band(descriptor.getName(), descriptor.getDataType(),
                                       product.getSceneRasterWidth(),
                                       product.getSceneRasterHeight());
            band.setScalingFactor(descriptor.getScaleFactor());
            band.setScalingOffset(descriptor.getOffsetValue());
            band.setDescription(descriptor.getDescription());
            band.setUnit(descriptor.getUnit());
            band.setNoDataValueUsed(descriptor.isFillValueUsed());
            band.setNoDataValue(descriptor.getFillValue());
            product.addBand(band);
            band.setSourceImage(getMultiLevelImage(band));
        }
    }

    protected void addGeoCoding(Product product) throws IOException {
        GeoPos ulPos = getUpperLeftPosition();
        final Rectangle rect = new Rectangle(product.getSceneRasterWidth(), product.getSceneRasterHeight());
        AffineTransform transform = new AffineTransform();
        transform.translate(ulPos.getLon(), ulPos.getLat());
        transform.scale(PIXEL_SIZE_DEG, -PIXEL_SIZE_DEG);
        transform.translate(-PIXEL_CENTER, -PIXEL_CENTER);

        try {
            product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, rect, transform));
        } catch (Exception e) {
            throw new IOException("Cannot create GeoCoding: ", e);
        }
    }

    protected void addIndexCodingAndBitmasks(Band smBand) {
        final IndexCoding coding = new IndexCoding("SM_coding");
        final MetadataAttribute land = coding.addSample("LAND", 0, "Not cloud, shadow or edge AND land");
        final MetadataAttribute flooded = coding.addSample("FLOODED", 1, "Not land and not cloud, shadow or edge");
        final MetadataAttribute suspect = coding.addSample("SUSPECT", 2, "Cloud shadow or cloud edge");
        final MetadataAttribute cloud = coding.addSample("CLOUD", 3, "Cloud");
        final MetadataAttribute water = coding.addSample("WATER", 4, "Not land");
        final MetadataAttribute snow = coding.addSample("SNOW", 5, "Snow");
        final MetadataAttribute invalid = coding.addSample("INVALID", 6, "Invalid");
        final Product product = smBand.getProduct();
        product.getIndexCodingGroup().add(coding);
        smBand.setSampleCoding(coding);
        final ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[]{
                new ColorPaletteDef.Point(0, Color.GREEN.darker(), "land"),
                new ColorPaletteDef.Point(1, Color.BLUE, "flooded"),
                new ColorPaletteDef.Point(2, Color.ORANGE, "suspect"),
                new ColorPaletteDef.Point(3, Color.GRAY, "cloud"),
                new ColorPaletteDef.Point(4, Color.BLUE.darker(), "water"),
                new ColorPaletteDef.Point(5, Color.LIGHT_GRAY, "snow"),
                new ColorPaletteDef.Point(6, Color.RED, "invalid")
        };
        smBand.setImageInfo(new ImageInfo(new ColorPaletteDef(points)));
        product.addBitmaskDef(new BitmaskDef("land", land.getDescription(), "SM == 0", Color.GREEN.darker(), 0.5f));
        product.addBitmaskDef(
                new BitmaskDef("flooded", flooded.getDescription(), "SM == 1", Color.BLUE, 0.5f));
        product.addBitmaskDef(new BitmaskDef("suspect", suspect.getDescription(), "SM == 2", Color.ORANGE, 0.5f));
        product.addBitmaskDef(new BitmaskDef("cloud", cloud.getDescription(), "SM == 3", Color.GRAY, 0.5f));
        product.addBitmaskDef(new BitmaskDef("water", water.getDescription(), "SM == 4", Color.BLUE.darker(), 0.5f));
        product.addBitmaskDef(new BitmaskDef("snow", snow.getDescription(), "SM == 5", Color.LIGHT_GRAY, 0.5f));
        product.addBitmaskDef(new BitmaskDef("invalid", invalid.getDescription(), "SM == 6", Color.RED, 0.5f));
    }


    protected String getProductType(GCTileFile refGcFile) {
        final String prodType;
        if (refGcFile.isAnnualFile()) {
            prodType = getAnnualProductType();
        } else {
            prodType = getBimonthlyProductType();
        }
        return prodType;
    }

    protected abstract String getBimonthlyProductType();

    protected abstract String getAnnualProductType();

    protected abstract GeoPos getUpperLeftPosition() throws IOException;

    protected abstract MultiLevelImage getMultiLevelImage(Band band);

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        throw new IOException(getClass().getSimpleName() + ".readBandRasterDataImpl not implemented");
    }
}
