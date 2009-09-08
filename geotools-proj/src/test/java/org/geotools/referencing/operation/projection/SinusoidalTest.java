package org.geotools.referencing.operation.projection;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.parameter.ParameterGroup;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;


public final class SinusoidalTest {
    // Tolerance for test when units are degrees.
    private static final double[] TOL_DEG = {1.0E-6, 1.0E-6};

    // Tolerance for test when units are metres.
    private static final double[] TOL_M = {1.0E-2, 1.0E-2};

    private Sinusoidal.Provider provider;

    @Before
    public void setUp() {
        provider = new Sinusoidal.Provider();
    }

    @After
    public void tearDown() {
        provider = null;
    }

    @Test
    public void testSinusoidal() throws TransformException {

        MathTransform transform;

        ParameterValueGroup params = new ParameterGroup(provider.getParameters());
        params.parameter("semi_major")         .setValue(DefaultEllipsoid.WGS84.getSemiMajorAxis());
        params.parameter("semi_minor")         .setValue(DefaultEllipsoid.WGS84.getSemiMinorAxis());
        params.parameter("central_meridian")   .setValue(  0.000);
        params.parameter("false_easting")      .setValue(  0.0  );
        params.parameter("false_northing")     .setValue(  0.0  );

        transform = provider.createMathTransform(params);
        doTransform(new DirectPosition2D(-2.5, 51.37),
                    new DirectPosition2D(-173738.75446886677, 5718482.242050462), transform);
    }

    /*
     * Check if two coordinate points are equals, in the limits of the specified
     * tolerance vector.
     *
     * @param expected  The expected coordinate point.
     * @param actual    The actual coordinate point.
     * @param tolerance The tolerance vector. If this vector length is smaller than the number
     *                  of dimension of <code>actual</code>, then the last tolerance value will
     *                  be reused for all extra dimensions.
     */
    private static void assertPositionEquals(final DirectPosition expected,
                                             final DirectPosition actual,
                                             final double[]       tolerance) {
        final int dimension = actual.getDimension();
        final int lastToleranceIndex = tolerance.length-1;
        assertEquals("The coordinate point doesn't have the expected dimension",
                     expected.getDimension(), dimension);
        for (int i=0; i<dimension; i++) {
            final String message = String.format("Mismatch for ordinate %d (zero-based):", i);
            assertEquals(message,
                         expected.getOrdinate(i), actual.getOrdinate(i),
                         tolerance[Math.min(i, lastToleranceIndex)]);
        }
    }

    /*
     * Helper method to test transform from a source to a target point.
     * Coordinate points are (x,y) or (long, lat)
     */
    private static void doTransform(DirectPosition source,
                                    DirectPosition target,
                                    MathTransform transform)
            throws TransformException
    {
        doTransform(source, target, transform, TOL_M);
    }

    /*
     * Helper method to test transform from a source to a target point.
     * Coordinate points are (x,y) or (long, lat)
     */
    private static void doTransform(DirectPosition source,   DirectPosition target,
                                    MathTransform transform, final double[] tolerance)
            throws TransformException
    {
        DirectPosition calculated;
        calculated = transform.transform(source, null);
        assertPositionEquals(target, calculated, tolerance);

        // The inverse
        target = source;
        source = calculated;
        calculated = transform.inverse().transform(source, null);
        assertPositionEquals(target, calculated, TOL_DEG);
    }

}