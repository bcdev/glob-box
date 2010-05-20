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

    private ImageInputStream stream;

    IgbpGlccOpImage(int sourceWidth, int sourceHeight, ResolutionLevel level, File file) throws IOException {
        super(DataBuffer.TYPE_BYTE, sourceWidth, sourceHeight,
              JAIUtils.computePreferredTileSize(sourceWidth, sourceHeight, 1),
              null, level);
        this.stream = new FileImageInputStream(file);
    }

    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        final int sourceX = getSourceX((int) destRect.getX());
        final int sourceY = getSourceY((int) destRect.getY());
        final int sourceWidth = getSourceWidth((int) destRect.getWidth());
        final int sourceHeight = getSourceHeight((int) destRect.getHeight());

        int subsampling = (int) Math.floor(getScale());

        final DataBuffer buffer = dest.getDataBuffer();
        try {
            stream.seek(sourceX + sourceY * getWidth());
            for (int i = 0; i < buffer.getSize(); i++) {
                final int val = stream.read();
                buffer.setElem(i, val);
                stream.skipBytes(subsampling);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
