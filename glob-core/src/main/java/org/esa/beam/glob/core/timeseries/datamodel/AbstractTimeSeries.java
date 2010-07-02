package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TimeCoding;
import org.esa.beam.util.Guardian;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 23.06.2010
 * Time: 18:12:03
 */
public abstract class AbstractTimeSeries {

    /**
     * may NOT contain underscore character '_'
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

    public abstract Band getBand(String destBandName);

    public abstract Product getTsProduct();

    public abstract List<Product> getProducts();

    public abstract List<Band> getBandsForVariable(String variableName);

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

    public static void sortBands(List<RasterDataNode> rasterList) {
        Collections.sort(rasterList, new Comparator<RasterDataNode>() {
            @Override
            public int compare(RasterDataNode raster1, RasterDataNode raster2) {
                final Date raster1Date = raster1.getProduct().getStartTime().getAsDate();
                final Date raster2Date = raster2.getProduct().getStartTime().getAsDate();
                return raster1Date.compareTo(raster2Date) * -1;
            }
        });
    }
}
