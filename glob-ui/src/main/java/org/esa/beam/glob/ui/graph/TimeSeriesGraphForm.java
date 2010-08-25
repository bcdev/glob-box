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

package org.esa.beam.glob.ui.graph;

import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.export.text.ExportTimeBasedText;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


class TimeSeriesGraphForm {


    private final JComponent mainPanel;
    private final AbstractButton showTimeSeriesForSelectedPinsButton;
    private final AbstractButton showTimeSeriesForAllPinsButton;
    private final AbstractButton exportTimeSeriesButton;
//    private final AbstractButton filterButton;

    TimeSeriesGraphForm(JFreeChart chart, Action showSelectedPinsAction, Action showAllPinsAction,
                        final String helpID) {
        mainPanel = new JPanel(new BorderLayout(4, 4));
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(300, 200));

        mainPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        mainPanel.add(chartPanel, BorderLayout.CENTER);
        mainPanel.setPreferredSize(new Dimension(320, 200));

//        filterButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Filter24.gif"),
//                                                      false);
//        filterButton.setName("filterButton");
//        filterButton.setToolTipText("Choose which variables to display");
//        filterButton.setEnabled(true);
//        filterButton.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                choose variables to display
//            }
//        });
        //////////////////////////////////////////
        showTimeSeriesForSelectedPinsButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/SelectedPinSpectra24.gif"), true);
        showTimeSeriesForSelectedPinsButton.addActionListener(showSelectedPinsAction);
        showTimeSeriesForSelectedPinsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowingAllPins()) {
                    showTimeSeriesForAllPinsButton.setSelected(false);
                }
            }
        });
        showTimeSeriesForSelectedPinsButton.setName("showTimeSeriesForSelectedPinsButton");
        showTimeSeriesForSelectedPinsButton.setToolTipText("Show time series for selected pin");
        //////////////////////////////////////////
        showTimeSeriesForAllPinsButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/PinSpectra24.gif"), true);
        showTimeSeriesForAllPinsButton.addActionListener(showAllPinsAction);
        showTimeSeriesForAllPinsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowingSelectedPins()) {
                    showTimeSeriesForSelectedPinsButton.setSelected(false);
                }
            }
        });
        showTimeSeriesForAllPinsButton.setName("showTimeSeriesForAllPinsButton");
        showTimeSeriesForAllPinsButton.setToolTipText("Show time series for all pins");
        //////////////////////////////////////////
        exportTimeSeriesButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Export24.gif"),
                false);
        exportTimeSeriesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final VisatApp app = VisatApp.getApp();
                final ProductSceneView view = app.getSelectedProductSceneView();
                if (view != null &&
                    view.getProduct() != null &&
                    view.getProduct().getProductType().equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE) &&
                    TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct()) != null) {

                    AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct());
                    if (timeSeries != null) {
                        ExportTimeBasedText.export(mainPanel, timeSeries, helpID);
                    }
                }
            }
        });
        exportTimeSeriesButton.setToolTipText("Export time series of all pins");
        exportTimeSeriesButton.setName("exportTimeSeriesButton");
        final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (sceneView != null) {
            exportTimeSeriesButton.setEnabled(sceneView.getProduct().getPinGroup().getNodeCount() > 0);
        } else {
            exportTimeSeriesButton.setEnabled(false);
        }

        AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"), false);
        helpButton.setToolTipText("Help");

        JPanel buttonPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.top = 2;
        gbc.gridy = 0;
//        buttonPanel.add(filterButton, gbc);
//        gbc.gridy++;
        buttonPanel.add(showTimeSeriesForSelectedPinsButton, gbc);
        gbc.gridy++;
        buttonPanel.add(showTimeSeriesForAllPinsButton, gbc);
        gbc.gridy++;
        buttonPanel.add(exportTimeSeriesButton, gbc);
        gbc.gridy++;
        gbc.insets.bottom = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonPanel.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridy = 10;
        gbc.anchor = GridBagConstraints.EAST;
        buttonPanel.add(helpButton, gbc);

        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPanel.add(BorderLayout.EAST, buttonPanel);

        if (helpID != null) {
            HelpSys.enableHelpOnButton(helpButton, helpID);
            HelpSys.enableHelpKey(buttonPanel, helpID);
        }
    }

    JComponent getControl() {
        return mainPanel;
    }

    void setButtonsEnabled(boolean enabled) {
//        filterButton.setEnabled(enabled);
        showTimeSeriesForSelectedPinsButton.setEnabled(enabled);
        showTimeSeriesForAllPinsButton.setEnabled(enabled);
    }

    boolean isShowingSelectedPins() {
        return showTimeSeriesForSelectedPinsButton.isSelected();
    }

    boolean isShowingAllPins() {
        return showTimeSeriesForAllPinsButton.isSelected();
    }

    public void setExportEnabled(boolean placemarksSet) {
        exportTimeSeriesButton.setEnabled(placemarksSet);
    }
}
