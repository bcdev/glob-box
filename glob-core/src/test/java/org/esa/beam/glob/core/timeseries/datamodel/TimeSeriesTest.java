package org.esa.beam.glob.core.timeseries.datamodel;

/**
 * User: Thomas Storm
 * Date: 11.06.2010
 * Time: 11:17:33
 */
public class TimeSeriesTest {

//    private TimeSeries timeSeries;
//
//    @Before
//    public void setUp() {
//        timeSeries = new TimeSeries(new Product("prod", "schnurz", 10, 2));
//    }
//
//    @Test
//    public void testDefaultTimeSeries() throws Exception {
//        assertEquals(true, timeSeries != null);
//        assertEquals(0, timeSeries.getRasterList().size());
//    }
//
//    @Test
//    public void testAddRasterToTimeSeries() throws Exception {
//        final RasterDataNode raster = createDummyBand();
//        timeSeries.addRaster(raster);
//        assertEquals(1, timeSeries.getRasterList().size());
//        assertEquals(raster, timeSeries.getRefRaster());
//        assertEquals(ProductData.UTC.parse("01-Jan-1970 00:00:00").getAsDate().getTime(),
//                     timeSeries.getStartTime().getAsDate().getTime());
//        assertEquals(ProductData.UTC.parse("31-Dec-2039 23:59:59").getAsDate().getTime(),
//                     timeSeries.getEndTime().getAsDate().getTime());
//
//    }
//
//    @Test
//    public void testIsWithinTimeSpan() throws Exception {
//        timeSeries.setStartTime(ProductData.UTC.parse("01-Jan-2000 00:00:00"));
//        timeSeries.setEndTime(ProductData.UTC.parse("01-Jan-2040 00:00:00"));
//        assertEquals(true, timeSeries.isWithinTimeSpan(ProductData.UTC.parse("01-Jan-2000 00:00:00")));
//        assertEquals(false, timeSeries.isWithinTimeSpan(ProductData.UTC.parse("31-Dec-1999 23:59:59")));
//        assertEquals(false, timeSeries.isWithinTimeSpan(ProductData.UTC.parse("01-Jan-1912 00:00:00")));
//        assertEquals(true, timeSeries.isWithinTimeSpan(ProductData.UTC.parse("01-Jan-2020 00:00:00")));
//        assertEquals(true, timeSeries.isWithinTimeSpan(ProductData.UTC.parse("01-Aug-2021 00:00:00")));
//        assertEquals(true, timeSeries.isWithinTimeSpan(ProductData.UTC.parse("01-Jan-2040 00:00:00")));
//        assertEquals(false, timeSeries.isWithinTimeSpan(ProductData.UTC.parse("01-Jan-2040 00:00:01")));
//        assertEquals(false, timeSeries.isWithinTimeSpan(ProductData.UTC.parse("02-Jan-2040 00:00:00")));
//    }
//
//    @Test
//    public void testIsRasterWithinTimeSpan() throws Exception {
//        timeSeries.setStartTime(ProductData.UTC.parse("01-Jan-2000 00:00:00"));
//        timeSeries.setEndTime(ProductData.UTC.parse("01-Jan-2040 00:00:00"));
//        assertEquals(true, timeSeries.isWithinTimeSpan(createDummyBand()));
//
//        timeSeries.setStartTime(ProductData.UTC.parse("31-Dec-1999 23:59:59"));
//        timeSeries.setEndTime(ProductData.UTC.parse("31-Dec-2039 23:59:59"));
//        assertEquals(true, timeSeries.isWithinTimeSpan(createDummyBand()));
//
//        timeSeries.setStartTime(ProductData.UTC.parse("31-Dec-1999 23:59:59"));
//        timeSeries.setEndTime(ProductData.UTC.parse("31-Dec-2039 23:59:58"));
//        assertEquals(false, timeSeries.isWithinTimeSpan(createDummyBand()));
//    }
//
//    @Test(expected = UnsupportedOperationException.class)
//    public void testFailWhenAddingRasterDirectlyToList() throws Exception {
//        final List<RasterDataNode> list = timeSeries.getRasterList();
//        final Band raster = new Band("b", ProductData.TYPE_INT32, 2, 2);
//        list.add(raster);
//    }
//
//    private RasterDataNode createDummyBand() throws ParseException {
//        product.setStartTime(ProductData.UTC.parse("01-Jan-2000 00:00:00"));
//        product.setEndTime(ProductData.UTC.parse("31-Dec-2039 23:59:59"));
//        return product.addBand("test", ProductData.TYPE_INT8);
//    }
}
