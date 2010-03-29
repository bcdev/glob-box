package org.esa.beam.glob.export.netcdf;

/**
 * User: Thomas Storm
 * Date: 26.03.2010
 * Time: 09:42:29
 */
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
