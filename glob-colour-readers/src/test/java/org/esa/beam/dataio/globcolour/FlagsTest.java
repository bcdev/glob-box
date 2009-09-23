/*
    $Id: FlagsTest.java 1288 2007-11-06 13:53:25Z ralf $

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
 * Test methods for class {@link Flags}.
 *
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 */
public class FlagsTest extends TestCase {

    public void testMerisFlag() {
        final short value = (short) Flags.MERIS.getMask();

        assertFalse(Flags.NO_MEASUREMENT.isSet(value));
        assertFalse(Flags.INVALID.isSet(value));
        assertFalse(Flags.REPLICA.isSet(value));
        assertFalse(Flags.LAND.isSet(value));
        assertFalse(Flags.CLOUD1.isSet(value));
        assertFalse(Flags.CLOUD2.isSet(value));
        assertFalse(Flags.DEPTH1.isSet(value));
        assertFalse(Flags.DEPTH2.isSet(value));
        assertFalse(Flags.DEPTH2.isSet(value));
        assertFalse(Flags.TURBID.isSet(value));
        assertFalse(Flags.SEAWIFS.isSet(value));
        assertFalse(Flags.MODIS.isSet(value));

        assertEquals(-32768, value);
        assertTrue(Flags.MERIS.isSet(value));
    }
}
