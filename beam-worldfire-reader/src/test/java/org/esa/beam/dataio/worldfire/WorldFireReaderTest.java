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

package org.esa.beam.dataio.worldfire;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.TreeNode;
import org.esa.beam.util.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class WorldFireReaderTest {

    private WorldFireReader reader;

    @Before
    public void setup() {
        final WorldFireReaderPlugIn plugIn = new WorldFireReaderPlugIn();
        reader = (WorldFireReader) plugIn.createReaderInstance();
    }

    @Test
    public void testReader() throws IOException {
        final File file = TestResourceHelper.getAatsrAlgo1AsFile();
        reader.readProductNodes(file, null);
        assertNotNull(reader.getInput());
        assertNull(reader.getSubsetDef());
        assertSame(file, reader.getInput());
        final TreeNode<File> productComponents = reader.getProductComponents();
        assertNotNull(productComponents);
        assertEquals(1, productComponents.getChildren().length);
    }

    @Test
    public void testReading() throws IOException {
        final File file = TestResourceHelper.getAatsrAlgo1AsFile();
        final Product product = reader.readProductNodes(file, null);
        assertNotNull(product);
        assertEquals(FileUtils.getFilenameWithoutExtension(file), product.getName());
        assertEquals(WorldFireReader.PRODUCT_TYPE_AATSR_ALGO1, product.getProductType());
        assertEquals(3600, product.getSceneRasterWidth());
        assertEquals(1800, product.getSceneRasterHeight());

        final GeoCoding geoCoding = product.getGeoCoding();
        assertEquals(new GeoPos(0, 0), geoCoding.getGeoPos(new PixelPos(3600 / 2, 1800 / 2), null));
        assertEquals(new GeoPos(90, -180), geoCoding.getGeoPos(new PixelPos(0, 0), null));
        assertEquals(new GeoPos(90, 180), geoCoding.getGeoPos(new PixelPos(3600, 0), null));
        assertEquals(new GeoPos(-90, 180), geoCoding.getGeoPos(new PixelPos(3600, 1800), null));
        assertEquals(new GeoPos(-90, -180), geoCoding.getGeoPos(new PixelPos(0, 1800), null));

        assertEquals(new PixelPos(3600 / 2, 1800 / 2), geoCoding.getPixelPos(new GeoPos(0, 0), null));
        assertEquals(new PixelPos(0, 0), geoCoding.getPixelPos(new GeoPos(90, -180), null));
        assertEquals(new PixelPos(3600, 0), geoCoding.getPixelPos(new GeoPos(90, 180), null));
        assertEquals(new PixelPos(3600, 1800), geoCoding.getPixelPos(new GeoPos(-90, 180), null));
        assertEquals(new PixelPos(0, 1800), geoCoding.getPixelPos(new GeoPos(-90, -180), null));

    }

    @Test
    public void testDetectProductType() {
        final File aatsrAlgo1 = TestResourceHelper.getAatsrAlgo1AsFile();
        assertEquals(WorldFireReader.PRODUCT_TYPE_AATSR_ALGO1, reader.getProductType(aatsrAlgo1));
        final File aatsrAlgo2 = TestResourceHelper.getAatsrAlgo2AsFile();
        assertEquals(WorldFireReader.PRODUCT_TYPE_AATSR_ALGO2, reader.getProductType(aatsrAlgo2));
        final File atsr2Algo1 = TestResourceHelper.getAtsr2Algo1AsFile();
        assertEquals(WorldFireReader.PRODUCT_TYPE_ATSR2_ALGO1, reader.getProductType(atsr2Algo1));
        final File atsr2Algo2 = TestResourceHelper.getAtsr2Algo2AsFile();
        assertEquals(WorldFireReader.PRODUCT_TYPE_ATSR2_ALGO2, reader.getProductType(atsr2Algo2));
    }
}
