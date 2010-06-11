package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimedRaster;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 10:26:15
 */
public class TimeSeriesHandler {

    private TimeSeries timeSeries;

    private List<Product> products;

    private TimeSeriesHandler() {
        timeSeries = new TimeSeries();
        products = new ArrayList<Product>();
    }

    public List<Product> getProducts() {
        return products;
    }

    public TimeSeries getTimeSeries() {
        return timeSeries;
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
    public boolean addRasterToTimeSeries(final RasterDataNode raster) {
        final TimedRaster timedRaster = new TimedRaster(raster, generateTimeCoding(raster));
        if (timeSeries.getRasterList().isEmpty()) {
            timeSeries.setRefRaster(timedRaster);
        }
        final ProductData.UTC startTime = timedRaster.getTimeCoding().getStartTime();
        if (!isWithinTimeSpan(startTime)) {
            timeSeries.setStartTime(startTime);
        }
        final ProductData.UTC endTime = timedRaster.getTimeCoding().getEndTime();
        if (!isWithinTimeSpan(endTime)) {
            timeSeries.setEndTime(endTime);
        }
        final boolean added = timeSeries.addRaster(timedRaster);
        final Product product = timedRaster.getRaster().getProduct();
        if (added && !products.contains(product)) {
            products.add(product);
        }
        return added;
    }

    private TimeCoding generateTimeCoding(RasterDataNode raster) {
        // TODO dummy implementation, move to RasterDataNode
        final Product product = raster.getProduct();
        return new TimeCoding(raster, product.getStartTime(), product.getEndTime(), false);
    }

    public boolean removeRasterFromTimeSeries(final TimedRaster raster) {
        return timeSeries.removeRaster(raster);
    }

    public boolean isWithinTimeSpan(ProductData.UTC utc) {
        final long utcSecs = utc.getAsDate().getTime();
        return (utcSecs >= timeSeries.getStartTime().getAsDate().getTime()) &&
               (utcSecs <= timeSeries.getEndTime().getAsDate().getTime());
    }

    public boolean isWithinTimeSpan(RasterDataNode raster) {
        final ProductData.UTC startTime = raster.getProduct().getStartTime();
        final ProductData.UTC endTime = raster.getProduct().getEndTime();
        return isWithinTimeSpan(startTime) && isWithinTimeSpan(endTime);
    }

    public static TimeSeriesHandler getInstance() {
        return Holder.timeSeriesHandler;
    }

    private static class Holder {

        private static final TimeSeriesHandler timeSeriesHandler = new TimeSeriesHandler();
    }

}
