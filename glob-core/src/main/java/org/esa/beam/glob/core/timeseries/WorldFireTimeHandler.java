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
 * @author Thomas Storm
 */
public class WorldFireTimeHandler {

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
