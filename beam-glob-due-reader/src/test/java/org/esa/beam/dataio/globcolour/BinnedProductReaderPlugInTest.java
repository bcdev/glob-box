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
import org.esa.beam.framework.dataio.DecodeQualification;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Test methods for class {@link BinnedProductReaderPlugIn}.
 *
 * @author Norman Fomferra
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 */
public class BinnedProductReaderPlugInTest extends TestCase {

    public void testGetDecodeQualification() throws UnsupportedEncodingException {
        final String path = getResourcePath("mapped.nc");

        final File file = new File(path);
        assertEquals(file.getName(), "mapped.nc");
        assertTrue(file.exists());
        assertTrue(file.canRead());

        assertEquals(DecodeQualification.UNABLE, new BinnedProductReaderPlugIn().getDecodeQualification(file));

        // todo - add test with Binned product
    }

    private static String getResourcePath(final String name) throws UnsupportedEncodingException {
        final URL url = BinnedProductReaderPlugInTest.class.getResource(name);
        assertNotNull(url);

        final String path = URLDecoder.decode(url.getPath(), "UTF-8");
        assertTrue(path.endsWith(name));

        return path;
    }

}
