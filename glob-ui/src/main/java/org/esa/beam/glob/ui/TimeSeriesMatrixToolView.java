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

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.event.MouseEvent;

/**
 * @author Thomas Storm
 */
public class TimeSeriesMatrixToolView extends AbstractToolView {

    private AbstractButton configureButton;
    private AbstractButton exportTimeSeriesButton;
    private AbstractButton helpButton;
    private JLabel dateLabel;
    private ProductSceneView currentView;
    private AbstractTimeSeries timeSeries;
    private SceneViewListener sceneViewListener;
    private TimeSeriesPPL pixelPosListener;

    public TimeSeriesMatrixToolView(AbstractButton configureButton) {
        pixelPosListener = new TimeSeriesPPL();
        sceneViewListener = new SceneViewListener();
    }

    @Override
    protected JComponent createControl() {
        VisatApp.getApp().addInternalFrameListener(sceneViewListener);
        final JPanel panel = new JPanel(new BorderLayout());

        configureButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Config24.png"),
                false);

        exportTimeSeriesButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Export24.gif"),
                false);
        exportTimeSeriesButton.setToolTipText("Export values");

        helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"), false);
        helpButton.setToolTipText("Help");

        JPanel buttonPanel = createButtonPanel();

        panel.add(createMainPanel(), BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        dateLabel = new JLabel("Date: ");
        mainPanel.add(BorderLayout.NORTH, dateLabel);
        JPanel matrixPanel = new JPanel(new TableLayout(3));

        mainPanel.add(BorderLayout.CENTER, matrixPanel);
        return mainPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.top = 2;
        gbc.gridy = 0;
        buttonPanel.add(configureButton, gbc);
        gbc.gridy++;
        buttonPanel.add(exportTimeSeriesButton, gbc);
        gbc.gridy++;
        buttonPanel.add(helpButton, gbc);
        gbc.gridy++;

        gbc.insets.bottom = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonPanel.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        return buttonPanel;
    }

    private void setUIEnabled(boolean enable) {
        dateLabel.setEnabled(enable);
        configureButton.setEnabled(enable);
        helpButton.setEnabled(enable);
        exportTimeSeriesButton.setEnabled(enable);
    }

    private void setCurrentView(ProductSceneView newView) {
        if (currentView != null) {
            currentView.removePixelPositionListener(pixelPosListener);
        }
        if (currentView != newView) {
            currentView = newView;
            if (currentView != null) {
                currentView.addPixelPositionListener(pixelPosListener);
                timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct());
            } else {
                timeSeries = null;
            }
        }
    }

    private class SceneViewListener extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView view = (ProductSceneView) contentPane;
                final RasterDataNode viewRaster = view.getRaster();
                final String viewProductType = viewRaster.getProduct().getProductType();
                if (currentView != view &&
                    !view.isRGB() &&
                    viewProductType.equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE) &&
                    TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct()) != null) {
                    setCurrentView(view);
                }
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (currentView == contentPane) {
                setCurrentView(null);
            }
        }
    }

    private class TimeSeriesPPL implements PixelPositionListener {

        @Override
        public void pixelPosChanged(ImageLayer imageLayer, int pixelX, int pixelY,
                                    int currentLevel, boolean pixelPosValid, MouseEvent e) {
            if (pixelPosValid && isVisible() && currentView != null) {
            }

        }

        @Override
        public void pixelPosNotAvailable() {
        }
    }

}
