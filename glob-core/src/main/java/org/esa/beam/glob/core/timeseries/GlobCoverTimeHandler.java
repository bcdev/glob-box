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
