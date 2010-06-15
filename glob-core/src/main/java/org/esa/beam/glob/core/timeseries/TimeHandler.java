package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TimeCoding;

import java.io.IOException;
import java.text.ParseException;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 11:22:49
 */
public class TimeHandler {

    public TimeCoding generateTimeCoding(RasterDataNode raster) throws ParseException, IOException {
        // todo might be removed
        return raster.getTimeCoding();
    }

}
