package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.DefaultTimeCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TimeCoding;
import org.esa.beam.glob.core.TimeSeriesMapper;
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
public class TimeSeries {

    public static final String TIME_SERIES_PRODUCT_TYPE = "org.esa.beam.glob.timeseries";
    
    private static final String TIME_SERIES_ROOT_NAME = "TIME_SERIES";
    private static final String PRODUCT_LOCATIONS = "PRODUCT_LOCATIONS";
    private static final String VARIABLES = "VARIABLES";

    private Product product;
    private List<Product> productList;
    private Map<String, Product> productTimeMap;

    /**
     * should only be called by the reader
     *
     * @param product
     * @return
     */
    public static TimeSeries create(Product product) {
        final TimeSeries timeSeries = new TimeSeries(product);
        TimeSeriesMapper.getInstance().put(product, timeSeries);
        return timeSeries;
    }

    public static TimeSeries create( String name, List<ProductLocation> productLocations, List<TimeVariable> variables ) {
        Guardian.assertNotNull( "productLocations", productLocations );
        Guardian.assertGreaterThan( "productLocations.size()", productLocations.size(), 0 );
        Guardian.assertNotNull( "variables", variables );
        Guardian.assertGreaterThan( "variables.size()", variables.size(), 0 );
        Guardian.assertNotNullOrEmpty( "name", name );

        // todo get ref product in a smarter way
        final List<Product> productList = new ArrayList<Product>();
        for (ProductLocation productLocation : productLocations) {
            productList.addAll(productLocation.findProducts());
        }
        if( productList.isEmpty() ) {
            return null;
        }
        Product refProduct = productList.get( 0 );
        final Product tsProduct = new Product(name, TIME_SERIES_PRODUCT_TYPE,
                                              refProduct.getSceneRasterWidth(),
                                              refProduct.getSceneRasterHeight());
        tsProduct.setDescription("A time series product");
        ProductUtils.copyGeoCoding(refProduct, tsProduct);


        final TimeSeries timeSeries = new TimeSeries(tsProduct, productLocations, variables);
        for (TimeVariable timeVariable : variables) {
            for (Product product : timeSeries.getProducts()) {
                addSpecifiedBandOfGivenProductToTimeSeriesProduct(timeVariable.getName(), tsProduct, product);
            }
        }
        TimeSeriesMapper.getInstance().put(tsProduct, timeSeries);
        return timeSeries;
    }

    private static boolean addSpecifiedBandOfGivenProductToTimeSeriesProduct(String nodeName, Product tsProduct,
                                                                             Product product) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
        if (isProductCompatible(product, tsProduct, nodeName)) {
            final RasterDataNode raster = product.getRasterDataNode(nodeName);
            TimeCoding rasterTimeCoding = raster.getTimeCoding();
            if (rasterTimeCoding == null) {
                return false;
            }

            final ProductData.UTC rasterStartTime = rasterTimeCoding.getStartTime();
            final ProductData.UTC rasterEndTime = rasterTimeCoding.getEndTime();
            final Band band = tsProduct.addBand(nodeName + "_" + dateFormat.format(rasterStartTime.getAsDate()),
                                                raster.getDataType());
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
            if (tsEndTime == null || rasterEndTime.getAsDate().after(tsEndTime.getAsDate())) {
                tsProduct.setEndTime(rasterEndTime);
            }
            return true;
        }
        return false;
    }


    private TimeSeries(Product product) {
        init(product);
        handleProductLocations(getProductLocations(), false);
        updateAutogrouping();
    }

    private TimeSeries(Product product, List<ProductLocation> productLocations, List<TimeVariable> variables) {
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

    private void handleProductLocations(List<ProductLocation> productLocations, boolean addToMetadata) {
        for (ProductLocation productLocation : productLocations) {
            if (addToMetadata) {
                addProductLocationMetadata( productLocation );
            }
            for (Product aProduct : productLocation.findProducts()) {
                storeProductInternally(aProduct);
                if (addToMetadata) {
                    addToVariableList(aProduct);
                }
            }
        }
    }

    public Product getTsProduct() {
        return product;
    }

    public List<Product> getProducts() {
        return Collections.unmodifiableList(productList);
    }

    private static List<ProductLocation> getProductLocations(Product tsProduct) {
        MetadataElement tsElem = tsProduct.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
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

    private static List<TimeVariable> getTimeVariables(Product tsProduct) {
        MetadataElement tsElem = tsProduct.getMetadataRoot().getElement(TIME_SERIES_ROOT_NAME);
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


    public List<ProductLocation> getProductLocations() {
        return getProductLocations(product);
    }

    public List<TimeVariable> getTimeVariables() {
        return getTimeVariables(product);
    }

    public void addProductLocation(ProductLocationType type, String path) {
        final ProductLocation location = new ProductLocation(type, path);
        addProductLocationMetadata(location);
        for (Product product : location.findProducts()) {
            addToVariableList(product);
            storeProductInternally(product);
        }
    }

    public void removeProductLocation( ProductLocation productLocation ) {
        // remove metadata
        MetadataElement productLocationsElement = product.getMetadataRoot().
                getElement(TIME_SERIES_ROOT_NAME).
                getElement(PRODUCT_LOCATIONS);
        final MetadataElement[] productLocations = productLocationsElement.getElements();
        MetadataElement removeElem = null;
        for (MetadataElement elem : productLocations) {
            if(elem.getAttributeString( "path" ).equals( productLocation.getPath() )) {
                removeElem = elem;
                break;
            }
        }
        productLocationsElement.removeElement( removeElem );
        // remove variables for this productLocation
        updateAutogrouping();


        //remove from internal model
        //productList.removeAll( productLocation.findProducts() );   //TODO
        //productTimeMap remove too
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
        elem.addAttribute(new MetadataAttribute("path", productPath, true));
        elem.addAttribute(new MetadataAttribute("type", productType, true));
        productLocationsElement.addElement(elem);
    }

    private void storeProductInternally(Product product) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
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
                newVariables.add(new TimeVariable(bandName));
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

    public void setVariableSelected( String variableName, boolean selected ) {
        MetadataElement variableListElement = product.getMetadataRoot().
                getElement(TIME_SERIES_ROOT_NAME).
                getElement(VARIABLES);
        final MetadataElement[] variables = variableListElement.getElements();
        for (MetadataElement elem : variables) {
            if(elem.getAttributeString( "name" ).equals( variableName )) {
                elem.setAttributeString( "selection", String.valueOf(selected));
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

    private static boolean isProductCompatible(Product product, Product tsProduct, String rasterName) {
        return product.getFileLocation() != null &&
               product.containsRasterDataNode(rasterName) &&
               tsProduct.isCompatibleProduct(product, 0.1e-6f);
    }

    //getVariables                                -- DONE
    //getProductLocations                         -- DONE
    //addProductLocation                          --> add metadata, maybe add unselected variables, add to internal model -- DONE
    //removeProductLocation                       --> remove metadata, remove from internal model -- DONE | maybe remove variables UNDONE
    //setVariableSelected(variableName, boolean)  --> change metadata -- DONE
    //create (from reader: product)               --> set variables, set productLocations, add productLocations to internal model
    //create (from wizard: name, pls, vars)       --> create metadata, set variables, set productLocations, add productLocations to internal model
}
