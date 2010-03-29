package org.esa.beam.glob.export.kmz;

import com.bc.ceres.core.ProgressMonitor;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class KmzExporter {

    private static final String OVERLAY_KML = "overlay.kml";
    private static final String IMAGE_TYPE = "PNG";


    public void export(KmlFeature kmlFeature, ZipOutputStream zipOutputStream, final ProgressMonitor pm) throws
                                                                                                         IOException {

        final int numOverlaysToExport = getNumOverlaysToExport(kmlFeature);
        pm.beginTask("Exporting KMZ...", numOverlaysToExport);
        try {
            exportImages(kmlFeature, zipOutputStream, pm);

            zipOutputStream.putNextEntry(new ZipEntry(OVERLAY_KML));
            final String kml = createKml(kmlFeature);
            zipOutputStream.write(kml.getBytes());
            pm.isCanceled();
        } finally {
            zipOutputStream.close();
            pm.done();
        }

    }

    static String createKml(KmlFeature kmlFeature) {
        StringBuilder result = new StringBuilder();
        result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        result.append("<kml xmlns=\"http://earth.google.com/kml/2.0\">");
        kmlFeature.createKml(result);
        result.append("</kml>");
        return result.toString();
    }

    private int getNumOverlaysToExport(KmlFeature kmlFeature) {
        int count = 0;
        if (kmlFeature instanceof KmlOverlay) {
            count++;
        }
        if (kmlFeature instanceof KmlContainer) {
            KmlContainer container = (KmlContainer) kmlFeature;
            for (KmlFeature feature : container.getChildren()) {
                count += getNumOverlaysToExport(feature);
            }
        }
        return count;
    }

    private void exportImages(KmlFeature kmlFeature, ZipOutputStream zipOutputStream, ProgressMonitor pm) throws IOException {
        if(pm.isCanceled()) {
            return;
        }
        if (kmlFeature instanceof KmlOverlay) {
            KmlOverlay overlay = (KmlOverlay) kmlFeature;
            zipOutputStream.putNextEntry(new ZipEntry(overlay.getIconFileName()));
            ImageEncoder encoder = ImageCodec.createImageEncoder(IMAGE_TYPE, zipOutputStream, null);
            encoder.encode(overlay.getOverlay());
            pm.worked(1);
        }
        if (kmlFeature instanceof KmlContainer) {
            KmlContainer container = (KmlContainer) kmlFeature;
            for (KmlFeature feature : container.getChildren()) {
                exportImages(feature, zipOutputStream, pm);
            }
        }

    }

}
