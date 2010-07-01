package org.esa.beam.glob.core;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Thomas Storm
 * Date: 22.06.2010
 * Time: 15:30:14
 */
public class TimeSeriesMapper {

    private Map<Product, AbstractTimeSeries> map = new HashMap<Product, AbstractTimeSeries>();

    private TimeSeriesMapper() {
    }

    public static TimeSeriesMapper getInstance() {
        return Holder.instance;
    }

    private static class Holder {

        private static final TimeSeriesMapper instance = new TimeSeriesMapper();
    }

    public void put(Product product, AbstractTimeSeries timeSeries) {
        map.put(product, timeSeries);
    }

    public void remove(Product product) {
        map.remove(product);
    }

    public AbstractTimeSeries getTimeSeries(Product product) {
        return map.get(product);
    }

}

