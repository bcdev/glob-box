package org.esa.beam.glob.core;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Thomas Storm
 * Date: 22.06.2010
 * Time: 15:30:14
 */
public class TimeSeriesMapper {

    private Map<Product, TimeSeries> map = new HashMap<Product, TimeSeries>();

    private TimeSeriesMapper() {
    }

    public static TimeSeriesMapper getInstance() {
        return Holder.instance;
    }

    private static class Holder {

        private static final TimeSeriesMapper instance = new TimeSeriesMapper();
    }

    public void put(Product product, TimeSeries timeSeries) {
        map.put(product, timeSeries);
    }

    public void remove(Product product) {
        map.remove(product);
    }

    public TimeSeries getTimeSeries(Product product) {
        return map.get(product);
    }

}

