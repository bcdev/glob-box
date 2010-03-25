package org.esa.beam.glob.export.netcdf;

import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.util.Locale;

/**
 * User: Thomas Storm
 * Date: 25.03.2010
 * Time: 08:41:59
 */
public class NetCdfWriterPlugIn implements ProductWriterPlugIn {

    private String outputLocation;

    public NetCdfWriterPlugIn(String outputLocation) {
        this.outputLocation = outputLocation;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[0];
    }

    @Override
    public String[] getFormatNames() {
        return new String[0];
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
        return new Class[0];
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new NetCdfWriter(this, outputLocation);
    }

}
