package org.esa.beam.glob.export.kmz;

import org.esa.beam.framework.datamodel.ProductData;
import org.opengis.geometry.BoundingBox;

import java.awt.geom.Point2D;
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
        result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        result.append("<kml xmlns=\"http://earth.google.com/kml/2.0\">");
        return result.toString();
    }

    public static String createPlacemarks(List<KmlPlacemark> placemarks) {
        StringBuilder result = new StringBuilder();
        result.append("<Document>");
        for (KmlPlacemark placemark : placemarks) {
            Point2D pos = placemark.getPos();
            if (pos != null) {
                result.append("<Placemark>");
                result.append(String.format("<name>%s</name>", placemark.getLabel()));
                result.append("<Point>");
                result.append(String.format("<coordinates>%s,%s,0</coordinates>", pos.getX(), pos.getY()));
                result.append("</Point>");
                result.append("</Placemark>");
            }
        }
        result.append("</Document>");
        return result.toString();
    }

    public static String createOverlays(List<KmlLayer> kmlLayers, boolean isTimeSeries) {
        StringBuilder result = new StringBuilder();

        if (kmlLayers.size() == 1) {
            result.append("<Document>");
            result.append(createGroundOverlay(kmlLayers.get(0), isTimeSeries));
            result.append("</Document>");
        } else {
            result.append("<Folder>");
            result.append("<name>").append("root").append("</name>");
            createOverlaysRecursive(kmlLayers, isTimeSeries, result);
            result.append("</Folder>");
        }

        return result.toString();
    }

    private static void createOverlaysRecursive(List<KmlLayer> kmlLayers, boolean isTimeSeries, StringBuilder result) {
        for (KmlLayer layer : kmlLayers) {

            if (layer.hasChildren()) {
                result.append("<Folder>");
                result.append("<name>").append(layer.getName()).append("</name>");
                createOverlaysRecursive(layer.getChildren(), isTimeSeries, result);
                result.append("</Folder>");
            } else {
                result.append(createGroundOverlay(layer, isTimeSeries));
            }
        }
    }

    private static String createGroundOverlay(KmlLayer layer, boolean isTimeSeries) {
        StringBuilder result = new StringBuilder();
        String overlayName = layer.getName();
        result.append("<GroundOverlay>");

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
            result.append("<TimeSpan>");
            result.append("<begin>").append(startTimeString).append("</begin>");
            result.append("<end>").append(endTimeString).append("</end>");
            result.append("</TimeSpan>");
        }

        result.append("<name>").append(overlayName).append("</name>");
        result.append("<Icon>").append(imageName).append(".png").append("</Icon>");
        result.append("<LatLonBox>");
        result.append("<north>").append(bbox.getMaxY()).append("</north>");
        result.append("<south>").append(bbox.getMinY()).append("</south>");
        result.append("<east>").append(bbox.getMaxX()).append("</east>");
        result.append("<west>").append(bbox.getMinX()).append("</west>");
        result.append("</LatLonBox>");
        result.append("</GroundOverlay>");

        return result.toString();
    }

    public static String createLegend(String legendName) {

        return "<Document>" 
               + "<ScreenOverlay>"
               + "<name>" + legendName + "</name>"
               + "<Icon>"
               + "<href>" + legendName + ".png</href>"
               + "</Icon>"
               + "<overlayXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\" />"
               + "<screenXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\" />"
               + "</ScreenOverlay>"
               + "</Document>";
    }

    public static String createFooter() {
        StringBuilder result = new StringBuilder();
        result.append("</kml>");
        return result.toString();
    }

}
