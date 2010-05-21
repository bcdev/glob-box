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

    IgbpGlccOpImage(int sourceWidth, int sourceHeight, ResolutionLevel level, File file) {
        super(DataBuffer.TYPE_BYTE, sourceWidth, sourceHeight,
              JAIUtils.computePreferredTileSize(sourceWidth, sourceHeight, 1),
              null, level);
        this.file = file;
    }

    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        int x1 = destRect.x;
        int x2 = destRect.x + destRect.width - 1;
        int y1 = destRect.y;
        int y2 = destRect.y + destRect.height - 1;

        ImageInputStream stream = null;
        try {
            stream = new FileImageInputStream(file);
            final byte[] data = new byte[1];
            for (int y = y1; y <= y2; y++) {
                final int sourceIndexY = getSourceY(y) * (getSourceWidth(getWidth() + 1));
                for (int x = x1; x <= x2; x++) {
                    final int sourceX = getSourceX(x);
                    final int indexPos = sourceX + sourceIndexY;
                    stream.seek(indexPos);
                    long position = stream.getStreamPosition();
                    final int val = stream.read(data);
                    position = stream.getStreamPosition();

                    if (val != -1) {
                        dest.setSample(x, y, 0, data[0]);
                    }
                }
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
