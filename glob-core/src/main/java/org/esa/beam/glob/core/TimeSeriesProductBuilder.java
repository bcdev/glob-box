package org.esa.beam.glob.core;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TimeCoding;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeVariable;
import org.esa.beam.util.ProductUtils;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * User: Marco
 * Date: 19.06.2010
 */
public class TimeSeriesProductBuilder {

    public static final String TIME_SERIES_ROOT_NAME = "TIME_SERIES";
    public static final String TIME_SERIES_PRODUCT_TYPE = "org.esa.beam.glob.timeseries";
    public static final String PRODUCT_LOCATIONS = "PRODUCT_LOCATIONS";
    public static final String VARIABLES = "VARIABLES";

    public static TimeSeries createTimeSeriesFromScratch(String timeSeriesName) {
        final Product tsProduct = new Product(timeSeriesName, TIME_SERIES_PRODUCT_TYPE, -1, -1);
        tsProduct.setDescription("A time series product");
        createTimeSeriesMetadataStructure(tsProduct);
        final TimeSeries timeSeries = new TimeSeries(tsProduct);
        TimeSeriesMapper.getInstance().put(tsProduct, timeSeries);
        return timeSeries;
    }

    public static TimeSeries createTimeSeriesProductFromProductsView(String timeSeriesName, RasterDataNode refRaster,
                                                                     ProductManager productManager) {
        final Product tsProduct = new Product(timeSeriesName, TIME_SERIES_PRODUCT_TYPE,
                                              refRaster.getSceneRasterWidth(),
                                              refRaster.getSceneRasterHeight());
        tsProduct.setDescription("A time series product");
        final Product refProduct = refRaster.getProduct();
        ProductUtils.copyGeoCoding(refProduct, tsProduct);
        // todo replace default time coding

        final Product[] products = productManager.getProducts();
        final String nodeName = refRaster.getName();
        boolean addedAny = false;
        for (Product product : products) {
            addedAny |= addSpecifiedBandOfGivenProductToTimeSeriesProduct(nodeName, tsProduct, product);
        }
        final TimeSeries timeSeries = new TimeSeries(tsProduct);
        for (Product product : products) {
            if (product.getFileLocation() != null) {
                timeSeries.addProduct(product);
            }
        }
        if (addedAny) {
            final List<TimeVariable> list = timeSeries.getTimeVariables();
            for (TimeVariable timeVariable : list) {
                if (timeVariable.getName().equals(nodeName)) {
                    timeVariable.setSelected(true);
                }
            }
        }
        TimeSeriesMapper.getInstance().put(tsProduct, timeSeries);
        return timeSeries;
    }

    public static void createTimeSeriesMetadataStructure(Product tsProduct) {
        if (!tsProduct.getMetadataRoot().containsElement(TIME_SERIES_ROOT_NAME)) {
            final MetadataElement timeSeriesRoot = new MetadataElement(TIME_SERIES_ROOT_NAME);
            final MetadataElement productListElement = new MetadataElement(PRODUCT_LOCATIONS);
            final MetadataElement variablesListElement = new MetadataElement(VARIABLES);
            timeSeriesRoot.addElement(productListElement);
            timeSeriesRoot.addElement(variablesListElement);
            tsProduct.getMetadataRoot().addElement(timeSeriesRoot);
        }
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

    public static boolean isProductCompatible(Product product, Product tsProduct, String rasterName) {
        return product.getFileLocation() != null &&
               product.containsRasterDataNode(rasterName) &&
               tsProduct.isCompatibleProduct(product, 0.1e-6f);
    }
}
