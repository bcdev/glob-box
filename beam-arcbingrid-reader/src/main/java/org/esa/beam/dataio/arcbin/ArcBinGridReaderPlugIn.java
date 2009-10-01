package org.esa.beam.dataio.arcbin;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

public class ArcBinGridReaderPlugIn implements ProductReaderPlugIn {

    private static final String DESCRIPTION = "Reads Arc/Info Binary Grids into BEAM";
    private static final String[] FILE_EXTENSIONS = new String[]{""};
    private static final String FORMAT_NAME = "ARC_INFO_BIN_GRID";
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME};
    private static final Class[] INPUT_TYPES = new Class[]{String.class, File.class};
    private static final BeamFileFilter FILE_FILTER = new ArcBinGridFileFilter();

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
    
    private static class ArcBinGridFileFilter extends BeamFileFilter {

        public ArcBinGridFileFilter() {
            setFormatName(FORMAT_NAMES[0]);
            setDescription(DESCRIPTION);
        }

        /**
         * Tests whether or not the given file is accepted by this filter. The default implementation returns
         * <code>true</code> if the given file is a directory or the path string ends with one of the registered extensions.
         * if no extension are defined, the method always returns <code>true</code>
         *
         * @param file the file to be or not be accepted.
         *
         * @return <code>true</code> if given file is accepted by this filter
         */
        @Override
        public boolean accept(final File file) {
            if (super.accept(file)) {
                if (file.isDirectory() || file.getName().equals(HdrAdf.FILE_NAME)) {
                    return true;
                }
            }
            return false;
        }

    }
}
