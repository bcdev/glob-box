package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.glob.core.TimeSeriesProductBuilder;

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

    public List<Product> getProducts() {
        return Collections.unmodifiableList(productList);
    }

    public List<ProductLocation> getProductLocations() {
        return Collections.unmodifiableList(productLocationList);
    }

    // add entry
    // remove entry

    public List<TimeVariable> getTimeVariables() {
        return Collections.unmodifiableList(timeVariables);
    }

    public Product getProduct() {
        return product;
    }

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
            for (TimeVariable variable : timeVariables) {
                final String bandName = band.getName();
                if (!variable.fitsToPattern(bandName)) {
                    newVariables.add(new TimeVariable(bandName));
                }
            }
        }
        timeVariables.addAll(newVariables);
//        addToMetaDataVariablesList( newVariables );
    }

    private void addToMetaDataVariablesList(List<TimeVariable> newVariables) {
        MetadataElement variableListElement = this.product.getMetadataRoot().
                getElement(TimeSeriesProductBuilder.TIME_SERIES_ROOT_NAME).
                getElement(TimeSeriesProductBuilder.VARIABLES_LIST_NAME);
        for (TimeVariable variable : newVariables) {
            if (!variableListElement.containsAttribute(variable.getName())) {
                final ProductData isSelected = ProductData.createInstance(Boolean.toString(variable.isSelected()));
                variableListElement.addAttribute(new MetadataAttribute(variable.getName(), isSelected, false));
            }
        }
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
