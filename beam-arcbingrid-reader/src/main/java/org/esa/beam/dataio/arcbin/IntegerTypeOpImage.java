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

import org.esa.beam.dataio.arcbin.RasterData.RasterDataTile;
import org.esa.beam.dataio.arcbin.TileIndex.IndexEntry;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

import javax.media.jai.PlanarImage;

class IntegerTypeOpImage extends SingleBandedOpImage {
    
    private final HdrAdf hdrAdf;
    private final TileIndex tileIndex;
    private final RasterDataFile rasterDataFile;    

    IntegerTypeOpImage(int sourceWidth, int sourceHeight, Dimension tileSize, HdrAdf hdrAdf, TileIndex tileIndex, RasterDataFile rasterDataFile) {
        super(DataBuffer.TYPE_BYTE, 
              sourceWidth, 
              sourceHeight, 
              tileSize, 
              null, // no configuration
              ResolutionLevel.MAXRES);
        this.hdrAdf = hdrAdf;
        this.tileIndex = tileIndex;
        this.rasterDataFile = rasterDataFile;
    }
    
    @Override
    protected final void computeRect(PlanarImage[] planarImages, WritableRaster targetRaster, Rectangle rectangle) {
        int tileIndexY = (targetRaster.getMinY() / hdrAdf.tileYSize) * hdrAdf.tilesPerRow;
        int currentTileIndex = (targetRaster.getMinX() / hdrAdf.tileXSize) + tileIndexY;
        int rectSize = targetRaster.getHeight()*targetRaster.getWidth();
        IndexEntry indexEntry = tileIndex.getIndexEntry(currentTileIndex);
        DataBuffer dataBuffer = targetRaster.getDataBuffer();
        if (indexEntry == null ) {
            fillBuffer(dataBuffer, 42);
        } else {
            try {
                int tileType;
                byte[] rawBytes = rasterDataFile.loadRawTileData(indexEntry);
                tileType = rawBytes[2];
                fillBuffer(dataBuffer, tileType);
            } catch (Exception e) {
                fillBuffer(dataBuffer, 42);
            }
        }
    }
    
    private void fillBuffer(DataBuffer dataBuffer, int value) {
        for (int i = 0; i < dataBuffer.getSize(); i++) {
            dataBuffer.setElem(i, value);
        }
    }
}
