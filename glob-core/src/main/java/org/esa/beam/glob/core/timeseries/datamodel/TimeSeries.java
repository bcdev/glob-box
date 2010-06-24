package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

import java.util.List;

/**
 * User: Thomas Storm
 * Date: 23.06.2010
 * Time: 18:12:03
 */
public interface TimeSeries {

    public static final String TIME_SERIES_PRODUCT_TYPE = "org.esa.beam.glob.timeseries";

    public List<TimeVariable> getTimeVariables();

    public List<ProductLocation> getProductLocations();

    public void addProductLocation(ProductLocationType type, String path);

    public void removeProductLocation(ProductLocation productLocation);

    public void setVariableSelected(String variableName, boolean selected);

    public Band getBand(String destBandName);

    public Product getTsProduct();

    List<Product> getProducts();
}
