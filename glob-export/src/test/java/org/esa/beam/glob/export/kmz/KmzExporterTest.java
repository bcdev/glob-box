package org.esa.beam.glob.export.kmz;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ImageUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.geometry.BoundingBox;

import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static junit.framework.Assert.*;

public class KmzExporterTest {

    @Test
    public void testExporter() throws IOException {
        final KmzExporter kmzExporter = new KmzExporter("description", "name");
        RenderedImage layer = new DummyTestOpImage(10, 10);
        final BoundingBox boundBox = new ReferencedEnvelope(0, 20, 70, 30, DefaultGeographicCRS.WGS84);
        KmlLayer unTimedLayer = new KmlLayer("untimedLayer", layer, boundBox);
        KmlLayer timedLayer = new KmlLayer("timedLayer", layer, boundBox, new ProductData.UTC(), new ProductData.UTC());

        kmzExporter.addLayer(unTimedLayer);

        assertEquals(1, kmzExporter.getLayerCount());

        kmzExporter.addLayer(timedLayer);

        assertEquals(2, kmzExporter.getLayerCount());

        final OutputStream outStream = createOutputStream();
        kmzExporter.export(outStream, ProgressMonitor.NULL);
    }

    private OutputStream createOutputStream() {
        return new BufferedOutputStream(new ByteArrayOutputStream());
    }

    private static class DummyTestOpImage extends SourcelessOpImage {

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


}
