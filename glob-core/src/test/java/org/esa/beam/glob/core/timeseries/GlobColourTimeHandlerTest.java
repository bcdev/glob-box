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

package org.esa.beam.glob.core.timeseries;

import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;

public class GlobColourTimeHandlerTest {


    @Test
    public void testTimeCodingGeneration() throws ParseException, IOException {

//        final ProductData.UTC startTime = ProductData.UTC.parse("05_07_1982_14:53:22", "dd_MM_yyyy_hh:mm:ss");
//        final ProductData.UTC endTime = ProductData.UTC.parse("06 08 2065 15 54 23", "dd MM yyyy hh mm ss");
//
//        Product dummyProduct = new Product("testProd", "super product", 10, 20);
//        Band band = new Band("test", ProductData.TYPE_INT16, 10, 20);
//        dummyProduct.addBand(band);
//        dummyProduct.setStartTime(startTime);
//        dummyProduct.setEndTime(endTime);
//
//        TimeHandler timeHandler = new TimeHandler();
//        final TimeCoding timeCoding = timeHandler.generateTimeCoding(band);
//
//        assertNotNull(timeCoding);
//
//        assertEquals(startTime.getMicroSecondsFraction(), timeCoding.getStartTime().getMicroSecondsFraction());
//        assertEquals(endTime.getMicroSecondsFraction(), timeCoding.getEndTime().getMicroSecondsFraction());

//        final ProductData.UTC date = timeCoding.getDatesAtPixel(new PixelPos(5.0f, 10.0f))[0];
//        assertEquals(startTime.getMicroSecondsFraction(), date.getMicroSecondsFraction());
//
//        final ProductData.UTC[] wrongDate = timeCoding.getDatesAtPixel(new PixelPos(10.0f, 10.0f));
//        assertNull(wrongDate);
    }

}
