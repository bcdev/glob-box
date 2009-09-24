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

import static com.bc.ceres.binio.TypeBuilder.BYTE;
import static com.bc.ceres.binio.TypeBuilder.COMPOUND;
import static com.bc.ceres.binio.TypeBuilder.MEMBER;
import static com.bc.ceres.binio.TypeBuilder.SHORT;
import static com.bc.ceres.binio.TypeBuilder.VAR_SEQUENCE;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.internal.VarElementCountSequenceType;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * Contains the Raster Data
 */
class ReasterData {
    
    static final String FILE_NAME = "w001001.adf";
    
    private static final CompoundType RASTER_TYPE = COMPOUND("Raster", 
                                                             MEMBER("RTileSize", SHORT),
                                                             MEMBER("RTileType", BYTE),
                                                             MEMBER("RMinSize", BYTE),
                                                             MEMBER("RMin", VAR_SEQUENCE(BYTE, "RMinSize")),
                                                             MEMBER("RTileData", new RTileDataVarSequenceType(BYTE)));

    private DataContext context;
    
    ReasterData(DataContext context) {
        this.context = context;
    }

    private CompoundData getTileData(int tileOffset) {
        CompoundData data = context.createData(RASTER_TYPE, tileOffset);
        return data;
    }
    
    void dispose() {
        if (context != null) {
            context.dispose();
        }
    }
    
    static ReasterData create(File file, int numtiles) throws IOException {
        DataFormat dataFormat = new DataFormat(RASTER_TYPE, ByteOrder.BIG_ENDIAN);
        DataContext context = dataFormat.createContext(file, "r");
        return new ReasterData(context);
    }
    
    final static class RTileDataVarSequenceType extends VarElementCountSequenceType {
        private static final int rTileSizeIndex = 0;
        private static final int rMinSizeIndex = 2;

        public RTileDataVarSequenceType(Type elementType) {
            super(elementType.getName() + "[$" + rMinSizeIndex + "]", elementType);
        }

        @Override
        protected int resolveElementCount(CollectionData parent) throws IOException {
            int rMinSize = parent.getInt(rMinSizeIndex);
            int rTileSize = parent.getInt(rTileSizeIndex);
            return rTileSize * 2 - 3 - rMinSize;
        }
    }
}
