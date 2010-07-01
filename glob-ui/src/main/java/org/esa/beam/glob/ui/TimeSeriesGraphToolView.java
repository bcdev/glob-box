package org.esa.beam.glob.ui;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Placemark;
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

import javax.swing.*;
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
    private final ProductNodeListener pinMovedListener;
    private final ProductNodeListener productNodeListener;
    private final Action showPinAction;

    private String titleBase;
    private JFreeChart chart;
    private TimeSeriesGraphForm graphForm;
    private TimeSeriesGraphModel graphModel;

    private ProductSceneView currentView;


    public TimeSeriesGraphToolView() {
        pixelPosListener = new TimeSeriesPPL();
        pinSelectionListener = new PinSelectionListener();
        sliderListener = new SliderListener();
        pinMovedListener = new PinMovedListener();
        productNodeListener = new TimeSeriesProductNodeListener();
        showPinAction = new ShowPinAction();
    }

    @Override
    protected JComponent createControl() {
        titleBase = getDescriptor().getTitle();

        chart = ChartFactory.createTimeSeriesChart(null,
                DEFAULT_DOMAIN_LABEL,
                DEFAULT_RANGE_LABEL,
                null, false, true, false);
        graphModel = new TimeSeriesGraphModel(chart.getXYPlot());
        graphForm = new TimeSeriesGraphForm(chart, showPinAction);

        final VisatApp visatApp = VisatApp.getApp();
        visatApp.addInternalFrameListener(new TimeSeriesIFL());

        ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view != null) {
            final String viewProductType = view.getProduct().getProductType();
            if (!view.isRGB() && viewProductType.equals(
                    AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
                setCurrentView(view);
            }
        }
        return graphForm.getControl();
    }

    private void setCurrentView(ProductSceneView newView) {
        if (currentView != null) {
            currentView.getProduct().removeProductNodeListener(productNodeListener);
            currentView.getProduct().removeProductNodeListener(pinMovedListener);
            currentView.removePixelPositionListener(pixelPosListener);
            currentView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionListener);
            currentView.removePropertyChangeListener(TimeSeriesPlayerToolView.TIME_PROPERTY, sliderListener);
        }
        if (newView != null) {
            newView.getProduct().addProductNodeListener(productNodeListener);
            newView.getProduct().addProductNodeListener(pinMovedListener);
            newView.addPixelPositionListener(pixelPosListener);
            newView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionListener);
            newView.addPropertyChangeListener(TimeSeriesPlayerToolView.TIME_PROPERTY, sliderListener);

            final RasterDataNode raster = newView.getRaster();
            AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(newView.getProduct());
            graphModel.adaptToTimeSeries(timeSeries);

            String variableName = rasterToVariableName(raster.getName());
            setTitle(String.format("%s - %s", titleBase, variableName));

            graphModel.updateTimeAnnotation(raster);
            showPinAction.setEnabled(newView.getSelectedPin() != null);
        } else {
            graphModel.removeCursorTimeSeries();
            graphModel.removePinTimeSeries();
            graphModel.removeTimeAnnotation();
            graphModel.adaptToTimeSeries(null);

            setTitle(titleBase);
            showPinAction.setEnabled(false);
            graphForm.setShowingSelectPinSelected(false);
        }
        currentView = newView;
        graphForm.setButtonsEnabled(currentView != null);
    }

    private void updatePins() {
        graphModel.removePinTimeSeries();
        if (graphForm.isShowingSelectedPins()) {
            for (Placemark pin : currentView.getSelectedPins()) {
                final Viewport viewport = currentView.getViewport();
                final ImageLayer baseLayer = currentView.getBaseImageLayer();
                final int currentLevel = baseLayer.getLevel(viewport);
                final AffineTransform levelZeroToModel = baseLayer.getImageToModelTransform();
                final AffineTransform modelToCurrentLevel = baseLayer.getModelToImageTransform(currentLevel);
                final Point2D modelPos = levelZeroToModel.transform(pin.getPixelPos(), null);
                final Point2D currentPos = modelToCurrentLevel.transform(modelPos, null);
                graphModel.updateTimeSeries((int) currentPos.getX(), (int) currentPos.getY(), currentLevel, false);
            }
        }
    }

    private class ShowPinAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            updatePins();
        }
    }

    private class TimeSeriesIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView view = (ProductSceneView) contentPane;
                final String viewProductType = view.getProduct().getProductType();
                if (currentView != view && !view.isRGB() && viewProductType.equals(
                        AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
                    setCurrentView(view);
                }
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
                graphModel.updateTimeSeries(pixelX, pixelY, currentLevel, true);
            }
//            loadingMessage = new XYTextAnnotation("Loading data...",
//                                                  getTimeSeriesPlot().getDomainAxis().getRange().getCentralValue(),
//                                                  getTimeSeriesPlot().getRangeAxis().getRange().getCentralValue());
//            getTimeSeriesPlot().addAnnotation(loadingMessage);


//            final ValueAxis rangeAxis = chart.getXYPlot().getRangeAxis();
//            if (e.isShiftDown()) {
//                rangeAxis.setAutoRange(true);
//            } else {
//                if (rangeAxis.isAutoRange()) {
//                    rangeAxis.setRange(TimeSeriesGraphModel.computeYAxisRange(graphModel.getVariableBandList()));
//                }
//            }
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
            showPinAction.setEnabled(pin != null);
            updatePins();
        }
    }

    private class PinMovedListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getPropertyName().equals(Placemark.PROPERTY_NAME_PIXELPOS)) {
                updatePins();
            }
        }
    }

    private class TimeSeriesProductNodeListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Band) {
                timeSeriesProductChanged();
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Band) {
                timeSeriesProductChanged();
            }
        }

        private void timeSeriesProductChanged() {
            AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct());
            graphModel.adaptToTimeSeries(timeSeries);
            graphModel.updateTimeAnnotation(currentView.getRaster());
            updatePins();
        }
    }

    private class SliderListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            graphModel.updateTimeAnnotation(currentView.getRaster());
        }
    }
}
