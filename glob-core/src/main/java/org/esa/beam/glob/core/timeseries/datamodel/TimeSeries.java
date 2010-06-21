package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 10:26:15
 */
public class TimeSeries {

    private TimeSeriesModel timeSeriesModel;
    private ArrayList<TimeSeriesListener> listenerList;


    public TimeSeries() {
        timeSeriesModel = new TimeSeriesModel();
        listenerList = new ArrayList<TimeSeriesListener>();

    }

    public static TimeSeries getInstance() {
        return Holder.timeSeries;
    }

    /**
     * Adds a raster to the time series. If necessary, the time bounds of
     * the time series are extended to the time bounds of the raster.
     * <p/>
     * If a raster is added to the time series, the raster's product is added to the product list as well.
     *
     * @param raster the raster to add
     *
     * @return true if raster has been successfully added
     */
    public boolean addRaster(final RasterDataNode raster) {
        if (timeSeriesModel.getRasterList().isEmpty()) {
            timeSeriesModel.setRefRaster(raster);
        }
        final ProductData.UTC startTime = raster.getTimeCoding().getStartTime();
        if (!isWithinTimeSpan(startTime)) {
            timeSeriesModel.setStartTime(startTime);
        }
        final ProductData.UTC endTime = raster.getTimeCoding().getEndTime();
        if (!isWithinTimeSpan(endTime)) {
            timeSeriesModel.setEndTime(endTime);
        }
        final boolean added = timeSeriesModel.addRaster(raster);
        if (added) {
            fireTimeSeriesChanged(TimeSeriesEventType.RASTER_ADDED, -1, timeSeriesModel.getRasterList().size() - 1);
            final Product product = raster.getProduct();
            if (!timeSeriesModel.getProductList().contains(product)) {
                addProduct(product);
            }
        }
        return added;
    }

    boolean removeRaster(final RasterDataNode raster) {
        final int index = timeSeriesModel.getRasterList().indexOf(raster);
        final boolean removed = timeSeriesModel.removeRaster(raster);
        if (removed) {
            fireTimeSeriesChanged(TimeSeriesEventType.RASTER_REMOVED, index, -1);
        }
        return removed;
    }

    public boolean isWithinTimeSpan(ProductData.UTC utc) {
        final long utcSecs = utc.getAsDate().getTime();
        return (utcSecs >= timeSeriesModel.getStartTime().getAsDate().getTime()) &&
               (utcSecs <= timeSeriesModel.getEndTime().getAsDate().getTime());
    }

    public boolean isWithinTimeSpan(RasterDataNode raster) {
        final ProductData.UTC startTime = raster.getProduct().getStartTime();
        final ProductData.UTC endTime = raster.getProduct().getEndTime();
        return isWithinTimeSpan(startTime) && isWithinTimeSpan(endTime);
    }

    public void setStartTime(final ProductData.UTC startTime) {
        ProductData.UTC oldStartTime = timeSeriesModel.getStartTime();
        timeSeriesModel.setStartTime(startTime);
        fireTimeSeriesChanged(TimeSeriesEventType.START_TIME, oldStartTime, startTime);
    }

    public void setEndTime(ProductData.UTC endTime) {
        final ProductData.UTC oldEndTime = timeSeriesModel.getEndTime();
        timeSeriesModel.setEndTime(endTime);
        fireTimeSeriesChanged(TimeSeriesEventType.END_TIME, endTime, oldEndTime);
    }

    public ProductData.UTC getStartTime() {
        return timeSeriesModel.getStartTime();
    }

    public ProductData.UTC getEndTime() {
        return timeSeriesModel.getEndTime();
    }

    public List<RasterDataNode> getRasterList() {
        return Collections.unmodifiableList(timeSeriesModel.getRasterList());
    }

    public List<Product> getProductList() {
        return Collections.unmodifiableList(timeSeriesModel.getProductList());
    }

    void addProduct(Product product) {

        final List<Product> products = timeSeriesModel.getProductList();
        if (!products.contains(product)) {
            products.add(product);
            fireTimeSeriesChanged(TimeSeriesEventType.PRODUCT_ADDED, -1, products.size() - 1);
        }
    }

    public void removeProductsAt(int minIndex, int maxIndex) {
        for (int i = minIndex; i <= maxIndex; i++) {
            final Product product = timeSeriesModel.getProductList().get(i);
            if (timeSeriesModel.removeProductAt(i)) {
                fireTimeSeriesChanged(TimeSeriesEventType.PRODUCT_REMOVED, i, -1);
                updateRasterList(TimeSeriesEventType.PRODUCT_REMOVED);
            }
        }
    }

    private void updateRasterList(TimeSeriesEventType eventType) {
    }

    void setRefRaster(RasterDataNode refRaster) {
        RasterDataNode oldRefRaster = timeSeriesModel.getRefRaster();
        timeSeriesModel.setRefRaster(refRaster);
        fireTimeSeriesChanged(TimeSeriesEventType.REF_RASTER, oldRefRaster, refRaster);
    }

    RasterDataNode getRefRaster() {
        return timeSeriesModel.getRefRaster();
    }

    void setCrs(CoordinateReferenceSystem crs) {
        CoordinateReferenceSystem oldCrs = timeSeriesModel.getCRS();
        if (!crs.equals(oldCrs)) {
            timeSeriesModel.setCrs(crs);
            fireTimeSeriesChanged(TimeSeriesEventType.CRS, oldCrs, crs);
        }
    }

    public CoordinateReferenceSystem getCRS() {
        return timeSeriesModel.getCRS();
    }

    public void addListener(TimeSeriesListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    void removeListener(TimeSeriesListener listener) {
        listenerList.remove(listener);
    }

    private void fireTimeSeriesChanged(TimeSeriesEventType eventType, Object oldValue, Object newValue) {
        final TimeSeriesChangeEvent changeEvent = new TimeSeriesChangeEvent(eventType, oldValue, newValue);
        for (TimeSeriesListener listener : listenerList) {
            listener.timeSeriesChanged(changeEvent);
        }
    }

    private static class Holder {

        private static final TimeSeries timeSeries = new TimeSeries();
    }

}
