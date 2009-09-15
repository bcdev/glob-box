package org.esa.glob.reader.globcover;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

public class GlobCoverMosaicReaderPlugIn implements ProductReaderPlugIn {

    private static final String DESCRIPTION = "GlobCover Bimonthly or Annual MERIS FR Mosaic Tile";
    private static final String[] FILE_EXTENSIONS = new String[]{".hdf"};
    private static final String FORMAT_NAME = "GLOBCOVER-L3-MOSAIC";
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

        if (file.getName().startsWith("GLOBCOVER-L3_MOSAIC")) {
            return DecodeQualification.INTENDED;
        }

        return DecodeQualification.UNABLE;
    }

    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    public ProductReader createReaderInstance() {
        return new GlobCoverMosaicReader(this);
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
