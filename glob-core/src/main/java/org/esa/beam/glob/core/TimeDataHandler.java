package org.esa.beam.glob.core;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.datamodel.TimeCoding;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 11:22:49
 */
public interface TimeDataHandler {

    public TimeCoding generateTimeCoding(RasterDataNode raster, ProductData.UTC startTime, ProductData.UTC endTime);

}
