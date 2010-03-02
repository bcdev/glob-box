/*
    $Id: IsinGrid.java 1288 2007-11-06 13:53:25Z ralf $

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

/**
 * The class <code>IsinGrid</code> represents the <em>integerised sinusoidal (ISIN)</em>
 * grid which is used by Binned GlobColour products.
 *
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 */
class IsinGrid {

    // the number of rows in a grid
    private final int rowCount;
    // the number of columns in a row
    private final int[] colCounts;
    // the number of bins in a grid
    private final int binCount;
    // the bin numbers corresponding to the first column in each row
    private final int[] binNumbers;

    // the latidudinal increment between adjacent rows
    private final double latStep;
    // the longitudinal increment between adjacent columns in a row
    private final double[] lonSteps;


    /**
     * Calculates the number of columns in the row which is closest to a given
     * latitude of interest. The result is equal to the value of the expression:
     * <p/>
     * <code>(int) round((2 * rowCount) * cos(toRadians(lat)))</code>
     *
     * @param lat      the latitude of interest.
     * @param rowCount the total number of rows within the grid of interest.
     * @return the number of columns.
     */
    public static int getColCount(final double lat, final int rowCount) {
        return (int) Math.round((2 * rowCount) * Math.cos(Math.toRadians(lat)));
    }

    /**
     * Constructs an ISIN grid with the given number of rows.
     *
     * @param rowCount the number of ISIN grid rows, must be a positive number.
     * @throws IllegalArgumentException if <code>rowCount</code> is not a positive number.
     */
    IsinGrid(final int rowCount) {
        if (rowCount < 1) {
            throw new IllegalArgumentException("rowCount < 1");
        }

        this.rowCount = rowCount;

        colCounts = new int[rowCount];
        binNumbers = new int[rowCount];
        latStep = 180.0 / rowCount;
        lonSteps = new double[rowCount];

        int binCount = 0;
        for (int i = 0; i < rowCount; ++i) {
            colCounts[i] = getColCount(getLat(i), rowCount);
            lonSteps[i] = 360.0 / colCounts[i];
            binNumbers[i] = binCount;
            binCount += colCounts[i];
        }
        this.binCount = binCount;
    }

    /**
     * Returns the total number of bins in the grid.
     *
     * @return the total number of bins.
     */
    public final int getBinCount() {
        return binCount;
    }

    /**
     * Returns the number of rows in the grid.
     *
     * @return the number of rows in the grid.
     */
    public final int getRowCount() {
        return rowCount;
    }

    /**
     * Returns the number of columns in a given row of interest.
     *
     * @param row the index number of the row of interest.
     * @return the number of columns in the row.
     * @throws ArrayIndexOutOfBoundsException if <code>row</code> is less than 0 or not less
     *                                        than the number of rows in the grid.
     */
    public final int getColCount(final int row) {
        return colCounts[row];
    }

    /**
     * Returns the latitudinal increment between adjacent rows.
     *
     * @return the latitudinal increment.
     */
    public final double getLatStep() {
        return latStep;
    }

    /**
     * Returns the longitudinal increment between adjacent columns.
     *
     * @param row the row of interest.
     * @return the longitudinal increment for the row of interest.
     * @throws ArrayIndexOutOfBoundsException if <code>row</code> is less than 0 or not less
     *                                        than the number of rows in the grid.
     */
    public final double getLonStep(final int row) {
        return lonSteps[row];
    }

    /**
     * Returns the number of the bin corresponding to a given row and column of interest.
     *
     * @param row the index number of the row of interest.
     * @param col the index number of the column of interest.
     * @return the number of the bin corresponding to the row and column of interest.
     * @throws ArrayIndexOutOfBoundsException if <code>row</code> is less than 0 or not less
     *                                        than the number of rows in the grid, or if
     *                                        <code>col</code> is less than 0 or not less than
     *                                        the number of columns in row <code>row</code>.
     */
    public final int getBin(final int row, final int col) {
        return binNumbers[row] + col;
    }

    /**
     * Returns the number of the row corresponding to a given latitude of interest.
     * The result is equal to the value of the expression:
     * <p/>
     * <code>(int) ((lat + 90.0) / getLatStep())</code>
     *
     * @param lat the latitude of interest.
     * @return the index number of the row corresponding to the latitude of interest.
     */
    public final int getRow(final double lat) {
        return (int) ((lat + 90.0) / latStep);
    }

    /**
     * Returns the number of the column corresponding to a given longitude of interest.
     * The result is equal to the value of the expression:
     * <p/>
     * <code>(int) ((lon + 180.0) / getLonStep(row))</code>
     *
     * @param row the index number of the row of interest.
     * @param lon the longitude of interest.
     * @return the index number of the column which (within the row of interest) corresponds
     *         to the longitude of interest.
     * @throws ArrayIndexOutOfBoundsException if <code>row</code> is less than 0 or not less
     *                                        than the number of rows in the grid.
     */
    public final int getCol(final int row, final double lon) {
        return (int) ((lon + 180.0) / lonSteps[row]);
    }

    /**
     * Returns the (central) latitude of a given row of interest.
     * The result is equal to the value of the expression:
     * <p/>
     * <code>(row + 0.5) * getLatStep() - 90.0</code>
     *
     * @param row the index number of the row of interest.
     * @return the central latitude of the row of interest.
     */
    public final double getLat(final int row) {
        return (row + 0.5) * latStep - 90.0;
    }

    /**
     * Returns the (central) longitude for a given row and column of interest.
     * The result is equal to the value of the expression:
     * <p/>
     * <code>(col + 0.5) * getLonStep(row) - 180.0</code>
     *
     * @param row the index number of the row of interest.
     * @param col the index number of the column of interest.
     * @return the central longitude of the column of interest within the row of interest.
     * @throws ArrayIndexOutOfBoundsException if <code>row</code> is less than 0 or not less
     *                                        than the number of rows in the grid, or if
     *                                        <code>col</code> is less than 0 or not less than
     *                                        the number of columns in row <code>row</code>.
     */
    public final double getLon(final int row, final int col) {
        return (col + 0.5) * lonSteps[row] - 180.0;
    }

    /**
     * Returns the northern latitude of a given row of interest.
     * The result is equal to the value of the expression:
     * <p/>
     * <code>getLatSouth(row + 1)</code>
     *
     * @param row the index number of the row of interest.
     * @return the northern latitude of the row of interest.
     */
    public final double getLatNorth(final int row) {
        return getLatSouth(row + 1);
    }

    /**
     * Returns the southern latitude of a given row of interest.
     * The result is equal to the value of the expression:
     * <p/>
     * <code>row * latStep - 90.0</code>
     *
     * @param row the index number of the row of interest.
     * @return the southern latitude of the row of interest.
     */
    public final double getLatSouth(final int row) {
        return row * latStep - 90.0;
    }

    /**
     * Returns the eastern longitude for a given row and column of interest.
     * The result is equal to the value of the expression:
     * <p/>
     * <code>getLonWest(row, col + 1)</code>
     *
     * @param row the index number of the row of interest.
     * @param col the index number of the column of interest.
     * @return the eastern longitude of the column of interest within the row of interest.
     * @throws ArrayIndexOutOfBoundsException if <code>row</code> is less than 0 or not less
     *                                        than the number of rows in the grid, or if
     *                                        <code>col</code> is less than 0 or not less than
     *                                        the number of columns in row <code>row</code>.
     */
    public final double getLonEast(final int row, final int col) {
        return getLonWest(row, col + 1);
    }

    /**
     * Returns the western longitude for a given row and column of interest.
     * The result is equal to the value of the expression:
     * <p/>
     * <code>col * lonSteps[row] - 180.0</code>
     *
     * @param row the index number of the row of interest.
     * @param col the index number of the column of interest.
     * @return the western longitude of the column of interest within the row of interest.
     * @throws ArrayIndexOutOfBoundsException if <code>row</code> is less than 0 or not less
     *                                        than the number of rows in the grid, or if
     *                                        <code>col</code> is less than 0 or not less than
     *                                        the number of columns in row <code>row</code>.
     */
    public final double getLonWest(final int row, final int col) {
        return col * lonSteps[row] - 180.0;
    }
}
