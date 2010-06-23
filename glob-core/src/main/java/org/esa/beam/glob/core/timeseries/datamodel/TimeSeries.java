package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 10:26:15
 */
public class TimeSeries {

    private final Product product;
    private List<ProductLocation> productLocationList;
    private List<TimeVariable> timeVariables;
    private List<Product> productList;
    private List<TimeSeriesListener> listeners = new ArrayList<TimeSeriesListener>();

    // from reader

    public TimeSeries(Product product) {
        this.product = product;
        productLocationList = new ArrayList<ProductLocation>();
        timeVariables = new ArrayList<TimeVariable>();
        productList = new ArrayList<Product>();
        //initFromMetadata();
    }
    // from NEW
    //-> name, product[] (can be empty)
    // product created from TimeSeriesProductBuilder
    // call constructor above

    // from CLONE ??

    // ??? needed ???

    public Product getTsProduct() {
        return product;
    }

    public List<Product> getProducts() {
        return Collections.unmodifiableList(productList);
    }

    public List<ProductLocation> getProductLocations() {
        return Collections.unmodifiableList(productLocationList);
    }

    public List<TimeVariable> getTimeVariables() {
        return Collections.unmodifiableList(timeVariables);
    }

    // add entry

    // remove entry

    public void addProductLocation(ProductLocationType type, String path) {
        productLocationList.add(new ProductLocation(type, path));
        productList.addAll(type.findProducts(path));
        // update variable list ??? TODO ???
    }

    public void addProduct(Product product) {
        productLocationList.add(new ProductLocation(ProductLocationType.FILE, product.getFileLocation().getPath()));
        productList.add(product);
        addToVariableList(product);
        fireTimeSeriesChanged(TimeSeriesEventType.PRODUCT_ADDED, null, product);
    }

    private void addToVariableList(Product product) {
        final Band[] bands = product.getBands();
        final ArrayList<TimeVariable> newVariables = new ArrayList<TimeVariable>();
        for (Band band : bands) {
            final String bandName = band.getName();
//            if( timeVariables.size() == 0 ) {
//                newVariables.add(new TimeVariable(bandName));
//            } else {
            boolean varExist = false;
            for (TimeVariable variable : timeVariables) {
                varExist |= variable.fitsToPattern(bandName);
            }
            if (!varExist) {
                newVariables.add(new TimeVariable(bandName));
            }
//            }
        }
        timeVariables.addAll(newVariables);
    }

    private List<TimeVariable> getSelectedVariables() {
        final ArrayList<TimeVariable> list = new ArrayList<TimeVariable>();
        for (TimeVariable variable : timeVariables) {
            if (variable.isSelected()) {
                list.add(variable);
            }
        }
        return list;
    }

    private void fireTimeSeriesChanged(TimeSeriesEventType eventType, Object oldValue, Object newValue) {
        final TimeSeriesChangeEvent event = new TimeSeriesChangeEvent(eventType, oldValue, newValue);
        for (TimeSeriesListener listener : listeners) {
            listener.timeSeriesChanged(event);
        }
    }

    public void addListener(TimeSeriesListener timeSeriesListener) {
        this.listeners.add(timeSeriesListener);
    }

    public void removeProduct(Product product) {
        productList.remove(product);
//        todo implement
//        productLocationList.remove();
        fireTimeSeriesChanged(TimeSeriesEventType.PRODUCT_REMOVED, product, null);
    }
}
