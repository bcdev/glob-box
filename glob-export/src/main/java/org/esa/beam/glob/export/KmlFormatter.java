package org.esa.beam.glob.export;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.image.RenderedImage;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 17.03.2010
 * Time: 17:21:00
 */
class KmlFormatter {

    static String formatKML(RasterDataNode refRaster, final List<RasterDataNode> rasterList,
                            final String legendName) {
        String description = String.format("%s (%s)", refRaster.getName(), refRaster.getUnit().replace('*', ' '));

        String legendKml =
                "    <ScreenOverlay>\n"
                + "      <name>Legend</name>\n"
                + "      <Icon>\n"
                + "        <href>" + legendName + ".png</href>\n"
                + "      </Icon>\n"
                + "      <overlayXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\" />\n"
                + "      <screenXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\" />\n"
                + "    </ScreenOverlay>\n";

        String pinKml = "";
        final Product product = refRaster.getProduct();
        ProductNodeGroup<Placemark> pinGroup = product.getPinGroup();
        Placemark[] pins = pinGroup.toArray(new Placemark[pinGroup.getNodeCount()]);
        for (Placemark placemark : pins) {
            GeoPos geoPos = placemark.getGeoPos();
            if (geoPos != null && product.containsPixel(placemark.getPixelPos())) {
                pinKml += String.format(
                        "<Placemark>\n"
                        + "  <name>%s</name>\n"
                        + "  <Point>\n"
                        + "    <coordinates>%f,%f,0</coordinates>\n"
                        + "  </Point>\n"
                        + "</Placemark>\n",
                        placemark.getLabel(),
                        geoPos.lon,
                        geoPos.lat);
            }
        }

        StringBuffer result = new StringBuffer();
        result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        result.append("<kml xmlns=\"http://earth.google.com/kml/2.0\">\n");
        result.append("  <Folder>\n");
        result.append("    <name>").append(description).append("</name>\n");

        for (RasterDataNode raster : rasterList) {
            String imageName = raster.getDisplayName() + ".png";
            final ProductData.UTC startTime = raster.getProduct().getStartTime();
            final ProductData.UTC endTime = raster.getProduct().getEndTime();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
            sdf.setCalendar(startTime.getAsCalendar());
            String startTimeString = sdf.format(startTime.getAsDate());
            sdf.setCalendar(endTime.getAsCalendar());
            String endTimeString = sdf.format(endTime.getAsDate());

            sdf = new SimpleDateFormat("MMM-dd-yyyy");
            sdf.setCalendar(startTime.getAsCalendar());
            String name = raster.getDisplayName();
            name += " (" + sdf.format(startTime.getAsDate()) + ")";

            result.append("      <GroundOverlay>\n");
            result.append("        <name>").append(name).append("</name>\n");
            result.append("        <TimeSpan>\n");
            result.append("          <begin>").append(startTimeString).append("</begin>\n");
            result.append("          <end>").append(endTimeString).append("</end>\n");
            result.append("        </TimeSpan>\n");
            result.append("        <Icon>").append(imageName).append("</Icon>\n");
            result.append("        <LatLonBox>\n");
            result.append("          <north>").append(getUpperLeftLat(raster)).append("</north>\n");
            result.append("          <south>").append(getLowerRightLat(raster)).append("</south>\n");
            result.append("          <east>").append(getEastLon(raster)).append("</east>\n");
            result.append("          <west>").append(upperLeftGPLon(raster)).append("</west>\n");
            result.append("        </LatLonBox>\n");
            result.append("      </GroundOverlay>\n");
        }

        result.append(legendKml);
        result.append(pinKml);
        result.append("  </Folder>\n");
        result.append("</kml>\n");

        return result.toString();
    }

    private static float getUpperLeftLat(final RasterDataNode raster) {
        final GeoCoding geoCoding = raster.getGeoCoding();
        final PixelPos upperLeftPP = new PixelPos(0.5f, 0.5f);
        return geoCoding.getGeoPos(upperLeftPP, null).getLat();
    }

    private static float getLowerRightLat(final RasterDataNode raster) {
        final GeoCoding geoCoding = raster.getGeoCoding();
        final Product product = raster.getProduct();
        final PixelPos lowerRightPP = new PixelPos(product.getSceneRasterWidth() - 0.5f,
                                                   product.getSceneRasterHeight() - 0.5f);
        return geoCoding.getGeoPos(lowerRightPP, null).getLat();
    }

    private static float getEastLon(final RasterDataNode raster) {
        final PixelPos upperLeftPP = new PixelPos(0.5f, 0.5f);
        final GeoCoding geoCoding = raster.getGeoCoding();
        final GeoPos upperLeftGP = geoCoding.getGeoPos(upperLeftPP, null);
        final Product product = raster.getProduct();
        final PixelPos lowerRightPP = new PixelPos(product.getSceneRasterWidth() - 0.5f,
                                                   product.getSceneRasterHeight() - 0.5f);
        final GeoPos lowerRightGP = geoCoding.getGeoPos(lowerRightPP, null);
        float eastLon = lowerRightGP.getLon();
        if (upperLeftGP.getLon() > lowerRightGP.getLon()) {
            eastLon += 360;
        }
        return eastLon;
    }

    private static float upperLeftGPLon(RasterDataNode raster) {
        final GeoCoding geoCoding = raster.getGeoCoding();
        final PixelPos upperLeftPP = new PixelPos(0.5f, 0.5f);
        final GeoPos upperLeftGP = geoCoding.getGeoPos(upperLeftPP, null);
        return upperLeftGP.getLon();
    }

    public static String createHeader() {
        return null;
    }

    public static String createPlacemarks(List<Placemark> placemarks) {
        return null;
    }

    public static String createOverlays(List<KmlLayer> kmlLayers) {
        return null;
    }

    public static String createLegend(RenderedImage legend, String legendName) {
        return null;
    }

    public static String createFooter() {
        return null;
    }
}
