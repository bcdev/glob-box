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
public class Sinusoidal extends MapProjection {

    /**
     */
    public static final class Provider extends AbstractProvider {

        /**
         * The parameters group.
         */
        static final ParameterDescriptorGroup PARAMETERS = createDescriptorGroup(new NamedIdentifier[]{
                new NamedIdentifier(Citations.OGC, "Sinusoidal"),
                new NamedIdentifier(Citations.GEOTOOLS, "CT_Sinusoidal"),
                new NamedIdentifier(Citations.ESRI, "Sinusoidal")
        }, new ParameterDescriptor[]{
                SEMI_MAJOR, SEMI_MINOR,
                CENTRAL_MERIDIAN, SCALE_FACTOR,
                FALSE_EASTING, FALSE_NORTHING
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
            return PseudoCylindricalProjection.class;
        }

        /**
         * Create a new map projection based on the parameters.
         */
        @Override
        public MathTransform createMathTransform(final ParameterValueGroup parameters)
                throws ParameterNotFoundException {
            return new Sinusoidal(parameters);
        }
    }

    /**
     * Constructs a new map projection from the supplied parameters.
     *
     * @param parameters The parameter values in standard units.
     *
     * @throws org.opengis.parameter.ParameterNotFoundException if a mandatory parameter is missing.
     */
    protected Sinusoidal(final ParameterValueGroup parameters)
            throws ParameterNotFoundException {
        super(parameters);
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
        double mapX = x * Math.cos(y);
        double mapY = y;

        if (ptDst != null) {
            ptDst.setLocation(mapX, mapY);
            return ptDst;
        }
        return new Point2D.Double(mapX, mapY);
    }

    /**
     * Transforms the specified (<var>x</var>,<var>y</var>) coordinates
     * and stores the result in {@code ptDst}.
     */
    @Override
    protected Point2D inverseTransformNormalized(double x, double y, final Point2D ptDst)
            throws ProjectionException {
        double lon = x / Math.cos(y);
        double lat = y;
        if (ptDst != null) {
            ptDst.setLocation(lon, lat);
            return ptDst;
        }
        return new Point2D.Double(lon, lat);
    }

}