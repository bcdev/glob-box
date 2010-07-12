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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 10:26:15
 */
class TimeSeriesImpl extends AbstractTimeSeries {

    private Product tsProduct;
    private List<ProductLocation> productLocationList;
    private Map<String, Product> productTimeMap;

    TimeSeriesImpl(Product tsProduct) {
        init(tsProduct);
        productLocationList = getProductLocations();
        handleProductLocations(false);
        setSourceImages();
        fixBandTimeCodings();
        updateAutogrouping();
    }

    private void setSourceImages() {
        for (Band destBand : tsProduct.getBands()) {
            final Band raster = getBand(destBand.getName());
            destBand.setSourceImage(raster.getSourceImage());
        }
    }

    private void fixBandTimeCodings() {
        for (Band destBand : tsProduct.getBands()) {
            final Band raster = getBand(destBand.getName());
            TimeCoding rasterTimeCoding = raster.getTimeCoding();
            final ProductData.UTC startTime = rasterTimeCoding.getStartTime();
            final ProductData.UTC endTime = rasterTimeCoding.getEndTime();
            destBand.setTimeCoding(new DefaultTimeCoding(startTime, endTime, raster.getSceneRasterHeight()));
        }
    }

    TimeSeriesImpl(Product tsProduct, List<ProductLocation> productLocations, List<String> variableNames) {
        init(tsProduct);
        productLocationList = productLocations;
        handleProductLocations(true);
        for (String variable : variableNames) {
            setVariableSelected(variable, true);
        }
    }

    private void init(Product product) {
        this.tsProduct = product;
        productTimeMap = new HashMap<String, Product>();
        createTimeSeriesMetadataStructure(product);
    }

    public Band getBand(String destBandName) {
        final int lastUnderscore = destBandName.lastIndexOf(SEPARATOR);
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
        ProductLocation location = new ProductLocation(type, path);
        if (!productLocationList.contains(location)) {
            addProductLocationMetadata(location);
            List<String> variables = getTimeVariables();
            for (Product product : location.getProducts()) {
                if (product.getTimeCoding() != null && product.getTimeCoding().getStartTime() != null) {
                    addToVariableList(product);
                    productTimeMap.put(formatTimeString(product), product);
                    for (String variable : variables) {
                        if (isVariableSelected(variable)) {
                            addSpecifiedBandOfGivenProduct(variable, product);
                        }
                    }
                } else {
                    // todo log in gui as well as in console
                }
            }
            tsProduct.fireProductNodeChanged(PROPERTY_PRODUCT_LOCATIONS);
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
        updateAutogrouping(); // TODO ???

        List<Product> products = productLocation.getProducts();
        final Band[] bands = tsProduct.getBands();
        for (Product product : products) {
            String timeString = formatTimeString(product);
            productTimeMap.remove(timeString);
            for (Band band : bands) {
                if (band.getName().endsWith(timeString)) {
                    tsProduct.removeBand(band);
                }
            }
        }
        productLocation.closeProducts();
        productLocationList.remove(productLocation);

        tsProduct.fireProductNodeChanged( PROPERTY_PRODUCT_LOCATIONS );
    }

    private void handleProductLocations(boolean addToMetadata) {
        for (ProductLocation productLocation : productLocationList) {
            if (addToMetadata) {
                addProductLocationMetadata(productLocation);
            }
            for (Product product : productLocation.getProducts()) {
                if (product.getTimeCoding() != null && product.getTimeCoding().getStartTime() != null) {
                    productTimeMap.put(formatTimeString(product), product);
                    if (addToMetadata) {
                        addToVariableList(product);
                    }
                } else {
                    // todo log in gui as well as in console
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
            for (ProductLocation productLocation : productLocationList) {
                for (Product product : productLocation.getProducts()) {
                    addSpecifiedBandOfGivenProduct(variableName, product);
                }
            }
        } else {
            final Band[] bands = tsProduct.getBands();
            for (Band band : bands) {
                if (band.getName().startsWith(variableName)) {
                    tsProduct.removeBand(band);
                }
            }
        }
        tsProduct.fireProductNodeChanged( PROPERTY_VARIABLE_SELECTION );
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
    public List<Band> getBandsForVariable(String variableName) {
        final List<Band> bands = new ArrayList<Band>();
        for (Band band : tsProduct.getBands()) {
            if (variableName.equals(rasterToVariableName(band.getName()))) {
                bands.add(band);
            }
        }
        sortBands(bands);
        return bands;
    }

    @Override
    public List<Band> getBandsForProductLocation(ProductLocation location) {
        final List<Band> bands = new ArrayList<Band>();
        List<Product> products = location.getProducts();
        for (Product product : products) {
            String timeString = formatTimeString(product);
            // TODO relies on one timecoding per product... thats not good (mz, ts, 2010-07-12)
            for (Band band : tsProduct.getBands()) {
                if (band.getName().endsWith(timeString)) {
                    bands.add(band);
                }
            }
        }
        return bands;
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

    private static String formatTimeString(Product product) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        TimeCoding timeCoding = product.getTimeCoding();
        final ProductData.UTC startTime = timeCoding.getStartTime();
        return dateFormat.format(startTime.getAsDate());
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

    private void addSpecifiedBandOfGivenProduct(String nodeName, Product product) {
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
                final Band band = new Band(bandName, raster.getDataType(), tsProduct.getSceneRasterWidth(),
                                           tsProduct.getSceneRasterHeight());
                band.setSourceImage(raster.getSourceImage());
                ProductUtils.copyRasterDataNodeProperties(raster, band);
                // todo copy also referenced band in valid pixel expression
                band.setValidPixelExpression(null);
                band.setTimeCoding(new DefaultTimeCoding(rasterStartTime, rasterEndTime,
                                                         raster.getSceneRasterHeight()));
                tsProduct.addBand(band);
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

}
