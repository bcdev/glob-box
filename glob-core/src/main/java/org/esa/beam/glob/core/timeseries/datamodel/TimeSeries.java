package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.TimeCoding;
import org.esa.beam.glob.core.TimeSeriesProductBuilder;
import org.esa.beam.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.esa.beam.glob.core.TimeSeriesProductBuilder.*;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 10:26:15
 */
public class TimeSeries {

    private final Product product;
    private List<Product> productList;
    private List<TimeSeriesListener> listeners = new ArrayList<TimeSeriesListener>();
    private Map<String, Product> productTimeMap;

    public TimeSeries(Product product) {
        this.product = product;
        productList = new ArrayList<Product>();
        productTimeMap = new HashMap<String, Product>();

        TimeSeriesProductBuilder.createTimeSeriesMetadataStructure(product);

        // TODO update on change
        final List<ProductLocation> locationList = getProductLocations();
        for (ProductLocation productLocation : locationList) {
            final ProductLocationType type = productLocation.getProductLocationType();
            final String path = productLocation.getPath();
            final Collection<Product> productCollection = type.findProducts(path);
            for (Product aProduct : productCollection) {
                storeProductInternally(aProduct);
            }
        }

        // TODO update on change
        final List<TimeVariable> variables = getTimeVariables();
        String[] autoGroupings = new String[variables.size()];
        for (int i = 0; i < autoGroupings.length; i++) {
            autoGroupings[i] = variables.get(i).getName();
        }
        product.setAutoGrouping(StringUtils.join(autoGroupings, ":"));

        product.addProductNodeListener(new ProductNodeListenerAdapter() {

            @Override
            public void nodeChanged(ProductNodeEvent event) {
                addToVariableList(event.getSourceNode().getProduct());
                super.nodeChanged(event);
            }

            @Override
            public void nodeDataChanged(ProductNodeEvent event) {
                super.nodeDataChanged(event);
            }

            @Override
            public void nodeAdded(ProductNodeEvent event) {
                super.nodeAdded(event);
            }

            @Override
            public void nodeRemoved(ProductNodeEvent event) {
                super.nodeRemoved(event);
            }
        });
    }

    public Product getTsProduct() {
        return product;
    }

    public List<Product> getProducts() {
        return Collections.unmodifiableList(productList);
    }

    public List<ProductLocation> getProductLocations() {
        MetadataElement tsElem = product.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
        MetadataElement productListElem = tsElem.getElement(PRODUCT_LOCATIONS);
        MetadataElement[] productElems = productListElem.getElements();
        List<ProductLocation> productLocations = new ArrayList<ProductLocation>(productElems.length);
        for (MetadataElement productElem : productElems) {
            String path = productElem.getAttributeString("path");
            String type = productElem.getAttributeString("type");
            productLocations.add(new ProductLocation(ProductLocationType.valueOf(type), path));
        }
        return productLocations;
    }

    public List<TimeVariable> getTimeVariables() {
        MetadataElement tsElem = product.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
        MetadataElement variablesListElem = tsElem.getElement(VARIABLES);
        MetadataElement[] variableElems = variablesListElem.getElements();
        List<TimeVariable> variables = new ArrayList<TimeVariable>();
        for (MetadataElement varElem : variableElems) {
            String name = varElem.getAttributeString("name");
            String selection = varElem.getAttributeString("selection");
            variables.add(new TimeVariable(name, Boolean.parseBoolean(selection)));
        }
        return variables;
    }

    public void addProductLocation(ProductLocationType type, String path) {
        addProductLocationMetadata(new ProductLocation(type, path));
        final Collection<Product> productCollection = type.findProducts(path);
        for (Product product : productCollection) {
            storeProductInternally(product);
        }
        // update variable list ??? TODO ???
    }

    public void addProduct(Product product) {
        addProductLocationMetadata(new ProductLocation(ProductLocationType.FILE, product.getFileLocation().getPath()));
        storeProductInternally(product);
    }

    private void addProductLocationMetadata(ProductLocation productLocation) {
        MetadataElement productListElement = product.getMetadataRoot().
                getElement(TimeSeriesProductBuilder.TIME_SERIES_ROOT_NAME).
                getElement(TimeSeriesProductBuilder.PRODUCT_LOCATIONS);
        ProductData productPath = ProductData.createInstance(productLocation.getPath());
        ProductData productType = ProductData.createInstance(productLocation.getProductLocationType().toString());
        int length = productListElement.getElements().length + 1;
        MetadataElement elem = new MetadataElement(
                TimeSeriesProductBuilder.PRODUCT_LOCATIONS + "." + Integer.toString(length));
        elem.addAttribute(new MetadataAttribute("path", productPath, true));
        elem.addAttribute(new MetadataAttribute("type", productType, true));
        productListElement.addElement(elem);
    }

    private void storeProductInternally(Product product) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
        TimeCoding timeCoding = product.getTimeCoding();
        final ProductData.UTC startTime = timeCoding.getStartTime();
        String timeString = dateFormat.format(startTime.getAsDate());

        productList.add(product);
        productTimeMap.put(timeString, product);
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

    private void addToVariableList(Product product) {
        final ArrayList<TimeVariable> newVariables = new ArrayList<TimeVariable>();
        final List<TimeVariable> timeVariables = getTimeVariables();
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            final String bandName = band.getName();
            boolean varExist = false;
            for (TimeVariable variable : timeVariables) {
                varExist |= variable.fitsToPattern(bandName);
            }
            if (!varExist) {
                newVariables.add(new TimeVariable(bandName));
            }
        }
        for (TimeVariable variable : newVariables) {
            addVariable(variable);
        }
    }

    public void addVariable(TimeVariable variable) {
        MetadataElement variableListElement = product.getMetadataRoot().
                getElement(TimeSeriesProductBuilder.TIME_SERIES_ROOT_NAME).
                getElement(TimeSeriesProductBuilder.VARIABLES);
        final ProductData variableName = ProductData.createInstance(variable.getName());
        final ProductData isSelected = ProductData.createInstance(Boolean.toString(variable.isSelected()));
        int length = variableListElement.getElements().length + 1;
        MetadataElement elem = new MetadataElement(
                TimeSeriesProductBuilder.VARIABLES + "." + Integer.toString(length));
        elem.addAttribute(new MetadataAttribute("name", variableName, true));
        elem.addAttribute(new MetadataAttribute("selection", isSelected, true));
        variableListElement.addElement(elem);
    }

    public Band getBand(String destBandName) {
        final int lastUnderscore = destBandName.lastIndexOf("_");
        String normalizedBandName = destBandName.substring(0, lastUnderscore);
        String timePart = destBandName.substring(lastUnderscore + 1);
        Product srcProduct = productTimeMap.get(timePart);
        if (srcProduct == null) {
            return null;
        }
        for (Band band : srcProduct.getBands()) {
            if (normalizedBandName.equals(band.getName())) {
                return band;
            }
        }
        return null;
    }
}
