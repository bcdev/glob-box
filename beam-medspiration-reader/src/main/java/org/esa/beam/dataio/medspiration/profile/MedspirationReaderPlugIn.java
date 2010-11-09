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

import org.esa.beam.dataio.netcdf.AbstractNetCdfReaderPlugIn;
import org.esa.beam.dataio.netcdf.metadata.ProfileInitPartReader;
import org.esa.beam.dataio.netcdf.metadata.ProfilePartReader;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfDescriptionPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfGeocodingPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfMetadataPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfNetCdfReaderPlugIn;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfTiePointGridPart;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfTimePart;
import org.esa.beam.dataio.netcdf.util.RasterDigest;
import org.esa.beam.framework.dataio.DecodeQualification;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.util.Locale;

public class MedspirationReaderPlugIn extends CfNetCdfReaderPlugIn {

    @Override
    public DecodeQualification getDecodeQualification(NetcdfFile netcdfFile) {
        Attribute gdsAttribute = netcdfFile.findGlobalAttribute("GDS_version_id");
        if (gdsAttribute != null) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{"Medspiration"};
    }

    @Override
    public String getDescription(Locale locale) {
        return "Medspiration products";
    }

    @Override
    public ProfileInitPartReader createInitialisationPartReader() {
        return new MedspirationInitialisationPart();
    }

    @Override
    public ProfilePartReader createBandPartReader() {
        return new MedspirationBandPart();
    }

    @Override
    public ProfilePartReader createFlagCodingPartReader() {
        return new MedspirationFlagCodingPart();
    }

    @Override
    public ProfilePartReader createIndexCodingPartReader() {
        return new MedspirationIndexCodingPart();
    }
}
