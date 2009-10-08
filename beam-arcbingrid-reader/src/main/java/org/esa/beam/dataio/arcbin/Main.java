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

import org.esa.beam.dataio.arcbin.TileIndex.IndexEntry;
import org.esa.beam.util.math.MathUtils;

import java.io.File;
import java.io.IOException;

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
        ProjectionReader projectionReader = new ProjectionReader(new File(dir, "prj.adf"));
        System.exit(0);
        
        
        GeorefBounds georefBounds = GeorefBounds.create(new File(dir, GeorefBounds.FILE_NAME));
        Header header = Header.create(new File(dir, Header.FILE_NAME));
        
        int pixels = MathUtils.floorInt((georefBounds.urx - georefBounds.llx) / header.pixelSizeX);
        int lines = MathUtils.floorInt((georefBounds.ury - georefBounds.lly) / header.pixelSizeY);
        System.out.println("pixels width " + pixels);
        System.out.println("rows height  " + lines);
        
        int numTiles = header.tilesPerColumn * header.tilesPerRow;
        System.out.println("numTiles  " + numTiles);
        TileIndex tileIndex = TileIndex.create(new File(dir, TileIndex.FILE_NAME), numTiles);
        
//        Set<Integer> keySet = tileIndex.getKeySet();
//        Map<Integer, Integer> useTypes = new HashMap<Integer, Integer>();
//        for (Integer index : keySet) {
//            IndexEntry indexEntry = tileIndex.getIndexEntry(index);
//            int offset = indexEntry.offset;
//            int size = indexEntry.size;
//            int tileType = reasterData.getTile(offset).getTileType();
////            System.out.println("index " + index + "  tile raster type " + tileType);
//            int count = 0;
//            if (useTypes.containsKey(tileType)) {
//                count = useTypes.get(tileType);
//            }
//            count ++;
//            useTypes.put(tileType, count);
//        }
//        System.out.println();
//        for (Integer integer : useTypes.keySet()) {
//            System.out.println(Integer.toHexString((integer&0xff))+"  "+useTypes.get(integer));
//        }
        
        System.out.println();
        
//        RasterData rasterData = RasterData.create(new File(dir, RasterData.FILE_NAME));
//        byte[] bytes = rasterData.loadRawData(tileIndex.getIndexEntry(8200));
//        for (int i = 0; i < bytes.length; i++) {
//            System.out.println("i "+i+"  "+bytes[i]);
//        }
        
        RasterDataFile rasterDataFile = RasterDataFile.create(new File(dir, RasterDataFile.FILE_NAME));
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
        
        String eightzeroes = "00000000";
        for (int i = 0; i < rawTileData.length; i++) {
            System.out.print("["+i+"]="+rawTileData[i]+" ");
        }
        System.out.println();
        System.out.println("-------------");
        for (int i = tileOffset; i < rawTileData.length; i++) {
            String binaryString = Integer.toBinaryString(rawTileData[i]&0xff);
            String out = eightzeroes.substring(binaryString.length()) + binaryString;
            System.out.print(out+" ");
        }
        System.out.println();
        System.out.println("-------------");
        
        byte[] buffer = IntegerCoverOpImage.doFoo(rawTileData, tileOffset, tileDataSize);
        
//        for (int i = 0; i < buffer.length; i++) {
//            System.out.print(buffer[i]+", ");
//        }
        for (int i = 0; i < buffer.length; i++) {
            if (i%32==0) {
                System.out.println();
            }
            String binaryString = Integer.toBinaryString(buffer[i]&0xff);
            String out = eightzeroes.substring(binaryString.length()) + binaryString;
            System.out.print(out+" ");
        }
        System.out.println();
        System.out.println("-------------");
        System.out.println();
    }
    


}
