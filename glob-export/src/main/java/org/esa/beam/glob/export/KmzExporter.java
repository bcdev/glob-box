package org.esa.beam.glob.export;

import com.bc.ceres.core.ProgressMonitor;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import org.esa.beam.framework.datamodel.Placemark;
import org.opengis.geometry.BoundingBox;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class KmzExporter {

    private static final String OVERLAY_KML = "overlay.kml";
    private static final String IMAGE_TYPE = "PNG";
    private RenderedImage legend;

    private final List<KmlLayer> kmlLayers;
    private List<Placemark> placemarks;
    private String legendName;

    public KmzExporter() {
        kmlLayers = new ArrayList<KmlLayer>();
        placemarks = new ArrayList<Placemark>();
    }

    public void export(final OutputStream outStream, final ProgressMonitor pm) throws IOException {


        int workload = kmlLayers.size() + 1 + (legend != null ? 1 : 0);
        pm.beginTask("Exporting KMZ", workload);

        ZipOutputStream zipStream = new ZipOutputStream(outStream);
        try {
            for (KmlLayer kmlLayer : kmlLayers) {
                zipStream.putNextEntry(new ZipEntry(kmlLayer.getName()));
                ImageEncoder encoder = ImageCodec.createImageEncoder(IMAGE_TYPE, outStream, null);
                encoder.encode(kmlLayer.getLayer());
                pm.worked(1);
            }

            zipStream.putNextEntry(new ZipEntry(legendName + ".png"));
            ImageEncoder encoder = ImageCodec.createImageEncoder(IMAGE_TYPE, outStream, null);
            encoder.encode(legend);

            zipStream.putNextEntry(new ZipEntry(OVERLAY_KML));

            final StringBuilder kmlContent = new StringBuilder();
            kmlContent.append(KmlFormatter.createHeader());
            kmlContent.append(KmlFormatter.createPlacemarks(placemarks));
            kmlContent.append(KmlFormatter.createOverlays(kmlLayers));
            kmlContent.append(KmlFormatter.createLegend(legend, legendName));
            kmlContent.append(KmlFormatter.createFooter());

            outStream.write(kmlContent.toString().getBytes());
            pm.worked(1);

        } finally {
            zipStream.close();
        }

    }

    public void addLayer(String name, RenderedImage layer, BoundingBox latLonBox) {
        kmlLayers.add(new KmlLayer(name, layer, latLonBox));
    }

    public void setLegend(String name, RenderedImage imageLegend) {
        legendName = name;
        legend = imageLegend;
    }

    public int getLayerCount() {
        return kmlLayers.size();
    }

}
