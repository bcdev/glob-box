package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimedRaster;
import org.esa.beam.glob.core.timeseries.parser.TimeDataHandler;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 10:26:15
 */
public class TimeSeriesFactory {

    private final TimeDataHandler timeDataHandler;

    public TimeSeriesFactory(TimeDataHandler timeDataHandler) {
        this.timeDataHandler = timeDataHandler;
    }

    public TimeSeries createTimeSeries(RasterDataNode refRaster, List<Product> products, ProductData.UTC startTime,
                                       ProductData.UTC endTime) throws ParseException {
        if (refRaster != null) {
            final String rasterName = refRaster.getName();
            List<TimedRaster> rasterList = new ArrayList<TimedRaster>();
            for (Product product : products) {
                final RasterDataNode newRaster = product.getRasterDataNode(rasterName);
                final TimeCoding timeCoding = timeDataHandler.generateTimeCoding(newRaster);
                final TimedRaster timedRaster = new TimedRaster(newRaster, timeCoding);
                rasterList.add(timedRaster);
            }
            final TimeCoding refTimeCoding = timeDataHandler.generateTimeCoding(refRaster);
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
    public TimeSeries createTimeSeries(RasterDataNode refRaster, List<Product> products) throws ParseException {
        final Product refProduct = refRaster.getProduct();
        return createTimeSeries(refRaster, products, refProduct.getStartTime(), refProduct.getEndTime());
    }

}
