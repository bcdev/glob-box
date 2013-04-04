/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
