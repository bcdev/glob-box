package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TimeCoding;
import org.esa.beam.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 10:26:15
 */
class TimeSeriesImpl implements TimeSeries {

    private static final String TIME_SERIES_ROOT_NAME = "TIME_SERIES";
    private static final String PRODUCT_LOCATIONS = "PRODUCT_LOCATIONS";
    private static final String VARIABLE_NAME = "name";
    private static final String VARIABLE_SELECTION = "selection";
    private static final String PL_PATH = "path";
    private static final String PL_TYPE = "type";

    static final String DATE_FORMAT = "yyyyMMdd.HHmmss.SSS";

    private static final String VARIABLES = "VARIABLES";
    private Product product;
    private List<Product> productList;

    private Map<String, Product> productTimeMap;

    TimeSeriesImpl(Product product) {
        init(product);
        handleProductLocations(getProductLocations(), false);
        updateAutogrouping();
    }

    TimeSeriesImpl(Product product, List<ProductLocation> productLocations, List<TimeVariable> variables) {
        init(product);
        handleProductLocations(productLocations, true);
        for (TimeVariable variable : variables) {
            setVariableSelected(variable.getName(), variable.isSelected());
        }
    }

    private void init(Product product) {
        this.product = product;
        productList = new ArrayList<Product>();
        productTimeMap = new HashMap<String, Product>();
        createTimeSeriesMetadataStructure(product);
    }

    @Override
    public List<Product> getProducts() {
        return Collections.unmodifiableList(productList);
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

    public Product getTsProduct() {
        return product;
    }

    public List<ProductLocation> getProductLocations() {
        return getProductLocations(product);
    }

    private static List<ProductLocation> getProductLocations(Product tsProduct) {
        MetadataElement tsElem = tsProduct.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
        MetadataElement productListElem = tsElem.getElement(PRODUCT_LOCATIONS);
        MetadataElement[] productElems = productListElem.getElements();
        List<ProductLocation> productLocations = new ArrayList<ProductLocation>(productElems.length);
        for (MetadataElement productElem : productElems) {
            String path = productElem.getAttributeString(PL_PATH);
            String type = productElem.getAttributeString(PL_TYPE);
            productLocations.add(new ProductLocation(ProductLocationType.valueOf(type), path));
        }
        return productLocations;
    }

    public List<TimeVariable> getTimeVariables() {
        return getTimeVariables(product);
    }

    private static List<TimeVariable> getTimeVariables(Product tsProduct) {
        MetadataElement tsElem = tsProduct.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
        MetadataElement variablesListElem = tsElem.getElement(VARIABLES);
        MetadataElement[] variableElems = variablesListElem.getElements();
        List<TimeVariable> variables = new ArrayList<TimeVariable>();
        for (MetadataElement varElem : variableElems) {
            String name = varElem.getAttributeString(VARIABLE_NAME);
            String selection = varElem.getAttributeString(VARIABLE_SELECTION);
            variables.add(new TimeVariable(name, Boolean.parseBoolean(selection)));
        }
        return variables;
    }

    public void addProductLocation(ProductLocationType type, String path) {
        final ProductLocation location = new ProductLocation(type, path);
        addProductLocationMetadata(location);
        for (Product product : location.findProducts()) {
            addToVariableList(product);
            storeProductInternally(product);
        }
    }

    public void removeProductLocation(ProductLocation productLocation) {
        // remove metadata
        MetadataElement productLocationsElement = product.getMetadataRoot().
                getElement(TIME_SERIES_ROOT_NAME).
                getElement(PRODUCT_LOCATIONS);
        final MetadataElement[] productLocations = productLocationsElement.getElements();
        MetadataElement removeElem = null;
        for (MetadataElement elem : productLocations) {
            if (elem.getAttributeString(PL_PATH).equals(productLocation.getPath())) {
                removeElem = elem;
                break;
            }
        }
        productLocationsElement.removeElement(removeElem);
        // remove variables for this productLocation
        updateAutogrouping();


        //remove from internal model
        //productList.removeAll( productLocation.findProducts() );   //TODO
        //productTimeMap remove too
    }

    private void handleProductLocations(List<ProductLocation> productLocations, boolean addToMetadata) {
        for (ProductLocation productLocation : productLocations) {
            if (addToMetadata) {
                addProductLocationMetadata(productLocation);
            }
            for (Product aProduct : productLocation.findProducts()) {
                storeProductInternally(aProduct);
                if (addToMetadata) {
                    addToVariableList(aProduct);
                }
            }
        }
    }

    public void setVariableSelected(String variableName, boolean selected) {
        MetadataElement variableListElement = product.getMetadataRoot().
                getElement(TIME_SERIES_ROOT_NAME).
                getElement(VARIABLES);
        final MetadataElement[] variables = variableListElement.getElements();
        for (MetadataElement elem : variables) {
            if (elem.getAttributeString(VARIABLE_NAME).equals(variableName)) {
                elem.setAttributeString(VARIABLE_SELECTION, String.valueOf(selected));
            }
        }
    }

    private void updateAutogrouping() {
        final List<TimeVariable> variables = getTimeVariables();
        String[] autoGroupings = new String[variables.size()];
        for (int i = 0; i < autoGroupings.length; i++) {
            autoGroupings[i] = variables.get(i).getName();
        }
        product.setAutoGrouping(StringUtils.join(autoGroupings, ":"));

    }

    private static void createTimeSeriesMetadataStructure(Product tsProduct) {
        if (!tsProduct.getMetadataRoot().containsElement(TIME_SERIES_ROOT_NAME)) {
            final MetadataElement timeSeriesRoot = new MetadataElement(TIME_SERIES_ROOT_NAME);
            final MetadataElement productListElement = new MetadataElement(PRODUCT_LOCATIONS);
            final MetadataElement variablesListElement = new MetadataElement(VARIABLES);
            timeSeriesRoot.addElement(productListElement);
            timeSeriesRoot.addElement(variablesListElement);
            tsProduct.getMetadataRoot().addElement(timeSeriesRoot);
        }
    }

    private void addProductLocationMetadata(ProductLocation productLocation) {
        MetadataElement productLocationsElement = product.getMetadataRoot().
                getElement(TIME_SERIES_ROOT_NAME).
                getElement(PRODUCT_LOCATIONS);
        ProductData productPath = ProductData.createInstance(productLocation.getPath());
        ProductData productType = ProductData.createInstance(productLocation.getProductLocationType().toString());
        int length = productLocationsElement.getElements().length + 1;
        MetadataElement elem = new MetadataElement(
                PRODUCT_LOCATIONS + "." + Integer.toString(length));
        elem.addAttribute(new MetadataAttribute(PL_PATH, productPath, true));
        elem.addAttribute(new MetadataAttribute(PL_TYPE, productType, true));
        productLocationsElement.addElement(elem);
    }

    private void storeProductInternally(Product product) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        TimeCoding timeCoding = product.getTimeCoding();
        final ProductData.UTC startTime = timeCoding.getStartTime();
        String timeString = dateFormat.format(startTime.getAsDate());

        productList.add(product);
        productTimeMap.put(timeString, product);
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
                newVariables.add(new TimeVariable(bandName, false));
            }
        }
        for (TimeVariable variable : newVariables) {
            addVariableToMetadata(variable);
        }
        if (!newVariables.isEmpty()) {
            updateAutogrouping();
        }
    }

    private void addVariableToMetadata(TimeVariable variable) {
        MetadataElement variableListElement = product.getMetadataRoot().
                getElement(TIME_SERIES_ROOT_NAME).
                getElement(VARIABLES);
        final ProductData variableName = ProductData.createInstance(variable.getName());
        final ProductData isSelected = ProductData.createInstance(Boolean.toString(variable.isSelected()));
        int length = variableListElement.getElements().length + 1;
        MetadataElement elem = new MetadataElement(
                VARIABLES + "." + Integer.toString(length));
        elem.addAttribute(new MetadataAttribute(VARIABLE_NAME, variableName, true));
        elem.addAttribute(new MetadataAttribute(VARIABLE_SELECTION, isSelected, true));
        variableListElement.addElement(elem);
    }

}