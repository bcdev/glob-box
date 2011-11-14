/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.glob.core.insitu;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.glob.core.insitu.csv.CsvInsituLoader;
import org.esa.beam.glob.core.insitu.csv.InsituRecord;
import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 * @author Sabine Embacher
 */
public class InsituSourceTest {

    private DateFormat dateFormat;
    private CsvInsituLoader insituLoader;

    @Before
    public void setUp() throws Exception {
        dateFormat = ProductData.UTC.createDateFormat("dd.MM.yyyy");
        Reader csvReader = new StringReader("# Test CSV\n"
                                                  + "LAT\tLON\tTIME\tCHL\tys\n"
                                                  + "53.3\t13.4\t08.04.2003\t0.9\t20\n"
                                                  + "53.1\t13.6\t03.04.2003\t0.5\t30\n"
                                                  + "53.1\t13.5\t11.04.2003\t0.4\t40\n");
        insituLoader = new CsvInsituLoader();
        insituLoader.setCsvReader(csvReader);
        insituLoader.setDateFormat(ProductData.UTC.createDateFormat("dd.MM.yyyy"));
    }

    @Test
    public void testGetValuesForCHL_TimeOrdered() throws Exception {
        // execution
        final InsituSource insituSource = new InsituSource(insituLoader);
        InsituRecord[] chlRecords = insituSource.getValuesFor("CHL");

        // verification
        InsituRecord expectedRecord = new InsituRecord(new GeoPos(53.1f, 13.6f), getDate("03.04.2003"), 0.5);
        assertEquals(expectedRecord, chlRecords[0]);

        expectedRecord = new InsituRecord(new GeoPos(53.3f, 13.4f), getDate("08.04.2003"), 0.9);
        assertEquals(expectedRecord, chlRecords[1]);

        expectedRecord = new InsituRecord(new GeoPos(53.1f, 13.5f), getDate("11.04.2003"), 0.4);
        assertEquals(expectedRecord, chlRecords[2]);
    }

    @Test
    public void testGetValuesForYS_TimeOrdered() throws Exception {
        // execution
        final InsituSource insituSource = new InsituSource(insituLoader);
        InsituRecord[] chlRecords = insituSource.getValuesFor("ys");

        // verification
        InsituRecord expectedRecord = new InsituRecord(new GeoPos(53.1f, 13.6f), getDate("03.04.2003"), 30);
        assertEquals(expectedRecord, chlRecords[0]);

        expectedRecord = new InsituRecord(new GeoPos(53.3f, 13.4f), getDate("08.04.2003"), 20);
        assertEquals(expectedRecord, chlRecords[1]);

        expectedRecord = new InsituRecord(new GeoPos(53.1f, 13.5f), getDate("11.04.2003"), 40);
        assertEquals(expectedRecord, chlRecords[2]);
    }

    @Test
    public void testGetParameterNames() throws Exception {
        // execution
        final InsituSource insituSource = new InsituSource(insituLoader);
        final String[] parameterNames = insituSource.getParameterNames();

        // verification
        assertArrayEquals(new String[]{"CHL", "ys"}, parameterNames);
    }

    private Date getDate(String dateString) throws ParseException {
        return dateFormat.parse(dateString);
    }
}
