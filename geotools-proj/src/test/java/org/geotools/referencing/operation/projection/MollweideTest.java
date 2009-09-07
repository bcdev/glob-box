package org.geotools.referencing.operation.projection;

import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.geometry.DirectPosition;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.parameter.ParameterWriter;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.AbstractIdentifiedObject;

import org.junit.*;
import static org.junit.Assert.*;


public final class MollweideTest {
    /**
     * Set to {@code true} for printing some informations to standard output while
     * performing tests. Consider this field as constants after the application launch.
     */
    private static boolean VERBOSE = false;

    /** Tolerance for test when units are degrees. */
    private final static double[] TOL_DEG = {1E-6, 1E-6};

    /** Tolerance for test when units are metres. */
    private final static double[] TOL_M = {1E-2, 1E-2};

    /** factory to use to create projection transforms*/
    private MathTransformFactory mtFactory;

    /**
     * Set up common objects used by all tests.
     */
    @Before
    public void setUp() {
        mtFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
    }

    /**
     * Release common objects used by all tests.
     */
    @After
    public void tearDown() {
        mtFactory = null;
    }

    @Test
    public void testMollweide() throws FactoryException, TransformException {

        MathTransform transform;
        ParameterValueGroup params;

//        params = mtFactory.getDefaultParameters("Mollweide");
//        params.parameter("semi_major")         .setValue(6378137);
//        params.parameter("semi_minor")         .setValue(6378137);
//        params.parameter("central_meridian")   .setValue(  0.000);
//        params.parameter("standard_parallel_1").setValue(  0.000);
//        params.parameter("false_easting")      .setValue(  0.0  );
//        params.parameter("false_northing")     .setValue(  0.0  );
//        transform = mtFactory.createParameterizedTransform(params);
//        if (VERBOSE) {
//            System.out.println(transform);
//        }
//        doTransform(new DirectPosition2D(-2.5, 51.37),
//                    new DirectPosition2D(-278298.73, 5718482.24), transform);
    }

    /**
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
                                             final double[]       tolerance)
    {
        final int dimension = actual.getDimension();
        final int lastToleranceIndex = tolerance.length-1;
        assertEquals("The coordinate point doesn't have the expected dimension",
                     expected.getDimension(), dimension);
        for (int i=0; i<dimension; i++) {
            assertEquals("Mismatch for ordinate "+i+" (zero-based):",
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
