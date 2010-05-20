package org.esa.beam.dataio.globcover.geotiff;

import org.junit.Before;
import org.junit.Test;

import java.awt.Color;
import java.io.InputStream;

import static org.junit.Assert.*;

public class XlsLegendParserTest {

    private XlsLegendParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new XlsLegendParser();
    }

    @Test
    public void testRegionalLegendParsingRegionalSheet() {
        final InputStream stream = XlsLegendParserTest.class.getResourceAsStream("regional_Legend.xls");
        final LegendClass[] regionalClasses = parser.parse(stream, true);
        assertEquals(41, regionalClasses.length);
        final LegendClass class1 = new LegendClass(11, "Post-flooding or irrigated croplands (or aquatic)", "Class_1",
                                                   new Color(170, 240, 240));
        assertLegendEquals(class1, regionalClasses[0]);

        final LegendClass class8 = new LegendClass(21, "Mosaic cropland (50-70%) / grassland or shrubland (20-50%)",
                                                   "Class_8",
                                                   new Color(220, 240, 100));
        assertLegendEquals(class8, regionalClasses[7]);
        final LegendClass class41 = new LegendClass(230, "No data (burnt areas, clouds,…)", "Class_41",
                                                    new Color(0, 0, 0));
        assertLegendEquals(class41, regionalClasses[40]);
    }

    @Test
    public void testRegionalLegendParsingGlobalSheet() {
        final InputStream stream = XlsLegendParserTest.class.getResourceAsStream("regional_Legend.xls");
        final LegendClass[] globalClasses = parser.parse(stream, false);
        assertEquals(23, globalClasses.length);
        final LegendClass class1 = new LegendClass(11, "Post-flooding or irrigated croplands (or aquatic)", "Class_1",
                                                   new Color(170, 240, 240));
        assertLegendEquals(class1, globalClasses[0]);

        final LegendClass class8 = new LegendClass(70, "Closed (>40%) needleleaved evergreen forest (>5m)", "Class_8",
                                                   new Color(0, 60, 0));
        assertLegendEquals(class8, globalClasses[7]);

        final LegendClass class20 = new LegendClass(200, "Bare areas", "Class_20", new Color(255, 245, 215));
        assertLegendEquals(class20, globalClasses[19]);

        final LegendClass class23 = new LegendClass(230, "No data (burnt areas, clouds,…)", "Class_23",
                                                    new Color(0, 0, 0));
        assertLegendEquals(class23, globalClasses[22]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGlobalLegendParsingRegionalSheet() {
        final InputStream stream = XlsLegendParserTest.class.getResourceAsStream("global_Legend.xls");
        parser.parse(stream, true);
    }

    @Test
    public void testGlobalLegendParsingGlobalSheet() {
        final InputStream stream = XlsLegendParserTest.class.getResourceAsStream("global_Legend.xls");
        final LegendClass[] globalClasses = parser.parse(stream, false);
        assertEquals(23, globalClasses.length);
        final LegendClass class1 = new LegendClass(11, "Post-flooding or irrigated croplands (or aquatic)", "Class_1",
                                                   new Color(170, 240, 240));
        assertLegendEquals(class1, globalClasses[0]);

        final LegendClass class8 = new LegendClass(70, "Closed (>40%) needleleaved evergreen forest (>5m)", "Class_8",
                                                   new Color(0, 60, 0));
        assertLegendEquals(class8, globalClasses[7]);

        final LegendClass class20 = new LegendClass(200, "Bare areas", "Class_20", new Color(255, 245, 215));
        assertLegendEquals(class20, globalClasses[19]);

        final LegendClass class23 = new LegendClass(230, "No data (burnt areas, clouds,…)", "Class_23",
                                                    new Color(0, 0, 0));
        assertLegendEquals(class23, globalClasses[22]);
    }

    private void assertLegendEquals(LegendClass expectedClass, LegendClass actualClass) {
        assertEquals(expectedClass.getName(), actualClass.getName());
        assertEquals(expectedClass.getValue(), actualClass.getValue());
        assertEquals(expectedClass.getColor(), actualClass.getColor());
        assertEquals(expectedClass.getDescription(), actualClass.getDescription());
    }
}
