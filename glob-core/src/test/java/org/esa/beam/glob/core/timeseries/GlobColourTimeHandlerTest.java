package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.glob.core.TimeCoding;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 01.04.2010
 * Time: 15:01:58
 */
public class GlobColourTimeHandlerTest {

    @Test
    public void testTimeCodingGeneration() throws ParseException, IOException {

        final ProductData.UTC startTime = ProductData.UTC.parse("05_07_1982_14:53:22", "dd_MM_yyyy_hh:mm:ss");
        final ProductData.UTC endTime = ProductData.UTC.parse("06 08 2065 15 54 23", "dd MM yyyy hh mm ss");

        Product dummyProduct = new Product("testProd", "super product", 10, 20);
        Band band = new Band("test", ProductData.TYPE_INT16, 10, 20);
        dummyProduct.addBand(band);
        dummyProduct.setStartTime(startTime);
        dummyProduct.setEndTime(endTime);

        TimeHandler timeHandler = new TimeHandler();
        final TimeCoding timeCoding = timeHandler.generateTimeCoding(band);

        assertNotNull(timeCoding);

        assertEquals(startTime.getMicroSecondsFraction(), timeCoding.getStartTime().getMicroSecondsFraction());
        assertEquals(endTime.getMicroSecondsFraction(), timeCoding.getEndTime().getMicroSecondsFraction());

//        final ProductData.UTC date = timeCoding.getDatesAtPixel(new PixelPos(5.0f, 10.0f))[0];
//        assertEquals(startTime.getMicroSecondsFraction(), date.getMicroSecondsFraction());
//
//        final ProductData.UTC[] wrongDate = timeCoding.getDatesAtPixel(new PixelPos(10.0f, 10.0f));
//        assertNull(wrongDate);
    }

}
