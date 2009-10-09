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
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.image.DataBuffer;
import java.io.IOException;
import java.nio.ByteOrder;
import java.text.MessageFormat;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

class IntegerGridTileProvider implements GridTileProvider {

    private static final ByteArrayCodec byteArrayCodec = ByteArrayCodec.getInstance(ByteOrder.BIG_ENDIAN);
    
    private final RasterDataFile rasterDataFile;
    private final TileIndex tileIndex;
    private final int nodataValue;
    private final int size;
    private final int productDataType;
    

    IntegerGridTileProvider(RasterDataFile rasterDataFile, TileIndex tileIndex, int nodataValue, int size, int productDataType) {
        this.rasterDataFile = rasterDataFile;
        this.tileIndex = tileIndex;
        this.nodataValue = nodataValue;
        this.size = size;
        this.productDataType = productDataType;
    }

    @Override
    public ProductData getData(int currentTileIndex) {
        ProductData dataBuffer = ProductData.createInstance(productDataType, size);
        IndexEntry indexEntry = tileIndex.getIndexEntry(currentTileIndex);
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
                        for (int i = 0; i < size; i++) {
                            int value;
                            if (i%2 == 0) {
                                rawValue = rawTileData[tileOffset++] & 0xff;
                                value = ((rawValue & 0xf0) >> 4);
                            } else {
                                value = (rawValue & 0xf);    
                            }
                            dataBuffer.setElemIntAt(i, value + min);
                        }
                    }
                    break;
                    case ArcBinGridConstants.RAW_8BIT:{
                      for (int i = 0; i < size; i++) {
                          dataBuffer.setElemIntAt(i, rawTileData[tileOffset++] + min);
                      }
                    }
                    break;
                    case ArcBinGridConstants.RAW_16BIT:{
                        for (int i = 0; i < size; i++) {
                            short value = byteArrayCodec.getShort(rawTileData, tileOffset);
                            tileOffset += 2;
                            dataBuffer.setElemIntAt(i, value + min);
                        }
                      }
                    break;
                    case ArcBinGridConstants.RAW_32BIT:{
                        for (int i = 0; i < size; i++) {
                            int value = byteArrayCodec.getInt(rawTileData, tileOffset);
                            tileOffset += 4;
                            dataBuffer.setElemIntAt(i, value + min);
                        }
                        break;
                    }
                    case ArcBinGridConstants.RLE_4BIT:
                    case ArcBinGridConstants.RLE_8BIT:{
                        int count = 0;
                        int value = 0;
                        for (int i = 0; i < size; i++) {
                            if (count == 0) {
                                count = rawTileData[tileOffset++] & 0xff;
                                value = (rawTileData[tileOffset++] & 0xff) + min;
                            }
                            dataBuffer.setElemIntAt(i, value);
                            count--;
                        }
                        break;
                    }
                    case ArcBinGridConstants.RLE_16BIT: {
                        int count = 0;
                        int value = 0;
                        for (int i = 0; i < size; i++) {
                            if (count == 0) {
                                count = rawTileData[tileOffset++] & 0xff;
                                value = byteArrayCodec.getShort(rawTileData, tileOffset) + min;
                                tileOffset +=2;
                            }
                            dataBuffer.setElemIntAt(i, value);
                            count--;
                        }
                        break;
                    }
                    case ArcBinGridConstants.RLE_32BIT:{
                        int count = 0;
                        int value = 0;
                        for (int i = 0; i < size; i++) {
                            if (count == 0) {
                                count = rawTileData[tileOffset++] & 0xff;
                                value = byteArrayCodec.getInt(rawTileData, tileOffset) + min;
                                tileOffset +=4;
                            }
                            dataBuffer.setElemIntAt(i, value);
                            count--;
                        }
                        break;
                    }
                    case ArcBinGridConstants.RUN_MIN:{
                        int count = 0;
                        int value = 0;
                        for (int i = 0; i < size; i++) {
                            if (count == 0) {
                                count = rawTileData[tileOffset++] & 0xff;
                                if (count < 128) {
                                    value = min;
                                } else {
                                    count = 256 - count;
                                    value = nodataValue;
                                }
                            }
                            dataBuffer.setElemIntAt(i, value);
                            count--;
                        }
                        break;
                    }
                    case ArcBinGridConstants.CCITT: {
                        byte[] buffer = doFoo(rawTileData, tileOffset, tileDataSize);
                        //  Convert the bit buffer into 32bit integers and account for nMin
                        for (int i = 0; i < size; i++) {
                            if((buffer[i>>3] & (0x80 >> (i&0x7))) != 0) {
                                dataBuffer.setElemIntAt(i, min+1);
                            } else {
                                dataBuffer.setElemIntAt(i, min);
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
        return dataBuffer;
    }
    
    @Override
    public void transferData(ProductData data, int sourceIndex, DataBuffer dataBuffer, int targetIndex) {
        int value = data.getElemIntAt(sourceIndex);
        dataBuffer.setElem(targetIndex, value);
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

    private void fillBuffer(ProductData data, int value) {
        for (int i = 0; i < size; i++) {
            data.setElemIntAt(i, value);
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
