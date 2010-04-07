package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimedRaster;
import org.esa.beam.util.Debug;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 10:26:15
 */
public class TimeSeriesHandler {

    private final TimeHandler timeHandler;

    public TimeSeriesHandler(TimeHandler timeHandler) {
        this.timeHandler = timeHandler;
    }

    public TimeSeries createTimeSeries(RasterDataNode refRaster, List<Product> products, ProductData.UTC startTime,
                                       ProductData.UTC endTime) throws ParseException, IOException {
        if (refRaster != null) {
            final String rasterName = refRaster.getName();
            List<TimedRaster> rasterList = new ArrayList<TimedRaster>();
            for (Product product : products) {
                final RasterDataNode newRaster = product.getRasterDataNode(rasterName);
                final TimeCoding timeCoding = timeHandler.generateTimeCoding(newRaster);
                final TimedRaster timedRaster = new TimedRaster(newRaster, timeCoding);
                rasterList.add(timedRaster);
            }
            final TimeCoding refTimeCoding = timeHandler.generateTimeCoding(refRaster);
            TimedRaster newRefRaster = new TimedRaster(refRaster, refTimeCoding);
            final TimeSeries timeSeries = new TimeSeries(rasterList, newRefRaster, startTime, endTime);
            timeSeries.applyGeoCoding(refRaster.getGeoCoding());
            return timeSeries;
        } else {
            return null;
        }
    }

    /**
     * Convenience method, delegates to createTimeSeries( TimedRaster, List<Product>, UTC, UTC) using the start and
     * end time of the reference raster's product.
     */
    public TimeSeries createTimeSeries(RasterDataNode refRaster, List<Product> products) throws ParseException,
                                                                                                IOException {
        final Product refProduct = refRaster.getProduct();
        return createTimeSeries(refRaster, products, refProduct.getStartTime(), refProduct.getEndTime());
    }

    public void removeProductFromTimeSeries(final Product product, final TimeSeries timeSeries) {
        final TimedRaster refRaster = timeSeries.getRefRaster();
        final RasterDataNode raster = product.getRasterDataNode(refRaster.getName());
        timeSeries.remove(raster);
    }

    public void addProductToTimeSeries(final Product product, final TimeSeries timeSeries) {
        final TimedRaster refRaster = timeSeries.getRefRaster();
        final RasterDataNode raster = product.getRasterDataNode(refRaster.getName());
        TimeCoding timeCoding = null;
        try {
            timeCoding = timeHandler.generateTimeCoding(raster);
            timeSeries.add(raster, timeCoding);
        } catch (ParseException e) {
            Debug.trace("No raster added. Reason: \n" + e.getMessage());
        } catch (IOException e) {
            Debug.trace("No raster added. Reason: \n" + e.getMessage());
        }
    }

}
