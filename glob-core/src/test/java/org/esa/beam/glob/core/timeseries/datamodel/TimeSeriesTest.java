package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.junit.Test;

import java.util.List;

/**
 * User: Thomas Storm
 * Date: 11.06.2010
 * Time: 11:35:39
 */
public class TimeSeriesTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testAddRasterToTimeSeries() throws Exception {
        final TimeSeries timeSeries = new TimeSeries();
        final List<RasterDataNode> list = timeSeries.getRasterList();
        final Band raster = new Band("b", ProductData.TYPE_INT32, 2, 2);
        list.add(raster);
    }


}
