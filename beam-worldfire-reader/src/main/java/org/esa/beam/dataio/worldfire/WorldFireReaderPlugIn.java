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

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

// TODO - TBD: really use the optional gif-file
// TODO - read from zip files
// TODO - consider reading zips in zip files (e.g. annual time series)

// TODO - consider changeable filename convention
/**
 * @author Marco Peters
 * @since GlobToolbox 2.0
 */
public class WorldFireReaderPlugIn implements ProductReaderPlugIn {

    private static final String FORMAT_NAME = "ATSR World Fire";
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME};
    private static final String DESCRIPTION = "ATSR2/AATSR based Global Fire Maps";
    private static final Class[] INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String FIRE_FILE_EXTENSION = ".FIRE";
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{FIRE_FILE_EXTENSION};

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        File inputFile = new File(String.valueOf(input));
        if(!inputFile.getName().toUpperCase().endsWith(FIRE_FILE_EXTENSION)) {
            return DecodeQualification.UNABLE;
        }

        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(inputFile);
            return getDecodeQualification(inputStream);
        } catch (IOException ignored) {
            return DecodeQualification.UNABLE;
        }finally {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new WorldFireReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return DEFAULT_FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), getDescription(null));
    }

    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    private DecodeQualification getDecodeQualification(InputStream inputStream) {
        if(inputStream == null) {
            return DecodeQualification.UNABLE;
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            final String line = bufferedReader.readLine();
            if (line != null && !line.isEmpty() && line.length() < 100) {
                final int columnsCount = line.split("[\\s]++").length;
                if (columnsCount == 5 || columnsCount == 6) {
                    return DecodeQualification.INTENDED;
                }
            }
            return DecodeQualification.UNABLE;
        } catch (IOException ignored) {
            return DecodeQualification.UNABLE;
        }
    }

}
