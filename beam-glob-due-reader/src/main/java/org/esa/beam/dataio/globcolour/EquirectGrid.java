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
 * The class <code>EquirectGrid</Code> is a simple representation of an equirectangular
 * grid. The grid must not cross the 180 degree meridian.
 *
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 */
class EquirectGrid {

    private int rowCount;
    private int colCount;
    private double minLat;
    private double minLon;
    private double latStep;
    private double lonStep;

    /**
     * Constructs an equirectangular grid.
     *
     * @param rowCount the number of rows in the grid, must be positive.
     * @param colCount the number of columns in the grid, must be positive.
     * @param minLat   the minimum latitude of the grid, must be in the interval [-90.0, 90.0].
     * @param minLon   the minimum longitude of the grid, must be in the interval [-180.0, 180.0].
     * @param latStep  latitudinal increment between adjacent rows, must be positive.
     * @param lonStep  longitudinal increment between adjacent columns, must be positive.
     * @throws IllegalArgumentException if any parameter is invalid,
     *                                  <code>minLat + rowCount * latStep > 90.0</code>, or
     *                                  <code>minLon + colCount * lonStep > 180.0</code>.
     */
    EquirectGrid(final int rowCount, final int colCount, final double minLat, final double minLon,
                        final double latStep, final double lonStep) {
        if (rowCount < 1) {
            throw new IllegalArgumentException("rowCount < 1");
        }
        if (colCount < 1) {
            throw new IllegalArgumentException("colCount < 1");
        }
        if (Math.abs(minLat) > 90.0) {
            throw new IllegalArgumentException("abs(minLat) > 90.0");
        }
        if (Math.abs(minLon) > 180.0) {
            throw new IllegalArgumentException("abs(minLon) > 180.0");
        }
        if (!(latStep > 0.0)) {
            throw new IllegalArgumentException("!(latStep > 0.0)");
        }
        if (!(lonStep > 0.0)) {
            throw new IllegalArgumentException("!(lonStep > 0.0)");
        }
        if (minLat + rowCount * latStep > 90.0) {
            throw new IllegalArgumentException("minLat + rowCount * latStep > 90.0");
        }
        if (minLon + colCount * lonStep > 180.0) {
            throw new IllegalArgumentException("minLon + colCount * lonStep > 180.0");
        }

        this.rowCount = rowCount;
        this.colCount = colCount;
        this.minLat = minLat;
        this.minLon = minLon;
        this.latStep = latStep;
        this.lonStep = lonStep;
    }

    /**
     * Retruns the number of rows in the grid.
     *
     * @return the number of rows.
     */
    public final int getRowCount() {
        return rowCount;
    }

    /**
     * Returns the number of columns in the grid.
     *
     * @return the number of columns.
     */
    public final int getColCount() {
        return colCount;
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
     * @return the longitudinal increment.
     */
    public final double getLonStep() {
        return lonStep;
    }

    /**
     * Returns the minimum latitude of the grid.
     *
     * @return the minimum latitude.
     */
    public final double getMinLat() {
        return minLat;
    }

    /**
     * Returns the maximum latitude of the grid.
     *
     * @return the maximum latitude.
     */
    public double getMaxLat() {
        return rowCount * latStep + minLat;
    }

    /**
     * Returns the minimum longitude of the grid
     *
     * @return the minimum longitude.
     */
    public final double getMinLon() {
        return minLon;
    }

    /**
     * Returns the maximum longitude of the grid
     *
     * @return the maximum longitude.
     */
    public double getMaxLon() {
        return colCount * lonStep + minLon;
    }

    /**
     * Returns the central latitude for a given row of interest.
     * The result is equal to the value of the expression:
     * <p/>
     * <code>(row + 0.5) * latStep + minLat</code>
     *
     * @param row the column of interest.
     * @return the central latitude for the row of interest.
     */
    public double getLat(int row) {
        return (row + 0.5) * latStep + minLat;
    }

    /**
     * Returns the central longitude for a given column of interest.
     * The result is equal to the value of the expression:
     * <p/>
     * <code>(col + 0.5) * lonStep + minLon</code>
     *
     * @param col the column of interest.
     * @return the central longitude for the column of interest.
     */
    public final double getLon(int col) {
        return (col + 0.5) * lonStep + minLon;
    }

}
