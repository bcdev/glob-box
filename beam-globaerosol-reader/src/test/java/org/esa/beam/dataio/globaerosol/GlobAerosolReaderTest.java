package org.esa.beam.dataio.globaerosol;

import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;

import static org.junit.Assert.*;

public class GlobAerosolReaderTest {


    @Test
    public void testEndTimeCalculation() throws ParseException {
        ProductData.UTC startTime = ProductData.UTC.parse("2010-05-26", GlobAerosolReader.UTC_DATE_PATTERN);
        ProductData.UTC endTime = GlobAerosolReader.calcEndTime(startTime, Period.DAILY);
        Calendar actualCalendar = endTime.getAsCalendar();

        Calendar expectCal = ProductData.UTC.parse("2010-05-26 23:59:59", "yyyy-MM-dd HH:mm:ss").getAsCalendar();
        assertEquals(expectCal, actualCalendar);

        startTime = ProductData.UTC.parse("2000-02-26", GlobAerosolReader.UTC_DATE_PATTERN);
        endTime = GlobAerosolReader.calcEndTime(startTime, Period.WEEKLY);
        expectCal = ProductData.UTC.parse("2000-03-03 23:59:59", "yyyy-MM-dd HH:mm:ss").getAsCalendar();
        actualCalendar = endTime.getAsCalendar();
        assertEquals(expectCal, actualCalendar);

        startTime = ProductData.UTC.parse("2000-02-01", GlobAerosolReader.UTC_DATE_PATTERN);
        endTime = GlobAerosolReader.calcEndTime(startTime, Period.MONTHLY);
        expectCal = ProductData.UTC.parse("2000-02-29 23:59:59", "yyyy-MM-dd HH:mm:ss").getAsCalendar();
        actualCalendar = endTime.getAsCalendar();
        assertEquals(expectCal, actualCalendar);

        startTime = ProductData.UTC.parse("2002-02-01", GlobAerosolReader.UTC_DATE_PATTERN);
        endTime = GlobAerosolReader.calcEndTime(startTime, Period.YEAR);
        expectCal = ProductData.UTC.parse("2003-01-31 23:59:59", "yyyy-MM-dd HH:mm:ss").getAsCalendar();
        actualCalendar = endTime.getAsCalendar();
        assertEquals(expectCal, actualCalendar);


    }

    @Test
    public void testStartDateParsing() throws Exception {
        final ProductData.UTC utc = ProductData.UTC.parse("2005-05-05", GlobAerosolReader.UTC_DATE_PATTERN);
        final Calendar asCalendar = utc.getAsCalendar();
        assertEquals(0, asCalendar.get(Calendar.HOUR));
        assertEquals(0, asCalendar.get(Calendar.MINUTE));
        assertEquals(0, asCalendar.get(Calendar.MILLISECOND));
    }
}
