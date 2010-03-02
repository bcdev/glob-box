package org.esa.beam.dataio.worldfire;

import org.junit.Ignore;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
@Ignore
class TestResourceHelper {

    private static final String AATSR_200807_ALGO1_FIRE = "aatsr/200807ALGO1.FIRE";
    private static final String AATSR_200807_ALGO2_FIRE = "aatsr/200807ALGO2.FIRE";
    private static final String ATSR2_9708_ESA01_FIRE = "atsr2/9708ESA01.FIRE";
    private static final String ATSR2_9708_ESA02_FIRE = "atsr2/9708ESA02.FIRE";

    private TestResourceHelper() {
    }

    static File getAatsrAlgo1AsFile() {
        return getResourceAsFile(AATSR_200807_ALGO1_FIRE);
    }

    static String getAatsrAlgo1AsString() {
        return getResourceAsString(AATSR_200807_ALGO1_FIRE);
    }

    static File getAatsrAlgo2AsFile() {
        return getResourceAsFile(AATSR_200807_ALGO2_FIRE);
    }

    static String getAatsrAlgo2AsString() {
        return getResourceAsString(AATSR_200807_ALGO2_FIRE);
    }

    static File getAtsr2Algo1AsFile() {
        return getResourceAsFile(ATSR2_9708_ESA01_FIRE);
    }

    static String getAtsr2Algo1AsString() {
        return getResourceAsString(ATSR2_9708_ESA01_FIRE);
    }

    static File getAtsr2Algo2AsFile() {
        return getResourceAsFile(ATSR2_9708_ESA02_FIRE);
    }

    static String getAtsr2Algo2AsString() {
        return getResourceAsString(ATSR2_9708_ESA02_FIRE);
    }

    private static String getResourceAsString(final String resourceName) {
        try {
            return new File(getResource(resourceName).toURI()).getCanonicalPath();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static File getResourceAsFile(final String resourceName) {
        try {
            return new File(getResource(resourceName).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    private static URL getResource(final String resourceName) {
        return TestResourceHelper.class.getResource(resourceName);
    }

}
