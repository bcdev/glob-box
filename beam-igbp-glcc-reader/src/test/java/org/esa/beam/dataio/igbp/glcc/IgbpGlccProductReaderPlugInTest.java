package org.esa.beam.dataio.igbp.glcc;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

public class IgbpGlccProductReaderPlugInTest {

    private IgbpGlccProductReaderPlugIn plugin;

    @Before
    public void setUp() throws Exception {
        plugin = new IgbpGlccProductReaderPlugIn();
    }

    @Test
    public void testGetDecodeQualification() throws Exception {
        assertEquals(DecodeQualification.UNABLE, plugin.getDecodeQualification( "horst" ) );
        assertEquals(DecodeQualification.UNABLE, plugin.getDecodeQualification( "gbats1_2.img" ) );

        assertEquals(DecodeQualification.INTENDED, plugin.getDecodeQualification( "gbats2_0ll.img" ) );
        assertEquals(DecodeQualification.INTENDED, plugin.getDecodeQualification( "gigbp2_0ll.img" ) );
    }
}
