package org.esa.beam.glob.export.kmz;

import org.esa.beam.framework.datamodel.ProductData;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.assertEquals;

/**
 * User: Marco
 * Date: 25.03.2010
 */
public class KmlGroundOverlayTest {

    @Test
    public void testExportWithoutTime() {
        ReferencedEnvelope latLonBox = new ReferencedEnvelope(12, 13, -30, -31, DefaultGeographicCRS.WGS84);
        KmlGroundOverlay groundOverlay = new KmlGroundOverlay("scene", new DummyTestOpImage(3, 3), latLonBox);

        String expected = getExpectedWithoutTime();
        StringBuilder builder = new StringBuilder();
        groundOverlay.createKml(builder);
        assertEquals(expected, builder.toString());

    }

    @Test
    public void testExportWithTime() throws ParseException {
        ReferencedEnvelope latLonBox = new ReferencedEnvelope(12, 13, -30, -31, DefaultGeographicCRS.WGS84);
        ProductData.UTC startTime = ProductData.UTC.parse("10-03-1999", "dd-MM-yyyy");
        ProductData.UTC endTime = ProductData.UTC.parse("11-03-1999", "dd-MM-yyyy");
        KmlGroundOverlay groundOverlay = new KmlGroundOverlay("scene", new DummyTestOpImage(3, 3),
                                                              latLonBox, startTime, endTime);
        String expected = getExpectedWithTime();
        StringBuilder builder = new StringBuilder();
        groundOverlay.createKml(builder);
        assertEquals(expected, builder.toString());

    }

    private String getExpectedWithoutTime() {
        return "<GroundOverlay>" +
                "<name>scene</name>" +
                "<Icon>" +
                "<href>scene.png</href>" +
                "</Icon>" +
                "<LatLonBox>" +
                "<north>-30.0</north>" +
                "<south>-31.0</south>" +
                "<east>13.0</east>" +
                "<west>12.0</west>" +
                "</LatLonBox>" +
                "</GroundOverlay>";
    }

    private String getExpectedWithTime() {
        return "<GroundOverlay>" +
                "<name>scene</name>" +
                "<Icon>" +
                "<href>scene.png</href>" +
                "</Icon>" +
                "<TimeSpan>" +
                "<begin>1999-03-10</begin>" +
                "<end>1999-03-11</end>" +
                "</TimeSpan>" +
                "<LatLonBox>" +
                "<north>-30.0</north>" +
                "<south>-31.0</south>" +
                "<east>13.0</east>" +
                "<west>12.0</west>" +
                "</LatLonBox>" +
                "</GroundOverlay>";
    }
}
