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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.insitu.InsituSource;
import org.esa.beam.glob.core.insitu.csv.InsituRecord;
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
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

import javax.swing.SwingWorker;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.text.MessageFormat;
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

    private static final String DEFAULT_FONT_NAME = "Verdana";
    private static final Color DEFAULT_FOREGROUND_COLOR = Color.BLACK;
    private static final Color DEFAULT_BACKGROUND_COLOR = new Color(180, 180, 180);
    private static final String NO_DATA_MESSAGE = "No data to display";
    private static final Stroke CURSOR_STROKE = new BasicStroke(1.0f);
    private static final Stroke PIN_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
                                                             new float[]{10.0f}, 0.0f);

    private final Map<AbstractTimeSeries, DisplayModel> displayModelMap;
    private final XYPlot timeSeriesPlot;
    private final List<List<Band>> eoVariableBands;
    private final List<String> insituVariables;
    private final List<TimeSeriesCollection> pinDatasets;
    private final List<TimeSeriesCollection> cursorDatasets;
    private final List<TimeSeriesCollection> insituDatasets;

    private final AtomicInteger version = new AtomicInteger(0);
    private DisplayModel displayModel;

    TimeSeriesGraphModel(XYPlot plot) {
        timeSeriesPlot = plot;
        eoVariableBands = new ArrayList<List<Band>>();
        insituVariables = new ArrayList<String>();
        displayModelMap = new WeakHashMap<AbstractTimeSeries, DisplayModel>();
        pinDatasets = new ArrayList<TimeSeriesCollection>();
        cursorDatasets = new ArrayList<TimeSeriesCollection>();
        insituDatasets = new ArrayList<TimeSeriesCollection>();
        initPlot();
    }

    private void initPlot() {
        final ValueAxis domainAxis = timeSeriesPlot.getDomainAxis();
        domainAxis.setAutoRange(true);
        XYLineAndShapeRenderer xyRenderer = new XYSplineRenderer();
        xyRenderer.setBaseShapesVisible(true);
        xyRenderer.setBaseShapesFilled(true);
        xyRenderer.setBaseLegendTextFont(Font.getFont(DEFAULT_FONT_NAME));
        xyRenderer.setBaseLegendTextPaint(DEFAULT_FOREGROUND_COLOR);
        timeSeriesPlot.setRenderer(xyRenderer);
        timeSeriesPlot.setBackgroundPaint(DEFAULT_BACKGROUND_COLOR);
        timeSeriesPlot.setNoDataMessage(NO_DATA_MESSAGE);
    }

    void adaptToTimeSeries(AbstractTimeSeries timeSeries) {
        version.incrementAndGet();
        eoVariableBands.clear();
        insituVariables.clear();

        final boolean hasData = timeSeries != null;
        if (hasData) {
            displayModel = displayModelMap.get(timeSeries);
            if (displayModel == null) {
                displayModel = new DisplayModel(timeSeries);
                displayModelMap.put(timeSeries, displayModel);
            } else {
                displayModel.adaptTo(timeSeries);
            }
            for (String eoVariableName : displayModel.eoVariablesToDisplay) {
                eoVariableBands.add(timeSeries.getBandsForVariable(eoVariableName));
            }
            for (String insituVariableName : displayModel.getInsituVariablesToDisplay()) {
                insituVariables.add(insituVariableName);
            }
        } else {
            displayModel = null;
        }
        updatePlot(hasData, timeSeries);
    }

    private void updatePlot(boolean hasData, AbstractTimeSeries timeSeries) {
        for (int i = 0; i < timeSeriesPlot.getDatasetCount(); i++) {
            timeSeriesPlot.setDataset(i, null);
        }
        timeSeriesPlot.clearRangeAxes();
        pinDatasets.clear();
        cursorDatasets.clear();
        insituDatasets.clear();

        if (hasData) {
            List<String> eoVariablesToDisplay = displayModel.getEoVariablesToDisplay();
            int numEoVariables = eoVariablesToDisplay.size();
            for (int i = 0; i < numEoVariables; i++) {
                String eoVariableName = eoVariablesToDisplay.get(i);
                final List<Band> bandList = eoVariableBands.get(i);

                Paint paint = displayModel.getVariablename2colorMap().get(eoVariableName);
                String axisLabel = getAxisLabel(eoVariableName, bandList.get(0).getUnit());
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
                timeSeriesPlot.setDataset(i + numEoVariables, pinDataset);
                pinDatasets.add(pinDataset);

                timeSeriesPlot.mapDatasetToRangeAxis(i, i);
                timeSeriesPlot.mapDatasetToRangeAxis(i + numEoVariables, i);

                XYLineAndShapeRenderer cursorRenderer = new XYLineAndShapeRenderer(true, true);
                cursorRenderer.setSeriesPaint(0, paint);
                cursorRenderer.setSeriesStroke(0, CURSOR_STROKE);

                XYLineAndShapeRenderer pinRenderer = new XYLineAndShapeRenderer(true, true);
                pinRenderer.setBasePaint(paint);
                pinRenderer.setBaseStroke(PIN_STROKE);
                pinRenderer.setAutoPopulateSeriesPaint(false);
                pinRenderer.setAutoPopulateSeriesStroke(false);

                timeSeriesPlot.setRenderer(i + numEoVariables, pinRenderer, true);
            }

            if (!timeSeries.hasInsituData()) {
                return;
            }
            final InsituSource insituSource = timeSeries.getInsituSource();
            final List<String> insituVariablesToDisplay = new ArrayList<String>();
            for (String parameterName : insituSource.getParameterNames()) {
                if (timeSeries.isInsituVariableSelected(parameterName)) {
                    insituVariablesToDisplay.add(parameterName);
                }
            }
            for (int i = 0; i < insituVariablesToDisplay.size(); i++) {
                Paint paint = getPaintFromCorrespondingPlacemark();
                TimeSeriesCollection insituDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(i + numEoVariables * 2, insituDataset);
                insituDatasets.add(insituDataset);

                timeSeriesPlot.mapDatasetToRangeAxis(i + numEoVariables * 2, i);

                XYLineAndShapeRenderer insituRenderer = new XYLineAndShapeRenderer(true, true);
                insituRenderer.setBasePaint(paint);
                // todo - ts - set better stroke
                insituRenderer.setBaseStroke(PIN_STROKE);
                insituRenderer.setAutoPopulateSeriesPaint(false);
                insituRenderer.setAutoPopulateSeriesStroke(false);

                timeSeriesPlot.setRenderer(i + numEoVariables + 2, insituRenderer, true);
            }
        }
    }

    private Paint getPaintFromCorrespondingPlacemark() {
        // todo - ts - get paint from corresponding placemark, similar to this:
        /**
         *   Paint paint = null;
         *   for(Placemark placemark : timeSeries.getInsituPlacemarks()) {
         *       final GeoPos[] geoPoses = timeSeries.getInsituSource().getInsituPositionsFor(insituVariableName);
         *       for (GeoPos geoPos : geoPoses) {
         *           if(placemark.getGeoPos().equals(geoPos)) {
         *               paint = placemark.getSymbol().getFillPaint();
         *           }
         *       }
         *   }
         *   if(paint == null) {
         *       throw new IllegalStateException("No placemark found for variable '" + insituVariableName + ".");
         *   }
         *   return paint;
         **/
        return new Color(255, 0, 0);
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

    private static String getAxisLabel(String variableName, String unit) {
        if (StringUtils.isNotNullAndNotEmpty(unit)) {
            return String.format("%s (%s)", variableName, unit);
        } else {
            return variableName;
        }
    }

    private static double getValue(Band band, int pixelX, int pixelY, int currentLevel) {
        final Rectangle pixelRect = new Rectangle(pixelX, pixelY, 1, 1);
        if (band.getValidMaskImage() != null) {
            final RenderedImage validMask = band.getValidMaskImage().getImage(currentLevel);
            final Raster validMaskData = validMask.getData(pixelRect);
            if (validMaskData.getSample(pixelX, pixelY, 0) > 0) {
                return ProductUtils.getGeophysicalSampleDouble(band, pixelX, pixelY, currentLevel);
            } else {
                return band.getNoDataValue();
            }
        } else {
            return ProductUtils.getGeophysicalSampleDouble(band, pixelX, pixelY, currentLevel);
        }
    }

    private void addTimeSeries(List<TimeSeries> timeSeries, TimeSeriesType type) {
        List<TimeSeriesCollection> collections = getDatasets(type);
        for (int i = 0; i < timeSeries.size(); i++) {
            collections.get(i).addSeries(timeSeries.get(i));
        }
    }

    void removeCursorTimeSeries() {
        removeTimeSeries(TimeSeriesType.CURSOR);
    }

    void removePinTimeSeries() {
        removeTimeSeries(TimeSeriesType.PIN);
    }

    void removeInsituTimeSeries() {
        removeTimeSeries(TimeSeriesType.INSITU);
    }

    private void removeTimeSeries(TimeSeriesType type) {
        List<TimeSeriesCollection> collections = getDatasets(type);
        for (TimeSeriesCollection dataset : collections) {
            dataset.removeAllSeries();
        }
    }

    private List<TimeSeriesCollection> getDatasets(TimeSeriesType type) {
        switch (type) {
            case CURSOR:
                return cursorDatasets;
            case PIN:
                return pinDatasets;
            case INSITU:
                return insituDatasets;
            default:
                throw new IllegalStateException(MessageFormat.format("Unknown type: ''{0}''.", type));
        }
    }

    void updateAnnotation(RasterDataNode raster) {
        removeAnnotation();

        ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(sceneView.getProduct());

        TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(raster);
        if (timeCoding != null) {
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
    }

    void removeAnnotation() {
        timeSeriesPlot.clearAnnotations();
    }

    private SwingWorker nextWorker;

    void updateInsituTimeSeries() {
        updateTimeSeries(-1, -1, -1, TimeSeriesType.INSITU);
    }

    void updateTimeSeries(int pixelX, int pixelY, int currentLevel, TimeSeriesType type) {
        final TimeSeriesUpdater w = new TimeSeriesUpdater(pixelX, pixelY, currentLevel, type, version.get());
        if (nextWorker == null) {
            nextWorker = w;
            nextWorker.execute();
        } else {
            nextWorker = w;
        }
    }

    private class TimeSeriesUpdater extends SwingWorker<List<TimeSeries>, Void> {

        private final int pixelX;
        private final int pixelY;
        private final int currentLevel;
        private final TimeSeriesType type;
        private final int myVersion;

        TimeSeriesUpdater(int pixelX, int pixelY, int currentLevel, TimeSeriesType type, int version) {
            super();
            this.pixelX = pixelX;
            this.pixelY = pixelY;
            this.currentLevel = currentLevel;
            this.type = type;
            this.myVersion = version;
        }

        @Override
        protected List<TimeSeries> doInBackground() throws Exception {
            if (version.get() != myVersion) {
                return Collections.emptyList();
            }
            if (type.equals(TimeSeriesType.INSITU)) {
                int variableCount = insituVariables.size();
                List<TimeSeries> timeSeriesList = new ArrayList<TimeSeries>(variableCount);
                ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
                AbstractTimeSeries globTimeSeries = TimeSeriesMapper.getInstance().getTimeSeries(sceneView.getProduct());
                final InsituSource insituSource = globTimeSeries.getInsituSource();
                final Product tsProduct = globTimeSeries.getTsProduct();
                final GeoCoding geoCoding = tsProduct.getGeoCoding();
                for (String insituVariable : insituVariables) {
                    final GeoPos[] insituPositions = insituSource.getInsituPositionsFor(insituVariable);
                    PixelPos pixelPos = new PixelPos();
                    for (GeoPos insituPosition : insituPositions) {
                        geoCoding.getPixelPos(insituPosition, pixelPos);
                        if (!AbstractTimeSeries.isPixelValid(tsProduct, pixelPos)) {
                            continue;
                        }
                        InsituRecord[] insituRecords = insituSource.getValuesFor(insituVariable, insituPosition);
                        timeSeriesList.add(computeTimeSeries(insituRecords));
                    }
                }
                return timeSeriesList;
            }

            final int variableCount = eoVariableBands.size();
            List<TimeSeries> timeSeriesList = new ArrayList<TimeSeries>(variableCount);
            for (List<Band> bandList : eoVariableBands) {
                timeSeriesList.add(computeTimeSeries(bandList, pixelX, pixelY, currentLevel));
            }
            return timeSeriesList;
        }

        @Override
        protected void done() {
            if (version.get() != myVersion) {
                return;
            }
            if (type.equals(TimeSeriesType.CURSOR)) {
                removeCursorTimeSeries();
            }
            try {
                addTimeSeries(get(), type);
            } catch (InterruptedException ignore) {
                ignore.printStackTrace();
            } catch (ExecutionException ignore) {
                ignore.printStackTrace();
            } finally {
                if (this == nextWorker) {
                    nextWorker = null;
                } else {
                    nextWorker.execute();
                }
            }
        }

        private TimeSeries computeTimeSeries(InsituRecord[] insituRecords) {
            TimeSeries timeSeries = new TimeSeries("insitu");
            for (InsituRecord insituRecord : insituRecords) {
                final ProductData.UTC startTime = ProductData.UTC.create(insituRecord.time, 0);
                final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                                                               ProductData.UTC.UTC_TIME_ZONE,
                                                               Locale.getDefault());
                timeSeries.addOrUpdate(timePeriod, insituRecord.value);
            }
            return timeSeries;
        }

        private TimeSeries computeTimeSeries(final List<Band> bandList, int pixelX, int pixelY, int currentLevel) {
            TimeSeries timeSeries = new TimeSeries("title");
            ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
            AbstractTimeSeries globTimeSeries = TimeSeriesMapper.getInstance().getTimeSeries(sceneView.getProduct());

            for (Band band : bandList) {
                TimeCoding timeCoding = globTimeSeries.getRasterTimeMap().get(band);
                if (timeCoding != null) {
                    final ProductData.UTC startTime = timeCoding.getStartTime();
                    final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                                                                   ProductData.UTC.UTC_TIME_ZONE,
                                                                   Locale.getDefault());
                    final double value = getValue(band, pixelX, pixelY, currentLevel);
                    timeSeries.add(new TimeSeriesDataItem(timePeriod, value));
                }
            }
            return timeSeries;
        }
    }

    private static class DisplayModel {

        private final Map<String, Paint> variablename2colorMap;
        private final List<String> eoVariablesToDisplay;
        private final List<String> insituVariablesToDisplay;
        private static final int ALPHA = 200;

        private int maxColorIndex;
        private Color[] colors = {
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
            eoVariablesToDisplay = new ArrayList<String>();
            insituVariablesToDisplay = new ArrayList<String>();
            variablename2colorMap = new HashMap<String, Paint>();
            adaptTo(timeSeries);

        }

        private Map<String, Paint> getVariablename2colorMap() {
            return Collections.unmodifiableMap(variablename2colorMap);
        }

        private List<String> getEoVariablesToDisplay() {
            return Collections.unmodifiableList(eoVariablesToDisplay);
        }

        private List<String> getInsituVariablesToDisplay() {
            return Collections.unmodifiableList(insituVariablesToDisplay);
        }

        void adaptTo(AbstractTimeSeries timeSeries) {
            for (String eoVariableName : timeSeries.getEoVariables()) {
                if (timeSeries.isEoVariableSelected(eoVariableName)) {
                    if (!eoVariablesToDisplay.contains(eoVariableName)) {
                        eoVariablesToDisplay.add(eoVariableName);
                    }
                    if (!variablename2colorMap.containsKey(eoVariableName)) {
                        variablename2colorMap.put(eoVariableName, getNextPaint());
                    }
                } else {
                    eoVariablesToDisplay.remove(eoVariableName);
                }
            }
            if (timeSeries.hasInsituData()) {
                for (String insituVariableName : timeSeries.getInsituSource().getParameterNames()) {
                    if (timeSeries.isInsituVariableSelected(insituVariableName)) {
                        if (!insituVariablesToDisplay.contains(insituVariableName)) {
                            insituVariablesToDisplay.add(insituVariableName);
                        }
                        // todo - ts - care for colour
                    } else {
                        insituVariablesToDisplay.remove(insituVariableName);
                    }
                }
            }
        }

        private Paint getNextPaint() {
            int numColors = DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE.length;
            return colors[maxColorIndex++ % numColors];
        }
    }
}
