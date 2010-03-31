package org.esa.beam.glob.core;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.datamodel.TimeCoding;
import org.esa.beam.glob.core.datamodel.TimeSeries;
import org.esa.beam.glob.core.datamodel.TimedRaster;

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

    public TimeSeries createTimeSeries(TimedRaster refRaster, List<Product> products, ProductData.UTC startTime,
                                       ProductData.UTC endTime) {
        if (refRaster != null) {
            final String rasterName = refRaster.getName();
            List<TimedRaster> rasterList = new ArrayList<TimedRaster>();
            for (Product product : products) {
                final RasterDataNode newRaster = product.getRasterDataNode(rasterName);
                final TimeCoding timeCoding = timeDataHandler.generateTimeCoding(newRaster, startTime, endTime);
                final TimedRaster timedRaster = new TimedRaster(newRaster, timeCoding);
                rasterList.add(timedRaster);
            }
            final TimeSeries timeSeries = new TimeSeries(rasterList, refRaster, startTime, endTime);
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
    public TimeSeries createTimeSeries(TimedRaster refRaster, List<Product> products) {
        final Product refProduct = refRaster.getRaster().getProduct();
        return createTimeSeries(refRaster, products, refProduct.getStartTime(), refProduct.getEndTime());
    }

}
