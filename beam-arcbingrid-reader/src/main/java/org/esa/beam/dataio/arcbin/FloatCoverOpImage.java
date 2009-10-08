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

import com.bc.ceres.binio.util.ByteArrayCodec;

import org.esa.beam.dataio.arcbin.TileIndex.IndexEntry;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.nio.ByteOrder;

import javax.media.jai.PlanarImage;


class FloatCoverOpImage extends SingleBandedOpImage {
    private static final ByteArrayCodec byteArrayCodec = ByteArrayCodec.getInstance(ByteOrder.BIG_ENDIAN);
    private final Header header;
    private final TileIndex tileIndex;
    private final RasterDataFile rasterDataFile;    

    FloatCoverOpImage(int sourceWidth, int sourceHeight, Dimension tileSize, Header header, TileIndex tileIndex, RasterDataFile rasterDataFile) {
        super(DataBuffer.TYPE_FLOAT, 
              sourceWidth, 
              sourceHeight, 
              tileSize, 
              null, // no configuration
              ResolutionLevel.MAXRES);
        this.header = header;
        this.tileIndex = tileIndex;
        this.rasterDataFile = rasterDataFile;
    }
    
    @Override
    protected final void computeRect(PlanarImage[] planarImages, WritableRaster targetRaster, Rectangle rectangle) {
        int tileIndexY = (targetRaster.getMinY() / header.tileYSize) * header.tilesPerRow;
        int currentTileIndex = (targetRaster.getMinX() / header.tileXSize) + tileIndexY;
        int rectSize = targetRaster.getHeight()*targetRaster.getWidth();
        IndexEntry indexEntry = tileIndex.getIndexEntry(currentTileIndex);
        DataBuffer dataBuffer = targetRaster.getDataBuffer();
        if (indexEntry == null ) {
            fillBuffer(dataBuffer, Float.NaN);
        } else {
            try {
                byte[] rawTileData = rasterDataFile.loadRawTileData(indexEntry);
                int tileOffset = 2;
                for (int i = 0; i < rectSize; i++) {
                    float value = byteArrayCodec.getFloat(rawTileData, tileOffset);
                    tileOffset += 4;
                    dataBuffer.setElemFloat(i, value);
                }
            } catch (Exception e) {
                fillBuffer(dataBuffer, 42);
            }
        }
    }
    
    private void fillBuffer(DataBuffer dataBuffer, float value) {
        for (int i = 0; i < dataBuffer.getSize(); i++) {
            dataBuffer.setElemFloat(i, value);
        }
    }

}
