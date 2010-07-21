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

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;

import static junit.framework.Assert.*;

public class MedspirationXmlTimeHandlerTest {

    private MedspirationXmlTimeHandler timeHandler;

    @Before
    public void setUp() {
        timeHandler = new MedspirationXmlTimeHandler();
    }

    public void parseTimeFromFileNameTest() throws ParseException {
        final GlobCarbonBaeAsciiHandler handler = new GlobCarbonBaeAsciiHandler();
        ProductData.UTC[] date = handler.parseTimeFromFileName("BAE_PLC_200007_ASCII_COMB.ascii");
        assertNotNull(date);
        final ProductData.UTC testDate = ProductData.UTC.parse("07 2004", "MM yyyy");
        assertEquals(testDate.getMicroSecondsFraction(), date[0].getMicroSecondsFraction());

        ProductData.UTC[] date2 = handler.parseTimeFromFileName("BAE_IGH_200107_ASCII_COMB.zip");
        assertNotNull(date2);
        final ProductData.UTC testDate2 = ProductData.UTC.parse("07 2001", "MM yyyy");
        assertEquals(testDate2.getMicroSecondsFraction(), date2[0].getMicroSecondsFraction());
    }

    public void fileNameParsingTestOfWrongFile() throws ParseException {
        final ProductData.UTC[] dates = timeHandler.parseTimeFromFileName("someProductWhichIsNoBAE-ASCII-Product.zip");
        assertEquals(null, dates);
    }

    @Test
    public void parseTimePerPixelTest() throws IOException, ParseException, URISyntaxException {
        final String path = "Example_XML_formatted_MDB_record.xml";
        final URL resource = MedspirationXmlTimeHandlerTest.class.getResource(path);
        final File file = new File(resource.toURI());
        final Map<PixelPos, ProductData.UTC[]> map = timeHandler.createPixelToDateMap(file);
        assertNotNull(map);
        assertEquals(4, map.values().size());
    }

}
