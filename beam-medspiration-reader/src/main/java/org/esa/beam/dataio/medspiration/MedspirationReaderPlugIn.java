package org.esa.beam.dataio.medspiration;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class MedspirationReaderPlugIn implements ProductReaderPlugIn {

    private static final String DESCRIPTION = "Medspiration";
    private static final String[] FILE_EXTENSIONS = new String[]{".nc", ".nc.gz"};
    private static final String FORMAT_NAME = "MEDSPIRATION";
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME};
    private static final Class[] INPUT_TYPES = new Class[]{String.class, File.class};
    private static final BeamFileFilter FILE_FILTER = new BeamFileFilter(FORMAT_NAME, FILE_EXTENSIONS, DESCRIPTION);

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file;
        if (input instanceof String) {
            file = new File((String) input);
        } else if (input instanceof File) {
            file = (File) input;
        } else {
            return DecodeQualification.UNABLE;
        }

        final NetcdfFile netcdfFile;
        try {
            netcdfFile = NetcdfFile.open(file.getAbsolutePath());
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }
        
        Attribute gdsAttribute = netcdfFile.findGlobalAttribute("GDS_version_id");
        if (gdsAttribute != null) {
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
        return new MedspirationReader(this);
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
