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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.Guardian;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i></p>
 * 
 * @author Thomas Storm
 */
public abstract class AbstractTimeSeries {

    /**
     * must NOT contain SEPARATOR
     */
    static final String DATE_FORMAT = "yyyyMMdd.HHmmss.SSS";

    static final String SEPARATOR = "_";

    public static final String TIME_SERIES_PRODUCT_TYPE = "org.esa.beam.glob.timeseries";
    public static final String TIME_SERIES_ROOT_NAME = "TIME_SERIES";
    public static final String PRODUCT_LOCATIONS = "PRODUCT_LOCATIONS";
    public static final String VARIABLE_NAME = "NAME";
    public static final String VARIABLE_SELECTION = "SELECTION";
    public static final String PL_PATH = "PATH";
    public static final String PL_TYPE = "TYPE";
    public static final String VARIABLES = "VARIABLES";

    public static final String PROPERTY_PRODUCT_LOCATIONS = "PROPERTY_PRODUCT_LOCATIONS";
    public static final String PROPERTY_VARIABLE_SELECTION = "PROPERTY_VARIABLE_SELECTION";

    public abstract List<String> getTimeVariables();

    public abstract List<ProductLocation> getProductLocations();

    public abstract void addProductLocation(ProductLocationType type, String path);

    public abstract void removeProductLocation(ProductLocation productLocation);

    public abstract void setVariableSelected(String variableName, boolean selected);

    public abstract boolean isVariableSelected(String variableName);

    public abstract Band getSourceBand(String destBandName);

    public abstract Product getTsProduct();

    public abstract List<Band> getBandsForVariable(String variableName);

    public abstract List<Band> getBandsForProductLocation(ProductLocation location);

    public abstract Map<RasterDataNode, TimeCoding> getRasterTimeMap();

    public static String variableToRasterName(String variableName, TimeCoding timeCoding) {
        final ProductData.UTC rasterStartTime = timeCoding.getStartTime();
        Guardian.assertNotNull("rasterStartTime", rasterStartTime);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(TimeSeriesImpl.DATE_FORMAT, Locale.ENGLISH);
        return variableName + SEPARATOR + dateFormat.format(rasterStartTime.getAsDate());
    }

    public static String rasterToVariableName(String rasterName) {
        final int lastUnderscore = rasterName.lastIndexOf(SEPARATOR);
        return rasterName.substring(0, lastUnderscore);
    }
}
