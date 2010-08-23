package org.esa.beam.dataio.envi;

import org.esa.beam.util.Debug;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnviCrsFactory {

    private static final HashMap<Integer, String> projectionNameMap;
    private static final HashMap<Integer, Map<String, Integer>> parameterMaps;

    static {
        projectionNameMap = new HashMap<Integer, String>();
        parameterMaps = new HashMap<Integer, Map<String, Integer>>();

        projectionNameMap.put(9, "Albers_Equal_Area_Conic");
        final HashMap<String, Integer> params = new HashMap<String, Integer>();
        params.put("semi_major", 0);
        params.put("semi_minor", 1);
        params.put("latitude_of_origin", 2);
        params.put("central_meridian", 3);
        params.put("standard_parallel_1", 6);
        params.put("standard_parallel_2", 7);
        params.put("false_easting", 4);
        params.put("false_northing", 5);
        parameterMaps.put(9, params);
    }

    private EnviCrsFactory() {
    }


    public static CoordinateReferenceSystem createCrs(int enviProjectionNumber, double[] parameter) {
        if (enviProjectionNumber == 9) {
            try {
                // Albers Equal Area Conic
                final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
                final ParameterValueGroup parameters = transformFactory.getDefaultParameters("EPSG:9822");
                final Map<String, Integer> map = parameterMaps.get(enviProjectionNumber);
                final List<GeneralParameterDescriptor> parameterDescriptors = parameters.getDescriptor().descriptors();
                for (GeneralParameterDescriptor parameterDescriptor : parameterDescriptors) {
                    final String paramName = parameterDescriptor.getName().getCode();
                    final Integer index = map.get(paramName);
                    parameters.parameter(paramName).setValue(parameter[index]);
                }

                MathTransform mathTransform = transformFactory.createParameterizedTransform(parameters);

                return new DefaultProjectedCRS(parameters.getDescriptor().getName().getCode() + " / WGS84",
                                               DefaultGeographicCRS.WGS84,
                                               mathTransform, DefaultCartesianCS.PROJECTED);
            } catch (FactoryException fe) {
                Debug.trace(fe);
            }
        }
        throw new IllegalArgumentException(String.format("Unknown ENVI projection number: %d", enviProjectionNumber));
    }

}
