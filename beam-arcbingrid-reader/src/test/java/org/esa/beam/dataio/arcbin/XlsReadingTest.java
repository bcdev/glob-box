package org.esa.beam.dataio.arcbin;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Map;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 19.05.2010
 * Time: 17:43:52
 */
public class XlsReadingTest {

    @Test
    public void testFirstCreateXlsDescriptionMap() throws URISyntaxException, FileNotFoundException {
        File file = new File(getClass().getResource("test_1_legend.xls").toURI());
        assertNotNull(file);
        file = LegendFile.findXlsLegendFile(file);
        assertNotNull(file);
        file = new File(getClass().getResource("test_1_legend.xls").toURI());
        final Map<Integer, String> map = LegendFile.createXlsDescriptionMap(file);
        assertEquals("broadleaved evergreen forest", map.get(3));
        assertEquals("seaside wet lands", map.get(7));
        assertEquals("meadow", map.get(12));
        assertEquals("dersert", map.get(20));
        assertEquals("Forest Mosaic / Degraded Forest", map.get(24));

    }

    @Test
    public void testSecondCreateXlsDescriptionMap() throws URISyntaxException, FileNotFoundException {

        File file = new File(getClass().getResource("test_2_legend.xls").toURI());
        assertNotNull(file);
        file = LegendFile.findXlsLegendFile(file);
        assertNotNull(file);
        file = new File(getClass().getResource("test_2_legend.xls").toURI());
        final Map<Integer, String> map = LegendFile.createXlsDescriptionMap(file);
        assertEquals("", map.get(0));
        assertEquals("Open forest (Eucalyptus)", map.get(3));
        assertEquals("Grasslands with sparse shrubs (A", map.get(7));
        assertEquals("Croplands", map.get(9));
        assertEquals("Cities", map.get(12));

    }

    @Test
    public void testThirdCreateXlsDescriptionMap() throws URISyntaxException, FileNotFoundException {
        File file = new File(getClass().getResource("test_3_legend.xls").toURI());
        assertNotNull(file);
        file = LegendFile.findXlsLegendFile(file);
        assertNotNull(file);
        file = new File(getClass().getResource("test_3_legend.xls").toURI());
        final Map<Integer, String> map = LegendFile.createXlsDescriptionMap(file);
        assertEquals("Tropical and sub-tropical mountain forests , broadleaved, evergreen >1000m", map.get(3));
        assertEquals("Swamp forest and woodlands", map.get(7));
        assertEquals("Deciduous shrubland / Mosiacs of deciudous shrubcover and cropping", map.get(10));
        assertEquals("Cropland", map.get(18));
        assertEquals("Urban Areas", map.get(24));
    }
}
