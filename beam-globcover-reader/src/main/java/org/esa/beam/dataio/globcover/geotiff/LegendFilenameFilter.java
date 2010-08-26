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

package org.esa.beam.dataio.globcover.geotiff;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

class LegendFilenameFilter implements FilenameFilter {

    private final List<String> regionLegendNames;


    LegendFilenameFilter() {
        regionLegendNames = new ArrayList<String>();
        regionLegendNames.add("Globcover_Legend.xls");
        regionLegendNames.add("NorthAmerica_Legend.xls");
        regionLegendNames.add("CentralAmerica_Legend.xls");
        regionLegendNames.add("SouthAmerica_Legend.xls");
        regionLegendNames.add("WesternEurope_Legend.xls");
        regionLegendNames.add("EasternEurope_Legend.xls");
        regionLegendNames.add("NorthAfrica_Legend.xls");
        regionLegendNames.add("Africa_Legend.xls");
        regionLegendNames.add("CentralAsia_Legend.xls");
        regionLegendNames.add("SEAsia_Legend.xls");
        regionLegendNames.add("Australia_Legend.xls");
        regionLegendNames.add("Greenland_Legend.xls");
    }

    @Override
    public boolean accept(File dir, String name) {
        if (name.endsWith(".xls")) {
            return regionLegendNames.contains(name);
        }
        return false;
    }
}
