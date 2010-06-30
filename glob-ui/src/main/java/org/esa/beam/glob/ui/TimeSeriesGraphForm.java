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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


class TimeSeriesGraphForm implements PropertyChangeListener {

    private final TimeSeriesGraphModel graphModel;

    private final JComponent mainPanel;
    private final JPanel buttonPanel;
    private final ChartPanel chartPanel;
    private final AbstractButton showTimeSeriesForSelectedPinButton;

    TimeSeriesGraphForm(JFreeChart chart, TimeSeriesGraphModel graphModel, Action showPinAction) {
        this.graphModel = graphModel;
        graphModel.getPropertyChangeSupport().addPropertyChangeListener(this);

        mainPanel = new JPanel(new BorderLayout(4, 4));
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(300, 200));

        mainPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        mainPanel.add(BorderLayout.CENTER, chartPanel);
        mainPanel.setPreferredSize(new Dimension(320, 200));

        final AbstractButton filterButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Filter24.gif"),
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
//
        showTimeSeriesForSelectedPinButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/SelectedPinSpectra24.gif"), true);
        showTimeSeriesForSelectedPinButton.addActionListener(showPinAction);
        showTimeSeriesForSelectedPinButton.setName("showTimeSeriesForSelectedPinButton");
        showTimeSeriesForSelectedPinButton.setToolTipText("Show time series for selected pin");

        final AbstractButton showTimeSeriesForAllPinsButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/PinSpectra24.gif"), false);
        showTimeSeriesForAllPinsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // show time series for all pins
//                showTimeSeriesForSelectedPinButton.isSelected();
            }
        });
        showTimeSeriesForAllPinsButton.setName("showTimeSeriesForAllPinsButton");
        showTimeSeriesForAllPinsButton.setToolTipText("Show time series for all pins");

        AbstractButton exportTimeSeriesButton = ToolButtonFactory.createButton(
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

        buttonPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.top = 2;
        gbc.gridy = 0;
        buttonPanel.add(filterButton, gbc);
        gbc.gridy++;
        buttonPanel.add(showTimeSeriesForSelectedPinButton, gbc);
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
        for (Component child : buttonPanel.getComponents()) {
            child.setEnabled(enabled);
        }
    }

    void updateChart() {
        chartPanel.updateUI();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(TimeSeriesGraphModel.IS_PIN_SELECTED)) {
            showTimeSeriesForSelectedPinButton.setEnabled(graphModel.isPinSelected());
        }
    }

    boolean isShowPinSeries() {
        return showTimeSeriesForSelectedPinButton.isSelected();
    }
}
