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
        File file = new File(String.valueOf(input));
        if (isGridDirectory(file.getParentFile())) {
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

        @Override
        public boolean accept(final File file) {
            return file.isDirectory() || isGridDirectory(file.getParentFile());
        }
    }

    static boolean isGridDirectory(File dir) {
        if (dir == null) {
            return false;
        }
        if (!dir.isDirectory()) {
            return false;
        }
        if (ArcBinGridReader.getCaseInsensitiveFile(dir, Header.FILE_NAME) == null) {
            return false;
        }
        if (ArcBinGridReader.getCaseInsensitiveFile(dir, GeorefBounds.FILE_NAME) == null) {
            return false;
        }
        if (ArcBinGridReader.getCaseInsensitiveFile(dir, TileIndex.FILE_NAME) == null) {
            return false;
        }
        if (ArcBinGridReader.getCaseInsensitiveFile(dir, RasterDataFile.FILE_NAME) == null) {
            return false;
        }
        return true;
    }
}
