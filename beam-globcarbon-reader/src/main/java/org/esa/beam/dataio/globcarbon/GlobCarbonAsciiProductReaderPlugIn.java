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

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Thomas Storm
 */
public class GlobCarbonAsciiProductReaderPlugIn implements ProductReaderPlugIn {

    /**
     * The name of the file format associated with this
     * {@link org.esa.beam.framework.dataio.ProductReaderPlugIn}.
     */
    public static final String FORMAT_NAME = "GLOBCARBON-ASCII";
    /**
     * The textual description of this {@link org.esa.beam.framework.dataio.ProductReaderPlugIn}.
     */
    public static final String FORMAT_DESCRIPTION = "GlobCarbon Ascii Data Products";
    private static final String EXTENSION_ASCII = ".ascii";
    private static final String EXTENSION_ZIP = ".zip";
    /**
     * The extension of the file format associated with this
     * {@link org.esa.beam.framework.dataio.ProductReaderPlugIn}.
     */
    public static final String[] FILE_EXTENSIONS = new String[]{EXTENSION_ASCII, EXTENSION_ZIP};
    private static final String TYPE_BAE = "BAE";

    @Override
    public ProductReader createReaderInstance() {
        return new GlobCarbonAsciiProductReader(this);
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (input == null) {
            return DecodeQualification.UNABLE;
        }
        File inputFile = new File(input.toString());

        if (!isFileNameOk(inputFile)) {
            return DecodeQualification.UNABLE;
        }

        if (EXTENSION_ZIP.equalsIgnoreCase(FileUtils.getExtension(inputFile)) && !isZipOk(inputFile)) {
            return DecodeQualification.UNABLE;
        }

        return DecodeQualification.INTENDED;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return FORMAT_DESCRIPTION;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), getDescription(null));
    }

    boolean isFileNameOk(File input) {
        String fileName = input.getName();
        if (fileName.toUpperCase().contains("IGH")) {
            return false;
        }

        boolean allowedExtension = false;
        for (String extension : FILE_EXTENSIONS) {
            allowedExtension |= fileName.toLowerCase().endsWith(extension);
        }
        if (!allowedExtension) {
            return false;
        }

        return fileName.toUpperCase().contains(TYPE_BAE);
    }

    private boolean isZipOk(File file) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            final Enumeration<? extends ZipEntry> entries = zipFile.entries();
            final List<? extends ZipEntry> entryList = Collections.list(entries);
            if (!entryList.isEmpty()) {
                ZipEntry zipEntry = entryList.get(0);
                final String entryName = zipEntry.getName();
                if (entryName.toLowerCase().endsWith(EXTENSION_ASCII) &&
                    entryName.toUpperCase().contains(TYPE_BAE)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
            return false;
        }finally {
            if(zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ignored) {}
            }
        }
        return false;
    }

}
