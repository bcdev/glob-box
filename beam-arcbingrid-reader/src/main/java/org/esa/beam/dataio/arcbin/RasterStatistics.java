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

import static com.bc.ceres.binio.TypeBuilder.COMPOUND;
import static com.bc.ceres.binio.TypeBuilder.DOUBLE;
import static com.bc.ceres.binio.TypeBuilder.MEMBER;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Contains the Raster statistics
 */
class RasterStatistics {
    public static final String FILE_NAME = "sta.adf";
    
    private static final CompoundType TYPE = 
        COMPOUND("RasterStatistics", 
                 MEMBER("MIN", DOUBLE), 
                 MEMBER("MAX", DOUBLE), 
                 MEMBER("MEAN", DOUBLE), 
                 MEMBER("STDDEV", DOUBLE) 
        );
    
    final double min;
    final double max;
    final double mean;
    final double stddev;
    
    private RasterStatistics(double min, double max, double mean, double stddev) {
        this.min = min;
        this.max = max;
        this.mean = mean;
        this.stddev = stddev;
    }

    public static RasterStatistics create(File file) {
        DataFormat dataFormat = new DataFormat(TYPE, ByteOrder.BIG_ENDIAN);
        DataContext context;
        try {
            context = dataFormat.createContext(file, "r");
        } catch (FileNotFoundException e) {
            return null;
        }
        CompoundData data = context.createData();
        try {
            RasterStatistics rasterStatistics = new RasterStatistics(data.getDouble(0), data.getDouble(1), 
                                 data.getDouble(2), data.getDouble(3));
            return rasterStatistics;
        } catch (IOException ignore) {
            return null;
        } finally {
            context.dispose();
        }
    }
}
