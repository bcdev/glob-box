package org.esa.beam.glob.ui;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimeSeriesToolView extends AbstractToolView {

    private static final String NO_DATA_MESSAGE = "No data to display.";
    private static final String DEFAULT_RANGE_LABEL = "Value";
    private static final String DEFAULT_DOMAIN_LABEL = "Time";

    private String titleBase;
    private TimeSeriesPPL pixelPosListener;
    private ProductSceneView currentView;
    private ChartPanel chartPanel;
    private TimeSeries timeSeries;
    private List<RasterDataNode> availableBands = new ArrayList<RasterDataNode>();

    public TimeSeriesToolView() {
        pixelPosListener = new TimeSeriesPPL();
    }

    @Override
    protected JComponent createControl() {
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
        visatApp.getProductManager().addListener(new TimeSeriesPML());
        setCurrentView(visatApp.getSelectedProductSceneView());

        final XYPlot timeSeriesPlot = getTimeSeriesPlot();
        final ValueAxis domainAxis = timeSeriesPlot.getDomainAxis();
        domainAxis.setAutoRange(true);
        final ValueAxis rangeAxis = timeSeriesPlot.getRangeAxis();
        rangeAxis.setAutoRange(true);
        XYItemRenderer r = timeSeriesPlot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setBaseShapesVisible(true);
            renderer.setBaseShapesFilled(true);
        }


        timeSeries = new TimeSeries("cursorTimeSeries");

        updateAvailableBands();
        updateUIState();
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

    public ProductSceneView getCurrentView() {
        return currentView;
    }

    private void updateAvailableBands() {
        List<Product> availableProducts = getAvailableProducts();
        availableBands = getAvailableBands(availableProducts);
    }

    private List<RasterDataNode> getAvailableBands(List<Product> availableProducts) {
        ArrayList<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        final ProductSceneView sceneView = getCurrentView();
        if (sceneView != null) {
            final String rasterName = sceneView.getRaster().getName();
            for (Product p : availableProducts) {
                rasterList.add(p.getRasterDataNode(rasterName));
            }
        }

        return rasterList;
    }

    private List<Product> getAvailableProducts() {
        final ProductSceneView sceneView = getCurrentView();
        List<Product> productList = new ArrayList<Product>();
        if (sceneView != null) {
            final String productType = sceneView.getProduct().getProductType();
            final ProductManager productManager = VisatApp.getApp().getProductManager();

            for (Product p : productManager.getProducts()) {
                if (p.getProductType().equals(productType)) {
                    productList.add(p);
                }
            }
        }

        return productList;
    }

    private void updateTimeSeries(int pixelX, int pixelY, int currentLevel) {
        for (RasterDataNode raster : availableBands) {
            final Product product = raster.getProduct();
            final ProductData.UTC startTime = product.getStartTime();
            final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                                                           ProductData.UTC.UTC_TIME_ZONE,
                                                           Locale.getDefault());
            final TimeSeriesDataItem dataItem = timeSeries.getDataItem(timePeriod);
            final double value = getValue(raster, pixelX, pixelY, currentLevel);
            if (dataItem != null) {
                dataItem.setValue(value);
            }
        }
        getTimeSeriesPlot().getRangeAxis().configure();
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
            initTimeSeries(getAvailableProducts());
            TimeSeriesCollection tsc = new TimeSeriesCollection(timeSeries);
            getTimeSeriesPlot().setDataset(tsc);
            updateAvailableBands();
            updateUIState();
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane == currentView) {
                setCurrentView(null);
            }
            getTimeSeriesPlot().setDataset(null);
            updateAvailableBands();
            updateUIState();
        }
    }

    private class TimeSeriesPML implements ProductManager.Listener {


        @Override
        public void productAdded(ProductManager.Event event) {
            if (currentView != null) {
                addToTimeSeries(event.getProduct());
                updateAvailableBands();
                updateUIState();
                getTimeSeriesPlot().setNoDataMessage(null);
            }
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            if (currentView != null) {
                removeFromTimeSeries(event.getProduct());
                updateAvailableBands();
                updateUIState();
                if (VisatApp.getApp().getProductManager().getProductCount() == 0) {
                    getTimeSeriesPlot().setNoDataMessage(NO_DATA_MESSAGE);
                }
            }
        }

    }

    private void initTimeSeries(List<Product> productList) {
        timeSeries.clear();
        for (Product product : productList) {
            addToTimeSeries(product);
        }
    }

    private void removeFromTimeSeries(Product product) {
        final ProductData.UTC startTime = product.getStartTime();
        final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                                                       ProductData.UTC.UTC_TIME_ZONE,
                                                       Locale.getDefault());
        timeSeries.delete(timePeriod);
    }

    private void addToTimeSeries(Product product) {
        final ProductData.UTC startTime = product.getStartTime();
        final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                                                       ProductData.UTC.UTC_TIME_ZONE,
                                                       Locale.getDefault());
        timeSeries.add(new TimeSeriesDataItem(timePeriod, Double.NaN));
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
                if(timeSeries.isEmpty()) {
                    initTimeSeries(getAvailableProducts());
                }
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateTimeSeries(pixelX, pixelY, currentLevel);
                    }
                });
            }
        }

        @Override
        public void pixelPosNotAvailable() {

            if (isActive()) {
                timeSeries.clear();
                getTimeSeriesPlot().setNoDataMessage(NO_DATA_MESSAGE);
                chartPanel.updateUI();
            }
        }

        private boolean isActive() {
            return isVisible() && getTimeSeriesChart() != null;
        }

    }

}
