package org.esa.beam.glob.export.kmz;

import com.bc.ceres.core.Assert;


public abstract class KmlFeature {

    private String name;
    private String description;

    protected KmlFeature(String name, String description) {
        Assert.notNull(name, "name");
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    protected abstract String getKmlElementName();

    public final void createKml(StringBuilder sb) {
        sb.append("<").append(getKmlElementName()).append(">");
        sb.append("<name>");
        sb.append(getName());
        sb.append("</name>");
        String description1 = getDescription();
        if (description1 != null && !description1.isEmpty()) {
            sb.append("<description>");
            sb.append(description1);
            sb.append("</description>");
        }
        createKmlSpecifics(sb);
        sb.append("</").append(getKmlElementName()).append(">");
    }

    protected abstract void createKmlSpecifics(StringBuilder sb);

}
