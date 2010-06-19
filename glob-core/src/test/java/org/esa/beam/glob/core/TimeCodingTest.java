package org.esa.beam.glob.core;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.BeforeClass;
import org.junit.Test;

import java.text.ParseException;

import static junit.framework.Assert.*;

public class TimeCodingTest {

    public static ProductData.UTC UTC_15_06_2010;
    public static ProductData.UTC UTC_10_06_2010;

    @BeforeClass
    public static void setUpClass() {
        try {
            UTC_15_06_2010 = ProductData.UTC.parse("15-06-2010", "dd-MM-yyyy");
            UTC_10_06_2010 = ProductData.UTC.parse("10-06-2010", "dd-MM-yyyy");
        } catch (ParseException ignore) {
        }
    }

    @Test
    public void testTimesInWrongOrder() throws ParseException {
        MyTimeCoding timeCodingSwapped = new MyTimeCoding(UTC_15_06_2010, UTC_10_06_2010);
        assertEquals(UTC_15_06_2010.getAsDate(), timeCodingSwapped.getStartTime().getAsDate());
        assertEquals(UTC_10_06_2010.getAsDate(), timeCodingSwapped.getEndTime().getAsDate());
    }

    @Test
    public void testSetStartTime() throws Exception {
        final TimeCoding timeCoding = new MyTimeCoding(UTC_10_06_2010, UTC_15_06_2010);
        assertEquals(UTC_10_06_2010.getAsDate().getTime(), timeCoding.getStartTime().getAsDate().getTime());
        assertEquals(UTC_15_06_2010.getAsDate().getTime(), timeCoding.getEndTime().getAsDate().getTime());

        final TimeCoding secondTimeCoding = new MyTimeCoding(UTC_10_06_2010, UTC_15_06_2010);
        assertTrue(timeCoding.equals(secondTimeCoding));
    }

    @Test
    public void testStartTimeAfterEndTime() throws Exception {
        final TimeCoding timeCoding = new MyTimeCoding(UTC_10_06_2010, UTC_15_06_2010);
        timeCoding.setStartTime(ProductData.UTC.parse("16-06-2010", "dd-MM-yyyy"));
    }

    @Test
    public void testEndTimeBeforeStartTime() throws Exception {
        final TimeCoding timeCoding = new MyTimeCoding(UTC_10_06_2010, UTC_15_06_2010);
        timeCoding.setEndTime(ProductData.UTC.parse("04-06-2010", "dd-MM-yyyy"));
    }

    private static class MyTimeCoding extends TimeCoding {

        private MyTimeCoding(ProductData.UTC startTime, ProductData.UTC endTime) {
            super(startTime, endTime);
        }

        @Override
        public ProductData.UTC getTime(PixelPos pos) {
            return null;
        }
    }
}
