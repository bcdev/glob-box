package org.esa.beam.glob.ui;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.Histogram;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.esa.beam.glob.core.timeseries.datamodel.TimeSeries.*;

public class TimeSeriesGraphToolView extends AbstractToolView {

    private static final String NO_DATA_MESSAGE = "No data to display";
    private static final String DEFAULT_RANGE_LABEL = "Value";
    private static final String DEFAULT_DOMAIN_LABEL = "Time";

    private final TimeSeriesPPL pixelPosListener;
    private final PropertyChangeListener pinSelectionListener;
    private final PropertyChangeListener sliderListener;
    private final ProductNodeListener pinMovedListener;
    private final ProductNodeListener productNodeListener;
    private final ExecutorService executorService;
    private final TimeSeriesCollection timeSeriesCollection;

    private String titleBase;
    private ProductSceneView currentView;
    private ChartPanel chartPanel;
    private TimeSeries cursorTimeSeries;
    private TimeSeries pinTimeSeries;
    private boolean somePinIsSelected = false;
    private JPanel buttonPanel;
    private AbstractButton showTimeSeriesForSelectedPinButton;
    private List<Band> bandList;

    public TimeSeriesGraphToolView() {
        pixelPosListener = new TimeSeriesPPL();
        pinSelectionListener = new PinSelectionListener();
        sliderListener = new SliderListener();
        pinMovedListener = new PinMovedListener();
        productNodeListener = new TimeSeriesProductNodeListener();
        executorService = Executors.newSingleThreadExecutor();
        timeSeriesCollection = new TimeSeriesCollection();
    }

    @Override
    protected JComponent createControl() {
        titleBase = getDescriptor().getTitle();
        JPanel mainPanel = new JPanel(new BorderLayout(4, 4));
        final JFreeChart chart = ChartFactory.createTimeSeriesChart(null,
                                                                    DEFAULT_DOMAIN_LABEL,
                                                                    DEFAULT_RANGE_LABEL,
                                                                    null, false, true, false);
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(300, 200));
        chart.getXYPlot().setNoDataMessage(NO_DATA_MESSAGE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        mainPanel.add(BorderLayout.CENTER, chartPanel);
        mainPanel.setPreferredSize(new Dimension(320, 200));

        final VisatApp visatApp = VisatApp.getApp();
        visatApp.addInternalFrameListener(new TimeSeriesIFL());

        final XYPlot timeSeriesPlot = getTimeSeriesPlot();
        final ValueAxis domainAxis = timeSeriesPlot.getDomainAxis();
        domainAxis.setAutoRange(true);
        final ValueAxis rangeAxis = timeSeriesPlot.getRangeAxis();
        rangeAxis.setAutoRange(false);
        rangeAxis.setUpperBound(1.0);
        XYItemRenderer renderer = timeSeriesPlot.getRenderer();
        if (renderer instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer xyRenderer = (XYLineAndShapeRenderer) renderer;
            xyRenderer.setBaseShapesVisible(true);
            xyRenderer.setBaseShapesFilled(true);
        }

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
        showTimeSeriesForSelectedPinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentView != null) {
                    Placemark pin = currentView.getSelectedPin();
                    final boolean pinCheckboxSelected = showTimeSeriesForSelectedPinButton.isSelected();
                    if (pinCheckboxSelected && pin != null && somePinIsSelected) {
                        addSelectedPinSeries(pin);
                    }
                    if (!pinCheckboxSelected) {
                        removePinTimeSeries();
                    }
                }
            }
        });
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

        ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view != null) {
            final String viewProductType = view.getProduct().getProductType();
            if (!view.isRGB() && viewProductType.equals(
                    org.esa.beam.glob.core.timeseries.datamodel.TimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
                setCurrentView(view);
            }
        }

        return mainPanel;
    }

    private void addSelectedPinSeries(Placemark pin) {
        PixelPos position = pin.getPixelPos();

        final Viewport viewport = currentView.getViewport();
        final ImageLayer baseLayer = currentView.getBaseImageLayer();
        final int currentLevel = baseLayer.getLevel(viewport);
        final AffineTransform levelZeroToModel = baseLayer.getImageToModelTransform();
        final AffineTransform modelToCurrentLevel = baseLayer.getModelToImageTransform(currentLevel);
        final Point2D modelPos = levelZeroToModel.transform(position, null);
        final Point2D currentPos = modelToCurrentLevel.transform(modelPos, null);

        pinTimeSeries = computeTimeSeries("pinTimeSeries", bandList,
                                          (int) currentPos.getX(), (int) currentPos.getY(), currentLevel);

        timeSeriesCollection.addSeries(pinTimeSeries);
        if (timeSeriesCollection.getSeries(0) == cursorTimeSeries) {
            getTimeSeriesPlot().getRenderer().setSeriesPaint(0, Color.RED);
            getTimeSeriesPlot().getRenderer().setSeriesPaint(1, Color.BLUE);
        } else {
            getTimeSeriesPlot().getRenderer().setSeriesPaint(0, Color.BLUE);
            getTimeSeriesPlot().getRenderer().setSeriesPaint(1, Color.RED);
        }
        getTimeSeriesPlot().setDataset(timeSeriesCollection);
    }

    private void removePinTimeSeries() {
        if (pinTimeSeries != null) {
            timeSeriesCollection.removeSeries(pinTimeSeries);
            getTimeSeriesPlot().getRenderer().setSeriesPaint(0, Color.RED);
        }
    }

    private static TimeSeries computeTimeSeries(String title, final List<Band> bandList, int pixelX, int pixelY,
                                                int currentLevel) {
        TimeSeries timeSeries = new TimeSeries(title);
        for (Band band : bandList) {
            final ProductData.UTC startTime = band.getTimeCoding().getStartTime();
            final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                                                           ProductData.UTC.UTC_TIME_ZONE,
                                                           Locale.getDefault());

            final double value = getValue(band, pixelX, pixelY, currentLevel);
            timeSeries.add(new TimeSeriesDataItem(timePeriod, value));
        }
        return timeSeries;
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
            getTimeSeriesPlot().getRangeAxis().setLabel(rangeAxisLabel);
            getTimeSeriesPlot().getRangeAxis().setRange(computeYAxisRange(bandList));
        } else {
            setTitle(titleBase);
            getTimeSeriesPlot().getRangeAxis().setLabel(DEFAULT_RANGE_LABEL);
            getTimeSeriesPlot().setDataset(null);
        }
        for (Component child : buttonPanel.getComponents()) {
            child.setEnabled(isTimeSeriesView);
        }
    }

    private void setCurrentView(ProductSceneView view) {
        if (currentView != null) {
            currentView.getProduct().removeProductNodeListener(productNodeListener);
            currentView.removePixelPositionListener(pixelPosListener);
            currentView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionListener);
            currentView.removePropertyChangeListener(TimeSeriesPlayerToolView.TIME_PROPERTY, sliderListener);
        }
        if (view != null) {
            view.getProduct().addProductNodeListener(productNodeListener);
            view.addPixelPositionListener(pixelPosListener);
            view.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN, pinSelectionListener);
            view.addPropertyChangeListener(TimeSeriesPlayerToolView.TIME_PROPERTY, sliderListener);

            if (view.getSelectedPin() != null) {
                somePinIsSelected = true;
                showTimeSeriesForSelectedPinButton.setEnabled(true);
            }
            bandList = getBandList(view);
        } else {
            somePinIsSelected = false;
            showTimeSeriesForSelectedPinButton.setEnabled(false);
            removePinTimeSeries();
            bandList = null;
        }
        currentView = view;
        updateUIState();
    }

    private static List<Band> getBandList(ProductSceneView view) {
        if (view != null) {
            org.esa.beam.glob.core.timeseries.datamodel.TimeSeries timeSeries;
            timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct());
            String variableName = rasterToVariableName(view.getRaster().getName());
            return timeSeries.getBandsForVariable(variableName);
        } else {
            return null;
        }
    }

    private static Range computeYAxisRange(List<Band> bands) {
        Range result = null;
        for (Band band : bands) {
            Stx stx = band.getStx();
            Histogram histogram = new Histogram(stx.getHistogramBins(), stx.getMin(), stx.getMax());
            org.esa.beam.util.math.Range rangeFor95Percent = histogram.findRangeFor95Percent();
            Range range = new Range(band.scale(rangeFor95Percent.getMin()), band.scale(rangeFor95Percent.getMax()));
            if (result == null) {
                result = range;
            } else {
                result = Range.combine(result, range);
            }
        }
        return result;
    }

    private static double getValue(RasterDataNode raster, int pixelX, int pixelY, int currentLevel) {
        final RenderedImage image = raster.getGeophysicalImage().getImage(currentLevel);
        final Rectangle pixelRect = new Rectangle(pixelX, pixelY, 1, 1);
        final Raster data = image.getData(pixelRect);
        final RenderedImage validMask = raster.getValidMaskImage().getImage(currentLevel);
        final Raster validMaskData = validMask.getData(pixelRect);
        final double value;
        if (validMaskData.getSample(pixelX, pixelY, 0) > 0) {
            value = data.getSampleDouble(pixelX, pixelY, 0);
        } else {
            value = Double.NaN;
        }
        return value;
    }

    private JFreeChart getTimeSeriesChart() {
        return chartPanel.getChart();
    }

    private XYPlot getTimeSeriesPlot() {
        return getTimeSeriesChart().getXYPlot();
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

        private Future<?> future;
//        private XYTextAnnotation loadingMessage;

        @Override
        public void pixelPosChanged(ImageLayer imageLayer, int pixelX, int pixelY,
                                    int currentLevel, boolean pixelPosValid, MouseEvent e) {
            if (pixelPosValid && isActive() && (future == null || future.isDone()) && bandList != null && bandList.size() > 1) {
                future = executorService.submit(new TimeSeriesUpdater(pixelX, pixelY, currentLevel, bandList));
            }
//            loadingMessage = new XYTextAnnotation("Loading data...",
//                                                  getTimeSeriesPlot().getDomainAxis().getRange().getCentralValue(),
//                                                  getTimeSeriesPlot().getRangeAxis().getRange().getCentralValue());
//            getTimeSeriesPlot().addAnnotation(loadingMessage);
            final ValueAxis rangeAxis = getTimeSeriesPlot().getRangeAxis();
            if (e.isShiftDown()) {
                rangeAxis.setAutoRange(true);
            } else {
                if (rangeAxis.isAutoRange()) {
                    rangeAxis.setRange(computeYAxisRange(bandList));
                }
            }
        }

        @Override
        public void pixelPosNotAvailable() {
            if (cursorTimeSeries != null) {
                timeSeriesCollection.removeSeries(cursorTimeSeries);
            }
            updateUIState();
            if (isActive()) {
                chartPanel.updateUI();
            }
        }

        private boolean isActive() {
            return isVisible() && getTimeSeriesChart() != null;
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
                return computeTimeSeries("cursorTimeSeries", bandList, pixelX, pixelY, currentLevel);
            }

            @Override
            protected void done() {
//                getTimeSeriesPlot().removeAnnotation(loadingMessage);
                if (cursorTimeSeries != null) {
                    timeSeriesCollection.removeSeries(cursorTimeSeries);
                }
                try {
                    cursorTimeSeries = get();
                } catch (InterruptedException ignore) {
                } catch (ExecutionException ignore) {
                }
                timeSeriesCollection.addSeries(cursorTimeSeries);
                if (timeSeriesCollection.getSeries(0) == cursorTimeSeries) {
                    getTimeSeriesPlot().getRenderer().setSeriesPaint(0, Color.RED);
                    getTimeSeriesPlot().getRenderer().setSeriesPaint(1, Color.BLUE);
                } else {
                    getTimeSeriesPlot().getRenderer().setSeriesPaint(0, Color.BLUE);
                    getTimeSeriesPlot().getRenderer().setSeriesPaint(1, Color.RED);
                }
                getTimeSeriesPlot().setDataset(timeSeriesCollection);
            }
        }

    }

    private class PinSelectionListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (currentView != null && currentView.getSelectedPins().length <= 1) {
                Placemark pin = (Placemark) evt.getNewValue();
                somePinIsSelected = pin != null;
                if (pin != null &&
                    !Arrays.asList(pin.getProduct().getProductNodeListeners()).contains(pinMovedListener)) {
                    pin.getProduct().addProductNodeListener(pinMovedListener);
                }
                if (showTimeSeriesForSelectedPinButton.isSelected() && somePinIsSelected) {
                    addSelectedPinSeries(pin);
                } else {
                    removePinTimeSeries();
                }
                showTimeSeriesForSelectedPinButton.setEnabled(somePinIsSelected);
            } else {
                showTimeSeriesForSelectedPinButton.setEnabled(false);
            }
        }
    }

    private class PinMovedListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getPropertyName().equals(Placemark.PROPERTY_NAME_PIXELPOS)) {
                removePinTimeSeries();
                if (showTimeSeriesForSelectedPinButton.isSelected() && somePinIsSelected) {
                    addSelectedPinSeries(currentView.getSelectedPin());
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
            bandList = getBandList(currentView);
            if ((currentView.getSelectedPin() != null) && (somePinIsSelected)) {
                removePinTimeSeries();
                addSelectedPinSeries(currentView.getSelectedPin());
            }
        }

    }

    private class SliderListener implements PropertyChangeListener {

        private XYLineAnnotation xyla;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (xyla != null) {
                getTimeSeriesPlot().removeAnnotation(xyla, true);
            }
            int timePeriodIndex = (Integer) evt.getNewValue();
            if (cursorTimeSeries != null) {
                double coord = cursorTimeSeries.getTimePeriod(timePeriodIndex).getFirstMillisecond();
                xyla = new XYLineAnnotation(coord, 0, coord, chartPanel.getHeight());
                getTimeSeriesPlot().addAnnotation(xyla, true);
            }
        }
    }
}
