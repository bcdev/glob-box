package org.esa.beam.glob.core;

import com.bc.ceres.core.ExtensionManager;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.ProductUtils;

/**
 * User: Marco
 * Date: 19.06.2010
 */
public class TimeSeriesProductBuilder {
    private static final String PRODUCT_TYPE = "TIME_SERIES";
    private static final String TIME_SERIES_ROOT_NAME = PRODUCT_TYPE;
    private static final String PRODUCT_LIST_ELEM_NAME = "PRODUCT_LIST";

    public static Product createTimeSeriesProduct(String timeSeriesName, RasterDataNode refRaster, ProductManager productManager) {
        final Product tsProduct = new Product(timeSeriesName, PRODUCT_TYPE,
                                              refRaster.getSceneRasterWidth(),
                                              refRaster.getSceneRasterHeight());
        tsProduct.setDescription("A time series product");
        final Product refProduct = refRaster.getProduct();
        ProductUtils.copyGeoCoding(refProduct, tsProduct);
        // todo replace default time coding
        final MetadataElement timeSeriesMetaData = new MetadataElement(TIME_SERIES_ROOT_NAME);
        final MetadataElement productListElement = new MetadataElement(PRODUCT_LIST_ELEM_NAME);
        timeSeriesMetaData.addElement(productListElement);
        tsProduct.getMetadataRoot().addElement(timeSeriesMetaData);

        ExtensionManager extensionManager = ExtensionManager.getInstance();
        final Product[] products = productManager.getProducts();
        for (Product product : products) {
            final String nodeName = refRaster.getName();
            if (isProductCompatible(product, tsProduct, nodeName)) {
                final RasterDataNode raster = product.getRasterDataNode(nodeName);
                TimeCoding rasterTimeCoding = extensionManager.getExtension(raster, TimeCoding.class);
                if (rasterTimeCoding == null) {
                    continue;
                }
                final ProductData productPath = ProductData.createInstance(product.getFileLocation().getPath());
                productListElement.addAttribute(new MetadataAttribute(product.getName(), productPath, true));

                final ProductData.UTC rasterStartTime = rasterTimeCoding.getStartTime();
                final ProductData.UTC rasterEndTime = rasterTimeCoding.getEndTime();
                final Band band = tsProduct.addBand(nodeName + "_" + rasterStartTime.format(),
                                                    raster.getDataType());
                band.setSourceImage(raster.getSourceImage());
                ProductUtils.copyRasterDataNodeProperties(raster, band);
                // todo copy also referenced bandy in valid pixel expression
                band.setValidPixelExpression(null);
                ProductData.UTC tsStartTime = tsProduct.getStartTime();
                if (tsStartTime == null || rasterStartTime.getAsDate().before(tsStartTime.getAsDate())) {
                    tsProduct.setStartTime(rasterStartTime);
                }
                ProductData.UTC tsEndTime = tsProduct.getEndTime();
                if (tsEndTime == null || rasterEndTime.getAsDate().after(tsEndTime.getAsDate())) {
                    tsProduct.setEndTime(rasterEndTime);
                }
            }
        }
        return tsProduct;
    }

    public static boolean isProductCompatible(Product product, Product tsProduct, String rasterName) {
        return product.getFileLocation() != null &&
                product.containsRasterDataNode(rasterName) &&
                tsProduct.isCompatibleProduct(product, 0.1e-6f);
    }
}
