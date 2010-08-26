/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.dataio.medspiration.profile;

import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.metadata.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfBandPart;
import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.netcdf.util.NetcdfMultiLevelImage;
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
            CfBandPart.readCfBandAttributes(variable, band);
            band.setSourceImage(new NetcdfMultiLevelImage(band, variable, ctx));
        }
    }

    private int getRasterDataType(Variable variable) {
        DataType dataType = variable.getDataType();
        int rasterDataType;
        if (dataType == DataType.BYTE) {
            rasterDataType = ProductData.TYPE_UINT8;
        } else {
            rasterDataType = DataTypeUtils.getRasterDataType(dataType, variable.isUnsigned());
        }
        return rasterDataType;
    }
}
