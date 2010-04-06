package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Thomas Storm
 * Date: 06.04.2010
 * Time: 08:59:55
 */
public class GlobCarbonBaeAsciiHandler implements TimeDataHandler {

    @Override
    public TimeCoding generateTimeCoding(RasterDataNode raster) throws ParseException, IOException {
        final String fileName = raster.getProduct().getFileLocation().getName();
        ProductData.UTC[] dates = parseTimeFromFileName(fileName);
        if (dates != null) {
            final TimeCoding timeCoding = new TimeCoding(raster, dates[0], dates[0], true);
            timeCoding.setPixelToDateMap(generateTimePerPixelMap(raster.getProduct().getFileLocation()));
            return timeCoding;
        }
        return null; // should not come here
    }

    ProductData.UTC[] parseTimeFromFileName(String fileName) throws ParseException {
        if (!fileName.contains("ASCII") || !(fileName.startsWith("BAE"))) { // no BAE ASCII Product
            return null;
        } else {
            String temp = fileName.split("_ASCII")[0];
            final String[] splitted = temp.split("_");
            String date = splitted[splitted.length - 1];
            final ProductData.UTC start = ProductData.UTC.parse(date, "yyyyMM");
            return new ProductData.UTC[]{start};
        }
    }

    Map<PixelPos, ProductData.UTC> generateTimePerPixelMap(final File productFile) throws IOException,
                                                                                          ParseException {
        if (productFile == null) {
            return null;
        }
        Map<PixelPos, ProductData.UTC> map = new HashMap<PixelPos, ProductData.UTC>();
        BufferedReader reader = new BufferedReader(new FileReader(productFile));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.contains("PROJECTION_ID") && !line.equals("$")) {
                line = line.replaceAll("\\s+", " ");
                final String[] fields = line.split(" ");
                if (fields.length > 1) {
                    ProductData.UTC date = ProductData.UTC.parse(fields[0], "yyyyMMdd");
                    float x = Float.parseFloat(fields[3]);
                    float y = Float.parseFloat(fields[4]);
                    map.put(new PixelPos(x, y), date);
                }
            }
        }

        return map;
    }

}
