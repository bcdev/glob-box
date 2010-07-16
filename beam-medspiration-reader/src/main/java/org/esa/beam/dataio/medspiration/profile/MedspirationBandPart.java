package org.esa.beam.dataio.medspiration.profile;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * The part of the medspiration profile representing the bands of the BEAM data model
 *
 * @author Thomas Storm
 */
public class MedspirationBandPart extends ProfilePart {

    @Override
    public void define(ProfileWriteContext ctx, Product p) throws IOException {
        // solely read; do nothing here
    }

    @Override
    public void read(ProfileReadContext ctx, Product p) throws IOException {
        final Variable[] variables = ctx.getRasterDigest().getRasterVariables();
        for (Variable variable : variables) {
            final Band band = p.addBand(variable.getName(), getRasterDataType(variable));
            CfBandPart.applyAttributes(band, variable);
        }
    }

    private int getRasterDataType(Variable variable) {
        DataType dataType = variable.getDataType();
        int rasterDataType;
        if (dataType == DataType.BYTE) {
            rasterDataType = ProductData.TYPE_UINT8;
        } else {
            rasterDataType = ReaderUtils.getRasterDataType(dataType, variable.isUnsigned());
        }
        return rasterDataType;
    }
}
