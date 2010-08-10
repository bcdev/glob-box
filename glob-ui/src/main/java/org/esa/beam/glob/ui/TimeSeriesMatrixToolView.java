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
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
    private MatrixMouseWheelListener mouseWheelListener;
    private RasterDataNode currentRaster;
    private MatrixPanel matrixPanel;

    public TimeSeriesMatrixToolView() {
        pixelPosListener = new TimeSeriesPPL();
        sceneViewListener = new SceneViewListener();
        mouseWheelListener = new MatrixMouseWheelListener();
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

        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        if (view != null) {
            maySetCurrentView(view);
        }

        return panel;
    }

    @Override
    public void componentShown() {
        addMouseWheelListener();
    }

    @Override
    public void componentOpened() {
        addMouseWheelListener();
    }

    @Override
    public void componentClosed() {
        removeMouseWheelListener();
    }

    @Override
    public void componentHidden() {
        removeMouseWheelListener();
    }

    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        String startDateString = getStartDateString();
        dateLabel = new JLabel(String.format("Date: %s", startDateString));
        mainPanel.add(BorderLayout.NORTH, dateLabel);
        matrixPanel = new MatrixPanel(3);
        matrixPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        mainPanel.add(BorderLayout.CENTER, matrixPanel);
        return mainPanel;
    }

    private String getStartDateString() {
        String startDateString = "";
        if (currentView != null && timeSeries != null) {
            final TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(currentView.getRaster());
            Date startDate = timeCoding.getStartTime().getAsDate();
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
            startDateString = formatter.format(startDate);
        }
        return startDateString;
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

    /*
     * Checks if the view displays a timeseries product.
     * If so it is set as the current view.
     */

    private void maySetCurrentView(ProductSceneView view) {
        final String viewProductType = view.getProduct().getProductType();
        if (view != currentView &&
            !view.isRGB() &&
            viewProductType.equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE) &&
            TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct()) != null) {
            setCurrentView(view);
        }
    }

    private void setCurrentView(ProductSceneView newView) {
        if (currentView != null) {
            currentView.removePixelPositionListener(pixelPosListener);
            removeMouseWheelListener();
        }
        if (currentView != newView) {
            currentView = newView;
            if (currentView != null) {
                currentView.addPixelPositionListener(pixelPosListener);
                addMouseWheelListener();
                timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct());
                currentRaster = currentView.getRaster();
            } else {
                timeSeries = null;
            }
            setUIEnabled(currentView != null);
        }
    }

    private void addMouseWheelListener() {
        if (currentView != null) {
            currentView.getLayerCanvas().addMouseWheelListener(mouseWheelListener);
        }
    }

    private void removeMouseWheelListener() {
        if (currentView != null) {
            currentView.getLayerCanvas().removeMouseWheelListener(mouseWheelListener);
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
                updateMatrix(pixelX, pixelY, currentLevel);
            }
        }

        @Override
        public void pixelPosNotAvailable() {
        }
    }

    private class MatrixMouseWheelListener implements MouseWheelListener {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            RasterDataNode nextRaster = null;
            if (e.isAltDown()) {
                final String varName = AbstractTimeSeries.rasterToVariableName(currentRaster.getName());
                final List<Band> bandList = timeSeries.getBandsForVariable(varName);
                final int currentIndex = bandList.indexOf(currentRaster);

                if (e.getWheelRotation() < 0) {
                    if (currentIndex > 0) {
                        nextRaster = bandList.get(currentIndex - 1);
                    }
                    if (nextRaster == currentRaster) {
                        return;
                    }
                } else {
                    if (currentIndex + 1 < bandList.size()) {
                        nextRaster = bandList.get(currentIndex + 1);
                    }
                    if (nextRaster == currentRaster) {
                        return;
                    }
                }
            }
            if (nextRaster != null) {
                dateLabel.setText(nextRaster.getName());
                currentRaster = nextRaster;
                final Viewport viewport = currentView.getViewport();
                final ImageLayer baseLayer = currentView.getBaseImageLayer();
                final int currentLevel = baseLayer.getLevel(viewport);
                final Point mousePosition = currentView.getMousePosition();
                final AffineTransform levelZeroToModel = baseLayer.getImageToModelTransform();
                final AffineTransform modelToCurrentLevel = baseLayer.getModelToImageTransform(currentLevel);
                final Point2D modelPos = levelZeroToModel.transform(mousePosition, null);
                final Point2D currentPos = modelToCurrentLevel.transform(modelPos, null);
                updateMatrix((int) currentPos.getX(), (int) currentPos.getY(), currentLevel);
            }
        }
    }

    private void updateMatrix(int x, int y, int currentLevel) {
        final String[][] values = {
                {
                        getValue(x - 1, y - 1, currentLevel),
                        getValue(x, y - 1, currentLevel),
                        getValue(x + 1, y - 1, currentLevel)
                },
                {
                        getValue(x - 1, y, currentLevel),
                        getValue(x, y, currentLevel),
                        getValue(x + 1, y, currentLevel)
                },
                {
                        getValue(x - 1, y + 1, currentLevel),
                        getValue(x, y + 1, currentLevel),
                        getValue(x + 1, y + 1, currentLevel)
                }
        };
        matrixPanel.setValues(values);
    }

    private String getValue(int x, int y, int currentLevel) {
        if (currentLevel == -1) {
            final Viewport viewport = currentView.getViewport();
            final ImageLayer baseLayer = currentView.getBaseImageLayer();
            currentLevel = baseLayer.getLevel(viewport);
        }
        if (currentRaster.isFloatingPointType()) {
            return String.valueOf(ProductUtils.getGeophysicalSampleDouble((Band) currentRaster, x, y, currentLevel));
        } else {
            return String.valueOf(ProductUtils.getGeophysicalSampleLong((Band) currentRaster, x, y, currentLevel));
        }
//        return data.getSampleDouble(x, y, 0);
    }

}
