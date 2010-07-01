package org.esa.beam.glob.ui;

import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


class TimeSeriesGraphForm {

    private final JComponent mainPanel;
    private final AbstractButton showTimeSeriesForSelectedPinsButton;
    private final AbstractButton showTimeSeriesForAllPinsButton;
    private final AbstractButton exportTimeSeriesButton;
    private final AbstractButton filterButton;

    TimeSeriesGraphForm(JFreeChart chart, Action showSelectedPinsAction, Action showAllPinsAction) {
        mainPanel = new JPanel(new BorderLayout(4, 4));
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(300, 200));

        mainPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        mainPanel.add(BorderLayout.CENTER, chartPanel);
        mainPanel.setPreferredSize(new Dimension(320, 200));

        filterButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Filter24.gif"),
                                                                           false);
        filterButton.setName("filterButton");
        filterButton.setToolTipText("Choose which variables to display");
        filterButton.setEnabled(true);
        filterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // choose variables to display
            }
        });
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
                // export values of graph to csv
            }
        });
        exportTimeSeriesButton.setToolTipText("Export time series graph to csv file");
        exportTimeSeriesButton.setName("exportTimeSeriesButton");

        JPanel buttonPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.top = 2;
        gbc.gridy = 0;
        buttonPanel.add(filterButton, gbc);
        gbc.gridy++;
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

        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPanel.add(BorderLayout.EAST, buttonPanel);
    }

    JComponent getControl() {
        return mainPanel;
    }

    void setButtonsEnabled(boolean enabled) {
        filterButton.setEnabled(enabled);
        showTimeSeriesForSelectedPinsButton.setEnabled(enabled);
        showTimeSeriesForAllPinsButton.setEnabled(enabled);
        exportTimeSeriesButton.setEnabled(enabled);
    }

    boolean isShowingSelectedPins() {
        return showTimeSeriesForSelectedPinsButton.isSelected();
    }

    boolean isShowingAllPins() {
        return showTimeSeriesForAllPinsButton.isSelected();
    }

}
