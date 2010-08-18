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

package org.esa.beam.dataio.globcarbon;

import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.text.ParseException;

import static org.esa.beam.dataio.globcarbon.GlobCarbonProductReader.*;
import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class GlobCarbonProductReaderTest {

    private static final String PATTERN = "dd-MMM_yyyy-HH:mm:ss";

    @Test
    public void testParseTimeInformation() throws Exception {

        ProductData.UTC[] timeInfos = parseTimeInformation("FAPAR_PLC_10KM_VGT_19980707_AAD.hdr");
        assertEquals(parseTime("07-JUL_1998-00:00:00"), timeInfos[0].getAsDate().getTime());
        assertEquals(parseTime("07-JUL_1998-23:59:59"), timeInfos[1].getAsDate().getTime());

        timeInfos = parseTimeInformation("FAPAR_PLC_10KM_VGT_19980707_AAD");
        assertEquals(parseTime("07-JUL_1998-00:00:00"), timeInfos[0].getAsDate().getTime());
        assertEquals(parseTime("07-JUL_1998-23:59:59"), timeInfos[1].getAsDate().getTime());

        timeInfos = parseTimeInformation("_19980707_AAD.hdr");
        assertEquals(parseTime("07-JUL_1998-00:00:00"), timeInfos[0].getAsDate().getTime());
        assertEquals(parseTime("07-JUL_1998-23:59:59"), timeInfos[1].getAsDate().getTime());

        timeInfos = parseTimeInformation("FAPAR_PLC_10KM_VGT_199806_AAD.hdr");
        assertEquals(parseTime("01-JUN_1998-00:00:00"), timeInfos[0].getAsDate().getTime());
        assertEquals(parseTime("30-JUN_1998-23:59:59"), timeInfos[1].getAsDate().getTime());

        timeInfos = parseTimeInformation("FAPAR_PLC_10KM_VGT_199802_AAD");
        assertEquals(parseTime("01-FEB_1998-00:00:00"), timeInfos[0].getAsDate().getTime());
        assertEquals(parseTime("28-FEB_1998-23:59:59"), timeInfos[1].getAsDate().getTime());

        timeInfos = parseTimeInformation("abcdefg_200002_AAD.hdr");
        assertEquals(parseTime("01-FEB_2000-00:00:00"), timeInfos[0].getAsDate().getTime());
        assertEquals(parseTime("29-FEB_2000-23:59:59"), timeInfos[1].getAsDate().getTime());

        timeInfos = parseTimeInformation("UNFUG_1998_AAD.hdr");
        assertEquals(parseTime("01-JAN_1998-00:00:00"), timeInfos[0].getAsDate().getTime());
        assertEquals(parseTime("31-DEC_1998-23:59:59"), timeInfos[1].getAsDate().getTime());

        timeInfos = parseTimeInformation("FAPAR_PLC_10KM_VGT_1999_AAD");
        assertEquals(parseTime("01-JAN_1999-00:00:00"), timeInfos[0].getAsDate().getTime());
        assertEquals(parseTime("31-DEC_1999-23:59:59"), timeInfos[1].getAsDate().getTime());

        timeInfos = parseTimeInformation("_2000_HURZGRMPF.hdr");
        assertEquals(parseTime("01-JAN_2000-00:00:00"), timeInfos[0].getAsDate().getTime());
        assertEquals(parseTime("31-DEC_2000-23:59:59"), timeInfos[1].getAsDate().getTime());

    }

    private long parseTime(final String timeString) throws ParseException {
        return ProductData.UTC.parse(timeString, PATTERN).getAsDate().getTime();
    }

}
