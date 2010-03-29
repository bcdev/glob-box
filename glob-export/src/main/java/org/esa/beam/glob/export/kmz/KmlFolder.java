package org.esa.beam.glob.export.kmz;

public class KmlFolder extends KmlContainer {

    protected KmlFolder(String name, String description) {
        super(name, description);
    }

    @Override
    protected String getKmlElementName() {
        return "Folder";
    }

}
