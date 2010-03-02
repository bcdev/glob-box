package org.esa.beam.dataio.globcover;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;

import java.io.File;

public class GlobCoverMosaicReaderPlugIn extends AbstractGlobCoverReaderPlugIn {

    public GlobCoverMosaicReaderPlugIn() {
        super("GLOBCOVER-L3-MOSAIC", "GlobCover Bimonthly or Annual MERIS Mosaic");
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file = new File(String.valueOf(input));
        if (file.getName().startsWith(FILE_PREFIX)) {
            return DecodeQualification.SUITABLE;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new GlobCoverMosaicProductReader(this);
    }

}