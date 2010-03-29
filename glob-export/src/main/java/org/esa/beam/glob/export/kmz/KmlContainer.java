package org.esa.beam.glob.export.kmz;

import java.util.ArrayList;
import java.util.List;

public abstract class KmlContainer extends KmlFeature {

    private final List<KmlFeature> children;

    protected KmlContainer(String name, String description) {
        super(name, description);
        this.children = new ArrayList<KmlFeature>();
    }

    public List<KmlFeature> getChildren() {
        return children;
    }

    public void addChild(final KmlFeature child) {
        children.add(child);
    }

    @Override
    protected void createKmlSpecifics(StringBuilder sb) {
        for (KmlFeature container : getChildren()) {
            container.createKml(sb);
        }
    }
}
