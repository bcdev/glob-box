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

/**
 * Test methods for class {@link IsinGridStorageInfo}.
 *
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 */
public class IsinGridStorageInfoTest extends TestCase {

    public void testConstructor() {
        try { // offset array is null
            new IsinGridStorageInfo(0, 0, null);
            fail();
        } catch (NullPointerException expected) {
        }
        try { // an offset is negative
            final int[] offsets = {3, 2, 1, -1};
            new IsinGridStorageInfo(0, 4, offsets);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try { // offsets are not sorted
            final int[] offsets = {3, 2, 0, 1};
            new IsinGridStorageInfo(0, 4, offsets);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try { // bin count is negative
            final int[] offsets = {3, 2, 1, 0};
            new IsinGridStorageInfo(0, -1, offsets);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try { // bin count is too small
            final int[] offsets = {3, 2, 1, 0};
            new IsinGridStorageInfo(0, 2, offsets);
            fail();
        } catch (IllegalArgumentException expected) {
        }
        try { // minimum ISIN row index is negative
            final int[] offsets = {3, 2, 1, 0};
            new IsinGridStorageInfo(-1, 4, offsets);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        // ok
        final int[] offsets = {3, 2, 1, 0};
        new IsinGridStorageInfo(2, 4, offsets);
    }

    public void testGetters() {
        final int[] offsets = {3, 2, 1, 0};
        final IsinGridStorageInfo info = new IsinGridStorageInfo(1, 4, offsets);

        assertFalse(info.isEmpty());

        assertFalse(info.isEmpty(0));
        assertFalse(info.isEmpty(1));
        assertFalse(info.isEmpty(2));
        assertFalse(info.isEmpty(3));

        assertEquals(4, info.getRowCount());

        assertEquals(1, info.getBinCount(0));
        assertEquals(1, info.getBinCount(1));
        assertEquals(1, info.getBinCount(2));
        assertEquals(1, info.getBinCount(3));

        assertEquals(4, info.getRow(0));
        assertEquals(3, info.getRow(1));
        assertEquals(2, info.getRow(2));
        assertEquals(1, info.getRow(3));

        assertEquals(3, info.getOffset(0));
        assertEquals(0, info.getOffset(3));
    }

}
