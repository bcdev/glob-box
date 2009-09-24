package org.esa.beam.dataio.arcbin;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

public class ArcBinGridReaderPlugIn implements ProductReaderPlugIn {

    private static final String DESCRIPTION = "Reads Arc/Info Binary Grids into BEAM";
    private static final String[] FILE_EXTENSIONS = new String[]{"hdr.adf"};
    private static final String FORMAT_NAME = "ARC_INFO_BIN_GRID";
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME};
    private static final Class[] INPUT_TYPES = new Class[]{String.class, File.class};
    private static final BeamFileFilter FILE_FILTER = new BeamFileFilter(FORMAT_NAME, FILE_EXTENSIONS, DESCRIPTION);

    public DecodeQualification getDecodeQualification(Object input) {
        final File file;
        if (input instanceof String) {
            file = new File((String) input);
        } else if (input instanceof File) {
            file = (File) input;
        } else {
            return DecodeQualification.UNABLE;
        }

        if (file.getName().equals(HdrAdf.FILE_NAME)) {
            return DecodeQualification.INTENDED;
        }

        return DecodeQualification.UNABLE;
    }

    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    public ProductReader createReaderInstance() {
        return new ArcBinGridReader(this);
    }

    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    public BeamFileFilter getProductFileFilter() {
        return FILE_FILTER;
    }
}
