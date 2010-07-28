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

package org.esa.beam.glob.core;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;

import java.util.Map;
import java.util.WeakHashMap;

public class TimeSeriesMapper {

    private Map<Product, AbstractTimeSeries> map = new WeakHashMap<Product, AbstractTimeSeries>();

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
        product.dispose();
    }

    public AbstractTimeSeries getTimeSeries(Product product) {
        return map.get(product);
    }

}

