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

import com.sun.media.imageioimpl.plugins.tiff.TIFFFaxDecompressor;
import com.sun.media.jai.codec.ByteArraySeekableStream;
import com.sun.media.jai.codec.SeekableStream;

import org.esa.beam.dataio.arcbin.TileIndex.IndexEntry;
import org.esa.beam.util.math.MathUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

/**
 * todo - add API doc
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class Main {

    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        if (!dir.exists()) {
            System.out.println("dir does not exist!!");
        }
        DblbndAdf dblbndAdf = DblbndAdf.create(new File(dir, DblbndAdf.FILE_NAME));
        HdrAdf hdrAdf = HdrAdf.create(new File(dir, HdrAdf.FILE_NAME));
        
        int pixels = MathUtils.floorInt((dblbndAdf.urx - dblbndAdf.llx) / hdrAdf.pixelSizeX);
        int lines = MathUtils.floorInt((dblbndAdf.ury - dblbndAdf.lly) / hdrAdf.pixelSizeY);
        System.out.println("pixels width " + pixels);
        System.out.println("rows height  " + lines);
        
        int numTiles = hdrAdf.tilesPerColumn * hdrAdf.tilesPerRow;
        System.out.println("numTiles  " + numTiles);
        TileIndex tileIndex = TileIndex.create(new File(dir, TileIndex.FILE_NAME), numTiles);
        RasterData reasterData = RasterData.create(new File(dir, RasterData.FILE_NAME));
        
        Set<Integer> keySet = tileIndex.getKeySet();
        Map<Integer, Integer> useTypes = new HashMap<Integer, Integer>();
        for (Integer index : keySet) {
            IndexEntry indexEntry = tileIndex.getIndexEntry(index);
            int offset = indexEntry.offset;
            int size = indexEntry.size;
            int tileType = reasterData.getTile(offset).getTileType();
//            System.out.println("index " + index + "  tile raster type " + tileType);
            int count = 0;
            if (useTypes.containsKey(tileType)) {
                count = useTypes.get(tileType);
            }
            count ++;
            useTypes.put(tileType, count);
        }
        System.out.println();
        for (Integer integer : useTypes.keySet()) {
            System.out.println(Integer.toHexString((integer&0xff))+"  "+useTypes.get(integer));
        }
        System.out.println();
        System.out.println();
//        
//        RasterData rasterData = RasterData.create(new File(dir, RasterData.FILE_NAME));
//        byte[] bytes = rasterData.loadRawData(tileIndex.getIndexEntry(8200));
//        for (int i = 0; i < bytes.length; i++) {
//            System.out.println("i "+i+"  "+bytes[i]);
//        }
        
        RasterDataFile rasterDataFile = RasterDataFile.create(new File(dir, RasterData.FILE_NAME));
        IndexEntry indexEntry = tileIndex.getIndexEntry(5558);
        byte[] rawTileData = rasterDataFile.loadRawTileData(indexEntry);
        int tileType = rawTileData[2] & 0xff;
        int minSize = rawTileData[3];
        int tileDataSize = indexEntry.size - 2 - minSize;
        int tileOffset = 2 + 2 + minSize;
        
        System.out.println("tileType "+tileType);
        System.out.println("minSize "+minSize);
        System.out.println("tileOffset "+tileOffset);
        System.out.println("tileDataSize "+tileDataSize);
        
        SeekableStream stream = new ByteArraySeekableStream(rawTileData, tileOffset, tileDataSize);
        TIFFFaxDecompressor decompressor = new TIFFFaxDecompressor();
        ImageInputStream imageInputStream = new MemoryCacheImageInputStream(stream);
        decompressor.setStream(imageInputStream);

        
//        decompressor.setSrcMinX(0);
//        decompressor.setSrcMinY(0);
//        decompressor.setSrcWidth(256);
//        decompressor.setSrcHeight(4);
//        decompressor.setDstMinX(dstMinX);
//        decompressor.setDstMinY(dstMinY);
//        decompressor.setDstWidth(dstWidth);
//        decompressor.setDstHeight(dstHeight);
//        decompressor.setActiveSrcMinX(j2);
//        decompressor.setActiveSrcMinY(i3);
//        decompressor.setActiveSrcWidth(l2);
//        decompressor.setActiveSrcHeight(k3);
//        
//        decompressor.setStream(stream);
//        decompressor.setOffset(l4);
//        decompressor.setByteCount((int)l5);
//        decompressor.beginDecoding();
//        stream.mark();
//        decompressor.decode();
//        stream.reset();
    }
}
