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
import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.SeekableStream;

import org.esa.beam.dataio.arcbin.TileIndex.IndexEntry;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteOrder;
import java.text.MessageFormat;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.media.jai.PlanarImage;


class IntegerCoverOpImage extends SingleBandedOpImage {

    private static final ByteArrayCodec byteArrayCodec = ByteArrayCodec.getInstance(ByteOrder.BIG_ENDIAN);
    private final Header header;
    private final TileIndex tileIndex;
    private final RasterDataFile rasterDataFile;
    private final int nodataValue;

    IntegerCoverOpImage(int sourceWidth, int sourceHeight, Dimension tileSize, Header header, TileIndex tileIndex, RasterDataFile rasterDataFile, int databufferType, int nodataValue) {
        super(databufferType, 
              sourceWidth, 
              sourceHeight, 
              tileSize, 
              null, // no configuration
              ResolutionLevel.MAXRES);
        this.header = header;
        this.tileIndex = tileIndex;
        this.rasterDataFile = rasterDataFile;
        this.nodataValue = nodataValue;
    }

    @Override
    protected final void computeRect(PlanarImage[] planarImages, WritableRaster targetRaster, Rectangle rectangle) {
        int tileIndexY = (targetRaster.getMinY() / header.tileYSize) * header.tilesPerRow;
        int currentTileIndex = (targetRaster.getMinX() / header.tileXSize) + tileIndexY;
        int rectSize = targetRaster.getHeight()*targetRaster.getWidth();
        IndexEntry indexEntry = tileIndex.getIndexEntry(currentTileIndex);
        DataBuffer dataBuffer = targetRaster.getDataBuffer();
        if (indexEntry == null ) {
            fillBuffer(dataBuffer, nodataValue);
        } else {
            try {
                byte[] rawTileData = rasterDataFile.loadRawTileData(indexEntry);
                int tileType = rawTileData[2] & 0xff;
                int minSize = getMinSize(rawTileData);
                int min = 0;
                if (minSize>0) {
                    min = getMin(minSize, rawTileData);
                }
                int tileDataSize = indexEntry.size - 2 - minSize;
                int tileOffset = 2 + 2 + minSize;
                switch (tileType) {
                    
//                    static final int RUN_8BIT = 0xd7;
//                    static final int RUN_16BIT = 0xcf;
//                    static final int RAW_1BIT = 0x01;
                    
                    case ArcBinGridConstants.CONST_BLOCK:
                        fillBuffer(dataBuffer, min);
                        break;
                    case ArcBinGridConstants.RAW_4BIT:{
                        int rawValue = 0;
                        for (int i = 0; i < rectSize; i++) {
                            int value;
                            if (i%2 == 0) {
                                rawValue = rawTileData[tileOffset++] & 0xff;
                                value = ((rawValue & 0xf0) >> 4);
                            } else {
                                value = (rawValue & 0xf);    
                            }
                            dataBuffer.setElem(i, value + min);
                        }
                    }
                    break;
                    case ArcBinGridConstants.RAW_8BIT:{
                      for (int i = 0; i < rectSize; i++) {
                          dataBuffer.setElem(i, rawTileData[tileOffset++] + min);
                      }
                    }
                    break;
                    case ArcBinGridConstants.RAW_16BIT:{
                        for (int i = 0; i < rectSize; i++) {
                            short value = byteArrayCodec.getShort(rawTileData, tileOffset);
                            tileOffset += 2;
                            dataBuffer.setElem(i, value + min);
                        }
                      }
                    break;
                    case ArcBinGridConstants.RAW_32BIT:{
                        for (int i = 0; i < rectSize; i++) {
                            int value = byteArrayCodec.getInt(rawTileData, tileOffset);
                            tileOffset += 4;
                            dataBuffer.setElem(i, value + min);
                        }
                        break;
                    }
                    case ArcBinGridConstants.RLE_4BIT:
                    case ArcBinGridConstants.RLE_8BIT:{
                        int count = 0;
                        int value = 0;
                        for (int i = 0; i < rectSize; i++) {
                            if (count == 0) {
                                count = rawTileData[tileOffset++] & 0xff;
                                value = (rawTileData[tileOffset++] & 0xff) + min;
                            }
                            dataBuffer.setElem(i, value);
                            count--;
                        }
                        break;
                    }
                    case ArcBinGridConstants.RLE_16BIT: {
                        int count = 0;
                        int value = 0;
                        for (int i = 0; i < rectSize; i++) {
                            if (count == 0) {
                                count = rawTileData[tileOffset++] & 0xff;
                                value = byteArrayCodec.getShort(rawTileData, tileOffset) + min;
                                tileOffset +=2;
                            }
                            dataBuffer.setElem(i, value);
                            count--;
                        }
                        break;
                    }
                    case ArcBinGridConstants.RLE_32BIT:{
                        int count = 0;
                        int value = 0;
                        for (int i = 0; i < rectSize; i++) {
                            if (count == 0) {
                                count = rawTileData[tileOffset++] & 0xff;
                                value = byteArrayCodec.getInt(rawTileData, tileOffset) + min;
                                tileOffset +=4;
                            }
                            dataBuffer.setElem(i, value);
                            count--;
                        }
                        break;
                    }
                    case ArcBinGridConstants.RUN_MIN:{
                        int count = 0;
                        int value = 0;
                        for (int i = 0; i < rectSize; i++) {
                            if (count == 0) {
                                count = rawTileData[tileOffset++] & 0xff;
                                if (count < 128) {
                                    value = min;
                                } else {
                                    count = 256 - count;
                                    value = nodataValue;
                                }
                            }
                            dataBuffer.setElem(i, value);
                            count--;
                        }
                        break;
                    }
                    case ArcBinGridConstants.CCITT: {
                        byte[] buffer = doFoo(rawTileData, tileOffset, tileDataSize);
                        //  Convert the bit buffer into 32bit integers and account for nMin
                        for (int i = 0; i < rectSize; i++) {
                            if((buffer[i>>3] & (0x80 >> (i&0x7))) != 0) {
                                dataBuffer.setElem(i, min+1);
                            } else {
                                dataBuffer.setElem(i, min);
                            }
                        }
                        break;
                    }
                    default:
                        fillBuffer(dataBuffer, nodataValue);
                        break;
                }
            } catch (IOException e) {
                fillBuffer(dataBuffer, nodataValue);
            }
        }
    }
    
    int getMinSize(byte[] bytes) {
        return bytes[3];
    }
    
    int getMin(int minSize, byte[] bytes) throws ProductIOException {
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
    
    static byte[] doFoo(byte[] rawTileData, int tileOffset, int tileDataSize) throws IOException {
        int width = 256;
        int height = 4;
        SeekableStream stream = new ByteArraySeekableStream(rawTileData, tileOffset, tileDataSize);
        TIFFFaxDecompressorExtension decompressor = new TIFFFaxDecompressorExtension(tileOffset, tileDataSize);
        ImageInputStream imageInputStream = new MemoryCacheImageInputStream(stream);
        decompressor.setStream(imageInputStream);
        int bufferSize = ((width*height)/8);
        byte[] buffer = new byte[bufferSize];
        decompressor.decodeRaw(buffer, 0, 1, 256/8);
        return buffer;
    }
    
    static final class TIFFFaxDecompressorExtension extends TIFFFaxDecompressor {
        TIFFFaxDecompressorExtension(int tileOffset, int tileDataSize) {
           this.compression = 2;
           
           this.fillOrder = 1;
           this.srcHeight = 4;
           this.srcWidth = 256;
           
           this.byteCount = tileDataSize;
           this.offset = tileOffset;
        }
    }
}
