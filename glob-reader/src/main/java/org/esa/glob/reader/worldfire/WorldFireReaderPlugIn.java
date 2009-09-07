package org.esa.glob.reader.worldfire;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

// TODO - TBD: really use the optional gif-file
// TODO - read from zip files
// TODO - consider reading zips in zip files (e.g. annual time series)

// TODO - consider changeable filename convention
/**
 * @author Marco Peters
 * @since GlobToolbox 2.0
 */
public class WorldFireReaderPlugIn implements ProductReaderPlugIn {

    private static final String FORMAT_NAME = "ATSR World Fire";
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME};
    private static final String DESCRIPTION = "ATSR2/AATSR based Global Fire Maps (Level 3)";
    private static final Class[] INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String FIRE_FILE_EXTENSION = "FIRE";
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{FIRE_FILE_EXTENSION};

    public DecodeQualification getDecodeQualification(Object input) {
        InputStream inputStream = null;
        try {
            inputStream = createInputStream(input);
            return getDecodeQualification(inputStream);
        } catch (IOException ignored) {
            return DecodeQualification.UNABLE;
        }finally {
            if(inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    public ProductReader createReaderInstance() {
        return new WorldFireReader(this);
    }

    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    public String[] getDefaultFileExtensions() {
        return DEFAULT_FILE_EXTENSIONS;
    }

    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAME, getDefaultFileExtensions(), getDescription(null));
    }

    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    private DecodeQualification getDecodeQualification(InputStream inputStream) {
        if(inputStream == null) {
            return DecodeQualification.UNABLE;
        }
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            final String line = bufferedReader.readLine();
            if (line != null && !line.isEmpty()) {
                final int columnsCount = line.split("[\\s]++").length;
                if (columnsCount == 5 || columnsCount == 6) {
                    return DecodeQualification.INTENDED;
                }
            }
            return DecodeQualification.UNABLE;
        } catch (IOException ignored) {
            return DecodeQualification.UNABLE;
        }
    }

    private InputStream createInputStream(final Object input) throws IOException {
        File inputFile = getInputFile(input);
        if (inputFile != null) {
            return new FileInputStream(inputFile);
        }
        return null;
    }

    /**
     * Converts the fiven input into a {@link File}.
     *
     * @param input The input. For allowd input types see: {@link #getInputTypes()}.
     * @return The {@link File}, or {@code null} if it could not be converted.
     */
    File getInputFile(Object input) {
        File inputFile = null;
        if (input instanceof String) {
            inputFile = new File((String) input);
        }

        if (input instanceof File) {
            inputFile = (File) input;
        }
        return inputFile;
    }


}
