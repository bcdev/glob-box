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

package org.esa.beam.dataio.globcover;

import org.esa.beam.framework.datamodel.GeoPos;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class GCTileFileTest {

    @Test
    public void testCreateGeoPos() {
        GeoPos geoPos;
        geoPos = GCTileFile.createGeoPos("012034059.567", "50012019.00000");
        assertEquals(12.583213, geoPos.getLon(), 1.0e-6);
        assertEquals(50.205280, geoPos.getLat(), 1.0e-6);
        geoPos = GCTileFile.createGeoPos("1005000.0", "0059059.99900");
        assertEquals(1.083333, geoPos.getLon(), 1.0e-6);
        assertEquals(0.999999, geoPos.getLat(), 1.0e-6);
        geoPos = GCTileFile.createGeoPos("5.00000", "2007.00000");
        assertEquals(0.001389, geoPos.getLon(), 1.0e-6);
        assertEquals(0.035278, geoPos.getLat(), 1.0e-6);

        geoPos = GCTileFile.createGeoPos("012034059.567", "-50012019.00000");
        assertEquals(12.583213, geoPos.getLon(), 1.0e-6);
        assertEquals(-50.205280, geoPos.getLat(), 1.0e-6);
    }

}
