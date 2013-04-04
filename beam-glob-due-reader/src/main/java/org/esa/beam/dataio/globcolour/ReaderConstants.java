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

/**
 * Various constants used by the GlobColour product readers.
 *
 * @author Ralf Quast
 * @version $Revision: 1299 $ $Date: 2007-11-07 08:48:13 +0100 (Mi, 07. Nov 2007) $
 */
public class ReaderConstants {

    /**
     * The name of the Bin dimension.
     */
    static final String BIN = "bin";
    /**
     * The name of the Row variable.
     */
    static final String ROW = "row";
    /**
     * The name of the Column variable.
     */
    static final String COL = "col";
    /**
     * The ISIN grid underlying the GlobColour Binned products.
     */
    static final IsinGrid IG = new IsinGrid(4320);
    /**
     * The type identifier for Binned global products.
     */
    public static final String BINNED_GLOBAL = "GlobColour-L3b";
    /**
     * The type identifier for Mapped global products.
     */
    public static final String MAPPED_GLOBAL = "GlobColour-L3m";
    /**
     * The type identifier for Binned DDS products.
     */
    public static final String BINNED_DDS = "GlobColour-L3b-DDS";
    /**
     * The type identifier for Mapped DDS products.
     */
    public static final String MAPPED_DDS = "GlobColour-L3m-DDS";

    private ReaderConstants() {
    }
}
