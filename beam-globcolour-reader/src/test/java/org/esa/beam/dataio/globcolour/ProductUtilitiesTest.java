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
package org.esa.beam.dataio.globcolour;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.util.Calendar;

/**
 * Test methods for class {@link ProductUtilities}.
 *
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 */
public class ProductUtilitiesTest extends TestCase {

    public void testAddBitmaskDefinitionsTest() {
        final Product product = new Product("name", "type", 1, 1);

        ProductUtilities.addBitmaskDefinitions(product, "CHL1_flags");

        for (final Flags flag : Flags.values()) {
            assertNotNull(product.getMaskGroup().get("CHL1_" + flag.name()));
        }
    }

    public void testGetTimeAttributeValue() {
        final Product product = new Product("name", "type", 1, 1);

        // Test product without metadata elements
        ProductData.UTC utc = ProductUtilities.getTimeAttrValue(product, ProductAttributes.START_TIME);
        assertNull(utc);

        // Test product with MPH metadata element
        product.getMetadataRoot().addElement(new MetadataElement(ProductUtilities.GLOBAL_ATTRIBUTES));
        final MetadataElement mph = product.getMetadataRoot().getElement(ProductUtilities.GLOBAL_ATTRIBUTES);

        utc = ProductUtilities.getTimeAttrValue(product, ProductAttributes.START_TIME);
        assertNull(utc);

        // Test product with empty start time attribute value
        ProductData str = ProductData.createInstance("");
        MetadataAttribute startTimeAttr = new MetadataAttribute(ProductAttributes.START_TIME, str, true);
        mph.addAttribute(startTimeAttr);

        utc = ProductUtilities.getTimeAttrValue(product, ProductAttributes.START_TIME);
        assertNull(utc);
        mph.removeAttribute(startTimeAttr);

        // Test product with incorrect start time attribute value
        str = ProductData.createInstance("2007");
        startTimeAttr = new MetadataAttribute(ProductAttributes.START_TIME, str, true);
        mph.addAttribute(startTimeAttr);

        utc = ProductUtilities.getTimeAttrValue(product, ProductAttributes.START_TIME);
        assertNull(utc);
        mph.removeAttribute(startTimeAttr);

        // Test product with correct start time attribute value
        str = ProductData.createInstance("20070228123610");
        startTimeAttr = new MetadataAttribute(ProductAttributes.START_TIME, str, true);
        mph.addAttribute(startTimeAttr);

        utc = ProductUtilities.getTimeAttrValue(product, ProductAttributes.START_TIME);
        assertNotNull(utc);
        mph.removeAttribute(startTimeAttr);

        Calendar calendar = utc.getAsCalendar();
        assertEquals(2007, calendar.get(Calendar.YEAR));
        assertEquals(2 - 1, calendar.get(Calendar.MONTH));
        assertEquals(28, calendar.get(Calendar.DATE));
        assertEquals(12, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(36, calendar.get(Calendar.MINUTE));
        assertEquals(10, calendar.get(Calendar.SECOND));

        // Test product with date-only start time attribute value
        str = ProductData.createInstance("20070228");
        startTimeAttr = new MetadataAttribute(ProductAttributes.START_TIME, str, true);
        mph.addAttribute(startTimeAttr);

        utc = ProductUtilities.getTimeAttrValue(product, ProductAttributes.START_TIME);
        assertNotNull(utc);
        mph.removeAttribute(startTimeAttr);

        calendar = utc.getAsCalendar();
        assertEquals(2007, calendar.get(Calendar.YEAR));
        assertEquals(2 - 1, calendar.get(Calendar.MONTH));
        assertEquals(28, calendar.get(Calendar.DATE));
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, calendar.get(Calendar.MINUTE));
        assertEquals(0, calendar.get(Calendar.SECOND));

        // Test product with new ISO 8601  format string
        str = ProductData.createInstance("20011203T111913Z");
        startTimeAttr = new MetadataAttribute(ProductAttributes.START_TIME, str, true);
        mph.addAttribute(startTimeAttr);

        utc = ProductUtilities.getTimeAttrValue(product, ProductAttributes.START_TIME);
        assertNotNull(utc);
        mph.removeAttribute(startTimeAttr);

        calendar = utc.getAsCalendar();
        assertEquals(2001, calendar.get(Calendar.YEAR));
        assertEquals(12 - 1, calendar.get(Calendar.MONTH));
        assertEquals(3, calendar.get(Calendar.DATE));
        assertEquals(11, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(19, calendar.get(Calendar.MINUTE));
        assertEquals(13, calendar.get(Calendar.SECOND));
    }

}
