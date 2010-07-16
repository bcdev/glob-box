package org.esa.beam.dataio.medspiration.profile;

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
