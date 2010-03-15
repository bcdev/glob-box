package org.esa.beam.glob.ui;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
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
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimeSeriesToolView extends AbstractToolView {

    private static final String NO_DATA_MESSAGE = "No data to display.";
    private static final String DEFAULT_RANGE_LABEL = "Value";
    private static final String DEFAULT_DOMAIN_LABEL = "Time";

    private String titleBase;
    private TimeSeriesPPL pixelPosListener;
    private ProductSceneView currentView;
    private ChartPanel chartPanel;
    private ExecutorService executorService;
    private static final String AUTO_MIN_MAX = "Auto min/max";

    private final JTextField minInput = new JTextField(8);
    private final JTextField maxInput = new JTextField(8);
    private static final String MINIMUM_MUST_BE_DOUBLE = "<HTML><BODY>Minimum must be<BR>a double value</BODY></HTML>";
    private static final String MAXIMUM_MUST_BE_DOUBLE = "<HTML><BODY>Maximum must be<BR>a double value</BODY></HTML>";
    private GlobBox globBox;
    private JCheckBox autoAdjustBox;

    public TimeSeriesToolView() {
        pixelPosListener = new TimeSeriesPPL();
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected JComponent createControl() {
        globBox = GlobBox.getInstance();
        titleBase = getDescriptor().getTitle();
        JPanel control = new JPanel(new BorderLayout(4, 4));
        final JFreeChart chart = ChartFactory.createTimeSeriesChart("Time Series",
                                                                    DEFAULT_DOMAIN_LABEL,
                                                                    DEFAULT_RANGE_LABEL,
                                                                    null, false, true, false);
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(300, 200));
        chart.getXYPlot().setNoDataMessage(NO_DATA_MESSAGE);
        control.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        control.add(BorderLayout.CENTER, chartPanel);
        control.setPreferredSize(new Dimension(320, 200));

        final VisatApp visatApp = VisatApp.getApp();
        visatApp.addInternalFrameListener(new TimeSeriesIFL());

        final XYPlot timeSeriesPlot = getTimeSeriesPlot();
        final ValueAxis domainAxis = timeSeriesPlot.getDomainAxis();
        domainAxis.setAutoRange(true);
        final ValueAxis rangeAxis = timeSeriesPlot.getRangeAxis();
        rangeAxis.setAutoRange(false);
        rangeAxis.setUpperBound(1.0);
        XYItemRenderer r = timeSeriesPlot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setBaseShapesVisible(true);
            renderer.setBaseShapesFilled(true);
        }

        setCurrentView(globBox.getCurrentView());
        updateUIState();

        final JPanel autoAdjustButtonPanel = new JPanel(new FlowLayout());
        autoAdjustButtonPanel.setPreferredSize(new Dimension(120, 200));

        autoAdjustBox = new JCheckBox(AUTO_MIN_MAX);

        final JPanel minPanel = new JPanel(new FlowLayout());
        final JLabel minLabel = new JLabel("Min:");
        final JPanel maxPanel = new JPanel(new FlowLayout());
        final JLabel maxLabel = new JLabel("Max:");
        final JPanel minErrorPanel = new JPanel(new FlowLayout());
        final JLabel minErrorLabel = new JLabel("");
        minErrorLabel.setForeground(Color.red);
        minErrorPanel.add(minErrorLabel);

        final JPanel maxErrorPanel = new JPanel(new FlowLayout());
        final JLabel maxErrorLabel = new JLabel("");
        maxErrorLabel.setForeground(Color.red);
        maxErrorPanel.add(maxErrorLabel);

        autoAdjustButtonPanel.add(autoAdjustBox);
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

        minPanel.setPreferredSize(new Dimension(120, 25));

        minInput.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        minPanel.add(minLabel);
        minPanel.add(minInput);

        maxPanel.setPreferredSize(new Dimension(120, 25));

        maxInput.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        minInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                try {
                    getTimeSeriesPlot().getRangeAxis().setLowerBound(Double.parseDouble(minInput.getText()));
                    minErrorLabel.setText("");
                } catch (final NumberFormatException ignore) {
                    minErrorLabel.setText(MINIMUM_MUST_BE_DOUBLE);
                }
            }
        });

        maxInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent e) {
                try {
                    getTimeSeriesPlot().getRangeAxis().setUpperBound(Double.parseDouble(maxInput.getText()));
                    maxErrorLabel.setText("");
                } catch (final NumberFormatException ignore) {
                    maxErrorLabel.setText(MAXIMUM_MUST_BE_DOUBLE);
                }
            }
        });

        maxPanel.add(maxLabel);
        maxPanel.add(maxInput);

        autoAdjustButtonPanel.add(minPanel);
        autoAdjustButtonPanel.add(maxPanel);
        autoAdjustButtonPanel.add(minErrorPanel);
        autoAdjustButtonPanel.add(maxErrorPanel);

        control.add(BorderLayout.EAST, autoAdjustButtonPanel);

        return control;
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
        }
        currentView = view;
        updateUIState();
    }


    private void updateTimeSeries(int pixelX, int pixelY, int currentLevel) {
        final List<RasterDataNode> rasterList = globBox.getRasterList();
        TimeSeries timeSeries = new TimeSeries("cursorTimeSeries");
        for (RasterDataNode raster : rasterList) {
            final Product product = raster.getProduct();
            final ProductData.UTC startTime = product.getStartTime();
            final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                                                           ProductData.UTC.UTC_TIME_ZONE,
                                                           Locale.getDefault());

            final double value = getValue(raster, pixelX, pixelY, currentLevel);
            timeSeries.add(new TimeSeriesDataItem(timePeriod, value));
        }

        getTimeSeriesPlot().setDataset(new TimeSeriesCollection(timeSeries));

        if (autoAdjustBox.isSelected()) {
            getTimeSeriesPlot().getRangeAxis().configure();
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

        @Override
        public void pixelPosChanged(ImageLayer imageLayer,
                                    final int pixelX,
                                    final int pixelY,
                                    final int currentLevel,
                                    boolean pixelPosValid,
                                    MouseEvent e) {
            if (pixelPosValid && isActive()) {
                getTimeSeriesPlot().setNoDataMessage(null);
                executorService.submit(new TimeSeriesUpdater(pixelX, pixelY, currentLevel));
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

            if (isActive()) {
                getTimeSeriesPlot().setNoDataMessage(NO_DATA_MESSAGE);
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
                updateTimeSeries(pixelX, pixelY, currentLevel);

            }
        }
    }

}
