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

import static com.bc.ceres.binio.TypeBuilder.*;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;


/**
 * Contains the Georef Bounds
 */
class DblbndAdf {
    
    public static final String FILE_NAME = "dblbnd.adf";
    
    private static final CompoundType TYPE = 
        COMPOUND("GeorefBounds", 
                 MEMBER("D_LLX", DOUBLE), 
                 MEMBER("D_LLY", DOUBLE), 
                 MEMBER("D_URX", DOUBLE), 
                 MEMBER("D_URY", DOUBLE) 
        );
    
    final double llx;
    final double lly;
    final double urx;
    final double ury;
    
    private DblbndAdf(double llx, double lly, double urx, double ury) {
        this.llx = llx;
        this.lly = lly;
        this.urx = urx;
        this.ury = ury;
    }

    public static DblbndAdf create(File file) throws IOException {
        DataFormat dataFormat = new DataFormat(TYPE, ByteOrder.BIG_ENDIAN);
        DataContext context = dataFormat.createContext(file, "r");
        CompoundData data = context.createData();
        
        DblbndAdf dblbndAdf = new DblbndAdf(data.getDouble(0), data.getDouble(1), 
                             data.getDouble(2), data.getDouble(3));
        context.dispose();
        return dblbndAdf;
    }
}
