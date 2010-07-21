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

import java.text.MessageFormat;

/**
 * The class <code>IsinGridStorageInfo</code> encapsulates information on the storage of
 * gridded data, of which the underlying grid is an {@link IsinGrid}. The class provides
 * storage information for any continuous sequence of {@link IsinGrid} rows.
 *
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 * @see IsinGrid
 */
class IsinGridStorageInfo {

    private final int maxRow;
    private final int binCount;
    private final int[] offsets;

    /**
     * Constructs an instance of this class.
     *
     * @param minRow   the {@link IsinGrid} index number of the southernmost row stored.
     * @param binCount the number of bins stored.
     * @param offsets  the array of row offsets. Each offset must point to the storage of the
     *                 first column in an {@link IsinGrid} row. The offsets must be sorted in
     *                 <em>descending</em> order. The largest offset points to the last (i.e.
     *                 northernmost) row stored, while the smallest offset corresponds to the
     *                 first (i.e. southernmost) row.
     */
    IsinGridStorageInfo(final int minRow, final int binCount, final int[] offsets) {
        if (minRow < 0) {
            throw new IllegalArgumentException("minRow < 0");
        }
        if (binCount < 0) {
            throw new IllegalArgumentException("binCount < 0");
        }
        if (offsets == null) {
            throw new NullPointerException("offsets == null");
        }
        if (offsets[0] > binCount) {
            throw new IllegalArgumentException("offsets[0] > binCount");
        }
        for (int i = 0; i < offsets.length; ++i) {
            if (offsets[i] < 0) {
                throw new IllegalArgumentException(MessageFormat.format("offsets[{0}] < 0", i));
            }
        }
        for (int i = 0; i + 1 < offsets.length; ++i) {
            if (offsets[i + 1] > offsets[i]) {
                throw new IllegalArgumentException(MessageFormat.format("offsets[{0} + 1] > offsets[{1}]", i, i));
            }
        }

        this.maxRow = minRow + offsets.length - 1;
        this.binCount = binCount;
        this.offsets = offsets;
    }

    /**
     * Tests if the objects contains storage information.
     *
     * @return true if the object contains no storage information, false otherwise.
     */
    public final boolean isEmpty() {
        return offsets.length == 0;
    }

    /**
     * Tests if a row of interest has stored bins (a row may have no stored bins,
     * when there are no valid data for the stored row).
     *
     * @param i the stored row of interest. The index i = 0 refers to the last
     *          (i.e. northernmost) {@link IsinGrid} row stored, while the largest
     *          index refers to the first (i.e. southernmost) row.
     * @return true if the stored row has no stored bins, false otherwise.
     * @throws IndexOutOfBoundsException if <code>i</code> is is less than 0
     *                                   or not less than the number of rows
     *                                   stored.
     */
    public final boolean isEmpty(int i) {
        return getBinCount(i) == 0;
    }

    /**
     * Returns the number of stored rows.
     *
     * @return the number of stored rows.
     */
    public final int getRowCount() {
        return offsets.length;
    }


    /**
     * Returns the {@link IsinGrid} index number of a stored row of interest.
     *
     * @param i the stored row of interest. The index i = 0 refers to the last
     *          (i.e. northernmost) {@link IsinGrid} row stored, while the largest
     *          index refers to the first (i.e. southernmost) row.
     * @return the {@link IsinGrid} row index number.
     * @throws IndexOutOfBoundsException if <code>i</code> is is less than 0
     *                                   or not less than the number of rows
     *                                   stored.
     */
    public final int getRow(final int i) {
        return maxRow - i;
    }

    /**
     * Returns the offset pointing to a stored row of interest.
     *
     * @param i the stored row of interest. The index i = 0 refers to the last
     *          (i.e. northernmost) {@link IsinGrid} row stored, while the largest
     *          index refers to the first (i.e. southernmost) row.
     * @return the offset pointing to the stored row of interest.
     * @throws IndexOutOfBoundsException if <code>i</code> is is less than 0
     *                                   or not less than the number of rows
     *                                   stored.
     */
    public final int getOffset(final int i) {
        return offsets[i];
    }

    /**
     * Returns the number of bins stored for a row.
     *
     * @param i the stored row of interest. The index i = 0 refers to the last
     *          (i.e. northernmost) {@link IsinGrid} row stored, while the largest
     *          index refers to the first (i.e. southernmost) row.
     * @return the number of bins stored.
     * @throws IndexOutOfBoundsException if <code>i</code> is is less than 0
     *                                   or not less than the number of rows
     *                                   stored.
     */
    public final int getBinCount(final int i) {
        if (i != 0) {
            return getOffset(i - 1) - getOffset(i);
        }
        return binCount - getOffset(i);
    }

    /**
     * Returns the total number of bins stored.
     *
     * @return the total number of bins stored.
     */
    public final int getBinCount() {
        return binCount;
    }

}
