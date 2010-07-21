package org.esa.beam.glob.export.netcdf;

import org.junit.Test;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;

public class GlobNetCdfWriterTest {

    @Test
    public void testGlobNetWriter() throws IOException, InvalidRangeException {
        final GlobNetCdfWriter writer = new GlobNetCdfWriter();
        writer.createData();
        writer.createHeader();
        writer.write();
    }

}
