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
 * Time: 16:39:45
 */
public class MedspirationXmlTimeHandlerTest {

    private MedspirationXmlTimeHandler timeHandler;

    @Before
    public void setUp() {
        timeHandler = new MedspirationXmlTimeHandler();
    }

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

    public void fileNameParsingTestOfWrongFile() throws ParseException {
        final ProductData.UTC[] dates = timeHandler.parseTimeFromFileName("someProductWhichIsNoBAE-ASCII-Product.zip");
        assertEquals(null, dates);
    }

    @Test
    public void parseTimePerPixelTest() throws IOException, ParseException, URISyntaxException {
        final String path = "Example_XML_formatted_MDB_record.xml";
        final URL resource = MedspirationXmlTimeHandlerTest.class.getResource(path);
        final File file = new File(resource.toURI());
        final Map<PixelPos, ProductData.UTC[]> map = timeHandler.createPixelToDateMap(file);
        assertNotNull(map);
        assertEquals(4, map.values().size());
    }

}
