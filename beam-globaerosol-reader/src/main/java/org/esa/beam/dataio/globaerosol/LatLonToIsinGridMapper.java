package org.esa.beam.dataio.globaerosol;

import org.esa.beam.framework.datamodel.GeoPos;

class LatLonToIsinGridMapper {

//    private static final double EARTH_RADIUS = 6378.145;
//    private static final int TILES_AT_EQ = 4008;
//    private static final double TILE_SIZE = TILES_AT_EQ / 360;

    static final int N_eq = 4008;
    static final double R_g = N_eq / 360.0;
    static final double u_0 = 180 * R_g + 0.5;
    static final double v_0 = 90 * R_g + 0.5;

    private LatLonToIsinGridMapper() {
    }

    static int toIsinGridIndex(GeoPos gp) {
        final float lat = gp.getLat();
        final double radLat = Math.toRadians(lat);
        final double radLon = Math.toRadians(gp.getLon());
        int u = (int) Math.round( R_g * Math.cos( radLat ) * radLon + u_0 );

        int N_v = computeN_v(lat);
        int B_v = computeB_v(lat);

        return u + B_v - N_eq/2 + N_v/2;
    }

    static int computeN_v(double lat) {
        int N_v = (int) Math.ceil(Math.abs(Math.cos(Math.toRadians(lat))) * N_eq);
        /**
         * according to product spec:
         * "N_v is equal to cos(lat) * N_eq, rounded up to the nearest even integer"
         */
        if( N_v % 2 == 0 ) {
            return N_v;
        } else {
            return ++N_v;
        }
    }

    static int computeB_v( double lat) {
        int B_v = 0;
        int v = (int) Math.ceil( R_g * -lat + v_0 );
        for( int i = 1; i <= v-1; i++){
//            double latv = (i - v_0 ) / R_g;
            double latv = 90 - ((i-1) * 180.0 /2004.0);
            B_v += computeN_v( latv );
        }
        return B_v;
    }

}
