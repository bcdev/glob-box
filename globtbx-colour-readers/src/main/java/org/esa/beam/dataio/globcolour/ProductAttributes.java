/*
    $Id: ProductAttributes.java 1288 2007-11-06 13:53:25Z ralf $

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
 * The class <code>ProductAttributes</code> specifies the identifiers
 * of the global attributes used by GlobColour products.
 *
 * @author Ralf Quast
 * @version $Revision: 1288 $ $Date: 2007-11-06 14:53:25 +0100 (Di, 06. Nov 2007) $
 */
public class ProductAttributes {

    /**
     * The name of the global attribute specifying the time of the first measurement in a product.
     */
    public static final String START_TIME = "start_time";
    /**
     * The name of the global attribute specifying the time of the last measurement in a product.
     */
    public static final String END_TIME = "end_time";
    /**
     * The name of the global attribute describing the grid type.
     */
    public static final String GRID_TYPE = "grid_type";
    /**
     * The name of the global attribute specifying the minimum latitude.
     */
    public static final String MIN_LAT = "max_south_grid";
    /**
     * The name of the global attribute specifying the maximum latitude.
     */
    public static final String MAX_LAT = "max_north_grid";
    /**
     * The name of the global attribute specifying the maximum longitude.
     */
    public static final String MIN_LON = "max_west_grid";
    /**
     * The name of the global attribute specifying the minimum longitude.
     */
    public static final String MAX_LON = "max_east_grid";
    /**
     * The name of the global attribute specifying the identifier of a diagnostic site.
     */
    public static final String SITE_ID = "site_id";
    /**
     * The names of the global attribute specifying the name of a diagnostic site.
     */
    public static final String SITE_NAME = "site_name";
    /**
     * The names of the global attribute specifying the latitude of a diagnostic site.
     */
    public static final String SITE_LAT = "site_latitude";
    /**
     * The names of the global attribute specifying the longitude of a diagnostic site.
     */
    public static final String SITE_LON = "site_longitude";
    /**
     * The names of the global attribute specifying the grid column of a diagnostic site.
     */
    public static final String SITE_COL = "site_col";
    /**
     * The names of the global attribute specifying the grid row of a diagnostic site.
     */
    public static final String SITE_ROW = "site_row";
    /**
     * The name of the global attribute specifying the title of a product.
     */
    public static final String TITLE = "title";
    /**
     * The name of the fill value attribute.
     */
    public static final String FILL_VALUE = "_FillValue";

}
