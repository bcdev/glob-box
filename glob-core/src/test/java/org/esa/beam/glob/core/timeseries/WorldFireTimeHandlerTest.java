package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * User: Thomas Storm
 * Date: 06.04.2010
 * Time: 14:44:49
 */
public class WorldFireTimeHandlerTest {

    @Test
    public void createPixelToDateMapTest() throws URISyntaxException, IOException, ParseException {
        final String path = "200807ALGO1.FIRE";
        final URL resource = GlobCarbonBaeAsciiHandlerTest.class.getResource(path);
        final File file = new File(resource.toURI());

        WorldFireTimeHandler handler = new WorldFireTimeHandler();
        final Map<PixelPos, ProductData.UTC[]> map = handler.createPixelToDateMap(file);

        assertNotNull(map);

        PixelPos testPixel1 = new PixelPos(19.406f, -155.083f);
        ProductData.UTC testDate1 = ProductData.UTC.parse("2008 07 01", "yyyy MM dd");
        final ProductData.UTC date1 = map.get(testPixel1)[0];
        assertEquals(date1.getMicroSecondsFraction(), testDate1.getMicroSecondsFraction());

        PixelPos testPixel2 = new PixelPos(33.927f, 12.692f);
        ProductData.UTC testDate2 = ProductData.UTC.parse("2008 07 31", "yyyy MM dd");
        final ProductData.UTC date2 = map.get(testPixel2)[0];
        assertEquals(date2.getMicroSecondsFraction(), testDate2.getMicroSecondsFraction());

        PixelPos testPixel3 = new PixelPos(19.387f, -155.089f);
        ProductData.UTC testDate3 = ProductData.UTC.parse("2000 07 01", "yyyy MM dd");
        final ProductData.UTC date3 = map.get(testPixel3)[0];
        assertEquals(date3.getMicroSecondsFraction(), testDate3.getMicroSecondsFraction());

        PixelPos doublePixel = new PixelPos(32.131f, 47.415f);
        assertEquals(2, map.get(doublePixel).length);

        PixelPos wrongPixel = new PixelPos(-999999999.000f, 7790.000f);
        final ProductData.UTC[] wrongDate = map.get(wrongPixel);
        assertNull(wrongDate);
    }


}
