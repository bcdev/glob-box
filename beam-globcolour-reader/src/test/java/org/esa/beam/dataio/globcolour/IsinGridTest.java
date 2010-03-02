/*
    $Id: IsinGridTest.java 1288 2007-11-06 13:53:25Z ralf $

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
 * Test methods for class {@link IsinGrid}.
 *
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 */
public class IsinGridTest extends TestCase {

    private static final int ROW_COUNT = 6;
    private static final int BIN_COUNT = 46;
    private static final int[] COL_COUNTS = {3, 8, 12, 12, 8, 3};

    private static final double LAT_STEP = 180.0 / ROW_COUNT;
    private static final IsinGrid GRID = new IsinGrid(ROW_COUNT);


    public void testConstructor() {
        try {
            new IsinGrid(0);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testRowCount() {
        assertEquals(ROW_COUNT, GRID.getRowCount());
    }

    public void testBinCount() {
        assertEquals(BIN_COUNT, GRID.getBinCount());
    }

    public void testColCount() {
        for (int i = 0; i < COL_COUNTS.length; ++i) {
            assertEquals(COL_COUNTS[i], GRID.getColCount(i));
        }
    }

    public void testGetLatStep() {
        assertEquals(LAT_STEP, GRID.getLatStep(), 0.0);
    }

    public void testGetLonStep() {
        for (int i = 0; i < ROW_COUNT; ++i) {
            assertEquals(360.0, GRID.getColCount(i) * GRID.getLonStep(i), 1.0e-10);
        }
    }

    public void testGetBin() {
        for (int i = 0, binIndex = 0; i < COL_COUNTS.length; binIndex += COL_COUNTS[i], ++i) {
            assertEquals(binIndex, GRID.getBin(i, 0));
            assertEquals(binIndex + i, GRID.getBin(i, i));
        }
    }

    public void testGetLat() {
        for (int i = 0; i < ROW_COUNT; ++i) {
            final double lat = -90.0 + (i + 0.5) * LAT_STEP;
            assertEquals(lat, GRID.getLat(i), 0.0);
        }
    }

    public void testGetLatNorth() {
        assertEquals(90.0, GRID.getLatNorth(ROW_COUNT - 1), 0.0);

        for (int i = 0; i < ROW_COUNT; ++i) {
            final double latNorth = -90.0 + (i + 1) * LAT_STEP;
            assertEquals(latNorth, GRID.getLatNorth(i), 0.0);
        }
    }

    public void testGetLatSouth() {
        assertEquals(-90.0, GRID.getLatSouth(0), 0.0);

        for (int i = 0; i < ROW_COUNT; ++i) {
            final double latSouth = -90.0 + i * LAT_STEP;
            assertEquals(latSouth, GRID.getLatSouth(i), 0.0);
        }
    }

    public void testGetLon() {
        for (int i = 0; i < ROW_COUNT; ++i) {
            final double lonStep = GRID.getLonStep(i);

            for (int j = 0; j < GRID.getColCount(i); ++j) {
                final double lon = -180.0 + (j + 0.5) * lonStep;
                assertEquals(lon, GRID.getLon(i, j), 0.0);
            }
        }
    }

    public void testGetLonEast() {
        for (int i = 0; i < ROW_COUNT; ++i) {
            final double lonStep = GRID.getLonStep(i);

            for (int j = 0; j < GRID.getColCount(i); ++j) {
                final double lonEast = (j + 1) * lonStep - 180.0;
                assertEquals(lonEast, GRID.getLonEast(i, j), 0.0);
            }
        }
    }

    public void testGetLonWest() {
        for (int i = 0; i < ROW_COUNT; ++i) {
            final double lonStep = GRID.getLonStep(i);

            for (int j = 0; j < GRID.getColCount(i); ++j) {
                final double lonWest = -180.0 + j * lonStep;
                assertEquals(lonWest, GRID.getLonWest(i, j), 0.0);
            }
        }
    }

    public void testGetRow() {
        assertEquals(0, GRID.getRow(-90.0));
        assertEquals(ROW_COUNT, GRID.getRow(90.0));

        assertTrue(GRID.getRow(100.0) >= ROW_COUNT);
        assertTrue(GRID.getRow(-120.0) < 0);

        for (int i = 0; i < ROW_COUNT; ++i) {
            final double lat = GRID.getLat(i);
            assertEquals(i, GRID.getRow(lat));
        }
    }

    public void testGetCol() {
        for (int i = 0; i < ROW_COUNT; ++i) {
            for (int j = 0; j < GRID.getColCount(i); ++j) {
                final double lon = GRID.getLon(i, j);
                assertEquals(j, GRID.getCol(i, lon));
            }
        }
    }
}
