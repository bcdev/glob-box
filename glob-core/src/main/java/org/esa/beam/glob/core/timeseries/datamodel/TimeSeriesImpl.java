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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i></p>
 *
 * @author Thomas Storm
 */
final class TimeSeriesImpl extends AbstractTimeSeries {

    private Product tsProduct;
    private List<ProductLocation> productLocationList;
    private Map<String, Product> productTimeMap;
    private final Map<RasterDataNode, TimeCoding> rasterTimeMap = new WeakHashMap<RasterDataNode, TimeCoding>();
    private final List<TimeSeriesListener> listeners = new ArrayList<TimeSeriesListener>();
    private volatile boolean isAdjustingImageInfos;

    /**
     * Used to create a TimeSeries from within a ProductReader
     *
     * @param tsProduct the product read
     */
    TimeSeriesImpl(Product tsProduct) {
        init(tsProduct);
        productLocationList = getProductLocations();
        storeProductsInMap();
        setSourceImages();
        fixBandTimeCodings();
        updateAutoGrouping();
        initImageInfos();
    }

    /**
     * Used to create a new TimeSeries from the user interface.
     *
     * @param tsProduct        the newly created time series product
     * @param productLocations the product location to be used
     * @param variableNames    the currently selected names of variables
     */
    TimeSeriesImpl(Product tsProduct, List<ProductLocation> productLocations, List<String> variableNames) {
        init(tsProduct);
        for (ProductLocation location : productLocations) {
            addProductLocation(location);
        }
        storeProductsInMap();
        for (String variable : variableNames) {
            setVariableSelected(variable, true);
        }
        setProductTimeCoding(tsProduct);
        initImageInfos();
    }

    @Override
    public Product getTsProduct() {
        return tsProduct;
    }

    @Override
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

    @Override
    public List<String> getVariables() {
        MetadataElement[] variableElems = getVariableMetadataElements();
        List<String> variables = new ArrayList<String>();
        for (MetadataElement varElem : variableElems) {
            variables.add(varElem.getAttributeString(VARIABLE_NAME));
        }
        return variables;
    }

    @Override
    public void addProductLocation(ProductLocation productLocation) {
        if (productLocationList == null) {
            productLocationList = new ArrayList<ProductLocation>();
        }
        if (!productLocationList.contains(productLocation)) {
            addProductLocationMetadata(productLocation);
            productLocationList.add(productLocation);
            List<String> variables = getVariables();
            for (Product product : productLocation.getProducts()) {
                if (product.getStartTime() != null) {
                    addToVariableList(product);
                    for (String variable : variables) {
                        if (isVariableSelected(variable)) {
                            addSpecifiedBandOfGivenProduct(variable, product);
                        }
                    }
                } else {
                    // todo log in gui as well as in console
                }
            }
            fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.PROPERTY_PRODUCT_LOCATIONS,
                                                      productLocationList));
        }
    }

    @Override
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
        updateAutoGrouping(); // TODO ???

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

        fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.PROPERTY_PRODUCT_LOCATIONS,
                                                  productLocationList));
    }

    private Band getSourceBand(String destBandName) {
        final int lastUnderscore = destBandName.lastIndexOf(SEPARATOR);
        String normalizedBandName = destBandName.substring(0, lastUnderscore);
        String timePart = destBandName.substring(lastUnderscore + TimeSeriesChangeEvent.BAND_TO_BE_REMOVED);
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

    private void setSourceImages() {
        for (Band destBand : tsProduct.getBands()) {
            final Band raster = getSourceBand(destBand.getName());
            if (raster != null) {
                destBand.setSourceImage(raster.getSourceImage());
            }
        }
    }

    private void fixBandTimeCodings() {
        for (Band destBand : tsProduct.getBands()) {
            final Band raster = getSourceBand(destBand.getName());
            rasterTimeMap.put(destBand, GridTimeCoding.create(raster.getProduct()));
        }
    }

    private void init(Product product) {
        this.tsProduct = product;
        productTimeMap = new HashMap<String, Product>();
        createTimeSeriesMetadataStructure(product);

        // to reconstruct the source image which will be nulled when
        // a product is reopened after saving
        tsProduct.addProductNodeListener(new SourceImageReconstructor());
    }

    private void storeProductsInMap() {
        for (Product product : getAllProducts()) {
            productTimeMap.put(formatTimeString(product), product);
        }
    }

    @Override
    public void setVariableSelected(String variableName, boolean selected) {
        // set in metadata
        final MetadataElement[] variables = getVariableMetadataElements();
        for (MetadataElement elem : variables) {
            if (elem.getAttributeString(VARIABLE_NAME).equals(variableName)) {
                elem.setAttributeString(VARIABLE_SELECTION, String.valueOf(selected));
            }
        }
        // set in product
        if (selected) {
            for (Product product : getAllProducts()) {
                addSpecifiedBandOfGivenProduct(variableName, product);
            }
        } else {
            final Band[] bands = tsProduct.getBands();
            for (Band band : bands) {
                if (band.getName().startsWith(variableName)) {
                    tsProduct.removeBand(band);
                }
            }
        }
        fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.PROPERTY_VARIABLE_SELECTION, null));

    }

    @Override
    public boolean isVariableSelected(String variableName) {
        final MetadataElement[] variables = getVariableMetadataElements();
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

    @Override
    public Map<RasterDataNode, TimeCoding> getRasterTimeMap() {
        return Collections.unmodifiableMap(rasterTimeMap);
    }

    @Override
    public boolean isAutoAdjustingTimeCoding() {
        final MetadataElement tsRootElement = tsProduct.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
        if (!tsRootElement.containsAttribute(AUTO_ADJUSTING_TIME_CODING)) {
            setAutoAdjustingTimeCoding(true);
        }
        final String autoAdjustString = tsRootElement.getAttributeString(AUTO_ADJUSTING_TIME_CODING);
        return Boolean.parseBoolean(autoAdjustString);
    }

    @Override
    public void setAutoAdjustingTimeCoding(boolean autoAdjust) {
        final MetadataElement tsRootElement = tsProduct.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
        tsRootElement.setAttributeString(AUTO_ADJUSTING_TIME_CODING, Boolean.toString(autoAdjust));
    }


    @Override
    public boolean isProductCompatible(Product product, String rasterName) {
        return product.getFileLocation() != null &&
               product.containsRasterDataNode(rasterName) &&
               tsProduct.isCompatibleProduct(product, 0.1e-6f);
    }

    @Override
    public TimeCoding getTimeCoding() {
        return GridTimeCoding.create(tsProduct);
    }

    @Override
    public void setTimeCoding(TimeCoding timeCoding) {
        final ProductData.UTC startTime = timeCoding.getStartTime();
        if (tsProduct.getStartTime().getAsCalendar().compareTo(startTime.getAsCalendar()) != 0) {
            tsProduct.setStartTime(startTime);
            fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.START_TIME_PROPERTY_NAME, startTime));
        }
        final ProductData.UTC endTime = timeCoding.getEndTime();
        if (tsProduct.getEndTime().getAsCalendar().compareTo(endTime.getAsCalendar()) != 0) {
            tsProduct.setEndTime(endTime);
            fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.END_TIME_PROPERTY_NAME, endTime));
        }
        List<String> variables = getVariables();
        for (Product product : getAllProducts()) {
            for (String variable : variables) {
                if (isVariableSelected(variable)) {
                    addSpecifiedBandOfGivenProduct(variable, product);
                }
            }
        }
        for (Band band : tsProduct.getBands()) {
            final TimeCoding bandTimeCoding = getRasterTimeMap().get(band);
            if (!timeCoding.contains(bandTimeCoding)) {
                fireChangeEvent(new TimeSeriesChangeEvent(TimeSeriesChangeEvent.BAND_TO_BE_REMOVED, band));
                tsProduct.removeBand(band);
            }
        }
    }


    @Override
    public void addTimeSeriesListener(TimeSeriesListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
            tsProduct.addProductNodeListener(listener);
        }
    }

    @Override
    public void removeTimeSeriesListener(TimeSeriesListener listener) {
        listeners.remove(listener);
        tsProduct.removeProductNodeListener(listener);
    }


    /////////////////////////////////////////////////////////////////////////////////
    // private methods
    /////////////////////////////////////////////////////////////////////////////////

    private MetadataElement[] getVariableMetadataElements() {
        MetadataElement variableListElement = tsProduct.getMetadataRoot().
                getElement(TIME_SERIES_ROOT_NAME).
                getElement(VARIABLES);
        return variableListElement.getElements();
    }

    private List<Product> getAllProducts() {
        List<Product> result = new ArrayList<Product>();
        for (ProductLocation productLocation : productLocationList) {
            for (Product product : productLocation.getProducts()) {
                result.add(product);
            }
        }
        return result;
    }

    private boolean isTimeCodingSet() {
        return tsProduct.getStartTime() != null;
    }

    private void adjustImageInfos(RasterDataNode raster) {
        if (!isAdjustingImageInfos) {
            try {
                isAdjustingImageInfos = true;
                final String variableName = AbstractTimeSeries.rasterToVariableName(raster.getName());
                final List<Band> bandList = getBandsForVariable(variableName);
                final ImageInfo imageInfo = raster.getImageInfo(ProgressMonitor.NULL);
                if (imageInfo != null) {
                    for (Band band : bandList) {
                        if (band != raster) {
                            band.setImageInfo(imageInfo.createDeepCopy());
                        }
                    }
                }
            } finally {
                isAdjustingImageInfos = false;
            }
        }
    }

    private void sortBands(List<Band> bandList) {
        Collections.sort(bandList, new Comparator<Band>() {
            @Override
            public int compare(Band band1, Band band2) {
                final Date date1 = rasterTimeMap.get(band1).getStartTime().getAsDate();
                final Date date2 = rasterTimeMap.get(band2).getStartTime().getAsDate();
                return date1.compareTo(date2);
            }
        });
    }

    private void updateAutoGrouping() {
        tsProduct.setAutoGrouping(StringUtils.join(getVariables(), ":"));
    }

    private void setProductTimeCoding(Product tsProduct) {
        for (Band band : tsProduct.getBands()) {
            final ProductData.UTC rasterStartTime = getRasterTimeMap().get(band).getStartTime();
            final ProductData.UTC rasterEndTime = getRasterTimeMap().get(band).getEndTime();

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
        int length = productLocationsElement.getElements().length + TimeSeriesChangeEvent.BAND_TO_BE_REMOVED;
        MetadataElement elem = new MetadataElement(
                String.format("%s.%s", PRODUCT_LOCATIONS, Integer.toString(length)));
        elem.addAttribute(new MetadataAttribute(PL_PATH, productPath, true));
        elem.addAttribute(new MetadataAttribute(PL_TYPE, productType, true));
        productLocationsElement.addElement(elem);
    }

    private static String formatTimeString(Product product) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH);
        final ProductData.UTC startTime = product.getStartTime();
        return dateFormat.format(startTime.getAsDate());
    }

    private void addToVariableList(Product product) {
        final ArrayList<String> newVariables = new ArrayList<String>();
        final List<String> variables = getVariables();
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            final String bandName = band.getName();
            boolean varExist = false;
            for (String variable : variables) {
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
            updateAutoGrouping();
        }
    }

    private void addVariableToMetadata(String variable) {
        MetadataElement variableListElement = tsProduct.getMetadataRoot().
                getElement(TIME_SERIES_ROOT_NAME).
                getElement(VARIABLES);
        final ProductData variableName = ProductData.createInstance(variable);
        final ProductData isSelected = ProductData.createInstance(Boolean.toString(false));
        int length = variableListElement.getElements().length + TimeSeriesChangeEvent.BAND_TO_BE_REMOVED;
        MetadataElement elem = new MetadataElement(String.format("%s.%s", VARIABLES, Integer.toString(length)));
        elem.addAttribute(new MetadataAttribute(VARIABLE_NAME, variableName, true));
        elem.addAttribute(new MetadataAttribute(VARIABLE_SELECTION, isSelected, true));
        variableListElement.addElement(elem);
    }

    private void addSpecifiedBandOfGivenProduct(String nodeName, Product product) {
        if (isProductCompatible(product, nodeName)) {
            final RasterDataNode raster = product.getRasterDataNode(nodeName);
            TimeCoding rasterTimeCoding = GridTimeCoding.create(product);
            final ProductData.UTC rasterStartTime = rasterTimeCoding.getStartTime();
            final ProductData.UTC rasterEndTime = rasterTimeCoding.getEndTime();
            Guardian.assertNotNull("rasterStartTime", rasterStartTime);
            final String bandName = variableToRasterName(nodeName, rasterTimeCoding);

            if (!tsProduct.containsBand(bandName)) {
                // band not already contained
                if (isAutoAdjustingTimeCoding() || !isTimeCodingSet()) {
                    // automatically setting time coding
                    // OR
                    // first band to add to time series; time bounds of this band will be used
                    // as ts-product's time bounds, no matter if auto adjust is true or false
                    autoAdjustTimeInformation(rasterStartTime, rasterEndTime);
                }
                if (getTimeCoding().contains(rasterTimeCoding)) {
                    // add only bands which are in the time bounds
                    final Band addedBand = addBand(raster, rasterTimeCoding, bandName);
                    final List<Band> bandsForVariable = getBandsForVariable(nodeName);
                    if (!bandsForVariable.isEmpty()) {
                        final ImageInfo imageInfo = bandsForVariable.get(0).getImageInfo(ProgressMonitor.NULL);
                        addedBand.setImageInfo(imageInfo.createDeepCopy());
                    }

                }
                // todo no bands added message
            }
        }
    }

    private Band addBand(RasterDataNode raster, TimeCoding rasterTimeCoding, String bandName) {
        final Band band = new Band(bandName, raster.getDataType(), tsProduct.getSceneRasterWidth(),
                                   tsProduct.getSceneRasterHeight());
        band.setSourceImage(raster.getSourceImage());
        ProductUtils.copyRasterDataNodeProperties(raster, band);
//                todo copy also referenced band in valid pixel expression
        band.setValidPixelExpression(null);
        rasterTimeMap.put(band, rasterTimeCoding);
        tsProduct.addBand(band);
        return band;
    }

    private void autoAdjustTimeInformation(ProductData.UTC rasterStartTime, ProductData.UTC rasterEndTime) {
        ProductData.UTC tsStartTime = tsProduct.getStartTime();
        if (tsStartTime == null || rasterStartTime.getAsDate().before(tsStartTime.getAsDate())) {
            tsProduct.setStartTime(rasterStartTime);
        }
        ProductData.UTC tsEndTime = tsProduct.getEndTime();
        if (tsEndTime == null || rasterEndTime.getAsDate().after(tsEndTime.getAsDate())) {
            tsProduct.setEndTime(rasterEndTime);
        }
    }

    private void initImageInfos() {
        for (String variable : getVariables()) {
            if (isVariableSelected(variable)) {
                final List<Band> bandList = getBandsForVariable(variable);
                adjustImageInfos(bandList.get(0));
            }
        }
    }

    private void fireChangeEvent(TimeSeriesChangeEvent event) {
        final TimeSeriesListener[] timeSeriesListeners = listeners.toArray(new TimeSeriesListener[listeners.size()]);
        for (TimeSeriesListener listener : timeSeriesListeners) {
            listener.timeSeriesChanged(event);
        }
    }

    private class SourceImageReconstructor extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if ("sourceImage".equals(event.getPropertyName()) &&
                event.getOldValue() != null &&
                event.getNewValue() == null) {
                ProductNode productNode = event.getSourceNode();
                if (productNode instanceof Band) {
                    Band destBand = (Band) productNode;
                    final Band sourceBand = getSourceBand(destBand.getName());
                    if (sourceBand != null) {
                        destBand.setSourceImage(sourceBand.getSourceImage());
                    }
                }
            }
            if (RasterDataNode.PROPERTY_NAME_IMAGE_INFO.equals(event.getPropertyName())) {
                if (event.getSourceNode() instanceof RasterDataNode) {
                    adjustImageInfos((RasterDataNode) event.getSourceNode());
                }
            }
        }
    }
}
