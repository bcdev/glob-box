package org.esa.beam.glob.ui;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.time.TimeSeries;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.esa.beam.glob.core.timeseries.datamodel.TimeSeries.*;

public class TimeSeriesGraphToolView extends AbstractToolView {

    private static final String DEFAULT_RANGE_LABEL = "Value";
    private static final String DEFAULT_DOMAIN_LABEL = "Time";

    private final TimeSeriesPPL pixelPosListener;
    private final PropertyChangeListener pinSelectionListener;
    private final PropertyChangeListener sliderListener;
    private final ProductNodeListener pinMovedListener;
    private final ProductNodeListener productNodeListener;

    private String titleBase;
    private ProductSceneView currentView;

    private JFreeChart chart;
    private TimeSeriesGraphForm graphForm;
    private TimeSeriesGraphModel graphModel;
    private Action showPinAction;


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
        graphForm = new TimeSeriesGraphForm(chart, graphModel, showPinAction);

        final VisatApp visatApp = VisatApp.getApp();
        visatApp.addInternalFrameListener(new TimeSeriesIFL());

        ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view != null) {
            final String viewProductType = view.getProduct().getProductType();
            if (!view.isRGB() && viewProductType.equals(
                    org.esa.beam.glob.core.timeseries.datamodel.TimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
                setCurrentView(view);
            }
        }

        return graphForm.getControl();
    }

    private void updateUIState() {
        final boolean isTimeSeriesView = currentView != null;

        if (isTimeSeriesView) {
            final RasterDataNode raster = currentView.getRaster();
            String variableName = rasterToVariableName(raster.getName());
            setTitle(String.format("%s - %s", titleBase, variableName));

            final String unit = raster.getUnit();
            final String rangeAxisLabel;
            if (StringUtils.isNotNullAndNotEmpty(unit)) {
                rangeAxisLabel = String.format("%s (%s)", variableName, unit);
            } else {
                rangeAxisLabel = variableName;
            }
            graphModel.updatePlot(rangeAxisLabel, true);
        } else {
            setTitle(titleBase);
            graphModel.updatePlot(DEFAULT_RANGE_LABEL, false);
        }
        graphForm.setButtonsEnabled(isTimeSeriesView);
    }

    private void setCurrentView(ProductSceneView view) {
        if (currentView != null) {
            currentView.getProduct().removeProductNodeListener(productNodeListener);
            currentView.getProduct().removeProductNodeListener(pinMovedListener);
            currentView.removePixelPositionListener(pixelPosListener);
            currentView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionListener);
            currentView.removePropertyChangeListener(TimeSeriesPlayerToolView.TIME_PROPERTY, sliderListener);
        }
        if (view != null) {
            view.getProduct().addProductNodeListener(productNodeListener);
            view.getProduct().addProductNodeListener(pinMovedListener);
            view.addPixelPositionListener(pixelPosListener);
            view.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionListener);
            view.addPropertyChangeListener(TimeSeriesPlayerToolView.TIME_PROPERTY, sliderListener);

            if (view.getSelectedPin() != null) {
                graphModel.setIsPinSelected(true);
            }
            graphModel.adaptToVariable(view.getRaster());
            graphModel.updateTimeAnnotation(view.getRaster());
        } else {
            graphModel.setIsPinSelected(false);
            graphModel.removePinTimeSeries();
            graphModel.adaptToVariable(null);
        }
        currentView = view;
        updateUIState();
    }

    private class ShowPinAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentView != null) {
                Placemark selectedPin = currentView.getSelectedPin();

                boolean showPinSeries = graphForm.isShowPinSeries();
                if (showPinSeries && selectedPin != null) {
                    graphModel.addSelectedPinSeries(selectedPin, currentView);
                }
                if (!showPinSeries) {
                    graphModel.removePinTimeSeries();
                }
            }
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
                        org.esa.beam.glob.core.timeseries.datamodel.TimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
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

        private SwingWorker<TimeSeries, Void> updater;
//        private XYTextAnnotation loadingMessage;

        @Override
        public void pixelPosChanged(ImageLayer imageLayer, int pixelX, int pixelY,
                                    int currentLevel, boolean pixelPosValid, MouseEvent e) {
            if (pixelPosValid && isVisible() && (updater == null || updater.isDone()) && currentView != null) {
                updater = new TimeSeriesUpdater(pixelX, pixelY, currentLevel, graphModel.getVariableBandList());
                updater.execute();
            }
//            loadingMessage = new XYTextAnnotation("Loading data...",
//                                                  getTimeSeriesPlot().getDomainAxis().getRange().getCentralValue(),
//                                                  getTimeSeriesPlot().getRangeAxis().getRange().getCentralValue());
//            getTimeSeriesPlot().addAnnotation(loadingMessage);
            final ValueAxis rangeAxis = chart.getXYPlot().getRangeAxis();
            if (e.isShiftDown()) {
                rangeAxis.setAutoRange(true);
            } else {
                if (rangeAxis.isAutoRange()) {
                    rangeAxis.setRange(TimeSeriesGraphModel.computeYAxisRange(graphModel.getVariableBandList()));
                }
            }
        }

        @Override
        public void pixelPosNotAvailable() {
            graphModel.removeCursorTimeSeries();
            updateUIState();
            if (isVisible()) {
                graphForm.updateChart();
            }
        }

        private class TimeSeriesUpdater extends SwingWorker<TimeSeries, Void> {

            private final int pixelX;
            private final int pixelY;
            private final int currentLevel;
            private final List<Band> bandList;

            TimeSeriesUpdater(int pixelX, int pixelY, int currentLevel, List<Band> bandList) {
                this.pixelX = pixelX;
                this.pixelY = pixelY;
                this.currentLevel = currentLevel;
                this.bandList = bandList;
            }

            @Override
            protected TimeSeries doInBackground() throws Exception {
                return TimeSeriesGraphModel.computeTimeSeries("cursorTimeSeries", bandList, pixelX, pixelY, currentLevel);
            }

            @Override
            protected void done() {
//                getTimeSeriesPlot().removeAnnotation(loadingMessage);
                graphModel.removeCursorTimeSeries();
                try {
                    graphModel.addCursortimeSeries(get());
                } catch (InterruptedException ignore) {
                } catch (ExecutionException ignore) {
                }
            }
        }
    }

    private class PinSelectionListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (currentView != null && currentView.getSelectedPins().length <= 1) {
                Placemark pin = (Placemark) evt.getNewValue();
                boolean pinSelected = pin != null;
                graphModel.setIsPinSelected(pinSelected);
                
                if (graphForm.isShowPinSeries() && pinSelected) {
                    graphModel.addSelectedPinSeries(pin, currentView);
                } else {
                    graphModel.removePinTimeSeries();
                }
            } else {
                graphModel.setIsPinSelected(false);
            }
        }
    }

    private class PinMovedListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getPropertyName().equals(Placemark.PROPERTY_NAME_PIXELPOS)) {
                graphModel.removePinTimeSeries();
                if (graphForm.isShowPinSeries() && graphModel.isPinSelected()) {
                    graphModel.addSelectedPinSeries(currentView.getSelectedPin(), currentView);
                }
            }
        }
    }

    private class TimeSeriesProductNodeListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Band) {
                updatePinTimeSeries();
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            if (event.getSourceNode() instanceof Band) {
                updatePinTimeSeries();
            }
        }

        private void updatePinTimeSeries() {
            if (currentView != null) {
                graphModel.adaptToVariable(currentView.getRaster());
                Placemark selectedPin = currentView.getSelectedPin();
                if ((selectedPin != null) && (graphForm.isShowPinSeries())) {
                    graphModel.removePinTimeSeries();
                    graphModel.addSelectedPinSeries(selectedPin, currentView);
                }
            }
        }
    }

    private class SliderListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            int timePeriodIndex = (Integer) evt.getNewValue();
            graphModel.updateTimeAnnotation(timePeriodIndex);
        }
    }
}
