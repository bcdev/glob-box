package org.esa.beam.dataio.globcover.geotiff;

import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class LegendParser {

    private LegendParser() {
    }

    static LegendClass[] parse(File inputFile, boolean isRegional) {
        if (".xls".equalsIgnoreCase(FileUtils.getExtension(inputFile))) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(inputFile);
                return new XlsLegendParser().parse(inputStream, isRegional);
            } catch (FileNotFoundException e) {
                Debug.trace(e);
            }finally {
                if(inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        return new LegendClass[0];
    }

}
