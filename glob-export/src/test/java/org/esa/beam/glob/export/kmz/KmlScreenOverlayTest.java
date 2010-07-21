package org.esa.beam.glob.export.kmz;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KmlScreenOverlayTest {
    @Test
    public void testExport() {
        KmlScreenOverlay screenOverlay = new KmlScreenOverlay("Legend", new DummyTestOpImage(2, 2));

        String expected = getExpected();
        StringBuilder builder = new StringBuilder();
        screenOverlay.createKml(builder);
        assertEquals(expected, builder.toString());

    }

    private String getExpected() {
        return "<ScreenOverlay>" +
                "<name>Legend</name>" +
                "<Icon>" +
                "<href>Legend.png</href>" +
                "</Icon>" +
                "<overlayXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\" />" +
                "<screenXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\" />" +
                "</ScreenOverlay>";
    }

}
