package org.esa.beam.glob.export;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ImageUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.geometry.BoundingBox;

import javax.media.jai.SourcelessOpImage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.*;

public class KmzExporterTest {

    @Test
    public void testExporter() throws IOException {
        final KmzExporter kmzExporter = new KmzExporter();
        RenderedImage layer = new DummyTestOpImage(10, 10);
        final BoundingBox boundBox = new ReferencedEnvelope(0, 20, 70, 30, DefaultGeographicCRS.WGS84);
        kmzExporter.addLayer("layerName", layer, boundBox);
        assertEquals(kmzExporter.getLayerCount(), 1);


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
        protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
            super.computeRect(sources, dest, destRect);

        }
    }


}
