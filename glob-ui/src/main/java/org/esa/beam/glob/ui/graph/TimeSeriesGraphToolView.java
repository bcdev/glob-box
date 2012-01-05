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

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.glob.ui.player.TimeSeriesPlayerToolView;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries.rasterToVariableName;

public class TimeSeriesGraphToolView extends AbstractToolView {

    private static final String DEFAULT_RANGE_LABEL = "Value";
    private static final String DEFAULT_DOMAIN_LABEL = "Time";

    private final TimeSeriesPPL pixelPosListener;
    private final PropertyChangeListener pinSelectionListener;
    private final PropertyChangeListener sliderListener;
    private final TimeSeriesListener timeSeriesGraphTSL;
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
        timeSeriesGraphTSL = new TimeSeriesGraphTSL();
        showSelectedPinAction = new ShowPinAction();
        showAllPinAction = new ShowPinAction();
    }

    @Override
    protected JComponent createControl() {
        titleBase = getDescriptor().getTitle();

        chart = ChartFactory.createTimeSeriesChart(null,
                                                   DEFAULT_DOMAIN_LABEL,
                                                   DEFAULT_RANGE_LABEL,
                                                   null, true, true, false);
        graphModel = new TimeSeriesGraphModel(chart.getXYPlot());
        graphForm = new TimeSeriesGraphForm(graphModel, chart, showSelectedPinAction, showAllPinAction,
                                            getDescriptor().getHelpId());

        final VisatApp visatApp = VisatApp.getApp();
        visatApp.addInternalFrameListener(new TimeSeriesIFL());

        ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view != null) {
            maySetCurrentView(view);
        }
        return graphForm.getControl();
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
        if (currentView == newView) {
            return;
        }
        if (currentView != null) {
            final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(
                    currentView.getProduct());
            timeSeries.removeTimeSeriesListener(timeSeriesGraphTSL);
            currentView.removePixelPositionListener(pixelPosListener);
            currentView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionListener);
            currentView.removePropertyChangeListener(TimeSeriesPlayerToolView.TIME_PROPERTY, sliderListener);
        }
        currentView = newView;
        graphForm.setButtonsEnabled(currentView != null);
        if (currentView != null) {
            final Product currentProduct = currentView.getProduct();
            final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentProduct);
            timeSeries.addTimeSeriesListener(timeSeriesGraphTSL);
            currentView.addPixelPositionListener(pixelPosListener);
            currentView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionListener);
            currentView.addPropertyChangeListener(TimeSeriesPlayerToolView.TIME_PROPERTY, sliderListener);

            final RasterDataNode raster = currentView.getRaster();
            graphModel.adaptToTimeSeries(timeSeries);

            String variableName = rasterToVariableName(raster.getName());
            setTitle(String.format("%s - %s", titleBase, variableName));

            graphModel.updateAnnotation(raster);
            graphModel.updateTimeSeries(null, TimeSeriesType.PIN);
            showSelectedPinAction.setEnabled(currentView.getSelectedPin() != null);
            showAllPinAction.setEnabled(currentProduct.getPinGroup().getNodeCount() > 0);
        } else {
            graphModel.removeAnnotation();
            graphModel.adaptToTimeSeries(null);

            setTitle(titleBase);
        }
    }

    private void updateTimeSeries(AbstractTimeSeries timeSeries) {
        graphModel.adaptToTimeSeries(timeSeries);
        graphModel.updateTimeSeries(null, TimeSeriesType.INSITU);
        graphModel.updateTimeSeries(null, TimeSeriesType.PIN);
    }

    private class ShowPinAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            graphModel.updateTimeSeries(null, TimeSeriesType.PIN);
            graphModel.updateTimeSeries(null, TimeSeriesType.INSITU);
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

        @Override
        public void pixelPosChanged(ImageLayer imageLayer, int pixelX, int pixelY,
                                    int currentLevel, boolean pixelPosValid, MouseEvent e) {
            if (pixelPosValid && isVisible() && currentView != null) {
                final TimeSeriesGraphUpdater.Position position = new TimeSeriesGraphUpdater.Position(pixelX, pixelY, currentLevel);
                graphModel.updateTimeSeries(position, TimeSeriesType.CURSOR);
            }

            final boolean autorange = e.isShiftDown();
            final XYPlot xyPlot = chart.getXYPlot();
            for (int i = 0; i < xyPlot.getRangeAxisCount(); i++) {
                xyPlot.getRangeAxis(i).setAutoRange(autorange);
            }
            graphModel.updateAnnotation(currentView.getRaster());
        }

        @Override
        public void pixelPosNotAvailable() {
            final AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct());
            updateTimeSeries(timeSeries);
        }
    }

    private class PinSelectionListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Placemark pin = (Placemark) evt.getNewValue();
            showSelectedPinAction.setEnabled(pin != null);
            if (!graphModel.isShowingAllPins()) {
                graphModel.updateTimeSeries(null, TimeSeriesType.PIN);
            }
        }
    }

    private class TimeSeriesGraphTSL extends TimeSeriesListener {

        @Override
        public void timeSeriesChanged(TimeSeriesChangeEvent event) {
            if (event.getType() == TimeSeriesChangeEvent.PROPERTY_PRODUCT_LOCATIONS ||
                event.getType() == TimeSeriesChangeEvent.PROPERTY_EO_VARIABLE_SELECTION) {
                graphModel.updateAnnotation(currentView.getRaster());
                updateTimeSeries(event.getTimeSeries());
            } else if(event.getType() == TimeSeriesChangeEvent.PROPERTY_INSITU_VARIABLE_SELECTION) {
                updateTimeSeries(event.getTimeSeries());
            } else if(event.getType() == TimeSeriesChangeEvent.PROPERTY_AXIS_MAPPING_CHANGED) {
                updateTimeSeries(event.getTimeSeries());
            }
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            String propertyName = event.getPropertyName();
            if (propertyName.equals(Placemark.PROPERTY_NAME_PIXELPOS)) {
                graphModel.updateTimeSeries(null, TimeSeriesType.PIN);
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            final ProductNode node = event.getSourceNode();
            if (node instanceof Placemark) {
                handlePlacemarkChanged();
            } else if (node instanceof RasterDataNode && currentView != null) {
                graphModel.adaptToTimeSeries(getTimeSeries());
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            final ProductNode node = event.getSourceNode();
            if (node instanceof Placemark) {
                handlePlacemarkChanged();
            } else if (node instanceof RasterDataNode && currentView != null) {
                graphModel.adaptToTimeSeries(getTimeSeries());
            }
        }

        private AbstractTimeSeries getTimeSeries() {
            final Product product = currentView.getProduct();
            return TimeSeriesMapper.getInstance().getTimeSeries(product);
        }

        private void handlePlacemarkChanged() {
            showSelectedPinAction.setEnabled(currentView.getSelectedPin() != null);
            final boolean placemarksSet = currentView.getProduct().getPinGroup().getNodeCount() > 0;
            showAllPinAction.setEnabled(placemarksSet);
            graphForm.setExportEnabled(placemarksSet);
            updateTimeSeries(getTimeSeries());
        }
    }

    private class SliderListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            graphModel.updateAnnotation(currentView.getRaster());
        }
    }
}
