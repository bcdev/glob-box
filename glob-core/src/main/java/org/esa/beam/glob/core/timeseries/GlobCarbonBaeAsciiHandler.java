package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Storm
 */
public class GlobCarbonBaeAsciiHandler {

    protected Map<PixelPos, ProductData.UTC[]> generateTimePerPixelMap(final File productFile) throws IOException,
                                                                                                      ParseException {
        if (productFile == null) {
            return null;
        }
        Map<PixelPos, ProductData.UTC[]> map = new HashMap<PixelPos, ProductData.UTC[]>();
        BufferedReader reader = new BufferedReader(new FileReader(productFile));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.contains("PROJECTION_ID") && !line.equals("$")) {
                line = line.trim().replaceAll("\\s+", " ");
                final String[] fields = line.split(" ");
                ProductData.UTC date = ProductData.UTC.parse(fields[0], "yyyyMMdd");
                float x = Float.parseFloat(fields[3]);
                float y = Float.parseFloat(fields[4]);
                final PixelPos pos = new PixelPos(x, y);
                if (map.keySet().contains(pos)) {
                    ProductData.UTC[] dates = new ProductData.UTC[map.get(pos).length + 1];
                    dates[dates.length - 1] = date;
                    map.put(pos, dates);
                } else {
                    map.put(pos, new ProductData.UTC[]{date});
                }
            }
        }
        reader.close();

        return map;
    }

    // TODO ts move into reader when reader has been written

    protected ProductData.UTC[] parseTimeFromFileName(String fileName) throws ParseException {
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
}
