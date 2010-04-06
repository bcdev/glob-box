package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.Map;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 06.04.2010
 * Time: 09:39:44
 */
public class GlobCarbonBaeAsciiHandlerTest {

    private GlobCarbonBaeAsciiHandler timeHandler;

    @Before
    public void setUp() {
        timeHandler = new GlobCarbonBaeAsciiHandler();
    }

    @Test
    public void parseTimeFromFileNameTest() throws ParseException {
        final GlobCarbonBaeAsciiHandler handler = new GlobCarbonBaeAsciiHandler();
        ProductData.UTC[] date = handler.parseTimeFromFileName("BAE_PLC_200007_ASCII_COMB.ascii");
        assertNotNull(date);
        final ProductData.UTC testDate = ProductData.UTC.parse("07 2004", "MM yyyy");
        assertEquals(testDate.getMicroSecondsFraction(), date[0].getMicroSecondsFraction());

        ProductData.UTC[] date2 = handler.parseTimeFromFileName("BAE_IGH_200107_ASCII_COMB.zip");
        assertNotNull(date2);
        final ProductData.UTC testDate2 = ProductData.UTC.parse("07 2001", "MM yyyy");
        assertEquals(testDate2.getMicroSecondsFraction(), date2[0].getMicroSecondsFraction());
    }

    @Test
    public void fileNameParsingTestOfWrongFile() throws ParseException {
        final ProductData.UTC[] dates = timeHandler.parseTimeFromFileName("someProductWhichIsNoBAE-ASCII-Product.zip");
        assertEquals(null, dates);
    }

    @Test
    public void parseTimePerPixelTest() throws IOException, ParseException, URISyntaxException {
        final String path = "BAE_PLC_200007_ASCII_COMB.ascii";
        final URL resource = GlobCarbonBaeAsciiHandlerTest.class.getResource(path);
        final File file = new File(resource.toURI());
        final Map<PixelPos, ProductData.UTC> map = timeHandler.generateTimePerPixelMap(file);
        assertNotNull(map);
        assertEquals(104187, map.values().size());
        PixelPos testPixel1 = new PixelPos(-8255.000f, 7714.000f);
        ProductData.UTC testDate1 = ProductData.UTC.parse("2000 07 27", "yyyy MM dd");
        final ProductData.UTC date1 = map.get(testPixel1);
        assertEquals(date1.getMicroSecondsFraction(), testDate1.getMicroSecondsFraction());

        PixelPos testPixel2 = new PixelPos(11067.000f, 8336.000f);
        ProductData.UTC testDate2 = ProductData.UTC.parse("2000 07 09", "yyyy MM dd");
        final ProductData.UTC date2 = map.get(testPixel2);
        assertEquals(date2.getMicroSecondsFraction(), testDate2.getMicroSecondsFraction());

        PixelPos testPixel3 = new PixelPos(19313.000f, 7790.000f);
        ProductData.UTC testDate3 = ProductData.UTC.parse("2000 07 11", "yyyy MM dd");
        final ProductData.UTC date3 = map.get(testPixel3);
        assertEquals(date3.getMicroSecondsFraction(), testDate3.getMicroSecondsFraction());

        PixelPos wrongPixel = new PixelPos(-999999999.000f, 7790.000f);
        final ProductData.UTC wrongDate = map.get(wrongPixel);
        assertNull(wrongDate);
    }
}
