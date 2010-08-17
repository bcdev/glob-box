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

package org.esa.beam.dataio.globcarbon;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Thomas Storm
 */
public class GlobCarbonProductReader extends AbstractProductReader {

    private static GlobCarbonProductReaderPlugIn readerPlugIn;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected GlobCarbonProductReader(GlobCarbonProductReaderPlugIn readerPlugIn) {
        super(GlobCarbonProductReader.readerPlugIn);
        GlobCarbonProductReader.readerPlugIn = readerPlugIn;
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        File inputFile = new File(getInput().toString());
        if (".zip".equalsIgnoreCase(FileUtils.getExtension(inputFile))) {
            String[] files = readerPlugIn.getProductFiles(inputFile.getAbsolutePath());
            for (String file : files) {
                if (".hdr".equalsIgnoreCase(FileUtils.getExtension(file))) {
                    parseHeader(file);
                }
            }
        }

        return null;
    }

    private void parseHeader(String file) {

    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
    }
}
