package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 29.03.2010
 * Time: 15:57:55
 */
public class TimeSeries {

    private ProductData.UTC startTime;

    private ProductData.UTC endTime;

    private CoordinateReferenceSystem crs;

    private List<RasterDataNode> rasterList;

    private boolean showWorldMap;

    private boolean syncColor;

    //raster currently shown in UI
    private RasterDataNode refRaster;
    private ArrayList<TimeSeriesListener> listenerList;

    public TimeSeries() {
        showWorldMap = false;
        syncColor = false;
        crs = DefaultGeographicCRS.WGS84;
        rasterList = new ArrayList<RasterDataNode>();
        try {
            startTime = ProductData.UTC.parse("01-01-1970", "dd-MM-yyyy");
            endTime = ProductData.UTC.create(new GregorianCalendar().getTime(), 0);
        } catch (ParseException ignore) {
        }
        listenerList = new ArrayList<TimeSeriesListener>();
    }

    public TimeSeries(final List<RasterDataNode> rasterList, final RasterDataNode refRaster,
                      final ProductData.UTC startTime,
                      final ProductData.UTC endTime) {
        this.endTime = endTime;
        this.rasterList = rasterList;
        this.refRaster = refRaster;
        this.startTime = startTime;
    }

    public List<RasterDataNode> getRasterList() {
        return Collections.unmodifiableList(rasterList);
    }

    public ProductData.UTC getEndTime() {
        return endTime;
    }

    public ProductData.UTC getStartTime() {
        return startTime;
    }

    public void setEndTime(final ProductData.UTC newEndTime) {
        final ProductData.UTC oldEndTime = this.endTime;
        this.endTime = newEndTime;
        fireTimeSeriesChanged(TimeSeriesProperty.END_TIME, newEndTime, oldEndTime);
    }

    public void setStartTime(final ProductData.UTC startTime) {
        ProductData.UTC oldStartTime = this.startTime;
        this.startTime = startTime;
        fireTimeSeriesChanged(TimeSeriesProperty.START_TIME, oldStartTime, startTime);
    }

    public RasterDataNode getRefRaster() {
        return refRaster;
    }

    public void setRefRaster(RasterDataNode refRaster) {
        RasterDataNode oldRefRaster = this.refRaster;
        this.refRaster = refRaster;
        fireTimeSeriesChanged(TimeSeriesProperty.REF_RASTER, oldRefRaster, refRaster);
    }

    public boolean removeRaster(final RasterDataNode raster) {
        final int index = rasterList.indexOf(raster);
        final boolean removed = rasterList.remove(raster);
        if (removed) {
            fireTimeSeriesChanged(TimeSeriesProperty.RASTER_REMOVED, index, -1);
        }
        return removed;
    }

    public boolean addRaster(final RasterDataNode timedRaster) {
        if (!rasterList.contains(timedRaster)) {
            final boolean added = rasterList.add(timedRaster);
            fireTimeSeriesChanged(TimeSeriesProperty.RASTER_ADDED, -1, rasterList.size() - 1);
            return added;
        }
        return false;
    }

    public void setCrs(CoordinateReferenceSystem crs) {
        CoordinateReferenceSystem oldCrs = this.crs;
        this.crs = crs;
        fireTimeSeriesChanged(TimeSeriesProperty.CRS, oldCrs, crs);
    }

    public void setShowWorldMap(boolean showWorldMap) {
        this.showWorldMap = showWorldMap;
    }

    public void setSyncColor(boolean syncColor) {
        this.syncColor = syncColor;
    }

    public CoordinateReferenceSystem getCRS() {
        return crs;
    }

    public int getRasterCount() {
        return rasterList.size();
    }

    public RasterDataNode getRasterAt(int index) {
        return rasterList.get(index);
    }

    public void addListener(TimeSeriesListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    public void removeListener(TimeSeriesListener listener) {
        listenerList.remove(listener);
    }

    private void fireTimeSeriesChanged(TimeSeriesProperty property, Object oldValue, Object newValue) {
        final TimeSeriesChangeEvent changeEvent = new TimeSeriesChangeEvent(property, oldValue, newValue);
        for (TimeSeriesListener listener : listenerList) {
            listener.timeSeriesChanged(changeEvent);
        }
    }
}
