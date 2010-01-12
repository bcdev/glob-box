/*
 * $Id$
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.glob.reader.medspiration;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;



public class MedspirationReaderTest {
    
    @Test
    public void testFlagCommentParsing() {
        String comment = "b0:1=grid cell is open sea water b1:1=land is present in this grid cell b2:1=lake surface is present in this grid cell b3:1=sea ice is present in this grid cell b4-b7:reserve for future grid mask data";
        List<MetadataAttribute> flagAttributes = MedspirationReader.getFlagAttributes(comment, ";");
        assertNotNull(flagAttributes);
        assertEquals(4, flagAttributes.size());
        assertEquals(1, flagAttributes.get(0).getData().getElemInt());
        assertEquals("grid cell is open sea water", flagAttributes.get(0).getDescription());
        assertEquals(2, flagAttributes.get(1).getData().getElemInt());
        assertEquals("land is present in this grid cell", flagAttributes.get(1).getDescription());
        List<MetadataAttribute> indexAttributes = MedspirationReader.getIndexAttributes(comment);
        assertNotNull(indexAttributes);
        assertEquals(0, indexAttributes.size());
    }
    
    @Test
    public void testFlagCommentParsing2() {
        String comment = "b0 : 1 = SST out of range; b1 : 1 = Cosmetic value; b2 : 1 = IR cloudy; b3 : 1 = MW rain; b4 : 1 = ice; b5 : 1 = spare; b6 : 1 = Land; b7 : 1 = unprocessed;";
        List<MetadataAttribute> flagAttributes = MedspirationReader.getFlagAttributes(comment, ";");
        assertNotNull(flagAttributes);
        assertEquals(8, flagAttributes.size());
        assertEquals(1, flagAttributes.get(0).getData().getElemInt());
        assertEquals("SST out of range", flagAttributes.get(0).getDescription());
        assertEquals(2, flagAttributes.get(1).getData().getElemInt());
        assertEquals("Cosmetic value", flagAttributes.get(1).getDescription());
        assertEquals(128, flagAttributes.get(7).getData().getElemInt());
        assertEquals("unprocessed", flagAttributes.get(7).getDescription());
        List<MetadataAttribute> indexAttributes = MedspirationReader.getIndexAttributes(comment);
        assertNotNull(indexAttributes);
        assertEquals(0, indexAttributes.size());
    }
    
    @Test
    public void testIndexCommentParsing() {
        String comment = "0 No wind speed data available; 1 AMSR-E data; 2 TMI data; 3 NWP:ECMWF; 4 NWP:Met Office; 5 NWP:NCEP; 6 Reference climatology; 9-15 Spare to be defined by RDAC as required";
        List<MetadataAttribute> flagAttributes = MedspirationReader.getFlagAttributes(comment, ";");
        assertNotNull(flagAttributes);
        assertEquals(0, flagAttributes.size());
        List<MetadataAttribute> indexAttributes = MedspirationReader.getIndexAttributes(comment);
        assertNotNull(indexAttributes);
        assertEquals(7, indexAttributes.size());
        assertEquals(0, indexAttributes.get(0).getData().getElemInt());
        assertEquals("No wind speed data available", indexAttributes.get(0).getDescription());
        assertEquals(1, indexAttributes.get(1).getData().getElemInt());
        assertEquals("AMSR-E data", indexAttributes.get(1).getDescription());
    }
}
