package org.esa.beam.glob.core.timeseries.parser;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.glob.core.timeseries.GlobCoverTimeHandler;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 01.04.2010
 * Time: 10:39:56
 */
public class GlobCoverTimeHandlerTest {

    private GlobCoverTimeHandler timeHandler;

    @Before
    public void setUp() {
        timeHandler = new GlobCoverTimeHandler();
    }

    @Test
    public void fileNameParsingTestTif() throws ParseException {
        final ProductData.UTC[] dates = timeHandler.parseTimeFromFileName("GLOBCOVER_200412_200606" +
                                                                          "_V2.2_Global_CLA.tif");
        final ProductData.UTC startTime = ProductData.UTC.parse("12 2004", "MM yyyy");
        final ProductData.UTC endTime = ProductData.UTC.parse("06 2006", "MM yyyy");

        assertEquals(startTime.getMicroSecondsFraction(), dates[0].getMicroSecondsFraction());
        assertEquals(endTime.getMicroSecondsFraction(), dates[1].getMicroSecondsFraction());
    }

    @Test
    public void fileNameParsingTestHdf() throws ParseException {
        final ProductData.UTC[] dates = timeHandler.parseTimeFromFileName("GLOBCOVER-L3_MOSAIC_1982_V2.3_" +
                                                                          "ANNUAL_H[00]V[71].hdf");
        final ProductData.UTC startTime = ProductData.UTC.parse("1982", "yyyy");

        assertEquals(startTime.getMicroSecondsFraction(), dates[0].getMicroSecondsFraction());
    }

    @Test
    public void fileNameParsingTestHdfWithStartAndEndDate() throws ParseException {
        final ProductData.UTC[] dates = timeHandler.parseTimeFromFileName("GLOBCOVER-L3_MOSAIC_198207" +
                                                                          "-201003_V2.3_ANNUAL_H[00]V[71].hdf");
        final ProductData.UTC startTime = ProductData.UTC.parse("07 1982", "MM yyyy");
        final ProductData.UTC endTime = ProductData.UTC.parse("03 2010", "MM yyyy");

        assertEquals(startTime.getMicroSecondsFraction(), dates[0].getMicroSecondsFraction());
        assertEquals(endTime.getMicroSecondsFraction(), dates[1].getMicroSecondsFraction());
    }

    @Test
    public void fileNameParsingTestOfWrongFile() throws ParseException {
        final ProductData.UTC[] dates = timeHandler.parseTimeFromFileName("someProductWhichIsNoGlobcOVErProduct.tif");

        assertEquals(null, dates);
    }
}
