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

import org.esa.beam.framework.datamodel.Band;
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
import org.esa.beam.visat.VisatApp;

import javax.swing.JInternalFrame;
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
    private Map<RasterDataNode, TimeCoding> rasterTimeMap = new WeakHashMap<RasterDataNode, TimeCoding>();
    private static final String START_TIME_PROPERTY_NAME = "startTime";
    private static final String END_TIME_PROPERTY_NAME = "endTime";

    /**
     * Used to create a TimeSeries from within a ProductReader
     *
     * @param tsProduct the product read
     */
    TimeSeriesImpl(Product tsProduct) {
        init(tsProduct);
        productLocationList = getProductLocations();
        handleProductLocations(false);
        setSourceImages();
        fixBandTimeCodings();
        updateAutoGrouping();
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
        productLocationList = new ArrayList<ProductLocation>(productLocations);
        handleProductLocations(true);
        for (String variable : variableNames) {
            setVariableSelected(variable, true);
        }
        setProductTimeCoding(tsProduct);
    }

    @Override
    public Band getSourceBand(String destBandName) {
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

    @Override
    public void addProductLocation(ProductLocationType type, String path) {
        ProductLocation location = new ProductLocation(type, path);
        if (!productLocationList.contains(location)) {
            addProductLocationMetadata(location);
            List<String> variables = getTimeVariables();
            for (Product product : location.getProducts()) {
                if (product.getStartTime() != null) {
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

        tsProduct.fireProductNodeChanged(PROPERTY_PRODUCT_LOCATIONS);
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
        tsProduct.addProductNodeListener(new ProductNodeListenerAdapter() {
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
            }
        });
    }

    private void handleProductLocations(boolean addToMetadata) {
        for (ProductLocation productLocation : productLocationList) {
            if (addToMetadata) {
                addProductLocationMetadata(productLocation);
            }
            for (Product product : productLocation.getProducts()) {
                if (product.getStartTime() != null) {
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

    @Override
    public void setVariableSelected(String variableName, boolean selected) {
        // set in metadata
        MetadataElement variableListElement = tsProduct.getMetadataRoot().
                getElement(TIME_SERIES_ROOT_NAME).
                getElement(VARIABLES);
        final MetadataElement[] variables = variableListElement.getElements();
        for (MetadataElement elem : variables) {
            if (elem.getAttributeString(VARIABLE_NAME).equals(variableName)) {
                elem.setAttributeString(VARIABLE_SELECTION, String.valueOf(selected));
            }
        }
        // set in product
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
        tsProduct.fireProductNodeChanged(PROPERTY_VARIABLE_SELECTION);
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

    @Override
    public Map<RasterDataNode, TimeCoding> getRasterTimeMap() {
        return Collections.unmodifiableMap(rasterTimeMap);
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
            tsProduct.fireProductNodeChanged(START_TIME_PROPERTY_NAME);
        }
        final ProductData.UTC endTime = timeCoding.getEndTime();
        if (tsProduct.getEndTime().getAsCalendar().compareTo(endTime.getAsCalendar()) != 0) {
            tsProduct.setEndTime(endTime);
            tsProduct.fireProductNodeChanged(END_TIME_PROPERTY_NAME);
        }
        List<String> variables = getTimeVariables();
        for (ProductLocation productLocation : productLocationList) {
            for (Product product : productLocation.getProducts()) {
                for (String variable : variables) {
                    if (isVariableSelected(variable)) {
                        addSpecifiedBandOfGivenProduct(variable, product);
                    }
                }
            }
        }
        final VisatApp visatApp = VisatApp.getApp();
        for (Band band : tsProduct.getBands()) {
            final TimeCoding bandTimeCoding = getRasterTimeMap().get(band);
            if (!timeCoding.contains(bandTimeCoding)) {
                final JInternalFrame view = visatApp.findInternalFrame(band);
                if (view != null) {
                    visatApp.getDesktopPane().closeFrame(view);
                }
                tsProduct.removeBand(band);
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
        tsProduct.setAutoGrouping(StringUtils.join(getTimeVariables(), ":"));
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
        int length = productLocationsElement.getElements().length + 1;
        MetadataElement elem = new MetadataElement(
                PRODUCT_LOCATIONS + "." + Integer.toString(length));
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
            updateAutoGrouping();
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
            TimeCoding rasterTimeCoding = GridTimeCoding.create(product);
            final ProductData.UTC rasterStartTime = rasterTimeCoding.getStartTime();
            final ProductData.UTC rasterEndTime = rasterTimeCoding.getEndTime();
            Guardian.assertNotNull("rasterStartTime", rasterStartTime);
            final String bandName = variableToRasterName(nodeName, rasterTimeCoding);
            if (!tsProduct.containsBand(bandName) && getTimeCoding().contains(rasterTimeCoding)) {
                final Band band = new Band(bandName, raster.getDataType(), tsProduct.getSceneRasterWidth(),
                                           tsProduct.getSceneRasterHeight());
                band.setSourceImage(raster.getSourceImage());
                ProductUtils.copyRasterDataNodeProperties(raster, band);
                // todo copy also referenced band in valid pixel expression
                band.setValidPixelExpression(null);
                rasterTimeMap.put(band, rasterTimeCoding);
                tsProduct.addBand(band);

//                ProductData.UTC tsStartTime = tsProduct.getStartTime();
//                if (tsStartTime == null || rasterStartTime.getAsDate().before(tsStartTime.getAsDate())) {
//                    tsProduct.setStartTime(rasterStartTime);
//                }
//                ProductData.UTC tsEndTime = tsProduct.getEndTime();
//                if (rasterEndTime != null) {
//                    if (tsEndTime == null || rasterEndTime.getAsDate().after(tsEndTime.getAsDate())) {
//                        tsProduct.setEndTime(rasterEndTime);
//                    }
//                }
            }
        }
    }

    private static boolean isProductCompatible(Product product, Product tsProduct, String rasterName) {
        return product.getFileLocation() != null &&
               product.containsRasterDataNode(rasterName) &&
               tsProduct.isCompatibleProduct(product, 0.1e-6f);
    }

}
