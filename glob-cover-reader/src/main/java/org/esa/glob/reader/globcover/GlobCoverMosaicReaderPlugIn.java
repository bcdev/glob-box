package org.esa.glob.reader.globcover;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public class GlobCoverMosaicReaderPlugIn implements ProductReaderPlugIn {

    static final String DESCRIPTION = "GlobCover Bimonthly or Annual MERIS FR Mosaic";
    static final String FORMAT_NAME = "GLOBCOVER-L3-MOSAIC";
    static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME};
    static final String FILE_PREFIX = "GLOBCOVER-L3_MOSAIC";
    static final String[] FILE_EXTENSIONS = new String[0];
    private static final Class[] INPUT_TYPES = new Class[]{String.class, File.class};
    private static final BeamFileFilter FILE_FILTER = new GlobCoverMosaicFileFilter(
    );

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        return canDecode(new File(String.valueOf(input)));
    }

    public static DecodeQualification canDecode(File file) {
        if(file.isDirectory()) {
            return isProductDir(file) ? DecodeQualification.INTENDED : DecodeQualification.UNABLE;
        }else {
            return isProductDir(file.getParentFile()) ? DecodeQualification.INTENDED : DecodeQualification.UNABLE;
        }
    }

    @Override
    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new GlobCoverMosaicProductReader(this);
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

    static boolean isProductDir(File dir) {
        final File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().startsWith(FILE_PREFIX);
            }
        });
        if(files == null) {
            return false;
        }
        return files.length > 0;
    }
}