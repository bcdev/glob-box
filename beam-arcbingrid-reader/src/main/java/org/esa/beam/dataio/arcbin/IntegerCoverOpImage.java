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
import java.io.IOException;

import javax.media.jai.PlanarImage;


class IntegerCoverOpImage extends SingleBandedOpImage {

    private final HdrAdf hdrAdf;
    private final TileIndex tileIndex;
    private final RasterData rasterData;

    IntegerCoverOpImage(int sourceWidth, int sourceHeight, Dimension tileSize, HdrAdf hdrAdf, TileIndex tileIndex, RasterData rasterData) {
        super(DataBuffer.TYPE_INT, 
              sourceWidth, 
              sourceHeight, 
              tileSize, 
              null, // no configuration
              ResolutionLevel.MAXRES);
        this.hdrAdf = hdrAdf;
        this.tileIndex = tileIndex;
        this.rasterData = rasterData;
    }

    @Override
    protected synchronized final void computeRect(PlanarImage[] planarImages, WritableRaster targetRaster, Rectangle rectangle) {
        int tileIndexY = (targetRaster.getMinY() / hdrAdf.tileYSize) * hdrAdf.tilesPerRow;
        int currentTileIndex = (targetRaster.getMinX() / hdrAdf.tileXSize) + tileIndexY;
        int rectSize = targetRaster.getHeight()*targetRaster.getWidth();
        IndexEntry indexEntry = tileIndex.getIndexEntry(currentTileIndex);
        DataBuffer dataBuffer = targetRaster.getDataBuffer();
        if (indexEntry == null ) {
            fillBuffer(dataBuffer, -9999);
        } else {
            try {
                int tileType;
                RasterDataTile rasterDataTile = rasterData.getTile(indexEntry.offset);
                tileType = rasterDataTile.getTileType();
                switch (tileType) {
                    case 0:
                        int constValue = 0;
                        if (rasterDataTile.getMinSize()>0) {
                            constValue = rasterDataTile.getMin();
                        }
                        fillBuffer(dataBuffer, constValue);
                        break;
                    case 4:{
                        System.out.println("type 4");
                        int index = 0;
                        int entries = rasterDataTile.getTileDataSize();
                        for (int i = 0; i < entries; i++) {
                            int value = rasterDataTile.getTileData(i);
                            int value1 = (value & 0xf0) >> 4;
                            int value2 = value & 0xf;
                        System.out.println("value "+value);
                        System.out.println("value1 "+value1);
                        System.out.println("value2 "+value2);
                            if (index<rectSize) {
                                dataBuffer.setElem(index++, value1);
                            }
                            if (index<rectSize) {
                                dataBuffer.setElem(index++, value2);
                            }
                        }
                    }
//                    case 0xfc:{
//                        int min = 0;
//                        if (rasterDataTile.getMinSize()>0) {
//                            min = rasterDataTile.getMin();
//                        }
//                        int index = 0;
//                        int entries = rasterDataTile.getTileDataSize();
//                        for (int i = 0; i < entries; i++) {
//                            int count = rasterDataTile.getTileData(i);
//                            i++;
//                            int value = rasterDataTile.getTileData(i) + min;
//                            for (int j = 0; j < count; j++) {
//                                if (index<rectSize) {
//                                    dataBuffer.setElem(index++, value);
//                                }
//                            }
//                        }
//                    }
//                    break;
//                    case 0xdf:{
//                        int min = 0;
//                        if (rasterDataTile.getMinSize()>0) {
//                            min = rasterDataTile.getMin();
//                        }
//                        int index = 0;
//                        int entries = rasterDataTile.getTileDataSize();
//                        for (int i = 0; i < entries; i++) {
//                            int count = rasterDataTile.getTileData(i);
//                            if (count<128) {
//                                for (int j = 0; j < count; j++) {
//                                    if (index<rectSize) {
//                                        dataBuffer.setElem(index++, min);
//                                    }
//                                }
//                            } else {
//                                for (int j = 0; j < 256-count; j++) {
//                                    if (index<rectSize) {
//                                        dataBuffer.setElem(index++, -9999);
//                                    }
//                                }
//                            }
//                        }
//                    }
//                    break;
//                    case 0xff:
//                        fillBuffer(dataBuffer, 20);
//                        break;
                    default:
                        fillBuffer(dataBuffer, -9999);
                        break;
                }
            } catch (IOException e) {
                fillBuffer(dataBuffer, -9999);
            }
        }
    }

    private void fillBuffer(DataBuffer dataBuffer, int value) {
        for (int i = 0; i < dataBuffer.getSize(); i++) {
            dataBuffer.setElem(i, value);
        }
    }
}
