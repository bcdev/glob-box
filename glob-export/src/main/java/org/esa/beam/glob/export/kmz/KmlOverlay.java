package org.esa.beam.glob.export.kmz;

import java.awt.image.RenderedImage;

public abstract class KmlOverlay extends KmlFeature {

    private RenderedImage overlay;
    private String iconName;
    private static final String ICON_EXTENSION = ".png";

    protected KmlOverlay(String name, RenderedImage overlay) {
        super(name, null);
        this.overlay = overlay;
        iconName = name;
    }

    public RenderedImage getOverlay() {
        return overlay;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }
    public String getIconFileName() {
        return getIconName() + ICON_EXTENSION;
    }

    @Override
    protected void createKmlSpecifics(StringBuilder sb) {
        sb.append("<Icon>");
        sb.append("<href>").append(getIconFileName()).append("</href>");
        sb.append("</Icon>");
    }
}
