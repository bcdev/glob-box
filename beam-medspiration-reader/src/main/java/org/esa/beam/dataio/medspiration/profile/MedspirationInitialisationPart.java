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

import org.esa.beam.dataio.netcdf.metadata.ProfileInitPart;
import org.esa.beam.dataio.netcdf.metadata.ProfileReadContext;
import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

import java.io.IOException;


public class MedspirationInitialisationPart implements ProfileInitPart {


    @Override
    public Product readProductBody(ProfileReadContext ctx) throws ProductIOException {
        Dimension xDim = null;
        Dimension yDim = null;
        for (Dimension dimension : ctx.getNetcdfFile().getDimensions()) {
            final String name = dimension.getName();
            if ("x".equalsIgnoreCase(name) || "lon".equalsIgnoreCase(name) || "ni".equalsIgnoreCase(name)) {
                xDim = dimension;
            } else if ("y".equalsIgnoreCase(name) || "lat".equalsIgnoreCase(name) || "nj".equalsIgnoreCase(name)) {
                yDim = dimension;
            }
        }
        if (xDim == null || yDim == null) {
            throw new ProductIOException("Illegal Dimensions: Dimensions named (x,lon,ni) and (y,lat,nj) expected.");
        }
        return new Product(
                (String) ctx.getProperty(Constants.PRODUCT_NAME_PROPERTY_NAME),
                "Medspiration",
                xDim.getLength(),
                yDim.getLength()
        );
    }

    @Override
    public void writeProductBody(NetcdfFileWriteable writeable, Product p) throws IOException {
    }
}
