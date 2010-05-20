package org.esa.beam.dataio.igbp.glcc;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;
import java.util.regex.Pattern;

public class IgbpGlccProductReaderPlugIn implements ProductReaderPlugIn{

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        File inputFile = new File( input.toString() );
        final String inputFileName = inputFile.getName().toLowerCase();
        if( inputFileName.matches("g(.*)2_0ll\\.img") ) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new IgbpGlccProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{"IGBP_GLCC"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".img"};
    }

    @Override
    public String getDescription(Locale locale) {
        return "IGBP GLCC 2.0 product";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions()[0], getDescription(null));
    }
}
