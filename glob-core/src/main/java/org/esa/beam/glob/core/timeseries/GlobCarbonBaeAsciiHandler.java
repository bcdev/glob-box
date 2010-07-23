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
 * Preliminary API. Do not use.
 *
 * @author Thomas Storm
 */

// TODO move functionality into GlobCarbonReader 
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
