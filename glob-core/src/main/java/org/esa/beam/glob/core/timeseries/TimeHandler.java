package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;

import java.io.IOException;
import java.text.ParseException;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 11:22:49
 */
public class TimeHandler {

    public TimeCoding generateTimeCoding(RasterDataNode raster) throws ParseException, IOException {
        final ProductData.UTC startTime = raster.getProduct().getStartTime();
        final ProductData.UTC endTime = raster.getProduct().getEndTime();
        if (endTime == null) {
            return new TimeCoding(raster, startTime);
        } else {
            return new TimeCoding(raster, startTime, endTime, false);
            // in any case: we do not use time per pixel
        }
    }

}
