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
import org.esa.beam.dataio.netcdf.metadata.ProfilePart;
import org.esa.beam.dataio.netcdf.metadata.profiles.def.DefaultProfileSpi;
import org.esa.beam.framework.dataio.DecodeQualification;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

/**
 * A Service Provider Interface (SPI) for the Medspiration metadata profile.
 *
 * @author Marco Z�hlke
 * @author Thomas Storm
 */
public class MedspirationSpi extends DefaultProfileSpi {

    @Override
    public ProfileInitPart createInitialisationPart() {
        return new MedspirationInitialisationPart();
    }

    @Override
    public ProfilePart createBandPart() {
        return new MedspirationBandPart();
    }

    @Override
    public ProfilePart createFlagCodingPart() {
        return new MedspirationFlagCodingPart();
    }

    @Override
    public ProfilePart createIndexCodingPart() {
        return new MedspirationIndexCodingPart();
    }

    @Override
    public DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        Attribute gdsAttribute = netcdfFile.findGlobalAttribute("GDS_version_id");
        if (gdsAttribute != null) {
            return DecodeQualification.INTENDED;
        }

        return DecodeQualification.UNABLE;
    }
}
