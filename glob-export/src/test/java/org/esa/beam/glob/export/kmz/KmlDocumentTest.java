package org.esa.beam.glob.export.kmz;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class KmlDocumentTest {

    @Test
    public void testExportSimple() throws Exception {
        KmlDocument kmlDocument = new KmlDocument("Pluto", "Dog of Mickey");
        StringBuilder builder = new StringBuilder();
        
        kmlDocument.createKml(builder);
        String actual = builder.toString();
        assertEquals(getExpectedExportSimple(), actual);
    }

    @Test
    public void testExportWithChildren() throws Exception {
        KmlDocument kmlDocument = new KmlDocument("Pluto", "Dog of Mickey");
        kmlDocument.addChild(new DummyTestFeature("Dummy"));

        StringBuilder builder = new StringBuilder();
        kmlDocument.createKml(builder);
        String actual = builder.toString();
        assertEquals(getExpectedExportWithChildren(), actual);
    }

    private String getExpectedExportSimple() {
        return "<Document>" +
                "<name>Pluto</name>" +
                "<description>Dog of Mickey</description>" +
                "</Document>";
    }

    private String getExpectedExportWithChildren() {
        return "<Document>" +
                "<name>Pluto</name>" +
                "<description>Dog of Mickey</description>" +
                "<Dummy>" +
                "<name>Dummy</name>" +
                "<innerElement>some valuable information</innerElement" +
                "</Dummy>"+
                "</Document>";
    }

}
