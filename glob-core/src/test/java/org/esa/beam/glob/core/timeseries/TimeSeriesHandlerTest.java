package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.util.ArrayList;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 11.06.2010
 * Time: 11:17:33
 */
public class TimeSeriesHandlerTest {

    private TimeSeriesHandler handler;

    @Before
    public void setUp() {
        handler = new TimeSeriesHandler();
    }

    @Test
    public void testDefaultTimeSeries() throws Exception {
        final TimeSeries timeSeries = handler.getTimeSeries();
        assertEquals(true, timeSeries != null);
        assertEquals(0, timeSeries.getRasterList().size());
    }

    @Test
    public void testAddRasterToTimeSeries() throws Exception {
        final ArrayList<Product> products = new ArrayList<Product>();
        final RasterDataNode raster = createDummyBand();
        products.add(raster.getProduct());
        handler.addRasterToTimeSeries(raster);
        final TimeSeries timeSeries = handler.getTimeSeries();
        assertEquals(1, timeSeries.getRasterList().size());
        assertEquals(raster, timeSeries.getRefRaster());
        assertEquals(ProductData.UTC.parse("01-Jan-1970 00:00:00").getAsDate().getTime(),
                     timeSeries.getStartTime().getAsDate().getTime());
        assertEquals(ProductData.UTC.parse("31-Dec-2039 23:59:59").getAsDate().getTime(),
                     timeSeries.getEndTime().getAsDate().getTime());

    }

    @Test
    public void testIsWithinTimeSpan() throws Exception {
        handler.getTimeSeries().setStartTime(ProductData.UTC.parse("01-Jan-2000 00:00:00"));
        handler.getTimeSeries().setEndTime(ProductData.UTC.parse("01-Jan-2040 00:00:00"));
        assertEquals(true, handler.isWithinTimeSpan(ProductData.UTC.parse("01-Jan-2000 00:00:00")));
        assertEquals(false, handler.isWithinTimeSpan(ProductData.UTC.parse("31-Dec-1999 23:59:59")));
        assertEquals(false, handler.isWithinTimeSpan(ProductData.UTC.parse("01-Jan-1912 00:00:00")));
        assertEquals(true, handler.isWithinTimeSpan(ProductData.UTC.parse("01-Jan-2020 00:00:00")));
        assertEquals(true, handler.isWithinTimeSpan(ProductData.UTC.parse("01-Aug-2021 00:00:00")));
        assertEquals(true, handler.isWithinTimeSpan(ProductData.UTC.parse("01-Jan-2040 00:00:00")));
        assertEquals(false, handler.isWithinTimeSpan(ProductData.UTC.parse("01-Jan-2040 00:00:01")));
        assertEquals(false, handler.isWithinTimeSpan(ProductData.UTC.parse("02-Jan-2040 00:00:00")));
    }

    @Test
    public void testIsRasterWithinTimeSpan() throws Exception {
        handler.getTimeSeries().setStartTime(ProductData.UTC.parse("01-Jan-2000 00:00:00"));
        handler.getTimeSeries().setEndTime(ProductData.UTC.parse("01-Jan-2040 00:00:00"));
        assertEquals(true, handler.isWithinTimeSpan(createDummyBand()));

        handler.getTimeSeries().setStartTime(ProductData.UTC.parse("31-Dec-1999 23:59:59"));
        handler.getTimeSeries().setEndTime(ProductData.UTC.parse("31-Dec-2039 23:59:59"));
        assertEquals(true, handler.isWithinTimeSpan(createDummyBand()));

        handler.getTimeSeries().setStartTime(ProductData.UTC.parse("31-Dec-1999 23:59:59"));
        handler.getTimeSeries().setEndTime(ProductData.UTC.parse("31-Dec-2039 23:59:58"));
        assertEquals(false, handler.isWithinTimeSpan(createDummyBand()));
    }

    private RasterDataNode createDummyBand() throws ParseException {
        final Product product = new Product("prod", "schnurz", 10, 2);
        product.setStartTime(ProductData.UTC.parse("01-Jan-2000 00:00:00"));
        product.setEndTime(ProductData.UTC.parse("31-Dec-2039 23:59:59"));
        return product.addBand("test", ProductData.TYPE_INT8);
    }
}
