package org.esa.beam.dataio.globcover.geotiff;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

class LegendFilenameFilter implements FilenameFilter {

    private List<String> regionLegendNames;


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
