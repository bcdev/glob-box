/*
    $Id: BinnedProductReaderPlugIn.java 1566 2007-12-14 12:39:40Z ralf $

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

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * The class <code>BinnedProductReaderPlugIn</code> implements the
 * {@link ProductReaderPlugIn}.
 *
 * @author Ralf Quast
 * @version $Revision: 1566 $ $Date: 2007-12-14 13:39:40 +0100 (Fr, 14. Dez 2007) $
 * @see org.esa.beam.framework.dataio.ProductReaderPlugIn
 */
public class BinnedProductReaderPlugIn implements ProductReaderPlugIn {

    /**
     * The name of the file format associated with this
     * {@link ProductReaderPlugIn}.
     */
    public static final String FORMAT_NAME = "GlobColour-binned";
    /**
     * The textual description of this {@link ProductReaderPlugIn}.
     */
    public static final String FORMAT_DESCRIPTION = "GlobColour-Binned Data Products";
    /**
     * The extension of the file format associated with this
     * {@link ProductReaderPlugIn}.
     */
    public static final String FILE_EXTENSION = ".nc";

    /**
     * Creates and returns an instance of the actual {@link ProductReader}.
     *
     * @return a new reader instance.
     */
    @Override
    public ProductReader createReaderInstance() {
        return new BinnedProductReader(this);
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), getDescription(null));
    }

    /**
     * Gets the qualification of the product reader to decode a given input object.
     *
     * @param input the input object
     * @return the decode qualification
     */
    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final String path = input.toString();
        if (!path.toLowerCase().endsWith(FILE_EXTENSION)) {
            return DecodeQualification.UNABLE;
        }

        NetcdfFile ncfile = null;
        try {
            ncfile = NetcdfFile.open(path);
//            if (!ncfile.isNetcdf3FileFormat()) {
//                return DecodeQualification.UNABLE;
//            }

            final Group ncroot = ncfile.getRootGroup();
            if (ncroot.findDimension(ReaderConstants.BIN) == null) {
                return DecodeQualification.UNABLE;
            }

            final Variable row = ncroot.findVariable(ReaderConstants.ROW);
            if (row == null || row.getRank() != 1 || !DataType.SHORT.equals(row.getDataType())) {
                return DecodeQualification.UNABLE;
            }
            final Variable col = ncroot.findVariable(ReaderConstants.COL);
            if (col == null || col.getRank() != 1 || !DataType.SHORT.equals(col.getDataType())) {
                return DecodeQualification.UNABLE;
            }

            final Attribute title = ncroot.findAttributeIgnoreCase(ProductAttributes.TITLE);
            if (title == null || !title.isString() || !title.getStringValue().toLowerCase().contains(
                    "globcolour")) {
                return DecodeQualification.UNABLE;
            }
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        } finally {
            try {
                if (ncfile != null) {
                    ncfile.close();
                }
            } catch (IOException e) {
                // OK, ignored
            }
        }

        return DecodeQualification.INTENDED;
    }

    /**
     * Returns an array of the classes representing the valid input types for this
     * {@link ProductReaderPlugIn}.
     * <p/>
     * Instances of the classes returned are valid parameters for the <code>readProductNodes</code>
     * method of the {@link ProductReader} interface.
     *
     * @return an array containing valid input types.
     */
    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    /**
     * Returns an array of the format names associated with this {@link ProductReaderPlugIn}.
     *
     * @return the format names associated with this {@link ProductReaderPlugIn}.
     */
    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    /**
     * Returns an array of the default file extensions associated with each format
     * name returned by the <code>{@link#getFormatNames}</code> method.
     * <p/>
     * The array returned has the same length as the array returned by the
     * <code>{@link #getFormatNames}</code> method.
     * <p/>
     * The returned extensions always start with a leading colon <code>'.'</code>
     * character.
     *
     * @return the file extensions for this {@link ProductReaderPlugIn}.
     */
    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{FILE_EXTENSION};
    }

    /**
     * Returns a textual description of this {@link ProductReaderPlugIn} plug-in.
     * If the given locale is set to <code>null</code> the default locale is used.
     * <p/>
     * In a GUI, the getDescription returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code>
     *               the default locale is used.
     * @return a textual description of this {@link ProductReaderPlugIn}.
     */
    @Override
    public String getDescription(Locale locale) {
        return FORMAT_DESCRIPTION;
    }
}
