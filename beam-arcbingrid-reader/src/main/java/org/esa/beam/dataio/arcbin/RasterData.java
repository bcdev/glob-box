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
import static com.bc.ceres.binio.TypeBuilder.UBYTE;
import static com.bc.ceres.binio.TypeBuilder.*;

import org.esa.beam.dataio.arcbin.TileIndex.IndexEntry;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

import com.bc.ceres.binio.CollectionData;
import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataContext;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.Type;
import com.bc.ceres.binio.internal.VarElementCountSequenceType;
import com.bc.ceres.binio.util.ByteArrayCodec;

/**
 * Contains the Raster Data
 */
class RasterData {
    
    static final String FILE_NAME = "w001001.adf";
    
    private static final CompoundType RASTER_TYPE = COMPOUND("Raster", 
                                                             MEMBER("RTileSize", SHORT),
                                                             MEMBER("RTileType", BYTE),
                                                             MEMBER("RMinSize", BYTE),
                                                             MEMBER("RMin", VAR_SEQUENCE(BYTE, "RMinSize")),
                                                             MEMBER("RTileData", new RTileDataVarSequenceType(UBYTE)));

    private DataContext context;
    
    RasterData(DataContext context) {
        this.context = context;
    }
    
    RasterDataTile getTile(int tileOffset) {
        CompoundData tileData = getTileData(tileOffset);
        return new RasterDataTile(tileData);
    }

//    int getTileType(int tileOffset) {
//        CompoundData tileData = getTileData(tileOffset);
//        try {
//            return tileData.getByte(1);
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return -1;
//    }
    
    private CompoundData getTileData(int tileOffset) {
        CompoundData data = context.createData(RASTER_TYPE, tileOffset);
        return data;
    }
    
    void dispose() {
        if (context != null) {
            context.dispose();
        }
    }
    
    static RasterData create(File file) throws IOException {
        DataFormat dataFormat = new DataFormat(RASTER_TYPE, ByteOrder.BIG_ENDIAN);
        DataContext context = dataFormat.createContext(file, "r");
        return new RasterData(context);
    }
    
    final static class RasterDataTile {
        private final CompoundData data;

        RasterDataTile(CompoundData data) {
            this.data = data;
        }
        
        int getTileSize() throws IOException {
            return data.getInt(0);
        }
        
        int getTileType() throws IOException {
            return data.getInt(1) & 0xff;
        }
        
        int getMinSize() throws IOException {
            return data.getInt(2);
        }
        
        int getMin() throws IOException {
            SequenceData rminSeq = data.getSequence(3);
            byte[] bytes = new byte[rminSeq.getElementCount()];
            int length = bytes.length;
            for (int i = 0; i < length; i++) {
                bytes[i] = rminSeq.getByte(i);
            }
            int min;
            ByteArrayCodec byteArrayCodec = ByteArrayCodec.getInstance(ByteOrder.BIG_ENDIAN);
            switch (length) {
                case 1:
                    min = byteArrayCodec.getByte(bytes, 0);
                    break;
                case 2:
                    min = byteArrayCodec.getShort(bytes, 0);
                    break;
                case 4:
                    min = byteArrayCodec.getInt(bytes, 0);
                    break;
                default:
                    throw new IllegalArgumentException("RMinSize not in[1,2,4], was:"+length);
            }
            return min;
        }
        
        int getTileDataSize() throws IOException {
            return data.getSequence(4).getElementCount();
        }
        
        int getTileData(int index) throws IOException {
            return data.getSequence(4).getInt(index);
        }
        
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            try {
                builder.append("RTileSize ").append(getTileSize());
                builder.append("RTileType ").append(getTileType());
                builder.append("RTileType ").append(getMinSize());
            } catch (IOException e) {
            }
            return builder.toString(); 
        }
        
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
            return rTileSize * 2 - 2 - rMinSize;
        }
    }

     byte[] loadRawData(IndexEntry indexEntry) throws IOException {
         System.out.println("offset "+indexEntry.offset);
         System.out.println("size "+indexEntry.size);
        SequenceType sequenceType = SEQUENCE(BYTE, indexEntry.size+2);
        CompoundType compoundType = COMPOUND("d", MEMBER("F", sequenceType));
        CompoundData byteComp = context.createData(compoundType, indexEntry.offset);
        SequenceData bytesSeq = byteComp.getSequence(0);
        byte[] bytes = new byte[indexEntry.size+2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = bytesSeq.getByte(i);
        }
        return bytes;
    }
}
