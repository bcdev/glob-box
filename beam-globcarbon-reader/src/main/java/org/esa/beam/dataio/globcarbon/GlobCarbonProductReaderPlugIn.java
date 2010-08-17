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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Thomas Storm
 */
public class GlobCarbonProductReaderPlugIn implements ProductReaderPlugIn {

    /**
     * The name of the file format associated with this
     * {@link ProductReaderPlugIn}.
     */
    public static final String FORMAT_NAME = "GlobCarbon";
    /**
     * The textual description of this {@link ProductReaderPlugIn}.
     */
    public static final String FORMAT_DESCRIPTION = "GlobCarbon Data Products";
    /**
     * The extension of the file format associated with this
     * {@link ProductReaderPlugIn}.
     */
    public static final String[] FILE_EXTENSIONS = new String[]{".hdr", ".ascii", ".zip"};

    @Override
    public ProductReader createReaderInstance() {
        return new GlobCarbonProductReader(this);
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        String fileName = input.toString();
        if (!isFileNameOk(fileName)) {
            return DecodeQualification.UNABLE;
        }

        if (!(existsImgFile(fileName) || FileUtils.getExtension(fileName).equalsIgnoreCase("ASCII")) ||
            FileUtils.getExtension(fileName).equalsIgnoreCase("ZIP")) {
            return DecodeQualification.UNABLE;
        }

        return DecodeQualification.INTENDED;
    }

    boolean existsImgFile(Object input) {
        String fileName = input.toString();
        File[] filesInDir = new File(new File(fileName).getParent()).listFiles();
        List<String> fileNamesInDir = new ArrayList<String>();
        for (File file : filesInDir) {
            fileNamesInDir.add(file.getName());
        }
        String fileNameWithoutExtension = FileUtils.getFilenameWithoutExtension(
                FileUtils.getFileNameFromPath(fileName));

        return fileNamesInDir.contains(fileNameWithoutExtension + ".img") ||
               fileNamesInDir.contains(fileNameWithoutExtension + ".IMG");

    }

    boolean isFileNameOk(Object input) {
        String fileName = FileUtils.getFileNameFromPath(input.toString());
        boolean allowedPostFix = false;
        for (String postFix : FILE_EXTENSIONS) {
            allowedPostFix |= fileName.toLowerCase().endsWith(postFix);
        }
        if (!allowedPostFix) {
            return false;
        }

        boolean allowedPrefix = false;
        for (String allowed : getAllowedPrefixes()) {
            allowedPrefix |= fileName.startsWith(allowed);
        }
        allowedPrefix |= fileName.startsWith("BAE") && fileName.endsWith("_ASCII.ascii");

        return allowedPrefix;
    }

    private List<String> getAllowedPrefixes() {
        List<String> allowedPrefixes = new ArrayList<String>();
        allowedPrefixes.add("BAE_PLC_");
        allowedPrefixes.add("LAI_PLC_");
        allowedPrefixes.add("VGCP_PLC_");
        allowedPrefixes.add("FAPAR_PLC_");
        return allowedPrefixes;
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
}
