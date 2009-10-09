package org.esa.glob.reader.globcover;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;

import java.io.File;

public class GlobCoverTileReaderPlugIn extends AbstractGlobCoverReaderPlugIn {

    public GlobCoverTileReaderPlugIn() {
        super("GLOBCOVER-L3-MOSAIC-TILE",
              "GlobCover Bimonthly or Annual MERIS FR Tile");
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file = new File(String.valueOf(input));
        if (file.getName().startsWith(FILE_PREFIX)) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new GlobCoverTileProductReader(this);
    }

}
