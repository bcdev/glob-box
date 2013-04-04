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

package org.esa.beam.dataio.globcover.geotiff;

import org.esa.beam.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

public class GlobCoverGeoTiffProductReaderPlugIn implements ProductReaderPlugIn {

    private final GeoTiffProductReaderPlugIn geoTiffPlugIn;
    private final LegendFilenameFilter legendFileFilter;
    private static final String[] FORMAT_NAMES = {"GeoTIFF-GC"};

    public GlobCoverGeoTiffProductReaderPlugIn() {
        geoTiffPlugIn = new GeoTiffProductReaderPlugIn();
        legendFileFilter = new LegendFilenameFilter();
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        DecodeQualification decodeQualification = geoTiffPlugIn.getDecodeQualification(input);

        if (DecodeQualification.INTENDED.equals(decodeQualification) ||
            DecodeQualification.SUITABLE.equals(decodeQualification)) {
            final File file = new File(input.toString());
            final File parentDir = file.getParentFile();
            final File[] excelFiles = parentDir.listFiles(legendFileFilter);
            if (excelFiles.length == 1) {
                return DecodeQualification.INTENDED;
            }
            return DecodeQualification.UNABLE;
        }
        return decodeQualification;
    }

    @Override
    public Class[] getInputTypes() {
        return geoTiffPlugIn.getInputTypes();
    }

    @Override
    public ProductReader createReaderInstance() {
        return new GlobCoverGeoTiffProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return geoTiffPlugIn.getDefaultFileExtensions();
    }

    @Override
    public String getDescription(Locale locale) {
        return "GlobCover Global/Regional Land Cover product";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

}
