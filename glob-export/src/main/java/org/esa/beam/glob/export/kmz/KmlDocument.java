package org.esa.beam.glob.export.kmz;

public class KmlDocument extends KmlContainer {

    protected KmlDocument(String name, String description) {
        super(name, description);
    }

    @Override
    protected String getKmlElementName() {
        return "Document";
    }

}
