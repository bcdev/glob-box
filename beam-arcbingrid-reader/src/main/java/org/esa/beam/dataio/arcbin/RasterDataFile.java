/*
 * $Id: $
 * 
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.esa.beam.dataio.arcbin;

import org.esa.beam.dataio.arcbin.TileIndex.IndexEntry;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

class RasterDataFile {
    private final ImageInputStream imageInputStream;

    RasterDataFile(ImageInputStream imageInputStream) {
        this.imageInputStream = imageInputStream;
    }

    byte[] loadRawTileData(IndexEntry indexEntry) throws IOException {
        byte[] bytes = new byte[indexEntry.size + 2];
        synchronized (imageInputStream) {
            imageInputStream.seek(indexEntry.offset);
            imageInputStream.read(bytes);
        }
        return bytes;
    }

    static RasterDataFile create(File file) throws IOException {
        ImageInputStream imageInputStream = new FileImageInputStream(file);
        imageInputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
        return new RasterDataFile(imageInputStream);
    }
}
