package org.esa.beam.glob.export.netcdf;

import org.junit.Test;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;

/**
 * User: Thomas Storm
 * Date: 25.03.2010
 * Time: 16:49:34
 */
public class GlobNetCdfWriterTest {

    @Test
    public void testGlobNetWriter() throws IOException, InvalidRangeException {
        final GlobNetCdfWriter writer = new GlobNetCdfWriter();
        writer.createData();
        writer.createHeader();
        writer.write();
    }

}
