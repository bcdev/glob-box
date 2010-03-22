package org.esa.beam.glob.export;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.ProductData;
import org.opengis.geometry.BoundingBox;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 17.03.2010
 * Time: 17:21:00
 */
class KmlFormatter {

    private KmlFormatter() {
    }

    public static String createHeader() {
        StringBuilder result = new StringBuilder();
        result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        result.append("<kml xmlns=\"http://earth.google.com/kml/2.0\">\n");
        return result.toString();
    }

    public static String createPlacemarks(List<Placemark> placemarks) {
        StringBuilder result = new StringBuilder();
        for (Placemark placemark : placemarks) {
            GeoPos geoPos = placemark.getGeoPos();
            if (geoPos != null) {
                result.append("    <Placemark>\n");
                result.append(String.format("       <name>%s</name>\n", placemark.getLabel()));
                result.append("       <Point>\n");
                result.append(String.format("         <coordinates>%f,%f,0</coordinates>\n", geoPos.lon, geoPos.lat));
                result.append("       </Point>\n");
                result.append("     </Placemark>\n");
            }
        }
        return result.toString();
    }

    public static String createOverlays(List<KmlLayer> kmlLayers, boolean isTimeSeries) {
        StringBuilder result = new StringBuilder();

        if (kmlLayers.size() == 1) {
            result.append("  <Document>\n");
            result.append(createGroundOverlay(kmlLayers.get(0), isTimeSeries));
            result.append("  </Document>\n");
        } else {
            result.append("  <Folder>\n");
            result.append("    <name>").append("root").append("</name>\n");
            createOverlaysRecursive(kmlLayers, isTimeSeries, result);
            result.append("  </Folder>\n");
        }

        return result.toString();
    }

    private static void createOverlaysRecursive(List<KmlLayer> kmlLayers, boolean isTimeSeries, StringBuilder result) {
        for (KmlLayer layer : kmlLayers) {

            if (layer.hasChildren()) {
                result.append("  <Folder>\n");
                result.append("    <name>").append(layer.getName()).append("</name>\n");
                createOverlaysRecursive(layer.getChildren(), isTimeSeries, result);
                result.append("  </Folder>\n");
            } else {
                result.append(createGroundOverlay(layer, isTimeSeries));
            }
        }
    }

    private static String createGroundOverlay(KmlLayer layer, boolean isTimeSeries) {
        StringBuilder result = new StringBuilder();
        String overlayName = layer.getName();
        result.append("      <GroundOverlay>\n");

        BoundingBox bbox = layer.getLatLonBox();
        String imageName = layer.getName();

        final ProductData.UTC startTime = layer.getStartTime();
        final ProductData.UTC endTime = layer.getEndTime();
        if (isTimeSeries && startTime != null && endTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
            String startTimeString = "";
            String endTimeString = "";
            sdf.setCalendar(startTime.getAsCalendar());
            startTimeString = sdf.format(startTime.getAsDate());
            sdf = new SimpleDateFormat("MMM-dd-yyyy");
            sdf.setCalendar(startTime.getAsCalendar());

            overlayName += " (" + sdf.format(startTime.getAsDate()) + ")";
            sdf.setCalendar(endTime.getAsCalendar());
            result.append("        <TimeSpan>\n");
            result.append("          <begin>").append(startTimeString).append("</begin>\n");
            result.append("          <end>").append(endTimeString).append("</end>\n");
            result.append("        </TimeSpan>\n");
        }

        result.append("        <name>").append(overlayName).append("</name>\n");
        result.append("        <Icon>").append(imageName).append(".png").append("</Icon>\n");
        result.append("        <LatLonBox>\n");
        result.append("          <north>").append(bbox.getMaxY()).append("</north>\n");
        result.append("          <south>").append(bbox.getMinY()).append("</south>\n");
        result.append("          <east>").append(bbox.getMaxX()).append("</east>\n");
        result.append("          <west>").append(bbox.getMinX()).append("</west>\n");
        result.append("        </LatLonBox>\n");
        result.append("      </GroundOverlay>\n");

        return result.toString();
    }

    public static String createLegend(String legendName) {

        return "    <ScreenOverlay>\n"
               + "      <name>" + legendName + "</name>\n"
               + "      <Icon>\n"
               + "        <href>" + legendName + ".png</href>\n"
               + "      </Icon>\n"
               + "      <overlayXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\" />\n"
               + "      <screenXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\" />\n"
               + "    </ScreenOverlay>\n";
    }

    public static String createFooter() {
        StringBuilder result = new StringBuilder();
        result.append("</kml>\n");
        return result.toString();
    }

}
