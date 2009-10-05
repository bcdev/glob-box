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
import com.sun.media.imageioimpl.plugins.tiff.TIFFFaxDecompressor;
import com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader;
import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.SeekableStream;
import com.sun.media.jai.codecimpl.TIFFImage;

import org.esa.beam.dataio.arcbin.TileIndex.IndexEntry;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.media.jai.PlanarImage;


class IntegerCoverOpImage extends SingleBandedOpImage {

    private final HdrAdf hdrAdf;
    private final TileIndex tileIndex;
    private final RasterDataFile rasterDataFile;
    private final ByteArrayCodec byteArrayCodec;

    IntegerCoverOpImage(int sourceWidth, int sourceHeight, Dimension tileSize, HdrAdf hdrAdf, TileIndex tileIndex, RasterDataFile rasterDataFile) {
        super(DataBuffer.TYPE_INT, 
              sourceWidth, 
              sourceHeight, 
              tileSize, 
              null, // no configuration
              ResolutionLevel.MAXRES);
        this.hdrAdf = hdrAdf;
        this.tileIndex = tileIndex;
        this.rasterDataFile = rasterDataFile;
        byteArrayCodec = ByteArrayCodec.getInstance(ByteOrder.BIG_ENDIAN);
    }

    @Override
    protected final void computeRect(PlanarImage[] planarImages, WritableRaster targetRaster, Rectangle rectangle) {
        int tileIndexY = (targetRaster.getMinY() / hdrAdf.tileYSize) * hdrAdf.tilesPerRow;
        int currentTileIndex = (targetRaster.getMinX() / hdrAdf.tileXSize) + tileIndexY;
        int rectSize = targetRaster.getHeight()*targetRaster.getWidth();
        IndexEntry indexEntry = tileIndex.getIndexEntry(currentTileIndex);
        DataBuffer dataBuffer = targetRaster.getDataBuffer();
        if (indexEntry == null ) {
            fillBuffer(dataBuffer, -9999);
        } else {
            try {
                byte[] rawTileData = rasterDataFile.loadRawTileData(indexEntry);
                int tileType = rawTileData[2] & 0xff;
                int minSize = getMinSize(rawTileData);
                int min = getMin(rawTileData);
                int tileDataSize = indexEntry.size - 2 - minSize;
                int tileOffset = 2 + 2 + minSize;
                switch (tileType) {
                    case 0:
                        fillBuffer(dataBuffer, min);
                        break;
                    case 4:{
                        int index = 0;
                        for (int i = 0; i < tileDataSize; i++) {
                            int value = rawTileData[tileOffset++] & 0xff;
                            int value1 = ((value & 0xf0) >> 4) + min;
                            int value2 = (value & 0xf) + min;
                            if (index<rectSize) {
                                dataBuffer.setElem(index++, value1);
                            }
                            if (index<rectSize) {
                                dataBuffer.setElem(index++, value2);
                            }
                        }
                    }
                    break;
                    case 0xf8:
                    case 0xfc:{
                        int index = 0;
                        for (int i = 0; i < tileDataSize/2; i++) {
                            int count = rawTileData[tileOffset++] & 0xff;
                            int value = (rawTileData[tileOffset++] & 0xff) + min;
                            for (int j = 0; j < count; j++) {
                                if (index<rectSize) {
                                    dataBuffer.setElem(index++, value);
                                }
                            }
                        }
                    }
                    break;
                    case 0xe0:{
                        int index = 0;
                        for (int i = 0; i < tileDataSize/5; i++) {
                            int count = rawTileData[tileOffset++] & 0xff;
                            int value = byteArrayCodec.getInt(rawTileData, tileOffset) + min;
                            tileOffset +=4;
                            for (int j = 0; j < count; j++) {
                                if (index<rectSize) {
                                    dataBuffer.setElem(index++, value);
                                }
                            }
                        }
                    }
                    break;
                    case 0xdf:{
                        int index = 0;
                        for (int i = 0; i < tileDataSize; i++) {
                            int count = rawTileData[tileOffset++] & 0xff;
                            if (count<128) {
                                for (int j = 0; j < count; j++) {
                                    if (index<rectSize) {
                                        dataBuffer.setElem(index++, min);
                                    }
                                }
                            } else {
                                for (int j = 0; j < 256-count; j++) {
                                    if (index<rectSize) {
                                        dataBuffer.setElem(index++, -9999);
                                    }
                                }
                            }
                        }
                    }
                    break;
                    case 0xff:
                        SeekableStream stream = new ByteArraySeekableStream(rawTileData, tileOffset, tileDataSize);
                        
//                        TIFFImage image = new TIFFImage(stream, null, 1);
//                        ImageIO.
//                        TIFFImageReader imageReader = new TIFFImageReader()
                        TIFFFaxDecompressor decompressor = new TIFFFaxDecompressor();
                        ImageInputStream imageInputStream = new MemoryCacheImageInputStream(stream);
                        decompressor.setStream(imageInputStream);
                        
                        decompressor.setSourceXOffset(0);
                        decompressor.setSourceYOffset(0);
                        decompressor.setSubsampleX(1);
                        decompressor.setSubsampleY(1);
                        decompressor.setDstXOffset(0);
                        decompressor.setDstYOffset(0);
                        decompressor.setSourceBands(new int[] {0});
                        decompressor.setDestinationBands(new int[] {0});
                        decompressor.setCompression(3);
                        
//                        decompressor.beginDecoding();
                        byte[] buffer = new byte[rawTileData.length];
                        //byte b[], int dstOffset, int pixelBitStride, int scanlineStride
                        decompressor.decodeRaw(buffer, 0, width/8, width);
                        
                        
                        fillBuffer(dataBuffer, 20);
                        break;
                    default:
                        fillBuffer(dataBuffer, -9999);
                        break;
                }
            } catch (IOException e) {
                fillBuffer(dataBuffer, -9999);
            }
        }
    }
    int getMinSize(byte[] bytes) {
        return bytes[3];
    }
    
    int getMin(byte[] bytes) throws ProductIOException {
        int minSize = getMinSize(bytes);
        if (minSize == 0) {
            return 0;
        }
        if( minSize > 4 ) {
            throw new ProductIOException(MessageFormat.format("Corrupt 'minsize' of %d in block header.  Read aborted.", minSize));
        }
        int min = 0;
        if (minSize == 4) {
            min = byteArrayCodec.getInt(bytes, 4);
        } else {
            for (int i = 0; i < minSize; i++) {
                min = min * 256 + bytes[4 + i];
            }
            if (bytes[4] > 127) {
                if( minSize == 2 ) {
                    min = min - 65536;
                }else if( minSize == 1 ) {
                    min = min - 256;
                }else if( minSize == 3 ) {
                    min = min - 256*256*256;
                }
            }
        }
        return min;
    }

    private void fillBuffer(DataBuffer dataBuffer, int value) {
        for (int i = 0; i < dataBuffer.getSize(); i++) {
            dataBuffer.setElem(i, value);
        }
    }
}
