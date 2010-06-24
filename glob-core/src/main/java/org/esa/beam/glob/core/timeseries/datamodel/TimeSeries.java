package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TimeCoding;
import org.esa.beam.util.Guardian;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 23.06.2010
 * Time: 18:12:03
 */
public abstract class TimeSeries {

    /**
     * may NOT contain underscore character '_'
     */
    static final String DATE_FORMAT = "yyyyMMdd.HHmmss.SSS";

    static final String SEPARATOR = "_";

    public static final String TIME_SERIES_PRODUCT_TYPE = "org.esa.beam.glob.timeseries";

    public abstract List<String> getTimeVariables();

    public abstract List<ProductLocation> getProductLocations();

    public abstract void addProductLocation(ProductLocationType type, String path);

    public abstract void removeProductLocation(ProductLocation productLocation);

    public abstract void setVariableSelected(String variableName, boolean selected);

    public abstract boolean isVariableSelected(String variableName);

    public abstract Band getBand(String destBandName);

    public abstract Product getTsProduct();

    public abstract List<Product> getProducts();

    public abstract Band[] getBandsForVariable(String variableName);


    public static String variableToRasterName(String variableName, TimeCoding timeCoding) {
        final ProductData.UTC rasterStartTime = timeCoding.getStartTime();
        Guardian.assertNotNull("rasterStartTime", rasterStartTime);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(TimeSeriesImpl.DATE_FORMAT);
        return variableName + SEPARATOR + dateFormat.format(rasterStartTime.getAsDate());
    }

    public static String rasterToVariableName(String rasterName) {
        final int lastUnderscore = rasterName.lastIndexOf(SEPARATOR);
        return rasterName.substring(0, lastUnderscore);
    }
}
