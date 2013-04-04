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

package org.esa.beam.dataio.igbp.glcc;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

public class IgbpGlccProductReaderPlugInTest {

    private IgbpGlccProductReaderPlugIn plugin;

    @Before
    public void setUp() throws Exception {
        plugin = new IgbpGlccProductReaderPlugIn();
    }

    @Test
    public void testGetDecodeQualification() throws Exception {
        assertEquals(DecodeQualification.UNABLE, plugin.getDecodeQualification( "horst" ) );
        assertEquals(DecodeQualification.UNABLE, plugin.getDecodeQualification( "gbats1_2.img" ) );

        assertEquals(DecodeQualification.INTENDED, plugin.getDecodeQualification( "gbats2_0ll.img" ) );
        assertEquals(DecodeQualification.INTENDED, plugin.getDecodeQualification( "gigbp2_0ll.img" ) );
    }
}
