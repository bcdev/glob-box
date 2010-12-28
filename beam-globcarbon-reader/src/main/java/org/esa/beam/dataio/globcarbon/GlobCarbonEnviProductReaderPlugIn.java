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
import java.util.Locale;

/**
 * @author Thomas Storm
 */
public class GlobCarbonEnviProductReaderPlugIn implements ProductReaderPlugIn {

    /**
     * The name of the file format associated with this
     * {@link ProductReaderPlugIn}.
     */
    public static final String FORMAT_NAME = "GLOBCARBON";
    /**
     * The textual description of this {@link ProductReaderPlugIn}.
     */
    public static final String FORMAT_DESCRIPTION = "GlobCarbon Data Products";
    /**
     * The extension of the file format associated with this
     * {@link ProductReaderPlugIn}.
     */
    public static final String[] FILE_EXTENSIONS = new String[]{".hdr", ".zip"};
    private static final String[] TYPE_IDENTIFIER = new String[]{"BAE", "LAI", "VGCP", "FAPAR"};

    @Override
    public ProductReader createReaderInstance() {
        return new GlobCarbonEnviProductReader(this);
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (input == null) {
            return DecodeQualification.UNABLE;
        }
        String fileName = input.toString();

        if (!isFileNameOk(fileName)) {
            return DecodeQualification.UNABLE;
        }

        String extension = FileUtils.getExtension(fileName);

        boolean isZipFile = ".ZIP".equalsIgnoreCase(extension);

        if (!(isZipFile || existsImgFile(fileName))) {
            return DecodeQualification.UNABLE;
        }

        if (isZipFile && !isZipComplete(input)) {
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

    boolean existsImgFile(Object input) {
        String fileName = input.toString();
        String[] filesInDir;
        try {
            filesInDir = getProductFiles(fileName);
        } catch (IOException ignored) {
            return false;
        }
        String fileNameWithoutExtension = FileUtils.getFilenameWithoutExtension(fileName);

        File image = getImageFile(filesInDir, fileNameWithoutExtension);
        return image != null;
    }

    File getImageFile(String[] filesInDir, String fileNameWithoutExtension) {
        for (String file : filesInDir) {
            if (file.equalsIgnoreCase(fileNameWithoutExtension + ".img")) {
                return new File(file);
            }
        }
        return null;
    }

    boolean isFileNameOk(String filePath) {
        String fileName = FileUtils.getFileNameFromPath(filePath);
        boolean allowedPostFix = false;
        if (fileName.toUpperCase().contains("IGH")) {
            return false;
        }

        for (String postFix : FILE_EXTENSIONS) {
            allowedPostFix |= fileName.toLowerCase().endsWith(postFix);
        }
        if (!allowedPostFix) {
            return false;
        }

        boolean isOk = false;
        for (String allowed : TYPE_IDENTIFIER) {
            isOk |= fileName.toUpperCase().contains(allowed);
        }
        return isOk;

    }

    String[] getProductFiles(String fileName) throws IOException {
        int index = fileName.indexOf('!');
        if (index != -1) {
            fileName = fileName.substring(0, index);
        }
        if (fileName.toLowerCase().endsWith(".zip")) {
            final VirtualDir productZip = VirtualDir.create(new File(fileName));
            String[] zipEntries = productZip.list("");
            if (zipEntries.length > 1) {
                String basePath = productZip.getBasePath();
                for (int i = 0; i < zipEntries.length; i++) {
                    zipEntries[i] = basePath + "!" + zipEntries[i];
                }
                return zipEntries;
            }
        } else {
            File dir = new File(fileName).getParentFile();
            String[] fileNames = dir.list();
            if (fileNames.length > 1) {
                for (int i = 0; i < fileNames.length; i++) {
                    fileNames[i] = dir + File.separator + fileNames[i];
                }
                return fileNames;
            } else {
                return getProductFiles(fileNames[0]);
            }
        }
        return new String[0];
    }


    // a zip is considered complete if it contains at least one pair of hdr and img file

    private boolean isZipComplete(Object input) {
        boolean isComplete = false;
        try {
            String[] productFiles = getProductFiles(input.toString());
            for (String productFile : productFiles) {
                isComplete |= ".hdr".equalsIgnoreCase(FileUtils.getExtension(productFile))
                              && existsImgFile(productFile);
            }
        } catch (IOException ignored) {
            return false;
        }
        return isComplete;
    }
}
