/*
    $Id: MappedProductReaderTest.java 1288 2007-11-06 13:53:25Z ralf $

    Copyright (c) 2006 Brockmann Consult. All rights reserved. Use is
    subject to license terms.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the Lesser GNU General Public License as
    published by the Free Software Foundation; either version 2 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the BEAM software; if not, download BEAM from
    http://www.brockmann-consult.de/beam/ and install it.
*/
package org.esa.beam.dataio.globcolour;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Calendar;

/**
 * Test methods for class {@link MappedProductReader}.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 */
public final class MappedProductReaderTest extends TestCase {

    Product product;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        product = getResourceAsProduct("mapped.nc");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        product.dispose();
    }

    public void testSetQuicklookBandName() {
        assertNotNull(product);
        assertFalse(ProductUtilities.setQuicklookBandName(product));

        assertNotNull(product.getQuicklookBandName());
        Assert.assertEquals("CHL1_value", product.getQuicklookBandName());
    }

    public void testDiagnosticSitePin() {
        assertNotNull(product);
        assertFalse(ProductUtilities.addDiagnosticSitePin(product));

        final Pin pin = product.getPinGroup().get("SITE_1");
        assertNotNull(pin);

        assertEquals("SITE_1", pin.getName());
        assertEquals("Site Name", pin.getLabel());

        final GeoPos geoPos = pin.getGeoPos();
        assertNotNull(geoPos);
        assertEquals(0.0, geoPos.getLat(), 0.0);
        assertEquals(0.0, geoPos.getLon(), 0.0);
    }

    public void testFlagCodings() {
        assertNotNull(product);
        assertFalse(ProductUtilities.setFlagCodingsAndBitmaskDefs(product));

        final Band band = product.getBand("CHL1_flags");
        assertNotNull(band);

        final FlagCoding flagCoding = band.getFlagCoding();
        assertNotNull(flagCoding);
        assertEquals("GLOBCOLOUR", flagCoding.getName());

        for (Flags flag : Flags.values()) {
            flagCoding.getFlag(flag.name());
        }
    }

    public void testStartTime() {
        assertNotNull(product);

        assertFalse(ProductUtilities.setStartTime(product));
        assertNotNull(product.getStartTime());

        final Calendar startTime = product.getStartTime().getAsCalendar();
        assertEquals(2006, startTime.get(Calendar.YEAR));
        assertEquals(10, startTime.get(Calendar.MONTH));
        assertEquals(24, startTime.get(Calendar.DATE));
        assertEquals(16, startTime.get(Calendar.HOUR_OF_DAY));
        assertEquals(34, startTime.get(Calendar.MINUTE));
        assertEquals(11, startTime.get(Calendar.SECOND));
    }

    public void testEndTime() {
        assertNotNull(product);

        assertFalse(ProductUtilities.setEndTime(product));
        assertNotNull(product.getEndTime());

        final Calendar calendar = product.getEndTime().getAsCalendar();
        assertEquals(2006, calendar.get(Calendar.YEAR));
        assertEquals(10, calendar.get(Calendar.MONTH));
        assertEquals(24, calendar.get(Calendar.DATE));
        assertEquals(16, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(34, calendar.get(Calendar.MINUTE));
        assertEquals(14, calendar.get(Calendar.SECOND));
    }

    private static Product getResourceAsProduct(final String name) throws IOException {
        final URL url = MappedProductReaderTest.class.getResource(name);
        assertNotNull(url);

        final String path = URLDecoder.decode(url.getPath(), "UTF-8");
        assertTrue(path.endsWith(name));

        final File file = new File(path);
        assertEquals(file.getName(), name);
        assertTrue(file.exists());
        assertTrue(file.canRead());

        final ProductReader reader = new MappedProductReaderPlugIn().createReaderInstance();

        return reader.readProductNodes(path, null);
    }

}
