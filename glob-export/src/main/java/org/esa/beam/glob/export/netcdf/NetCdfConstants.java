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

package org.esa.beam.glob.export.netcdf;

public interface NetCdfConstants {

    // CF-conventions

    public static final String LONG_NAME = "long_name";
    public static final String STANDARD_NAME = "standard_name";
    public static final String UNITS = "units";
    public static final String AXIS = "axis";
    public static final String DEGREES_NORTH = "degrees_north";
    public static final String DEGREES_EAST = "degrees_east";
    public static final String MISSING_VALUE = "missing_value";
    public static final String LON_VAR_NAME = "lon";

    // Non-CF-Conventions

    public static final String LAT_VAR_NAME = "lat";
    public static final String TIME_VAR_NAME = "time";
    public static final String NOT_A_NUMBER = "NaN";

    public static final String LATITUDE = "Latitude";
    public static final String LONGITUDE = "Longitude";
    public static final String TIME = "Time";

}
