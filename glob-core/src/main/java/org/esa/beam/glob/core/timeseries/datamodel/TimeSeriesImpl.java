package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.DefaultTimeCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TimeCoding;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
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
    private Product tsProduct;
    private List<Product> productList;

    private Map<String, Product> productTimeMap;

    TimeSeriesImpl(Product tsProduct) {
        init(tsProduct);
        handleProductLocations(getProductLocations(), false);
        fixBandTimeCodings();
        updateAutogrouping();
    }

    private void fixBandTimeCodings() {
        for (Band destBand : tsProduct.getBands()) {
            final Band raster = getBand(destBand.getName());
            TimeCoding rasterTimeCoding = raster.getTimeCoding();
            final ProductData.UTC rasterStartTime = rasterTimeCoding.getStartTime();
            final ProductData.UTC rasterEndTime = rasterTimeCoding.getEndTime();
            destBand.setTimeCoding(new DefaultTimeCoding(rasterStartTime, rasterEndTime,
                                                         raster.getSceneRasterHeight()));

        }
    }

    TimeSeriesImpl(Product tsProduct, List<ProductLocation> productLocations, List<String> variableNames) {
        init(tsProduct);
        handleProductLocations(productLocations, true);
        for (String variable : variableNames) {
            setVariableSelected(variable, true);
        }
    }

    private void init(Product product) {
        this.tsProduct = product;
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
        return tsProduct;
    }

    public List<ProductLocation> getProductLocations() {
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

    public List<String> getTimeVariables() {
        MetadataElement tsElem = tsProduct.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
        MetadataElement variablesListElem = tsElem.getElement(VARIABLES);
        MetadataElement[] variableElems = variablesListElem.getElements();
        List<String> variables = new ArrayList<String>();
        for (MetadataElement varElem : variableElems) {
            variables.add(varElem.getAttributeString(VARIABLE_NAME));
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
        MetadataElement productLocationsElement = tsProduct.getMetadataRoot().
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
        MetadataElement variableListElement = tsProduct.getMetadataRoot().
                getElement(TIME_SERIES_ROOT_NAME).
                getElement(VARIABLES);
        final MetadataElement[] variables = variableListElement.getElements();
        for (MetadataElement elem : variables) {
            if (elem.getAttributeString(VARIABLE_NAME).equals(variableName)) {
                elem.setAttributeString(VARIABLE_SELECTION, String.valueOf(selected));
            }
        }
        if (selected) {
            for (Product product : productList) {
                addSpecifiedBandOfGivenProductToTimeSeriesProduct(variableName,
                                                                  tsProduct,
                                                                  product);
            }
        } else {
            final Band[] bands = tsProduct.getBands();
            for (Band band : bands) {
                if (band.getName().startsWith(variableName)) {
                    tsProduct.removeBand(band);
                }
            }
        }
    }

    @Override
    public boolean isVariableSelected(String variableName) {
        MetadataElement variableListElement = tsProduct.getMetadataRoot().
                getElement(TIME_SERIES_ROOT_NAME).
                getElement(VARIABLES);
        final MetadataElement[] variables = variableListElement.getElements();
        for (MetadataElement elem : variables) {
            if (elem.getAttributeString(VARIABLE_NAME).equals(variableName)) {
                return Boolean.parseBoolean(elem.getAttributeString(VARIABLE_SELECTION));
            }
        }
        return false;
    }

    @Override
    public Band[] getBandsForVariable(String variableName) {
        final ArrayList<Band> bands = new ArrayList<Band>();
        for (Band band : tsProduct.getBands()) {
            if (band.getName().startsWith(variableName + "_")) {
                bands.add(band);
            }
        }

        return bands.toArray(new Band[bands.size()]);
    }

    private void updateAutogrouping() {
        tsProduct.setAutoGrouping(StringUtils.join(getTimeVariables(), ":"));
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
        MetadataElement productLocationsElement = tsProduct.getMetadataRoot().
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
        if (startTime != null) {
            String timeString = dateFormat.format(startTime.getAsDate());
            productList.add(product);
            productTimeMap.put(timeString, product);
        }
    }

    private void addToVariableList(Product product) {
        final ArrayList<String> newVariables = new ArrayList<String>();
        final List<String> timeVariables = getTimeVariables();
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            final String bandName = band.getName();
            boolean varExist = false;
            for (String variable : timeVariables) {
                varExist |= variable.equals(bandName);
            }
            if (!varExist) {
                newVariables.add(bandName);
            }
        }
        for (String variable : newVariables) {
            addVariableToMetadata(variable);
        }
        if (!newVariables.isEmpty()) {
            updateAutogrouping();
        }
    }

    private void addVariableToMetadata(String variable) {
        MetadataElement variableListElement = tsProduct.getMetadataRoot().
                getElement(TIME_SERIES_ROOT_NAME).
                getElement(VARIABLES);
        final ProductData variableName = ProductData.createInstance(variable);
        final ProductData isSelected = ProductData.createInstance(Boolean.toString(false));
        int length = variableListElement.getElements().length + 1;
        MetadataElement elem = new MetadataElement(VARIABLES + "." + Integer.toString(length));
        elem.addAttribute(new MetadataAttribute(VARIABLE_NAME, variableName, true));
        elem.addAttribute(new MetadataAttribute(VARIABLE_SELECTION, isSelected, true));
        variableListElement.addElement(elem);
    }

    private static void addSpecifiedBandOfGivenProductToTimeSeriesProduct(String nodeName, Product tsProduct,
                                                                          Product product) {
        if (isProductCompatible(product, tsProduct, nodeName)) {
            final RasterDataNode raster = product.getRasterDataNode(nodeName);
            TimeCoding rasterTimeCoding = raster.getTimeCoding();
            if (rasterTimeCoding == null) {
                return;
            }
            final ProductData.UTC rasterStartTime = rasterTimeCoding.getStartTime();
            final ProductData.UTC rasterEndTime = rasterTimeCoding.getEndTime();
            Guardian.assertNotNull("rasterStartTime", rasterStartTime);
            final String bandName = variableToRasterName(nodeName, rasterTimeCoding);
            if (!tsProduct.containsBand(bandName)) {
                final Band band = tsProduct.addBand(bandName, raster.getDataType());
                band.setSourceImage(raster.getSourceImage());
                ProductUtils.copyRasterDataNodeProperties(raster, band);
                // todo copy also referenced band in valid pixel expression
                band.setValidPixelExpression(null);
                band.setTimeCoding(new DefaultTimeCoding(rasterStartTime, rasterEndTime,
                                                         raster.getSceneRasterHeight()));
                ProductData.UTC tsStartTime = tsProduct.getStartTime();
                if (tsStartTime == null || rasterStartTime.getAsDate().before(tsStartTime.getAsDate())) {
                    tsProduct.setStartTime(rasterStartTime);
                }
                ProductData.UTC tsEndTime = tsProduct.getEndTime();
                if (rasterEndTime != null) {
                    if (tsEndTime == null || rasterEndTime.getAsDate().after(tsEndTime.getAsDate())) {
                        tsProduct.setEndTime(rasterEndTime);
                    }
                }
            }
        }
    }

    private static boolean isProductCompatible(Product product, Product tsProduct, String rasterName) {
        return product.getFileLocation() != null &&
               product.containsRasterDataNode(rasterName) &&
               tsProduct.isCompatibleProduct(product, 0.1e-6f);
    }

    private static String variableToRasterName(String variable, TimeCoding timeCoding) {
        final ProductData.UTC rasterStartTime = timeCoding.getStartTime();
        final ProductData.UTC rasterEndTime = timeCoding.getEndTime();
        Guardian.assertNotNull("rasterStartTime", rasterStartTime);
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        return variable + "_" + dateFormat.format(rasterStartTime.getAsDate());
    }

    private static String rasterToVariableName(RasterDataNode raster) {

        return "";
    }

}
