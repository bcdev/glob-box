package org.esa.beam.glob.core.timeseries;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.glob.export.netcdf.NetCdfConstants;
import org.esa.beam.glob.export.netcdf.NetCdfWriter;
import org.esa.beam.glob.export.netcdf.NetCdfWriterPlugIn;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import static junit.framework.Assert.*;

/**
 * User: Thomas Storm
 * Date: 01.04.2010
 * Time: 15:01:58
 */
public class GlobColourTimeHandlerTest implements NetCdfConstants {

    private static final String OUTPUT_FILE = System.getProperty("java.io.tmpdir") + System.getProperty(
            "file.separator") + "netcdfTest.nc";
    private TimeDataHandler timeHandler;

    @Before
    public void setUp() throws IOException {
        timeHandler = new TimeDataHandler();
        NetCdfWriter writer = (NetCdfWriter) new NetCdfWriterPlugIn(OUTPUT_FILE).createWriterInstance();
        writer.addGlobalAttribute("Conventions", "CF-1.0");
        writer.addGlobalAttribute("start_time", "19820705145322");
        writer.addGlobalAttribute("end_time", "20650806155423");

        writer.writeCDL();
        writer.close();
    }

    @Test
    public void testTimeCodingGeneration() throws ParseException, IOException {

        final ProductData.UTC startTime = ProductData.UTC.parse("05_07_1982_14:53:22", "dd_MM_yyyy_hh:mm:ss");
        final ProductData.UTC endTime = ProductData.UTC.parse("06 08 2065 15 54 23", "dd MM yyyy hh mm ss");

        Product dummy = new Product("testProd", "super product", 10, 20);
        Band band = new Band("test", ProductData.TYPE_INT16, 10, 20);
        dummy.addBand(band);
        dummy.setFileLocation(new File(OUTPUT_FILE));
        dummy.setStartTime(startTime);
        dummy.setEndTime(endTime);
        final TimeCoding timeCoding = timeHandler.generateTimeCoding(band);

        assertNotNull(timeCoding);

        assertEquals(startTime.getMicroSecondsFraction(), timeCoding.getStartTime().getMicroSecondsFraction());
        assertEquals(endTime.getMicroSecondsFraction(), timeCoding.getEndTime().getMicroSecondsFraction());

        final ProductData.UTC date = timeCoding.getDateAtPixel(new PixelPos(5.0f, 10.0f));
        assertEquals(startTime.getMicroSecondsFraction(), date.getMicroSecondsFraction());

        final ProductData.UTC wrongDate = timeCoding.getDateAtPixel(new PixelPos(10.0f, 10.0f));
        assertNull(wrongDate);
    }

}
