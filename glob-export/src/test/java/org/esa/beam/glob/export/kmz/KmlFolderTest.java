package org.esa.beam.glob.export.kmz;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * User: Marco
 * Date: 25.03.2010
 */
public class KmlFolderTest {

    @Test
    public void testExportSimple() throws Exception {
        KmlFolder kmlDocument = new KmlFolder("Pluto", "Dog of Mickey");
        StringBuilder builder = new StringBuilder();

        kmlDocument.createKml(builder);
        String actual = builder.toString();
        assertEquals(actual, getExpectedExportSimple());
    }

    @Test
    public void testExportWithChildren() throws Exception {
        KmlFolder kmlFolder = new KmlFolder("Pluto", "Dog of Mickey");
        kmlFolder.addChild(new KmlDocument("Tweety", "Birdy of Grandma"));
        kmlFolder.addChild(new DummyTestFeature("Dummy"));

        StringBuilder builder = new StringBuilder();
        kmlFolder.createKml(builder);
        String actual = builder.toString();
        assertEquals(actual, getExpectedExportWithChildren());
    }

    private String getExpectedExportSimple() {
        return "<Folder>" +
                "<name>Pluto</name>" +
                "<description>Dog of Mickey</description>" +
                "</Folder>";
    }

    private String getExpectedExportWithChildren() {
        return "<Folder>" +
                "<name>Pluto</name>" +
                "<description>Dog of Mickey</description>" +
                "<Document>" +
                "<name>Tweety</name>" +
                "<description>Birdy of Grandma</description>" +
                "</Document>" +
                "<Dummy>" +
                "<name>Dummy</name>" +
                "<innerElement>some valuable information</innerElement" +
                "</Dummy>"+
                "</Folder>";
    }

}
