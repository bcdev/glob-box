/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.NumberFormatter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.ParseException;

/**
 * @author Thomas Storm
 */
public class MatrixPanel extends JPanel {


    public MatrixPanel(int size) {
        update(size);
    }

    void setValues(Color[][] colors, double[][] values) {
        if (colors.length != values.length) {
            throw new IllegalArgumentException("There must be as many colours as values.");
        }
        for (int i = 0; i < colors.length; i++) {
            Color[] color = colors[i];
            double[] value = values[i];
            for (int j = 0; j < color.length; j++) {
                int compIndex = i * (color.length - 1) + i + j;
                final TilePanel panel = (TilePanel) getComponent(compIndex);
                Color v = color[j];
                double d = value[j];
                panel.setColor(v);
                panel.setValue(d);
            }
        }
    }

    public void setMatrixSize(int matrixSize) {
        update(matrixSize);
    }

    private void update(int size) {
        removeAll();
        final TableLayout layout = new TableLayout(size);
        for (int i = 0; i < size; i++) {
            final int weight = 1 / size;
            layout.setRowWeightX(i, weight);
            layout.setRowWeightX(i, weight);
            layout.setColumnWeightX(i, weight);
            layout.setColumnWeightY(i, weight);
        }
        layout.setTablePadding(new Insets(2, 2, 2, 2));
        setLayout(layout);
        for (int i = 0; i < size * size; i++) {
            final JPanel panel = new TilePanel();
            add(panel);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Component component : getComponents()) {
            component.setEnabled(enabled);
//            if (component instanceof JPanel) {
//                ((JPanel) component).getComponent(0).setEnabled(enabled);
//            }
        }
    }

    private class TilePanel extends JPanel {

        private JLabel label;

        private Color color = Color.BLACK;

        private TilePanel() {
            label = new JLabel();
            add(label);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(color);
            g.fillRect(0, 0, this.getWidth(), this.getHeight() - label.getHeight());
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public void setValue(Double value) {
            final NumberFormatter formatter = new NumberFormatter(new DecimalFormat("000.0000"));
            try {
                final String valueText = formatter.valueToString(value);
                label.setText(valueText);
            } catch (ParseException e) {
                label.setText(value + "");
            }
        }
    }
}
