package org.esa.glob.reader.globcover;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

public class GlobCoverTileReaderPlugIn implements ProductReaderPlugIn {

    private static final String DESCRIPTION = "GlobCover Bimonthly or Annual MERIS FR Mosaic Tile";
    private static final String[] FILE_EXTENSIONS = new String[]{".hdf"};
    private static final String FILE_PREFIX = "GLOBCOVER-L3_MOSAIC";
    private static final String FORMAT_NAME = "GLOBCOVER-L3-MOSAIC-TILE";
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME};
    private static final Class[] INPUT_TYPES = new Class[]{String.class, File.class};
    private static final BeamFileFilter FILE_FILTER = new BeamFileFilter(FORMAT_NAME, FILE_EXTENSIONS, DESCRIPTION);

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file = new File(String.valueOf(input));
        if (file.getName().startsWith(FILE_PREFIX)) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new GlobCoverTileProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return FILE_FILTER;
    }
}
