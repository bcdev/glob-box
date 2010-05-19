package org.esa.beam.dataio.arcbin;

import jxl.read.biff.BiffException;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import static junit.framework.Assert.*;

public class DbfReadingTest {

    @Before
    public void setup() {
    }

    @Test
    public void testLegend1() throws BiffException, IOException, URISyntaxException {
        final FileChannel channel = new FileInputStream(
                new File(DbfReadingTest.class.getResource("test_legend_1.dbf").toURI())).getChannel();
        final DbaseFileReader reader = new DbaseFileReader(channel, true, Charset.defaultCharset());

        final DbaseFileHeader dbfHeader = reader.getHeader();
        final int fieldCount = dbfHeader.getNumFields();
        assertEquals(5, fieldCount); // last three are empty
        assertEquals(Integer.class, dbfHeader.getFieldClass(0));
        assertEquals(String.class, dbfHeader.getFieldClass(1));

        try {
            testRow(reader, 0, "No Data");
            testRow(reader, 1, "Tropical Broadleaved Forest");
            testRow(reader, 2, "Fragmented Tropical Broadleav");
            testRow(reader, 3, "Cultivated and Managed");
            testRow(reader, 4, "Water");
            testRow(reader, 5, "Mangroves");
        } finally {
            reader.close();
            channel.close();
        }
    }

    @Test
    public void testLegend2() throws BiffException, IOException, URISyntaxException {
        final FileChannel channel = new FileInputStream(
                new File(DbfReadingTest.class.getResource("test_legend_2.dbf").toURI())).getChannel();
        final DbaseFileReader reader = new DbaseFileReader(channel, true, Charset.defaultCharset());

        final DbaseFileHeader dbfHeader = reader.getHeader();
        final int fieldCount = dbfHeader.getNumFields();
        assertEquals(5, fieldCount); // last three are empty
        assertEquals(Integer.class, dbfHeader.getFieldClass(0));
        assertEquals(String.class, dbfHeader.getFieldClass(1));

        try {
            testRow(reader, 0, "Unclassified");
            testRow(reader, 1, "Evergreen forest");
            testRow(reader, 2, "");
            testRow(reader, 3, "");
            testRow(reader, 4, "");
            testRow(reader, 5, "");
        } finally {
            reader.close();
            channel.close();
        }
    }

    private void testRow(DbaseFileReader reader, int value, String name ) throws IOException {
        DbaseFileReader.Row row = reader.readRow();
        final Object actual = row.read(0);
        assertEquals( Double.class, actual.getClass() );
        assertEquals(value, ((Double)actual).intValue());
        assertEquals(name, (String) row.read(1));
    }
}
