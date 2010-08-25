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

package org.esa.beam.glob.ui.matrix;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
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
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @author Thomas Storm
 */
public class TimeSeriesMatrixToolView extends AbstractToolView {

    private JSpinner configureSpinner;
    private AbstractButton helpButton;
    private JLabel dateLabel;
    private ProductSceneView currentView;
    private AbstractTimeSeries timeSeries;
    private SceneViewListener sceneViewListener;
    private TimeSeriesPPL pixelPosListener;
    private MatrixMouseWheelListener mouseWheelListener;
    private final TimeSeriesListener timeSeriesMatrixTSL;
    private JTable matrixTable;

    private int matrixSize = 3; // default value; value must be uneven

    private static final String DATE_PREFIX = "Date: ";
    private MatrixTableModel matrixModel;
    private SimpleDateFormat dateFormat;

    public TimeSeriesMatrixToolView() {
        pixelPosListener = new TimeSeriesPPL();
        sceneViewListener = new SceneViewListener();
        mouseWheelListener = new MatrixMouseWheelListener();
        timeSeriesMatrixTSL = new TimeSeriesMatrixTSL();
        dateFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss", Locale.getDefault());
    }

    @Override
    protected JComponent createControl() {
        Guardian.assertEquals("Specified matrix size must be uneven", matrixSize % 2 == 1, true);
        VisatApp.getApp().addInternalFrameListener(sceneViewListener);
        final JPanel panel = new JPanel(new BorderLayout());

        configureSpinner = new JSpinner(new MatrixSpinnerModel());
        configureSpinner.setUI(new BasicSpinnerUI());
        configureSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                matrixSize = (Integer) configureSpinner.getModel().getValue();
                matrixModel.setMatrixSize(matrixSize);
            }
        });

        helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"), false);
        helpButton.setToolTipText("Help");

        JPanel buttonPanel = createButtonPanel();

        panel.add(createMainPanel(), BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        panel.add(buttonPanel, BorderLayout.EAST);

        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        final boolean isTimeSeriesView = isTimeSeriesView(view);
        if (isTimeSeriesView) {
            setCurrentView(view);
        }
        setUIEnabled(isTimeSeriesView);

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
        dateLabel = new JLabel(String.format(DATE_PREFIX + " %s", startDateString));
        mainPanel.add(BorderLayout.NORTH, dateLabel);
        matrixModel = new MatrixTableModel(matrixSize);
        matrixTable = new JTable(matrixModel);
        matrixTable.setDefaultRenderer(Double.class, new MatrixCellRenderer(matrixModel));
        matrixTable.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        matrixTable.setEnabled(false);
        mainPanel.add(BorderLayout.CENTER, matrixTable);
        return mainPanel;
    }

    private String getStartDateString() {
        String startDateString = "";
        if (currentView != null && timeSeries != null) {
            final TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(currentView.getRaster());
            Date startDate = timeCoding.getStartTime().getAsDate();
            startDateString = dateFormat.format(startDate);
        }
        return startDateString;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets.left = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.top = 14;
        gbc.gridy = 0;
        buttonPanel.add(configureSpinner, gbc);
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
        return buttonPanel;
    }

    private void setUIEnabled(boolean enable) {
        dateLabel.setEnabled(enable);
        configureSpinner.setEnabled(enable);
        matrixTable.setEnabled(enable);
    }

    /*
     * Checks if the view displays a timeseries product.
     * If so it is set as the current view.
     */

    private void setCurrentView(ProductSceneView newView) {
        if (currentView == newView) {
            return;
        }
        if (currentView != null) {
            currentView.removePixelPositionListener(pixelPosListener);
            timeSeries.removeTimeSeriesListener(timeSeriesMatrixTSL);
            removeMouseWheelListener();
        }
        currentView = newView;
        if (currentView != null) {
            currentView.addPixelPositionListener(pixelPosListener);
            timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct());
            timeSeries.addTimeSeriesListener(timeSeriesMatrixTSL);
            addMouseWheelListener();
            final RasterDataNode raster = currentView.getRaster();
            if (raster instanceof Band) {
                matrixModel.setBand((Band) raster);
                updateDateLabel((Band) currentView.getRaster());
            }
        } else {
            timeSeries = null;
        }
        setUIEnabled(currentView != null);
    }

    private void updateDateLabel(Band band) {
        if (band != null) {
            final TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(band);
            final Date startTime = timeCoding.getStartTime().getAsDate();
            dateLabel.setText(String.format(DATE_PREFIX + " %s", dateFormat.format(startTime)));
        } else {
            dateLabel.setText("");
        }
    }

    // Depending on the direction value this method returns the next
    // band in the list of available bands in the time series.
    // Negative value of direction means previous band.
    // If there is no next band the current band is returned.

    private Band getNextBand(Band currentBand, int direction) {
        final String varName = AbstractTimeSeries.rasterToVariableName(currentBand.getName());
        final List<Band> bandList = timeSeries.getBandsForVariable(varName);
        final int currentIndex = bandList.indexOf(currentBand);

        if (direction < 0) {
            if (currentIndex > 0) {
                return bandList.get(currentIndex - 1);
            }
        } else {
            if (currentIndex + 1 < bandList.size()) {
                return bandList.get(currentIndex + 1);
            }
        }
        return currentBand;
    }

    private boolean isTimeSeriesView(ProductSceneView view) {
        if (view != null) {
            final RasterDataNode viewRaster = view.getRaster();
            final String viewProductType = viewRaster.getProduct().getProductType();
            return !view.isRGB() &&
                   viewProductType.equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE) &&
                   TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct()) != null;
        }
        return false;
    }


    private void addMouseWheelListener() {
        if (currentView != null) {
            final LayerCanvas layerCanvas = currentView.getLayerCanvas();
            final List<MouseWheelListener> listeners = Arrays.asList(layerCanvas.getMouseWheelListeners());
            if (!listeners.contains(mouseWheelListener)) {
                layerCanvas.addMouseWheelListener(mouseWheelListener);
            }
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
                if (isTimeSeriesView(view)) {
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
                matrixTable.setEnabled(true);
                AffineTransform i2mTransform = currentView.getBaseImageLayer().getImageToModelTransform(currentLevel);
                Point2D modelP = i2mTransform.transform(new Point2D.Double(pixelX + 0.5, pixelY + 0.5), null);
                AffineTransform m2iTransform = currentView.getBaseImageLayer().getModelToImageTransform();
                Point2D levelZeroP = m2iTransform.transform(modelP, null);
                matrixModel.setCenterPixel(MathUtils.floorInt(levelZeroP.getX()),
                                           MathUtils.floorInt(levelZeroP.getY()));
            } else {
                matrixTable.setEnabled(false);
            }
        }

        @Override
        public void pixelPosNotAvailable() {
            matrixTable.setEnabled(false);
        }
    }

    private class MatrixMouseWheelListener implements MouseWheelListener {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isAltDown()) {
                Band nextBand = getNextBand(matrixModel.getBand(), e.getWheelRotation());
                if (nextBand != null) {
                    matrixModel.setBand(nextBand);
                    updateDateLabel(nextBand);
                }
            }
        }
    }

    private class TimeSeriesMatrixTSL extends TimeSeriesListener {

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            final ProductNode node = event.getSourceNode();
            if (node == matrixModel.getBand()) {
                final Band band = matrixModel.getBand();
                Band nextBand = getNextBand(band, 1);
                if (nextBand == band) {
                    nextBand = getNextBand(band, -1);
                }
                if (nextBand == band) {
                    nextBand = null;
                }
                updateDateLabel(nextBand);
            }
        }
    }

    private static class MatrixSpinnerModel extends SpinnerNumberModel {

        private static final int DEFAULT_VALUE = 3;
        private static final int MINIMUM = 3;
        private static final int MAXIMUM = 15;
        private static final int STEP_SIZE = 2;

        private MatrixSpinnerModel() {
            super(DEFAULT_VALUE, MINIMUM, MAXIMUM, STEP_SIZE);
        }

        @Override
        public void setValue(Object value) {
            int iValue = (Integer) value;
            if (iValue < MINIMUM) {
                iValue = MINIMUM;
            } else if (iValue > MAXIMUM) {
                iValue = MAXIMUM;
            }
            if (iValue % 2 == 1) {
                super.setValue(iValue);
            } else {
                fireStateChanged();
            }
        }
    }

}
