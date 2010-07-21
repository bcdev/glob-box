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

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.TreeMap;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class TileIndexTest {

    @Test
    public void testFindInMap() {
        final TreeMap<TileIndex, Integer> map = new TreeMap<TileIndex, Integer>();
        map.put(new TileIndex(0, 3), 0);
        map.put(new TileIndex(1, 3), 1);
        map.put(new TileIndex(5, 3), 2);

        assertNotNull(map.get(new TileIndex(1, 3)));
        assertNotNull(map.get(new TileIndex(5, 3)));
        assertNotNull(map.get(new TileIndex(0, 3)));
    }

}
