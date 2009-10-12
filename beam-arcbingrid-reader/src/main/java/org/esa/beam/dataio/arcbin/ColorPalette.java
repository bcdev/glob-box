/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.arcbin;

import org.esa.beam.framework.datamodel.ColorPaletteDef;
import org.esa.beam.framework.datamodel.ColorPaletteDef.Point;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


class ColorPalette {
    
    private static final class ColorPaletteFilenameFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            if (name.endsWith(".clr") || name.endsWith(".CLR")) {
                return true;
            }
            return false;
        }
    }

    static File findColorPaletteFile(File gridDir) {
        FilenameFilter filter = new ColorPaletteFilenameFilter();
        File result = findFileInDir(gridDir, filter);
        if (result == null) {
            result = findFileInDir(gridDir.getParentFile(), filter);
        }
        return result;
    }
    
    private static File findFileInDir(File dir, FilenameFilter filter) {
        File[] fileList = dir.listFiles(filter);
        if (fileList.length > 0) {
            return fileList[0];
        }
        return null;
    }
    
    static ColorPaletteDef create(File file, RasterStatistics statistics) {
        FileReader fileReader;
        try {
            fileReader = new FileReader(file);
        } catch (FileNotFoundException e) {
            return null;
        }
        BufferedReader reader = new BufferedReader(fileReader);
        List<ColorPaletteDef.Point> pointList = new ArrayList<ColorPaletteDef.Point>();
        try {
            String line = reader.readLine();
            while(line != null) {
                line = line.trim();
                if (!line.startsWith("#") && !line.isEmpty()) {
                    String[] lineElems = line.split("\\s+");
                    if (lineElems.length == 4) {
                        try {
                            int sample = Integer.valueOf(lineElems[0]);
                            if (statistics != null && sample>= statistics.min && sample <= statistics.max) {
                                int red = Integer.valueOf(lineElems[1]);
                                int green = Integer.valueOf(lineElems[2]);
                                int blue = Integer.valueOf(lineElems[3]);
                        
                                final ColorPaletteDef.Point point = new ColorPaletteDef.Point();
                                point.setSample(sample);
                                point.setColor(new Color(red, green, blue));
                                System.out.println("add point "+point.getSample()+"  "+point.getColor());
                                pointList.add(point);
                            }
                        } catch (NumberFormatException ignoreLine) {
                        }
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e1) {
            return null;
        } finally {
            try {
                reader.close();
            } catch (IOException ignore) {
            }
        }
        if (!pointList.isEmpty()) {
            int numPoints = pointList.size();
            Point[] points = pointList.toArray(new ColorPaletteDef.Point[numPoints]);
            return new ColorPaletteDef(points);
        }
        return null;
    }
}
