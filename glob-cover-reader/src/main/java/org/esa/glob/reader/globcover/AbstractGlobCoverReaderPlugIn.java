package org.esa.glob.reader.globcover;

import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
abstract class AbstractGlobCoverReaderPlugIn implements ProductReaderPlugIn {

    protected static final String FILE_PREFIX = "GLOBCOVER-L3_MOSAIC";
    private static final Class[] INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String[] FILE_EXTENSIONS = new String[]{".hdf"};

    private final String formatName;
    private final String description;


    protected AbstractGlobCoverReaderPlugIn(String formatName, String description) {
        this.formatName = formatName;
        this.description = description;
    }

    @Override
    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{formatName};
    }

    @Override
    public String getDescription(Locale locale) {
        return description;
    }


}
