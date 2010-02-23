/*
    $Id: EquirectGridTest.java 1288 2007-11-06 13:53:25Z ralf $

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

import junit.framework.TestCase;

/**
 * Test methods for class {@link EquirectGrid}.
 *
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 */
public class EquirectGridTest extends TestCase {

    private static final int ROW_COUNT = 4320;
    private static final int COL_COUNT = 8640;
    private static final double MIN_LAT = -90.0;
    private static final double MAX_LAT = 90.0;
    private static final double MIN_LON = -180.0;
    private static final double MAX_LON = 180.0;
    private static final double LAT_STEP = 180.0 / 4320;
    private static final double LON_STEP = 360.0 / 8640;

    private static final EquirectGrid GRID = new EquirectGrid(ROW_COUNT, COL_COUNT, MIN_LAT, MIN_LON, LAT_STEP,
                                                              LON_STEP);

    public void testConstructor() {
        try {
            new EquirectGrid(ROW_COUNT, COL_COUNT, MIN_LAT, MIN_LON, 0.0, LON_STEP);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            new EquirectGrid(ROW_COUNT, COL_COUNT, MIN_LAT, MIN_LON, LAT_STEP, 0.0);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            new EquirectGrid(0, COL_COUNT, MIN_LAT, MIN_LON, LAT_STEP, LON_STEP);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            new EquirectGrid(ROW_COUNT, 0, MIN_LAT, MIN_LON, LAT_STEP, LON_STEP);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            new EquirectGrid(ROW_COUNT, COL_COUNT, -100.0, MIN_LON, LAT_STEP, LON_STEP);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            new EquirectGrid(ROW_COUNT, COL_COUNT, 100.0, MIN_LON, LAT_STEP, LON_STEP);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            new EquirectGrid(ROW_COUNT, COL_COUNT, MIN_LAT, -200.0, LAT_STEP, LON_STEP);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            new EquirectGrid(ROW_COUNT, COL_COUNT, MIN_LAT, 200.0, LAT_STEP, LON_STEP);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            new EquirectGrid(ROW_COUNT, COL_COUNT, MIN_LAT, MIN_LON, 2 * LAT_STEP, LON_STEP);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        try {
            new EquirectGrid(ROW_COUNT, COL_COUNT, MIN_LAT, MIN_LON, LAT_STEP, 2 * LON_STEP);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }


    public void testSimpleGetters() {
        assertEquals(ROW_COUNT, GRID.getRowCount());
        assertEquals(COL_COUNT, GRID.getColCount());

        assertEquals(MIN_LAT, GRID.getMinLat(), 0.0);
        assertEquals(MAX_LAT, GRID.getMaxLat(), 0.0);
        assertEquals(MIN_LON, GRID.getMinLon(), 0.0);
        assertEquals(MAX_LON, GRID.getMaxLon(), 0.0);

        assertEquals(LAT_STEP, GRID.getLatStep(), 0.0);
        assertEquals(LON_STEP, GRID.getLonStep(), 0.0);
    }


    public void testGetLat() {
        assertEquals(MIN_LAT + 0.5 * LAT_STEP, GRID.getLat(0), 1.0e-10);
        assertEquals(MAX_LAT - 0.5 * LAT_STEP, GRID.getLat(ROW_COUNT - 1), 1.0e-10);
    }


    public void testGetLon() {
        assertEquals(MIN_LON + 0.5 * LON_STEP, GRID.getLon(0), 1.0e-10);
        assertEquals(MAX_LON - 0.5 * LON_STEP, GRID.getLon(COL_COUNT - 1), 1.0e-10);
    }

}
