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

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.help.HelpSys;
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
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


class TimeSeriesGraphForm {


    private JComponent mainPanel;
    private AbstractButton showTimeSeriesForSelectedPinsButton;
    private AbstractButton showTimeSeriesForAllPinsButton;
    private AbstractButton exportTimeSeriesButton;
    private AbstractButton showCursorTimeSeriesButton;
    private TimeSeriesGraphModel graphModel;

    TimeSeriesGraphForm(TimeSeriesGraphModel graphModel, JFreeChart chart, Action showSelectedPinsAction, Action showAllPinsAction,
                        final String helpID) {
        this.graphModel = graphModel;
        createUI(chart, showSelectedPinsAction, showAllPinsAction, helpID);
    }

    private void createUI(JFreeChart chart, Action showSelectedPinsAction, Action showAllPinsAction, String helpID) {
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightY(1.0);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setColumnWeightX(1, 0.0);

        mainPanel = new JPanel(tableLayout);
        mainPanel.setPreferredSize(new Dimension(320, 200));


        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        JPanel buttonPanel = createButtonPanel(showSelectedPinsAction, showAllPinsAction, helpID);
        mainPanel.add(chartPanel);
        mainPanel.add(buttonPanel);
    }

    private JPanel createButtonPanel(Action showSelectedPinsAction, Action showAllPinsAction, final String helpID) {
        showTimeSeriesForSelectedPinsButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/SelectedPinSpectra24.gif"), true);
        showTimeSeriesForSelectedPinsButton.addActionListener(showSelectedPinsAction);
        showTimeSeriesForSelectedPinsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (graphModel.isShowingAllPins()) {
                    showTimeSeriesForAllPinsButton.setSelected(false);
                    graphModel.setIsShowingAllPins(false);
                }
                graphModel.setIsShowingSelectedPins(showTimeSeriesForSelectedPinsButton.isSelected());
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
                if (graphModel.isShowingSelectedPins()) {
                    showTimeSeriesForSelectedPinsButton.setSelected(false);
                    graphModel.setIsShowingSelectedPins(false);
                }
                graphModel.setIsShowingAllPins(showTimeSeriesForAllPinsButton.isSelected());
            }
        });
        showTimeSeriesForAllPinsButton.setName("showTimeSeriesForAllPinsButton");
        showTimeSeriesForAllPinsButton.setToolTipText("Show time series for all pins");
        //////////////////////////////////////////
        showCursorTimeSeriesButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("/org/esa/beam/glob/ui/icons/ViewCursor24.gif"), true);
        showCursorTimeSeriesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                graphModel.setIsShowingCursorTimeSeries(showCursorTimeSeriesButton.isSelected());
            }
        });
        showCursorTimeSeriesButton.setToolTipText("Show time series for cursor");
        showCursorTimeSeriesButton.setSelected(true);
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

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        JPanel buttonPanel = new JPanel(tableLayout);

        buttonPanel.add(showTimeSeriesForSelectedPinsButton);
        buttonPanel.add(showTimeSeriesForAllPinsButton);
        buttonPanel.add(showCursorTimeSeriesButton);
        buttonPanel.add(exportTimeSeriesButton);
        buttonPanel.add(tableLayout.createVerticalSpacer());
        buttonPanel.add(helpButton);
        if (helpID != null) {
            HelpSys.enableHelpOnButton(helpButton, helpID);
            HelpSys.enableHelpKey(buttonPanel, helpID);
        }
        return buttonPanel;
    }

    JComponent getControl() {
        return mainPanel;
    }

    void setButtonsEnabled(boolean enabled) {
//        filterButton.setEnabled(enabled);
        showTimeSeriesForSelectedPinsButton.setEnabled(enabled);
        showTimeSeriesForAllPinsButton.setEnabled(enabled);
    }

    public void setExportEnabled(boolean placemarksSet) {
        exportTimeSeriesButton.setEnabled(placemarksSet);
    }
}
