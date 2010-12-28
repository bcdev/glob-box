/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p><i>Note that this class is not yet public API. Interface may chhange in future releases.</i></p>
 *
 * @author Thomas Storm
 */
public class TimeSeriesFactory {

    private TimeSeriesFactory() {
    }

    /**
     * Creates a new TimeSeries from a given time series product. The given product has to be a time series product.
     * This method should only be called by the reader
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
        try {
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
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

}
