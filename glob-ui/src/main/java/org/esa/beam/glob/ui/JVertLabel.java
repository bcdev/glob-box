package org.esa.beam.glob.ui;

import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

/**
 * User: Thomas Storm
 * Date: 30.06.2010
 * Time: 10:57:31
 */
class JVertLabel extends JLabel {

    private static final double THETA_PLUS_90 = Math.toRadians(90.0);

    public JVertLabel(String s) {
        super(s);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        //noinspection SuspiciousNameCombination
        return new Dimension(dim.height, dim.width);
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        final Object oldValue = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform oldTransform = g2d.getTransform();
        g2d.rotate(THETA_PLUS_90);
        final int w = getWidth();
        final int h = getHeight();
        g2d.translate(h / 2 - w / 2, -h + w / 2);

        super.paintComponent(g2d);

        g2d.setTransform(oldTransform);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldValue);
    }
}