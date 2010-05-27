package org.esa.beam.dataio.globaerosol;

import org.esa.beam.framework.datamodel.GeoPos;
import org.junit.Test;

import static org.junit.Assert.*;

public class LatLonToIsinGridMapperTest {

    @Test
    public void testComputeN_v() throws Exception {
        final double cellSizeY = 180.0 / 2004.0;
        double lat;
        double isinGridRow = 1002.0;
        lat = cellSizeY * isinGridRow;
        assertEquals(2, LatLonToIsinGridMapper.computeN_v(lat));
        isinGridRow = 1001.0;
        lat = cellSizeY * isinGridRow;
        assertEquals(8, LatLonToIsinGridMapper.computeN_v(lat));
        isinGridRow = 1000.0;
        lat = cellSizeY * isinGridRow;
        assertEquals(14, LatLonToIsinGridMapper.computeN_v(lat));
        isinGridRow = 300.0;
        lat = cellSizeY * isinGridRow;
        assertEquals(3574, LatLonToIsinGridMapper.computeN_v(lat));
        lat = 0.0;
        assertEquals(LatLonToIsinGridMapper.N_eq, LatLonToIsinGridMapper.computeN_v(lat));
        isinGridRow = -300;
        lat = cellSizeY * isinGridRow;
        assertEquals(3574, LatLonToIsinGridMapper.computeN_v(lat));
        isinGridRow = -1001.0;
        lat = cellSizeY * isinGridRow;
        assertEquals(8, LatLonToIsinGridMapper.computeN_v(lat));
        isinGridRow = -1002.0;
        lat = cellSizeY * isinGridRow;
        assertEquals(2, LatLonToIsinGridMapper.computeN_v(lat));
    }

    @Test
    public void testComputeB_v() throws Exception {
        final double cellSizeY = 180.0 / 2004.0;
        assertEquals(0, LatLonToIsinGridMapper.computeB_v(cellSizeY * 1002));
        assertEquals(2, LatLonToIsinGridMapper.computeB_v(cellSizeY * 1001));
        assertEquals(10, LatLonToIsinGridMapper.computeB_v(cellSizeY * 1000));
        assertEquals(24, LatLonToIsinGridMapper.computeB_v(cellSizeY * 999));

        final int equatorB_v = LatLonToIsinGridMapper.computeB_v(cellSizeY * 0);
        assertEquals(2 * equatorB_v + LatLonToIsinGridMapper.N_eq - 2,
                     LatLonToIsinGridMapper.computeB_v(cellSizeY * -1002));
    }

    @Test
    public void testGeoPosToIsinGridIndex() throws Exception {
        int index = LatLonToIsinGridMapper.toIsinGridIndex( new GeoPos( 90.0f, -180.0f ) );
        index = LatLonToIsinGridMapper.toIsinGridIndex( new GeoPos( -90.0f, -180.0f ) );
        assertEquals( 0, index );
    }
}
