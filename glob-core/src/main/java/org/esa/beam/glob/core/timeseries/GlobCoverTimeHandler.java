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

package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.ProductData;

import java.text.ParseException;

public class GlobCoverTimeHandler {

    // TODO ts move into GlobCover-Reader

    protected ProductData.UTC[] parseTimeFromFileName(String fileName) throws ParseException {
        if (!fileName.contains("GLOBCOVER")) { // no Globcover Product
            return null;
        } else if (fileName.endsWith(".zip") || fileName.endsWith(".tif")) {    // at least possibly Globcover Land 
            // Cover Product
            String temp = fileName.split("GLOBCOVER_")[1];
            temp = temp.split("_V")[0];
            String[] stringDates = temp.split("_");

            final ProductData.UTC start = ProductData.UTC.parse(stringDates[0], "yyyyMM");
            final ProductData.UTC end = ProductData.UTC.parse(stringDates[1], "yyyyMM");

            return new ProductData.UTC[]{start, end};
        } else if (fileName.endsWith(".hdf")) { // bimonthly or annual Globcover MERIS FR mosaic product
            String temp = fileName.split("MOSAIC_")[1];
            String[] stringDates = temp.split("_");
            if (stringDates[0].contains("-")) { // start and end date
                stringDates = stringDates[0].split("-");
                final ProductData.UTC start = ProductData.UTC.parse(stringDates[0], "yyyyMM");
                final ProductData.UTC end = ProductData.UTC.parse(stringDates[1], "yyyyMM");
                return new ProductData.UTC[]{start, end};
            } else { // only start year
                final ProductData.UTC start = ProductData.UTC.parse(stringDates[0], "yyyy");
                return new ProductData.UTC[]{start};
            }
        }
        return null;
    }
}
