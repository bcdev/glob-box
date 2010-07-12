package org.esa.beam.glob.core.timeseries.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

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
    public static AbstractTimeSeries create(Product product) {
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
    public static AbstractTimeSeries create(String name, List<ProductLocation> productLocations,
                                    List<String> variableNames) {
        Guardian.assertNotNull("productLocations", productLocations);
        Guardian.assertGreaterThan("productLocations.size()", productLocations.size(), 0);
        Guardian.assertNotNull("variables", variableNames);
        Guardian.assertGreaterThan("variables.size()", variableNames.size(), 0);
        Guardian.assertNotNullOrEmpty("name", name);

        final List<Product> productList = new ArrayList<Product>();
        for (ProductLocation productLocation : productLocations) {
            productList.addAll(productLocation.getProducts());
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


        final AbstractTimeSeries timeSeries = new TimeSeriesImpl(tsProduct, productLocations, variableNames);
        TimeSeriesMapper.getInstance().put(tsProduct, timeSeries);
        return timeSeries;
    }

}
