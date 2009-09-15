package org.esa.glob.reader.globcover;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.esa.beam.framework.datamodel.GeoPos;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class GlobCoverMosaicReaderTest {

    @Test
    public void testCreateGeoPos() {
        GeoPos geoPos;
        geoPos = GlobCoverMosaicReader.createGeoPos("012034059.567", "50012019.00000");
        assertEquals(12.583213, geoPos.getLon(), 1.0e-6);
        assertEquals(50.205280, geoPos.getLat(), 1.0e-6);
        geoPos = GlobCoverMosaicReader.createGeoPos("1005000.0", "0059059.99900");
        assertEquals(1.083333, geoPos.getLon(), 1.0e-6);
        assertEquals(0.999999, geoPos.getLat(), 1.0e-6);
        geoPos = GlobCoverMosaicReader.createGeoPos("5.00000", "2007.00000");
        assertEquals(0.001389, geoPos.getLon(), 1.0e-6);
        assertEquals(0.035278, geoPos.getLat(), 1.0e-6);
    }

}
