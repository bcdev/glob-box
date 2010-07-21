/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.dataio.igbp.glcc;

import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;
import org.esa.beam.util.jai.JAIUtils;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

class IgbpGlccOpImage extends SingleBandedOpImage {

    private File file;
    private int sourceWidth;

    IgbpGlccOpImage(int sourceWidth, int sourceHeight, ResolutionLevel level, File file) {
        super(DataBuffer.TYPE_BYTE, sourceWidth, sourceHeight,
              JAIUtils.computePreferredTileSize(sourceWidth, sourceHeight, 1),
              null, level);
        this.sourceWidth = sourceWidth;
        this.file = file;
    }

    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        final int x1 = destRect.x;
        final int y1 = destRect.y;
        final int y2 = destRect.y + destRect.height - 1;

        ImageInputStream stream = null;
        try {
            stream = new FileImageInputStream(file);

            final byte[] srcBuf = new byte[getSourceWidth(destRect.width)];
            int[] destBuf = new int[destRect.width];
            final int sourceX1 = getSourceX(x1);
            for (int y = y1; y <= y2; y++) {
                final int sourceIndex = getSourceY(y) * sourceWidth + sourceX1;
                stream.seek(sourceIndex);
                stream.read(srcBuf);
                for (int i = 0; i < destBuf.length; i++) {
                    int srcIndex = (int) (i * getScale());
                    if (srcIndex >= srcBuf.length) {
                        srcIndex = srcBuf.length - 1;
                    }
                    destBuf[i] = srcBuf[srcIndex];
                }
                dest.setSamples(x1, y, destBuf.length, 1, 0, destBuf);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {

                }
            }
        }

    }
}
