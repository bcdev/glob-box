package org.esa.beam.glob.export.kmz;

import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ImageUtils;

import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.image.DataBuffer;

class DummyTestOpImage extends SourcelessOpImage {

    DummyTestOpImage(int width, int height) {
        super(ImageManager.createSingleBandedImageLayout(DataBuffer.TYPE_BYTE, width, height, width, height),
              null,
              ImageUtils.createSingleBandedSampleModel(DataBuffer.TYPE_BYTE, width, height),
              0, 0, width, height);
    }

    @Override
    protected void computeRect(PlanarImage[] sources, java.awt.image.WritableRaster dest,
                               java.awt.Rectangle destRect) {
        double[] value = new double[1];
        for (int y = 0; y < destRect.height; y++) {
            for (int x = 0; x < destRect.width; x++) {
                value[0] = x + y;
                dest.setPixel(x, y, value);
            }
        }
    }
}
