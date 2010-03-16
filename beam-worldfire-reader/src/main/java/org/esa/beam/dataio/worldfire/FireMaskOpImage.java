package org.esa.beam.dataio.worldfire;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.SingleBandedOpImage;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.util.List;

public class FireMaskOpImage extends SingleBandedOpImage {

    private static final byte FALSE = (byte) 0;
    private static final byte TRUE = (byte) 255;
    private final ColorModel colorModel;
    private Placemark[] fireList;

    public FireMaskOpImage(List<Placemark> fireList,
                           int width, int height,
                           Dimension preferredTileSize, ResolutionLevel level) {
        super(DataBuffer.TYPE_BYTE,
              width,
              height,
              preferredTileSize,
              null,
              level);
        this.fireList = fireList.toArray(new Placemark[fireList.size()]);
        this.colorModel = createColorModel(getSampleModel());
    }

    @Override
    protected void computeRect(PlanarImage[] sourceImages, WritableRaster tile, Rectangle destRect) {
        final BufferedImage image = new BufferedImage(colorModel,
                                                      RasterFactory.createWritableRaster(tile.getSampleModel(),
                                                                                         tile.getDataBuffer(),
                                                                                         new Point(0, 0)), false, null);
        final Graphics2D graphics2D = image.createGraphics();
        graphics2D.translate(-tile.getMinX(), -tile.getMinY());
        graphics2D.setColor(Color.WHITE);


        for (Placemark fire : fireList) {
            final PixelPos pixelPos = fire.getPixelPos();
            if (pixelPos != null) {
                final int x = (int) pixelPos.x;
                final int y = (int) pixelPos.y;
                graphics2D.fillRect(x, y, 1, 1);
            }
        }
        graphics2D.dispose();

        final byte[] data = ((DataBufferByte) tile.getDataBuffer()).getData();
        for (int i = 0; i < data.length; i++) {
            data[i] = (data[i] != 0) ? TRUE : FALSE;
        }
    }

}