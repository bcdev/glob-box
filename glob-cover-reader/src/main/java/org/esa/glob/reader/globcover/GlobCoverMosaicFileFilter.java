package org.esa.glob.reader.globcover;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.util.io.BeamFileFilter;
import static org.esa.glob.reader.globcover.GlobCoverMosaicReaderPlugIn.*;

import java.io.File;

/**
 * @author Marco Peters
* @version $ Revision $ Date $
* @since BEAM 4.7
*/
class GlobCoverMosaicFileFilter extends BeamFileFilter {

    GlobCoverMosaicFileFilter() {
        super(FORMAT_NAME, FILE_EXTENSIONS, DESCRIPTION);
    }

    @Override
    public boolean accept(File file) {
        return canDecode(file) == DecodeQualification.INTENDED;
    }

    @Override
    public boolean isCompoundDocument(File dir) {
        return isProductDir(dir);
    }

    @Override
    public FileSelectionMode getFileSelectionMode() {
        return FileSelectionMode.FILES_AND_DIRECTORIES;
    }

}
