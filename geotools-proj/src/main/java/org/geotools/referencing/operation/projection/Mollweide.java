package org.geotools.referencing.operation.projection;

// J2SE dependencies

import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.NamedIdentifier;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Projection;

import java.awt.geom.Point2D;

/**
 * Javadocs describing the projection
 */
public class Mollweide extends MapProjection {

    private static final int MAX_ITER = 10;
    private static final double TOLERANCE = 1.0e-7;

    private double cx;
    private double cy;
    private double cp;

    /**
     */
    public static final class Provider extends AbstractProvider {

        /**
         * The parameters group.
         */
        static final ParameterDescriptorGroup PARAMETERS = createDescriptorGroup(new NamedIdentifier[]{
                new NamedIdentifier(Citations.OGC, "Mollweide"),
                new NamedIdentifier(Citations.GEOTIFF, "Mollweide"),
                new NamedIdentifier(Citations.GEOTOOLS, "Mollweide")
        }, new ParameterDescriptor[]{
                SEMI_MAJOR, SEMI_MINOR,
                CENTRAL_MERIDIAN, SCALE_FACTOR,
                FALSE_EASTING, FALSE_NORTHING
                // Add or remove parameters here
        });

        /**
         * Constructs a new provider.
         */
        public Provider() {
            super(PARAMETERS);
        }

        /**
         * Returns the operation type for this map projection.
         */
        @Override
        public Class<? extends Projection> getOperationType() {
            return Projection.class;
        }

        /**
         * Create a new map projection based on the parameters.
         */
        @Override
        public MathTransform createMathTransform(final ParameterValueGroup parameters)
                throws ParameterNotFoundException {
            return new Mollweide(parameters);
        }
    }

    /**
     * Constructs a new map projection from the supplied parameters.
     *
     * @param parameters The parameter values in standard units.
     *
     * @throws ParameterNotFoundException if a mandatory parameter is missing.
     */
    protected Mollweide(final ParameterValueGroup parameters)
            throws ParameterNotFoundException {
        // Fetch parameters
        super(parameters);

        double p = Math.PI / 2;
        double p2 = p + p;

        double sp = Math.sin(p);
        double r = Math.sqrt(Math.PI * 2.0 * sp / (p2 + Math.sin(p2)));
        cx = 2.0 * r / Math.PI;
        cy = r / sp;
        cp = p2 + Math.sin(p2);
    }

    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Provider.PARAMETERS;
    }

    /**
     * Transforms the specified (<var>&lambda;</var>,<var>&phi;</var>) coordinates
     * (units in radians) and stores the result in {@code ptDst} (linear distance
     * on a unit sphere).
     */
    @Override
    protected Point2D transformNormalized(double x, double y, final Point2D ptDst) throws ProjectionException {
        double k;
        int i;

        k = cp * Math.sin(y);
        for (i = MAX_ITER; i != 0; i--) {
            double v;
            y -= v = (y + Math.sin(y) - k) / (1. + Math.cos(y));
            if (Math.abs(v) < TOLERANCE) {
                break;
            }
        }
        if (i == 0) {
            y = (y < 0.0) ? -Math.PI / 2 : Math.PI / 2;
        } else {
            y *= 0.5;
        }
        double pixX = cx * x * Math.cos(y);
        double pixY = cy * Math.sin(y);

        if (ptDst != null) {
            ptDst.setLocation(pixX, pixY);
            return ptDst;
        }
        return new Point2D.Double(pixX, pixY);
    }

    /**
     * Transforms the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code ptDst}.
     */
    @Override
    protected Point2D inverseTransformNormalized(double x, double y, final Point2D ptDst)
            throws ProjectionException {
        double lat, lon;

        lat = Math.asin(y / cy);
        lon = x / (cx * Math.cos(lat));
        lat += lat;
        lat = Math.asin((lat + Math.sin(lat)) / cp);
        if (ptDst != null) {
            ptDst.setLocation(lon, lat);
            return ptDst;
        }
        return new Point2D.Double(lon, lat);
    }


    /**
     * Provides the transform equations for the spherical case
     */
    private static final class Spherical extends Mollweide {

        /**
         * Constructs a new map projection from the suplied parameters.
         *
         * @param parameters The parameter values in standard units.
         *
         * @throws ParameterNotFoundException if a mandatory parameter is missing.
         */
        protected Spherical(final ParameterValueGroup parameters)
                throws ParameterNotFoundException {
            super(parameters);
            assert isSpherical;
        }

        /**
         * Transforms the specified (<var>&lambda;</var>,<var>&phi;</var>) coordinates
         * (units in radians) and stores the result in {@code ptDst} (linear distance
         * on a unit sphere) using equations for a sphere.
         */
        @Override
        protected Point2D transformNormalized(double x, double y, Point2D ptDst)
                throws ProjectionException {
            // Compute using ellipsoidal formulas, for comparaison later.
            assert (ptDst = super.transformNormalized(x, y, ptDst)) != null;

            // Calculate forward spherical here

            assert checkTransform(x, y, ptDst);
            if (ptDst != null) {
                ptDst.setLocation(x, y);
                return ptDst;
            }
            return new Point2D.Double(x, y);
        }

        /**
         * Transforms the specified (<var>x</var>,<var>y</var>) coordinate
         * and stores the result in <code>ptDst</code> using equations for a sphere.
         */
        @Override
        protected Point2D inverseTransformNormalized(double x, double y, Point2D ptDst)
                throws ProjectionException {
            // Compute using ellipsoidal formulas, for comparaison later.
            assert (ptDst = super.inverseTransformNormalized(x, y, ptDst)) != null;

            // Calculate inverse spherical here

            assert checkInverseTransform(x, y, ptDst);
            if (ptDst != null) {
                ptDst.setLocation(x, y);
                return ptDst;
            }
            return new Point2D.Double(x, y);
        }
    }

}