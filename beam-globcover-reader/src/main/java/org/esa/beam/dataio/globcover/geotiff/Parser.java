package org.esa.beam.dataio.globcover.geotiff;

import java.io.InputStream;

interface Parser {

    LegendClass[] parse(InputStream inputStream, boolean isRegional);
}
