package org.esa.beam.glob.export;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.Debug;
import org.opengis.geometry.BoundingBox;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 17.03.2010
 * Time: 17:21:00
 */
class KmlFormatter {

    private static float eastLon;
    private static float upperLeftLat;
    private static float lowerRightLat;
    private static float upperLeftGPLon;

    private KmlFormatter() {
    }

    public static String createHeader(boolean isTimeSeries, String description, String name) {
        StringBuilder result = new StringBuilder();
        result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        result.append("<kml xmlns=\"http://earth.google.com/kml/2.0\">\n");
        if (isTimeSeries) {
            result.append("  <Folder>\n");
        } else {
            result.append("  <Document>\n");
        }
        result.append("    <name>").append(name).append("</name>\n");
        result.append("   <description>").append(description).append("</description>");
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


        for (KmlLayer layer : kmlLayers) {

            BoundingBox bbox = layer.getLatLonBox();

            String imageName = layer.getName();
            String name = layer.getName();

            result.append("      <GroundOverlay>\n");
            result.append("        <name>").append(name).append("</name>\n");

            if (isTimeSeries) {
                TimedKmlLayer timedLayer = null;
                try {
                    timedLayer = (TimedKmlLayer) layer;
                } catch (final ClassCastException cce) {
                    Debug.trace(cce.getMessage());
                }
                final ProductData.UTC startTime = timedLayer.getStartTime();
                final ProductData.UTC endTime = timedLayer.getEndTime();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
                sdf.setCalendar(startTime.getAsCalendar());
                String startTimeString = sdf.format(startTime.getAsDate());
                sdf.setCalendar(endTime.getAsCalendar());
                String endTimeString = sdf.format(endTime.getAsDate());

                sdf = new SimpleDateFormat("MMM-dd-yyyy");
                sdf.setCalendar(startTime.getAsCalendar());

                name += " (" + sdf.format(startTime.getAsDate()) + ")";

                result.append("        <TimeSpan>\n");
                result.append("          <begin>").append(startTimeString).append("</begin>\n");
                result.append("          <end>").append(endTimeString).append("</end>\n");
                result.append("        </TimeSpan>\n");
            }

            result.append("        <Icon>").append(imageName).append("</Icon>\n");
            result.append("        <LatLonBox>\n");
            result.append("          <north>").append(bbox.getMaxY()).append("</north>\n");
            result.append("          <south>").append(bbox.getMinY()).append("</south>\n");
            result.append("          <east>").append(bbox.getMaxX()).append("</east>\n");
            result.append("          <west>").append(bbox.getMinX()).append("</west>\n");
            result.append("        </LatLonBox>\n");
            result.append("      </GroundOverlay>\n");
        }

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

    public static String createFooter(boolean isTimeSeries) {
        StringBuilder result = new StringBuilder();
        if (isTimeSeries) {
            result.append("  </Folder>\n");
        } else {
            result.append("  </Document>\n");
        }
        result.append("</kml>\n");
        return result.toString();
    }

}
