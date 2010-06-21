package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TimeCoding;

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
 * Time: 14:29:25
 */
public class WorldFireTimeHandler extends TimeHandler {

    @Override
    public TimeCoding generateTimeCoding(RasterDataNode raster) throws ParseException, IOException {
        final TimeCoding timeCoding = super.generateTimeCoding(raster);
//        timeCoding.setHasTimePerPixel(true);
//        timeCoding.setPixelToDateMap(createPixelToDateMap(raster.getProduct().getFileLocation()));
        return timeCoding;
    }

    protected Map<PixelPos, ProductData.UTC[]> createPixelToDateMap(File product) throws ParseException, IOException {
        Map<PixelPos, ProductData.UTC[]> map = new HashMap<PixelPos, ProductData.UTC[]>();
        BufferedReader reader = new BufferedReader(new FileReader(product));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().replaceAll("\\s+", " ");
                final String[] fields = line.split(" ");
                ProductData.UTC date = ProductData.UTC.parse(fields[0], "yyyyMMdd");
                float x = Float.parseFloat(fields[3]);
                float y = Float.parseFloat(fields[4]);
                final PixelPos pos = new PixelPos(x, y);
                if (map.containsKey(pos)) {
                    final ProductData.UTC[] oldValues = map.get(pos);
                    ProductData.UTC[] dates = new ProductData.UTC[oldValues.length + 1];
                    for (int i = 0; i < oldValues.length; i++) {
                        dates[i] = oldValues[i];
                    }
                    dates[oldValues.length] = date;
                    map.put(pos, dates);
                } else {
                    map.put(pos, new ProductData.UTC[]{date});
                }
            }
        } finally {
            reader.close();
        }
        return map;
    }
}
