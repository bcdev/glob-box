package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * User: Thomas Storm
 * Date: 29.03.2010
 * Time: 15:57:55
 */
class TimeSeriesModel {

    private ProductData.UTC startTime;

    private ProductData.UTC endTime;

    private CoordinateReferenceSystem crs;

    private List<RasterDataNode> rasterList;

    private List<Product> productList;

    private Map<RasterDataNode, List<Product>> variableMap;

    private boolean showWorldMap;

    private boolean syncColor;

    //raster currently shown in UI
    private RasterDataNode refRaster;

    TimeSeriesModel() {
        showWorldMap = false;
        syncColor = false;
        crs = DefaultGeographicCRS.WGS84;
        rasterList = new ArrayList<RasterDataNode>();
        productList = new ArrayList<Product>();
        variableMap = new WeakHashMap<RasterDataNode, List<Product>>();
        try {
            startTime = ProductData.UTC.parse("01-01-1970", "dd-MM-yyyy");
            endTime = ProductData.UTC.create(new GregorianCalendar().getTime(), 0);
        } catch (ParseException ignore) {
        }
    }

//    TimeSeriesModel(final List<RasterDataNode> rasterList, final RasterDataNode refRaster,
//                      final ProductData.UTC startTime,
//                      final ProductData.UTC endTime) {
//        this.endTime = endTime;
//        this.rasterList = rasterList;
//        this.refRaster = refRaster;
//        this.startTime = startTime;
//    }

    ProductData.UTC getStartTime() {
        return startTime;
    }

    ProductData.UTC getEndTime() {
        return endTime;
    }

    void setStartTime(final ProductData.UTC startTime) {
        this.startTime = startTime;
    }

    void setEndTime(final ProductData.UTC newEndTime) {
        this.endTime = newEndTime;
    }

    void setRefRaster(RasterDataNode refRaster) {
        this.refRaster = refRaster;
    }

    RasterDataNode getRefRaster() {
        return refRaster;
    }

    List<RasterDataNode> getRasterList() {
        return rasterList;
    }

    boolean addRaster(RasterDataNode rasterDataNode) {
        return rasterList.add(rasterDataNode);
    }

    boolean removeRaster(final RasterDataNode raster) {
        return rasterList.remove(raster);
    }

    List<Product> getProductList() {
        return productList;
    }

    boolean addProduct(Product product) {
        return productList.add(product);
    }

    boolean removeProductAt(int index) {
        return productList.remove(index) != null;
    }

    public Map<RasterDataNode, List<Product>> getVariableMap() {
        return variableMap;
    }

    void setCrs(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    CoordinateReferenceSystem getCRS() {
        return crs;
    }

    void setShowWorldMap(boolean showWorldMap) {
        this.showWorldMap = showWorldMap;
    }

    void setSyncColor(boolean syncColor) {
        this.syncColor = syncColor;
    }
}
