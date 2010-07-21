package org.esa.beam.glob.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.math.Histogram;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.event.PlotChangeListener;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

import javax.swing.JComponent;
import javax.swing.SwingWorker;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;


class TimeSeriesGraphModel {

    private final static String DEFAULT_FONT_NAME = "Verdana";
    private static final Color DEFAULT_FOREGROUND_COLOR = Color.BLACK;
    private static final Color DEFAULT_BACKGROUND_COLOR = new Color(180, 180, 180);
    private static final String NO_DATA_MESSAGE = "No data to display";
    private static final Stroke CURSOR_STROKE = new BasicStroke(1.0f);
    private static final Stroke PIN_STROKE = new BasicStroke(
            1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f}, 0.0f);

    private final Map<AbstractTimeSeries, DisplayModel> displayModelMap;
    private final XYPlot timeSeriesPlot;

    private final List<List<Band>> variableBands;
    private final List<TimeSeriesCollection> pinDatasets;
    private final List<TimeSeriesCollection> cursorDatasets;
    private DisplayModel displayModel;
    private final AtomicInteger version = new AtomicInteger(0);

    TimeSeriesGraphModel(XYPlot plot) {
        timeSeriesPlot = plot;
        timeSeriesPlot.addChangeListener(new PlotChangeListener() {
            @Override
            public void plotChanged(PlotChangeEvent plotChangeEvent) {
//                updateAnnotation();
            }
        });
        variableBands = new ArrayList<List<Band>>();
        displayModelMap = new WeakHashMap<AbstractTimeSeries, DisplayModel>();
        pinDatasets = new ArrayList<TimeSeriesCollection>();
        cursorDatasets = new ArrayList<TimeSeriesCollection>();
        initPlot();
    }

    private void initPlot() {
        final ValueAxis domainAxis = timeSeriesPlot.getDomainAxis();
        domainAxis.setAutoRange(true);
        XYItemRenderer renderer = timeSeriesPlot.getRenderer();
        if (renderer instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer xyRenderer = (XYLineAndShapeRenderer) renderer;
            xyRenderer.setBaseShapesVisible(true);
            xyRenderer.setBaseShapesFilled(true);
            xyRenderer.setBaseLegendTextFont(Font.getFont(DEFAULT_FONT_NAME));
            xyRenderer.setBaseLegendTextPaint(DEFAULT_FOREGROUND_COLOR);
        }
        timeSeriesPlot.setBackgroundPaint(DEFAULT_BACKGROUND_COLOR);
        timeSeriesPlot.setNoDataMessage(NO_DATA_MESSAGE);
    }

    void adaptToTimeSeries(AbstractTimeSeries timeSeries) {
        version.incrementAndGet();
        variableBands.clear();

        if (timeSeries != null) {
            displayModel = displayModelMap.get(timeSeries);
            if (displayModel == null) {
                displayModel = new DisplayModel(timeSeries);
                displayModelMap.put(timeSeries, displayModel);
            } else {
                displayModel.adaptTo(timeSeries);
            }
            for (String variableName : displayModel.variablesToDisplay) {
                variableBands.add(timeSeries.getBandsForVariable(variableName));
            }
        } else {
            displayModel = null;
        }
        updatePlot(timeSeries != null);
    }

    static Range computeYAxisRange(List<Band> bands) {
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

    private void updatePlot(boolean hasData) {
        for (int i = 0; i < timeSeriesPlot.getDatasetCount(); i++) {
            timeSeriesPlot.setDataset(i, null);
        }
        timeSeriesPlot.clearRangeAxes();
        pinDatasets.clear();
        cursorDatasets.clear();

        if (hasData) {
            List<String> variablesToDisplay = displayModel.getVariablesToDisplay();
            int numVariables = variablesToDisplay.size();
            for (int i = 0; i < numVariables; i++) {

                String variableName = variablesToDisplay.get(i);
                final List<Band> bandList = variableBands.get(i);

                Paint paint = displayModel.getVariablename2colorMap().get(variableName);
                String axisLabel = getAxisLabel(variableName, bandList.get(0).getUnit());
                NumberAxis valueAxis = new NumberAxis(axisLabel);
                valueAxis.setAutoRange(true);
                valueAxis.setRange(computeYAxisRange(bandList));
                valueAxis.setAxisLinePaint(paint);
                valueAxis.setLabelPaint(paint);
                valueAxis.setTickLabelPaint(paint);
                valueAxis.setTickMarkPaint(paint);
                timeSeriesPlot.setRangeAxis(i, valueAxis);

                TimeSeriesCollection cursorDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(i, cursorDataset);
                cursorDatasets.add(cursorDataset);

                TimeSeriesCollection pinDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(i + numVariables, pinDataset);
                pinDatasets.add(pinDataset);

                timeSeriesPlot.mapDatasetToRangeAxis(i, i);
                timeSeriesPlot.mapDatasetToRangeAxis(i + numVariables, i);

                XYLineAndShapeRenderer cursorRenderer = new XYLineAndShapeRenderer(true, true);
                cursorRenderer.setSeriesPaint(0, paint);
                cursorRenderer.setSeriesStroke(0, CURSOR_STROKE);

                XYLineAndShapeRenderer pinRenderer = new XYLineAndShapeRenderer(true, true);
                pinRenderer.setBasePaint(paint);
                pinRenderer.setBaseStroke(PIN_STROKE);
                pinRenderer.setAutoPopulateSeriesPaint(false);
                pinRenderer.setAutoPopulateSeriesStroke(false);

                timeSeriesPlot.setRenderer(i, cursorRenderer, true);
                timeSeriesPlot.setRenderer(i + numVariables, pinRenderer, true);

            }
        }
    }

    private String getAxisLabel(String variableName, String unit) {
        if (StringUtils.isNotNullAndNotEmpty(unit)) {
            return String.format("%s (%s)", variableName, unit);
        } else {
            return variableName;
        }
    }

    private static double getValue(Band band, int pixelX, int pixelY, int currentLevel) {
        final Rectangle pixelRect = new Rectangle(pixelX, pixelY, 1, 1);
        final RenderedImage validMask = band.getValidMaskImage().getImage(currentLevel);
        final Raster validMaskData = validMask.getData(pixelRect);
        double value = Double.NaN;
        if (validMaskData.getSample(pixelX, pixelY, 0) > 0) {
            value = ProductUtils.getGeophysicalSampleDouble(band, pixelX, pixelY, currentLevel);
        }
        return value;
    }

    private void addTimeSeries(List<TimeSeries> timeSeries, boolean cursor) {
        List<TimeSeriesCollection> collections = getDatasets(cursor);
        for (int i = 0; i < timeSeries.size(); i++) {
            collections.get(i).addSeries(timeSeries.get(i));
        }
    }

    void removeCursorTimeSeries() {
        removeTimeSeries(true);
    }

    void removePinTimeSeries() {
        removeTimeSeries(false);
    }

    private void removeTimeSeries(boolean cursor) {
        List<TimeSeriesCollection> collections = getDatasets(cursor);
        for (TimeSeriesCollection dataset : collections) {
            dataset.removeAllSeries();
        }
    }

    private List<TimeSeriesCollection> getDatasets(boolean cursor) {
        return cursor ? cursorDatasets : pinDatasets;
    }

    void updateAnnotation(RasterDataNode raster) {
        removeAnnotation();

        ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(sceneView.getProduct());

        TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(raster);
        final ProductData.UTC startTime = timeCoding.getStartTime();
        final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                                                       ProductData.UTC.UTC_TIME_ZONE,
                                                       Locale.getDefault());

        double millisecond = timePeriod.getFirstMillisecond();
        Range valueRange = null;
        for (int i = 0; i < timeSeriesPlot.getRangeAxisCount(); i++) {
            valueRange = Range.combine(valueRange, timeSeriesPlot.getRangeAxis(i).getRange());
        }
        if (valueRange != null) {
            XYAnnotation annotation = new XYLineAnnotation(millisecond, valueRange.getLowerBound(), millisecond,
                                                           valueRange.getUpperBound());
            timeSeriesPlot.addAnnotation(annotation, true);
        }
    }

    void removeAnnotation() {
        timeSeriesPlot.clearAnnotations();
    }

    private SwingWorker cursorUpdater;

    void updateTimeSeries(JComponent control, int pixelX, int pixelY, int currentLevel, boolean cursor) {
        if (cursor && (cursorUpdater == null || cursorUpdater.isDone())) {
            cursorUpdater = new TimeSeriesUpdater(control, "Loading...", pixelX, pixelY, currentLevel, cursor,
                                                  version.get());
            cursorUpdater.execute();
        } else {
            TimeSeriesUpdater updater = new TimeSeriesUpdater(control, "Loading...", pixelX, pixelY, currentLevel,
                                                              cursor, version.get());
            updater.execute();
        }
    }

    private class TimeSeriesUpdater extends ProgressMonitorSwingWorker<List<TimeSeries>, Void> {

        private final int pixelX;
        private final int pixelY;
        private final int currentLevel;
        private final boolean cursor;
        private final int myVersion;

        TimeSeriesUpdater(Component parent, String title, int pixelX, int pixelY, int currentLevel,
                          boolean cursor, int version) {
            super(parent, title);
            this.pixelX = pixelX;
            this.pixelY = pixelY;
            this.currentLevel = currentLevel;
            this.cursor = cursor;
            this.myVersion = version;
        }

        @Override
        protected List<TimeSeries> doInBackground(ProgressMonitor pm) throws Exception {
            if (version.get() != myVersion) {
                return Collections.emptyList();
            }
            final int variableCount = variableBands.size();
            List<TimeSeries> result = new ArrayList<TimeSeries>(variableCount);
            pm.beginTask("Loading data", variableCount);
            for (List<Band> bandList : variableBands) {
                result.add(computeTimeSeries(bandList, pixelX, pixelY, currentLevel));
                pm.worked(1);
            }
            pm.done();
            return result;
        }

        @Override
        protected void done() {
            if (version.get() != myVersion) {
                return;
            }
//                getTimeSeriesPlot().removeAnnotation(loadingMessage);
            if (cursor) {
                removeCursorTimeSeries();
            }
            try {
                addTimeSeries(get(), cursor);
            } catch (InterruptedException ignore) {
            } catch (ExecutionException ignore) {
            }
        }

        private TimeSeries computeTimeSeries(final List<Band> bandList, int pixelX, int pixelY, int currentLevel) {
            TimeSeries timeSeries = new TimeSeries("title");
            ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
            AbstractTimeSeries globTimeSeries = TimeSeriesMapper.getInstance().getTimeSeries(sceneView.getProduct());

            for (Band band : bandList) {
                TimeCoding timeCoding = globTimeSeries.getRasterTimeMap().get(band);
                final ProductData.UTC startTime = timeCoding.getStartTime();
                final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                                                               ProductData.UTC.UTC_TIME_ZONE,
                                                               Locale.getDefault());

                final double value = getValue(band, pixelX, pixelY, currentLevel);
                timeSeries.add(new TimeSeriesDataItem(timePeriod, value));
            }
            return timeSeries;
        }
    }

    private static class DisplayModel {

        private final Map<String, Paint> variablename2colorMap;
        private final List<String> variablesToDisplay;
        private static final int ALPHA = 200;

        private int maxColorIndex;
        private Color[] colors = {
                new Color(0, 0, 0, ALPHA),
                new Color(0, 0, 60, ALPHA),
                new Color(0, 60, 0, ALPHA),
                new Color(60, 0, 0, ALPHA),
                new Color(60, 60, 60, ALPHA),
                new Color(0, 0, 120, ALPHA),
                new Color(0, 120, 0, ALPHA),
                new Color(120, 0, 0, ALPHA),
                new Color(120, 120, 120, ALPHA)
        };

        private DisplayModel(AbstractTimeSeries timeSeries) {
            variablesToDisplay = new ArrayList<String>();
            variablename2colorMap = new HashMap<String, Paint>();
            for (String variableName : timeSeries.getTimeVariables()) {
                if (timeSeries.isVariableSelected(variableName)) {
                    variablesToDisplay.add(variableName);
                    variablename2colorMap.put(variableName, getNextPaint());
                }
            }
        }

        public Map<String, Paint> getVariablename2colorMap() {
            return variablename2colorMap;
        }

        public List<String> getVariablesToDisplay() {
            return variablesToDisplay;
        }

        void adaptTo(AbstractTimeSeries timeSeries) {
            for (String variableName : timeSeries.getTimeVariables()) {
                if (timeSeries.isVariableSelected(variableName)) {
                    if (!variablesToDisplay.contains(variableName)) {
                        variablesToDisplay.add(variableName);
                    }
                    if (!variablename2colorMap.containsKey(variableName)) {
                        variablename2colorMap.put(variableName, getNextPaint());
                    }
                } else {
                    variablesToDisplay.remove(variableName);
                }
            }
        }

        private Paint getNextPaint() {
            int numColors = DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE.length;
            return colors[maxColorIndex++ % numColors];
        }
    }
}
