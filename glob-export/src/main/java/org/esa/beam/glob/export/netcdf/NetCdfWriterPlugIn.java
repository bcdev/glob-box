package org.esa.beam.glob.export.netcdf;

import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

public class NetCdfWriterPlugIn implements ProductWriterPlugIn {

    private String outputLocation;
    public static final String NETCDF_FORMAT_NAME = "NetCDF4";
    public static final String NETCDF_FILE_EXTENSION = ".nc";

    public NetCdfWriterPlugIn(String outputLocation) {
        this.outputLocation = outputLocation;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{NETCDF_FILE_EXTENSION};
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{NETCDF_FORMAT_NAME};
    }

    @Override
    public String getDescription(Locale locale) {
        return null;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return null;
    }

    @Override
    public Class[] getOutputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new NetCdfWriter(this, outputLocation);
    }

}
