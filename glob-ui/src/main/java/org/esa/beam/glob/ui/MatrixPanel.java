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

/**
 * @author Thomas Storm
 */
public class MatrixPanel extends JPanel {


    public MatrixPanel(int cols) {
        setLayout(new TableLayout(cols));
        for (int i = 0; i < cols * cols; i++) {
            add(new JPanel());
        }
    }

    void setValues(String[][] values) {
        for (int i = 0; i < values.length; i++) {
            String[] value = values[i];
            for (int j = 0; j < value.length; j++) {
                String v = value[j];
                int compIndex = i * (value.length - 1) + i + j;
                final JPanel currentPanel = (JPanel) getComponent(compIndex);
                currentPanel.removeAll();
                currentPanel.add(new JLabel(v));
            }
        }
    }

}
