package org.esa.glob.reader.worldfire;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.util.io.BeamFileFilter;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;


public class WorldFireReaderPlugInTest {

    private WorldFireReaderPlugIn plugIn;

    @Before
    public void setup() {
        plugIn = new WorldFireReaderPlugIn();
    }

    @Test
    public void testCreatedReaderInstance() {
        assertNotNull(plugIn.createReaderInstance());
    }

    @Test
    public void testInputTypes() {
        final Class[] classes = plugIn.getInputTypes();
        assertArrayContains(String.class, classes);
        assertArrayContains(File.class, classes);
    }

    @Test
    public void testDefaultFileExtensions() {
        assertArrayContains(".FIRE", plugIn.getDefaultFileExtensions());
    }

    @Test
    public void testFormatNames() {
        assertNotNull(plugIn.getFormatNames());
        assertTrue(plugIn.getFormatNames().length >= 1);
    }

    @Test
    public void testDescription() {
        assertNotNull("null for description not expected", plugIn.getDescription(null));
        assertFalse("empty description not expected", plugIn.getDescription(Locale.ENGLISH).isEmpty());
    }

    @Test
    public void testProductFileFilter() throws Exception {
        final BeamFileFilter beamFileFilter = plugIn.getProductFileFilter();
        assertNotNull("null not expected for file filter", beamFileFilter);

        assertTrue(beamFileFilter.accept(TestResourceHelper.getAatsrAlgo1AsFile()));
        assertTrue(beamFileFilter.accept(TestResourceHelper.getAatsrAlgo2AsFile()));
        assertTrue(beamFileFilter.accept(TestResourceHelper.getAtsr2Algo1AsFile()));
        assertTrue(beamFileFilter.accept(TestResourceHelper.getAtsr2Algo2AsFile()));

        File tempFile = File.createTempFile("file", ".FIRE");
        assertTrue(beamFileFilter.accept(tempFile));

        tempFile = File.createTempFile("file", ".butWrongExtension");
        assertFalse(beamFileFilter.accept(tempFile));
        assertFalse(beamFileFilter.accept(new File("9708ESA02.notAFile")));

    }

    @Test
    public void testDecodeQualificationFile() throws Exception {
        File input;
        input = TestResourceHelper.getAatsrAlgo1AsFile();
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(input));
        input = TestResourceHelper.getAatsrAlgo2AsFile();
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(input));
        input = TestResourceHelper.getAtsr2Algo1AsFile();
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(input));
        input = TestResourceHelper.getAtsr2Algo2AsFile();
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(input));
        input = new File("9708ESA02.notAFile");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(input));
        input = File.createTempFile("emptyFile", ".FIRE");
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(input));
    }

    @Test
    public void testDecodeQualificationString() throws IOException {
        String input;
        input = TestResourceHelper.getAatsrAlgo1AsString();
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(input));
        input = TestResourceHelper.getAatsrAlgo2AsString();
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(input));
        input = TestResourceHelper.getAtsr2Algo1AsString();
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(input));
        input = TestResourceHelper.getAtsr2Algo2AsString();
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(input));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(null));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification("notAValidPath"));
    }

    private <T> void assertArrayContains(T obj, T[] array) {
        boolean found = false;
        for (Object elem : array) {
            if(elem.equals(obj)) {
                found = true;
                break;
            }
        }
        if(!found) {
            fail(String.format("<%s> is not contained in <%s>", obj, Arrays.toString(array)));
        }
    }

}
