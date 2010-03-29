package org.esa.beam.glob.export.kmz;

import java.awt.image.RenderedImage;

public class KmlScreenOverlay extends KmlOverlay {

    protected KmlScreenOverlay(String name, RenderedImage overlay) {
        super(name, overlay);
    }

    @Override
    protected String getKmlElementName() {
        return "ScreenOverlay";
    }

    @Override
    protected void createKmlSpecifics(StringBuilder sb) {
        super.createKmlSpecifics(sb);
        sb.append("<overlayXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\" />");
        sb.append("<screenXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\" />");
    }
}
