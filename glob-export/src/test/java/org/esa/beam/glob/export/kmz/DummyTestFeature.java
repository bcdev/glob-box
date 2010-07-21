package org.esa.beam.glob.export.kmz;

class DummyTestFeature extends KmlFeature {
    DummyTestFeature(String name) {
        super(name, null);
    }

    @Override
    protected String getKmlElementName() {
        return "Dummy";
    }

    @Override
    protected void createKmlSpecifics(StringBuilder sb) {
        sb.append("<innerElement>");
        sb.append("some valuable information");
        sb.append("</innerElement");
    }

}
