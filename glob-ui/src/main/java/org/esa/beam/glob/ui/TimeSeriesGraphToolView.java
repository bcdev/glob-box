package org.esa.beam.glob.ui;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.GlobBox;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TimeSeriesGraphToolView extends AbstractToolView {

    private static final String NO_DATA_MESSAGE = "No data to display";
    private static final String DEFAULT_RANGE_LABEL = "Value";
    private static final String DEFAULT_DOMAIN_LABEL = "Time";

    private String titleBase;
    private TimeSeriesPPL pixelPosListener;
    private final PinSelectionListener pinSelectionListener = new PinSelectionListener();
    private final ProductNodeListener pinMovedListener = new PinMovedListener();
    private ProductSceneView currentView;
    private ChartPanel chartPanel;
    private ExecutorService executorService;
    private static final String AUTO_MIN_MAX = "Auto min/max";

    private final JTextField minInput = new JTextField(8);
    private final JTextField maxInput = new JTextField(8);
    private GlobBox globBox;
    private JCheckBox autoAdjustBox;
    private TimeSeriesCollection timeSeriesCollection;
    private TimeSeries cursorTimeSeries;
    private TimeSeries pinTimeSeries;
    private boolean somePinIsSelected = false;
    private final JCheckBox showSelectedPinCheckbox = new JCheckBox("Show selected pin");

    public TimeSeriesGraphToolView() {
        pixelPosListener = new TimeSeriesPPL();
        executorService = Executors.newSingleThreadExecutor();
        timeSeriesCollection = new TimeSeriesCollection();
    }

    @Override
    protected JComponent createControl() {
        globBox = GlobBox.getInstance();
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
        timeSeriesPlot.setDataset(timeSeriesCollection);

        setCurrentView(globBox.getCurrentView());
        showSelectedPinCheckbox.setEnabled(somePinIsSelected);

        updateUIState();

        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setCellColspan(0, 0, 2);
        tableLayout.setCellColspan(3, 0, 2);

        final JPanel controlPanel = new JPanel(tableLayout);
        autoAdjustBox = new JCheckBox(AUTO_MIN_MAX);
        final JLabel minLabel = new JLabel("Min:");
        minInput.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        final JLabel maxLabel = new JLabel("Max:");
        maxInput.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        controlPanel.add(autoAdjustBox);
        controlPanel.add(minLabel);
        controlPanel.add(minInput);
        controlPanel.add(maxLabel);
        controlPanel.add(maxInput);
        controlPanel.add(showSelectedPinCheckbox);
        controlPanel.add(tableLayout.createVerticalSpacer());

        mainPanel.add(BorderLayout.EAST, controlPanel);

        autoAdjustBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean isSelected = ((AbstractButton) e.getSource()).isSelected();
                minLabel.setEnabled(!isSelected);
                maxLabel.setEnabled(!isSelected);
                minInput.setEnabled(!isSelected);
                maxInput.setEnabled(!isSelected);
                if (isSelected) {
                    minInput.setBorder(BorderFactory.createLineBorder(Color.gray));
                    maxInput.setBorder(BorderFactory.createLineBorder(Color.gray));
                } else {
                    minInput.setBorder(BorderFactory.createLineBorder(Color.black));
                    maxInput.setBorder(BorderFactory.createLineBorder(Color.black));
                }
                timeSeriesPlot.getRangeAxis().setAutoRange(isSelected);
            }
        });


        minInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                try {
                    getTimeSeriesPlot().getRangeAxis().setLowerBound(Double.parseDouble(minInput.getText()));
                } catch (final NumberFormatException ignore) {
                }
            }
        });

        maxInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                try {
                    getTimeSeriesPlot().getRangeAxis().setUpperBound(Double.parseDouble(maxInput.getText()));
                } catch (final NumberFormatException ignore) {
                }
            }

        });

        showSelectedPinCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentView != null) {
                    Placemark pin = currentView.getSelectedPin();
                    final boolean pinCheckboxSelected = showSelectedPinCheckbox.isSelected();
                    if (pinCheckboxSelected && pin != null && somePinIsSelected) {
                        addSelectedPinSeries(pin);
                    }
                    if (!pinCheckboxSelected) {
                        removePinTimeSeries();
                    }
                }
            }
        });

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

        Band[] timeSeriesBands = currentView.getProduct().getBands();
        pinTimeSeries = computeTimeSeries("pinTimeSeries", timeSeriesBands,
                                          (int) currentPos.getX(), (int) currentPos.getY(),
                                          currentLevel);

        timeSeriesCollection.addSeries(pinTimeSeries);

        getTimeSeriesPlot().setDataset(timeSeriesCollection);
    }

    private TimeSeries computeTimeSeries(String title, final Band[] bandList, int pixelX, int pixelY,
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
        if (currentView != null) {
            final RasterDataNode raster = currentView.getRaster();
            final String rasterName = raster.getName();
            setTitle(String.format("%s - %s", titleBase, rasterName));

            final String unit = raster.getUnit();
            final String rangeAxisLabel;
            if (StringUtils.isNotNullAndNotEmpty(unit)) {
                rangeAxisLabel = String.format("%s (%s)", rasterName, unit);
            } else {
                rangeAxisLabel = rasterName;
            }
            getTimeSeriesPlot().getRangeAxis().setLabel(rangeAxisLabel);
        } else {
            setTitle(titleBase);
            getTimeSeriesPlot().getRangeAxis().setLabel(DEFAULT_RANGE_LABEL);
        }
    }


    private JFreeChart getTimeSeriesChart() {
        return chartPanel.getChart();
    }

    private XYPlot getTimeSeriesPlot() {
        return getTimeSeriesChart().getXYPlot();
    }


    private void setCurrentView(ProductSceneView view) {

        if (view != null) {
            view.addPixelPositionListener(pixelPosListener);
            final boolean isViewPinSelectionListening = Arrays.asList(
                    view.getListeners(PropertyChangeListener.class)).contains(pinSelectionListener);
            if (!isViewPinSelectionListening) {
                view.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN,
                                               pinSelectionListener);
            }
            if (view.getSelectedPin() != null) {
                somePinIsSelected = true;
                showSelectedPinCheckbox.setEnabled(true);
            }
        } else {
            somePinIsSelected = false;
            showSelectedPinCheckbox.setEnabled(false);
            removePinTimeSeries();
        }
        currentView = view;
        updateUIState();
    }


    private void updateCursorTimeSeries(int pixelX, int pixelY, int currentLevel) {
        getTimeSeriesPlot().setDataset(null);
        getTimeSeriesPlot().setNoDataMessage("Loading data...");
        if (cursorTimeSeries != null) {
            timeSeriesCollection.removeSeries(cursorTimeSeries);
        }
        if (currentView.getProduct().getMetadataRoot().getElement(
                CreateTimeSeriesAction.TIME_SERIES_METADATA_ELEMENT) != null) {
            Band[] timeSeriesBands = currentView.getProduct().getBands();
            cursorTimeSeries = computeTimeSeries("cursorTimeSeries", timeSeriesBands,
                                                 pixelX, pixelY, currentLevel);
            timeSeriesCollection.addSeries(cursorTimeSeries);
            if (timeSeriesCollection.getSeries(0) == cursorTimeSeries) {
                getTimeSeriesPlot().getRenderer().setSeriesPaint(0, Color.RED);
                getTimeSeriesPlot().getRenderer().setSeriesPaint(1, Color.BLUE);
            } else {
                getTimeSeriesPlot().getRenderer().setSeriesPaint(0, Color.BLUE);
                getTimeSeriesPlot().getRenderer().setSeriesPaint(1, Color.RED);
            }
            getTimeSeriesPlot().setDataset(timeSeriesCollection);
            getTimeSeriesPlot().setNoDataMessage(NO_DATA_MESSAGE);

            if (autoAdjustBox.isSelected()) {
                getTimeSeriesPlot().getRangeAxis().configure();
            }
        }
    }

    private double getValue(RasterDataNode raster, int pixelX, int pixelY, int currentLevel) {
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


    private class TimeSeriesIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                setCurrentView((ProductSceneView) contentPane);
            }
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane == currentView) {
                setCurrentView(null);
            }
        }
    }

    private class TimeSeriesPPL implements PixelPositionListener {

        private Future<?> future;

        @Override
        public void pixelPosChanged(ImageLayer imageLayer,
                                    final int pixelX,
                                    final int pixelY,
                                    final int currentLevel,
                                    boolean pixelPosValid,
                                    MouseEvent e) {
            if (pixelPosValid && isActive() && (future == null || future.isDone())) {
                future = executorService.submit(new TimeSeriesUpdater(pixelX, pixelY, currentLevel));
            }
            final Range range = getTimeSeriesPlot().getRangeAxis().getRange();
            double lowerBound = range.getLowerBound();
            double upperBound = range.getUpperBound();

            DecimalFormat df = new DecimalFormat("0.000000");

            minInput.setText(df.format(lowerBound));
            maxInput.setText(df.format(upperBound));
        }

        @Override
        public void pixelPosNotAvailable() {
            getTimeSeriesPlot().setDataset(null);
            if (isActive()) {
                chartPanel.updateUI();
            }
        }

        private boolean isActive() {
            return isVisible() && getTimeSeriesChart() != null;
        }

        private class TimeSeriesUpdater implements Runnable {

            private final int pixelX;

            private final int pixelY;

            private final int currentLevel;

            TimeSeriesUpdater(int pixelX, int pixelY, int currentLevel) {
                this.pixelX = pixelX;
                this.pixelY = pixelY;
                this.currentLevel = currentLevel;
            }

            @Override
            public void run() {
                updateCursorTimeSeries(pixelX, pixelY, currentLevel);
            }

        }

    }

    private void removePinTimeSeries() {
        if (pinTimeSeries != null) {
            timeSeriesCollection.removeSeries(pinTimeSeries);
            getTimeSeriesPlot().getRenderer().setSeriesPaint(0, Color.RED);
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
                if (showSelectedPinCheckbox.isSelected() && somePinIsSelected) {
                    addSelectedPinSeries(pin);
                } else {
                    removePinTimeSeries();
                }
                showSelectedPinCheckbox.setEnabled(somePinIsSelected);
            } else {
                showSelectedPinCheckbox.setEnabled(false);
            }
        }
    }

    private class PinMovedListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (event.getPropertyName().equals(Placemark.PROPERTY_NAME_PIXELPOS)) {
                removePinTimeSeries();
                if (showSelectedPinCheckbox.isSelected() && somePinIsSelected) {
                    addSelectedPinSeries(currentView.getSelectedPin());
                }
            }
        }
    }

}
