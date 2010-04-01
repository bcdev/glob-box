package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;

import java.text.ParseException;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 11:22:49
 */
public interface TimeDataHandler {

    public TimeCoding generateTimeCoding(RasterDataNode raster) throws ParseException;

}
