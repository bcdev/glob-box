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

import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class LegendParser {

    private LegendParser() {
    }

    static LegendClass[] parse(File inputFile, boolean isRegional) {
        if (".xls".equalsIgnoreCase(FileUtils.getExtension(inputFile))) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(inputFile);
                return new XlsLegendParser().parse(inputStream, isRegional);
            } catch (FileNotFoundException e) {
                Debug.trace(e);
            }finally {
                if(inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        return new LegendClass[0];
    }

}
