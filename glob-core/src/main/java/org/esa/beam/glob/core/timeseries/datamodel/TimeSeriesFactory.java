package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.DefaultTimeCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TimeCoding;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 23.06.2010
 * Time: 18:14:12
 */
public class TimeSeriesFactory {

    /**
     * Creates a new ITimeSeries from a given time series product. Since this
     * product is already a complete time series, this method should only be called by the reader
     *
     * @param product a time series product
     *
     * @return a time series wrapping the given product
     */
    public static TimeSeries create(Product product) {
        final TimeSeriesImpl timeSeries = new TimeSeriesImpl(product);
        TimeSeriesMapper.getInstance().put(product, timeSeries);
        return timeSeries;
    }

    /**
     * Creates a new TimeSeries with a given name, a list of product locations and a list of variables (which are
     * placeholders for bands)
     *
     * @param name             a name for the time series
     * @param productLocations locations where to find the data the time series is based on
     * @param variableNames    the variables the time series is based on
     *
     * @return a time series
     */
    public static TimeSeries create(String name, List<ProductLocation> productLocations,
                                    List<String> variableNames) {
        Guardian.assertNotNull("productLocations", productLocations);
        Guardian.assertGreaterThan("productLocations.size()", productLocations.size(), 0);
        Guardian.assertNotNull("variables", variableNames);
        Guardian.assertGreaterThan("variables.size()", variableNames.size(), 0);
        Guardian.assertNotNullOrEmpty("name", name);

        // todo get ref product in a smarter way
        final List<Product> productList = new ArrayList<Product>();
        for (ProductLocation productLocation : productLocations) {
            productList.addAll(productLocation.findProducts());
        }
        if (productList.isEmpty()) {
            return null;
        }
        Product refProduct = productList.get(0);
        final Product tsProduct = new Product(name, TimeSeriesImpl.TIME_SERIES_PRODUCT_TYPE,
                                              refProduct.getSceneRasterWidth(),
                                              refProduct.getSceneRasterHeight());
        tsProduct.setDescription("A time series product");
        ProductUtils.copyGeoCoding(refProduct, tsProduct);


        final TimeSeries timeSeries = new TimeSeriesImpl(tsProduct, productLocations, variableNames);
//        for (TimeVariable timeVariable : variables) {
//            for (Product product : timeSeries.getProducts()) {
//                addSpecifiedBandOfGivenProductToTimeSeriesProduct(timeVariable.getName(), tsProduct, product);
//            }
//        }
        TimeSeriesMapper.getInstance().put(tsProduct, timeSeries);
        return timeSeries;
    }

    static boolean addSpecifiedBandOfGivenProductToTimeSeriesProduct(String nodeName, Product tsProduct,
                                                                     Product product) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(TimeSeriesImpl.DATE_FORMAT);
        if (isProductCompatible(product, tsProduct, nodeName)) {
            final RasterDataNode raster = product.getRasterDataNode(nodeName);
            TimeCoding rasterTimeCoding = raster.getTimeCoding();
            if (rasterTimeCoding == null) {
                return false;
            }

            final ProductData.UTC rasterStartTime = rasterTimeCoding.getStartTime();
            final ProductData.UTC rasterEndTime = rasterTimeCoding.getEndTime();
            final Band band = tsProduct.addBand(nodeName + "_" + dateFormat.format(rasterStartTime.getAsDate()),
                                                raster.getDataType());
            band.setSourceImage(raster.getSourceImage());
            ProductUtils.copyRasterDataNodeProperties(raster, band);
            // todo copy also referenced band in valid pixel expression
            band.setValidPixelExpression(null);
            band.setTimeCoding(new DefaultTimeCoding(rasterStartTime, rasterEndTime,
                                                     raster.getSceneRasterHeight()));
            ProductData.UTC tsStartTime = tsProduct.getStartTime();
            if (tsStartTime == null || rasterStartTime.getAsDate().before(tsStartTime.getAsDate())) {
                tsProduct.setStartTime(rasterStartTime);
            }
            ProductData.UTC tsEndTime = tsProduct.getEndTime();
            if (tsEndTime == null || rasterEndTime.getAsDate().after(tsEndTime.getAsDate())) {
                tsProduct.setEndTime(rasterEndTime);
            }
            return true;
        }
        return false;
    }

    private static boolean isProductCompatible(Product product, Product tsProduct, String rasterName) {
        return product.getFileLocation() != null &&
               product.containsRasterDataNode(rasterName) &&
               tsProduct.isCompatibleProduct(product, 0.1e-6f);
    }

}
