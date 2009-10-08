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

import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


class ProjectionReader {
    
    private List<KeyValuePair> content = new ArrayList<KeyValuePair>();
    private CoordinateReferenceSystem cs;

    ProjectionReader(File prjFile) {
        FileReader fileReader;
        try {
            fileReader = new FileReader(prjFile);
        } catch (FileNotFoundException e) {
            cs = null;
            return;
        }
        BufferedReader reader = new BufferedReader(fileReader);
        try {
            String line = reader.readLine();
            while(line != null) {
                handleLine(line);
                line = reader.readLine();
            }
        } catch (IOException e1) {
            cs = null;
            return;
        } finally {
            try {
                reader.close();
            } catch (IOException ignore) {
            }
        }
        
        for (KeyValuePair keyValuePair : content) {
            System.out.println("key:   "+keyValuePair.key);
            System.out.println("value: "+keyValuePair.value);
        }
        System.out.println("===============================");
        buildCRS();
//        if (wkt.isEmpty()) {
//            cs = null;
//        } else {
//            try {
//                cs = ReferencingFactoryFinder.getCRSFactory(null).createFromWKT(wkt);
//            } catch (FactoryException e) {
//                cs = null;
//            }
//        }
    }

    private void buildCRS() {
        int index = 0;
        KeyValuePair proj = content.get(index++);
        if (proj.value.isEmpty()) {
            return;
        }
        
        // TODO Auto-generated method stub
        
    }

    private void handleLine(String line) {
        String[] split = line.split("\\s", 2);
        KeyValuePair keyValuePair;
        if (split.length==1) {
            keyValuePair = new KeyValuePair(split[0].trim(), "");
        } else {
            keyValuePair = new KeyValuePair(split[0].trim(), split[1].trim());
        }
        content.add(keyValuePair);
    }
    
    private class KeyValuePair {
        final String key;
        final String value;

        public KeyValuePair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

}
