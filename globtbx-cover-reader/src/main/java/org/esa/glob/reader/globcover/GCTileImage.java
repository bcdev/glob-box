package org.esa.glob.reader.globcover;

import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.jai.SingleBandedSampleModel;
import org.esa.beam.util.math.MathUtils;
import ucar.ma2.Array;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

class GCTileImage extends SourcelessOpImage {

    private final String variableName;
    private final GCTileFile tileFile;
    private final int subsampling;

    public GCTileImage(GCTileFile tileFile, String variableName, int dataBufferType, int subsampling) {
        this(createImageLayout(tileFile, subsampling, dataBufferType), tileFile, variableName, subsampling);
    }

    private GCTileImage(ImageLayout imageLayout, GCTileFile tileFile, String variableName, int subsampling) {
        super(imageLayout, null, imageLayout.getSampleModel(null),
              0, 0, imageLayout.getWidth(null), imageLayout.getHeight(null));
        this.variableName = variableName;
        this.tileFile = tileFile;
        this.subsampling = subsampling;

    }

    private static ImageLayout createImageLayout(GCTileFile tileFile, int subsampling, int dataBufferType) {
        int imageWidth = MathUtils.ceilInt(tileFile.getWidth() / (float) subsampling);
        int imageHeight = MathUtils.ceilInt(tileFile.getHeight() / (float) subsampling);
        Dimension tileDimension = JAIUtils.computePreferredTileSize(imageWidth, imageHeight, 1);
        SampleModel sampleModel = new SingleBandedSampleModel(dataBufferType,
                                                              tileDimension.width,
                                                              tileDimension.height);
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
        if (colorModel == null) {
            final int dataType = sampleModel.getDataType();
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            int[] nBits = {DataBuffer.getDataTypeSize(dataType)};
            colorModel = new ComponentColorModel(cs, nBits, false, true,
                                                 Transparency.OPAQUE,
                                                 dataType);
        }

        return new ImageLayout(0, 0, imageWidth, imageHeight,
                               0, 0, tileDimension.width, tileDimension.height,
                               sampleModel, colorModel);
    }

    @Override
    protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle rectangle) {
        Array array;
        try {
            array = tileFile.readData(variableName,
                                      rectangle.x * subsampling, rectangle.y * subsampling,
                                      Math.min(1800, rectangle.width * subsampling),
                                      Math.min(1800, rectangle.height * subsampling),
                                      subsampling, subsampling);
        } catch (Exception e) {
            throw new RuntimeException("Could not read " + tileFile, e);
        }
        try {
            writableRaster.setDataElements(rectangle.x, rectangle.y,
                                           rectangle.width,
                                           rectangle.height,
                                           array.getStorage());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
