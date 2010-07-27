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
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries.*;

public class TimeSeriesGraphToolView extends AbstractToolView {

    private static final String DEFAULT_RANGE_LABEL = "Value";
    private static final String DEFAULT_DOMAIN_LABEL = "Time";

    private final TimeSeriesPPL pixelPosListener;
    private final PropertyChangeListener pinSelectionListener;
    private final PropertyChangeListener sliderListener;
    private final ProductNodeListener productNodeListener;
    private final Action showSelectedPinAction;
    private final Action showAllPinAction;

    private String titleBase;
    private JFreeChart chart;
    private TimeSeriesGraphForm graphForm;
    private TimeSeriesGraphModel graphModel;

    private ProductSceneView currentView;


    public TimeSeriesGraphToolView() {
        pixelPosListener = new TimeSeriesPPL();
        pinSelectionListener = new PinSelectionListener();
        sliderListener = new SliderListener();
        productNodeListener = new TimeSeriesProductNodeListener();
        showSelectedPinAction = new ShowPinAction(true);
        showAllPinAction = new ShowPinAction(false);
    }

    @Override
    protected JComponent createControl() {
        titleBase = getDescriptor().getTitle();

        chart = ChartFactory.createTimeSeriesChart(null,
                                                   DEFAULT_DOMAIN_LABEL,
                                                   DEFAULT_RANGE_LABEL,
                                                   null, false, true, false);
        graphModel = new TimeSeriesGraphModel(chart.getXYPlot());
        graphForm = new TimeSeriesGraphForm(chart, showSelectedPinAction, showAllPinAction, getDescriptor());

        final VisatApp visatApp = VisatApp.getApp();
        visatApp.addInternalFrameListener(new TimeSeriesIFL());

        ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view != null) {
            maySetCurrentView(view);
        }
        return graphForm.getControl();
    }

    /**
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
            currentView.getProduct().removeProductNodeListener(productNodeListener);
            currentView.removePixelPositionListener(pixelPosListener);
            currentView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionListener);
            currentView.removePropertyChangeListener(TimeSeriesPlayerToolView.TIME_PROPERTY, sliderListener);
        }
        currentView = newView;
        graphForm.setButtonsEnabled(currentView != null);
        if (currentView != null) {
            currentView.getProduct().addProductNodeListener(productNodeListener);
            currentView.addPixelPositionListener(pixelPosListener);
            currentView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionListener);
            currentView.addPropertyChangeListener(TimeSeriesPlayerToolView.TIME_PROPERTY, sliderListener);

            final RasterDataNode raster = currentView.getRaster();
            AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct());
            graphModel.adaptToTimeSeries(timeSeries);

            String variableName = rasterToVariableName(raster.getName());
            setTitle(String.format("%s - %s", titleBase, variableName));

            graphModel.updateAnnotation(raster);
            updatePins(graphForm.isShowingSelectedPins());
            showSelectedPinAction.setEnabled(currentView.getSelectedPin() != null);
            showAllPinAction.setEnabled(currentView.getProduct().getPinGroup().getNodeCount() > 0);
        } else {
            graphModel.removeCursorTimeSeries();
            graphModel.removePinTimeSeries();
            graphModel.removeAnnotation();
            graphModel.adaptToTimeSeries(null);

            setTitle(titleBase);
        }
    }

    private void updatePins(boolean selected) {
        graphModel.removePinTimeSeries();
        boolean showing;
        Placemark[] pins;
        if (selected) {
            showing = graphForm.isShowingSelectedPins();
            pins = currentView.getSelectedPins();
        } else {
            showing = graphForm.isShowingAllPins();
            PlacemarkGroup pinGroup = currentView.getProduct().getPinGroup();
            pins = pinGroup.toArray(new Placemark[pinGroup.getNodeCount()]);
        }
        if (showing) {
            for (Placemark pin : pins) {
                final Viewport viewport = currentView.getViewport();
                final ImageLayer baseLayer = currentView.getBaseImageLayer();
                final int currentLevel = baseLayer.getLevel(viewport);
                final AffineTransform levelZeroToModel = baseLayer.getImageToModelTransform();
                final AffineTransform modelToCurrentLevel = baseLayer.getModelToImageTransform(currentLevel);
                final Point2D modelPos = levelZeroToModel.transform(pin.getPixelPos(), null);
                final Point2D currentPos = modelToCurrentLevel.transform(modelPos, null);
                graphModel.updateTimeSeries(graphForm.getControl(), (int) currentPos.getX(), (int) currentPos.getY(),
                                            currentLevel, false);
            }
        }
    }

    private class ShowPinAction extends AbstractAction {

        private final boolean selected;

        private ShowPinAction(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            updatePins(selected);
        }
    }

    private class TimeSeriesIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                maySetCurrentView((ProductSceneView) contentPane);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane == currentView) {
                setCurrentView(null);
            }
        }
    }

    private class TimeSeriesPPL implements PixelPositionListener {

//        private XYTextAnnotation loadingMessage;

        @Override
        public void pixelPosChanged(ImageLayer imageLayer, int pixelX, int pixelY,
                                    int currentLevel, boolean pixelPosValid, MouseEvent e) {
            if (pixelPosValid && isVisible() && currentView != null) {
                graphModel.updateTimeSeries(graphForm.getControl(), pixelX, pixelY, currentLevel, true);
            }

            final ValueAxis rangeAxis = chart.getXYPlot().getRangeAxis();
            if (e.isShiftDown()) {
                rangeAxis.setAutoRange(true);
            } else {
                rangeAxis.setAutoRange(false);
            }
            graphModel.updateAnnotation(currentView.getRaster());
        }

        @Override
        public void pixelPosNotAvailable() {
            graphModel.removeCursorTimeSeries();
        }
    }

    private class PinSelectionListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Placemark pin = (Placemark) evt.getNewValue();
            showSelectedPinAction.setEnabled(pin != null);
            updatePins(graphForm.isShowingSelectedPins());
        }
    }

    private class TimeSeriesProductNodeListener extends ProductNodeListenerAdapter {


        @Override
        public void nodeChanged(ProductNodeEvent event) {
            String propertyName = event.getPropertyName();
            if (propertyName.equals(AbstractTimeSeries.PROPERTY_PRODUCT_LOCATIONS) ||
                propertyName.equals(AbstractTimeSeries.PROPERTY_VARIABLE_SELECTION)) {
                handleBandsChanged();
            } else if (propertyName.equals(Placemark.PROPERTY_NAME_PIXELPOS)) {
                updatePins(graphForm.isShowingSelectedPins());
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Placemark) {
                handlePlacemarkChanged();
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Placemark) {
                handlePlacemarkChanged();
            }
        }

        private void handlePlacemarkChanged() {
            showSelectedPinAction.setEnabled(currentView.getSelectedPin() != null);
            final boolean placemarksSet = currentView.getProduct().getPinGroup().getNodeCount() > 0;
            showAllPinAction.setEnabled(placemarksSet);
            graphForm.setExportEnabled(placemarksSet);
        }

        private void handleBandsChanged() {
            AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct());
            graphModel.adaptToTimeSeries(timeSeries);
            graphModel.updateAnnotation(currentView.getRaster());
            updatePins(graphForm.isShowingSelectedPins());
        }
    }

    private class SliderListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            graphModel.updateAnnotation(currentView.getRaster());
        }
    }
}
