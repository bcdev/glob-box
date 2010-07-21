package org.esa.beam.glob.export.kmz;

import org.junit.Test;

import java.awt.geom.Point2D;

import static org.junit.Assert.assertEquals;

public class KmlPlacemarkTest {
    @Test
    public void testExport() throws Exception {
        KmlPlacemark kmlPlacemark = new KmlPlacemark("Pin", null, new Point2D.Double(12.5, 60.9));
        StringBuilder builder = new StringBuilder();
        kmlPlacemark.createKml(builder);

        assertEquals(getExpected(), builder.toString());
    }

    private String getExpected() {
        return "<Placemark>" +
                "<name>Pin</name>" +
                "<Point>" +
                "<coordinates>12.5,60.9,0</coordinates>" +
                "</Point>" +
                "</Placemark>"; 
    }
}
