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
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.AxisMappingModel;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.glob.ui.WorkerChain;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;


class TimeSeriesGraphModel implements TimeSeriesGraphUpdater.TimeSeriesDataHandler, TimeSeriesGraphDisplayController.PinSupport {

    private static final Color DEFAULT_FOREGROUND_COLOR = Color.BLACK;
    private static final Color DEFAULT_BACKGROUND_COLOR = new Color(180, 180, 180);
    private static final String NO_DATA_MESSAGE = "No data to display";
    private static final int CURSOR_COLLECTION_INDEX_OFFSET = 0;
    private static final int PIN_COLLECTION_INDEX_OFFSET = 1;
    private static final int INSITU_COLLECTION_INDEX_OFFSET = 2;
    private static final Stroke PIN_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
            new float[]{10.0f}, 0.0f);
    private static final Stroke CURSOR_STROKE = new BasicStroke();
    private static final String IDENTIFIER_RASTER = "_r_";
    private static final String IDENTIFIER_INSITU = "_i_";

    private final Map<AbstractTimeSeries, TimeSeriesGraphDisplayController> displayControllerMap;
    private final XYPlot timeSeriesPlot;
    private final List<List<Band>> eoVariableBands;
    private final AtomicInteger version = new AtomicInteger(0);
    private final TimeSeriesGraphUpdater.WorkerChainSupport workerChainSupport;
    private final WorkerChain workerChain;
    private final Map<String, Paint> paintMap = new HashMap<String, Paint>();

    private TimeSeriesGraphDisplayController displayController;
    private boolean isShowingSelectedPins;
    private boolean isShowingAllPins;
    private AxisMappingModel displayAxisMapping;
    private boolean showCursorTimeSeries = true;

    TimeSeriesGraphModel(XYPlot plot) {
        timeSeriesPlot = plot;
        eoVariableBands = new ArrayList<List<Band>>();
        displayControllerMap = new WeakHashMap<AbstractTimeSeries, TimeSeriesGraphDisplayController>();
        workerChainSupport = createWorkerChainSupport();
        workerChain = new WorkerChain();
        initPlot();
    }

    void adaptToTimeSeries(AbstractTimeSeries timeSeries) {
        version.incrementAndGet();
        eoVariableBands.clear();

        // todo - replace variable hasData by behavior (remove datasets from chart)
        final boolean hasData = timeSeries != null;
        if (hasData) {
            displayController = displayControllerMap.get(timeSeries);
            if (displayController == null) {
                displayController = new TimeSeriesGraphDisplayController(this);
                displayControllerMap.put(timeSeries, displayController);
            }
            displayController.adaptTo(timeSeries);
            for (String eoVariableName : displayController.getEoVariablesToDisplay()) {
                eoVariableBands.add(timeSeries.getBandsForVariable(eoVariableName));
            }
        }
        updatePlot(hasData, timeSeries);
    }

    AtomicInteger getVersion() {
        return version;
    }

    void updateAnnotation(RasterDataNode raster) {
        removeAnnotation();

        final AbstractTimeSeries timeSeries = getTimeSeries();
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

    void setIsShowingSelectedPins(boolean isShowingSelectedPins) {
        if (isShowingSelectedPins && isShowingAllPins) {
            throw new IllegalStateException("isShowingSelectedPins && isShowingAllPins");
        }
        this.isShowingSelectedPins = isShowingSelectedPins;
        updateTimeSeries(null, TimeSeriesType.PIN);
        updateTimeSeries(null, TimeSeriesType.INSITU);
    }

    void setIsShowingAllPins(boolean isShowingAllPins) {
        if (isShowingAllPins && isShowingSelectedPins) {
            throw new IllegalStateException("isShowingAllPins && isShowingSelectedPins");
        }
        this.isShowingAllPins = isShowingAllPins;
        updateTimeSeries(null, TimeSeriesType.PIN);
        updateTimeSeries(null, TimeSeriesType.INSITU);
    }

    void setIsShowingCursorTimeSeries(boolean showCursorTimeSeries) {
        this.showCursorTimeSeries = showCursorTimeSeries;
    }

    synchronized void updateTimeSeries(TimeSeriesGraphUpdater.Position cursorPosition, TimeSeriesType type) {
        if(getTimeSeries() == null) {
            return;
        }
        final TimeSeriesGraphUpdater.PositionSupport positionSupport = createPositionSupport();
        final TimeSeriesGraphUpdater w = new TimeSeriesGraphUpdater(getTimeSeries(), createVersionSafeDataSources(),
                this, displayAxisMapping, workerChainSupport, cursorPosition, positionSupport, type,
                showCursorTimeSeries, version.get());
        final boolean chained = type != TimeSeriesType.CURSOR;
        workerChain.setOrExecuteNextWorker(w, chained);
    }

    @Override
    public void addTimeSeries(List<TimeSeries> timeSeriesList, TimeSeriesType type) {
        final int timeSeriesCount;
        final int collectionOffset;
        if (TimeSeriesType.INSITU.equals(type)) {
            timeSeriesCount = displayAxisMapping.getInsituCount();
            collectionOffset = INSITU_COLLECTION_INDEX_OFFSET;
        } else {
            timeSeriesCount = displayAxisMapping.getRasterCount();
            if (TimeSeriesType.CURSOR.equals(type)) {
                collectionOffset = CURSOR_COLLECTION_INDEX_OFFSET;
            } else {
                collectionOffset = PIN_COLLECTION_INDEX_OFFSET;
            }
        }
        final String[] aliasNames = getAliasNames();

        for (int aliasIdx = 0; aliasIdx < aliasNames.length; aliasIdx++) {
            final int targetCollectionIndex = collectionOffset + aliasIdx * 3;
            final TimeSeriesCollection targetTimeSeriesCollection = (TimeSeriesCollection) timeSeriesPlot.getDataset(targetCollectionIndex);
            targetTimeSeriesCollection.removeAllSeries();
        }
        if(timeSeriesCount == 0) {
            return;
        }
        final int numPositions = timeSeriesList.size() / timeSeriesCount;
        int timeSeriesIndexOffset = 0;
        for (int posIdx = 0; posIdx < numPositions; posIdx++) {
            final Shape posShape = getShapeForPosition(type, posIdx);
            for (int aliasIdx = 0; aliasIdx < aliasNames.length; aliasIdx++) {
                final int targetCollectionIndex = collectionOffset + aliasIdx * 3;
                final TimeSeriesCollection targetTimeSeriesCollection = (TimeSeriesCollection) timeSeriesPlot.getDataset(targetCollectionIndex);
                final XYItemRenderer renderer = timeSeriesPlot.getRenderer(targetCollectionIndex);
                final int dataSourceCount = getDataSourceCount(type, aliasNames[aliasIdx]);
                for (int ignoredIndex = 0; ignoredIndex < dataSourceCount; ignoredIndex++) {
                    targetTimeSeriesCollection.addSeries(timeSeriesList.get(timeSeriesIndexOffset));
                    final int timeSeriesTargetIdx = targetTimeSeriesCollection.getSeriesCount() - 1;
                    renderer.setSeriesShape(timeSeriesTargetIdx, posShape);
                    renderer.setSeriesPaint(timeSeriesTargetIdx, renderer.getSeriesPaint(timeSeriesTargetIdx % dataSourceCount));
                    timeSeriesIndexOffset++;
                }
            }
        }
    }

    @Override
    public boolean isShowingSelectedPins() {
        return isShowingSelectedPins;
    }

    @Override
    public Placemark[] getSelectedPins() {
        return getCurrentView().getSelectedPins();
    }

    @Override
    public boolean isShowingAllPins() {
        return isShowingAllPins;
    }

    private TimeSeriesGraphUpdater.WorkerChainSupport createWorkerChainSupport() {
        return new TimeSeriesGraphUpdater.WorkerChainSupport() {
            @Override
            public void removeWorkerAndStartNext(TimeSeriesGraphUpdater worker) {
                workerChain.removeCurrentWorkerAndExecuteNext(worker);
            }
        };
    }

    private TimeSeriesGraphUpdater.PositionSupport createPositionSupport() {
        return new TimeSeriesGraphUpdater.PositionSupport() {

            private final GeoCoding geoCoding = getTimeSeries().getTsProduct().getGeoCoding();
            private final PixelPos pixelPos = new PixelPos();
            private final Viewport viewport = getCurrentView().getViewport();
            private final ImageLayer baseLayer = getCurrentView().getBaseImageLayer();
            private final int currentLevel = baseLayer.getLevel(viewport);
            private final AffineTransform levelZeroToModel = baseLayer.getImageToModelTransform();
            private final AffineTransform modelToCurrentLevel = baseLayer.getModelToImageTransform(currentLevel);

            @Override
            public TimeSeriesGraphUpdater.Position transformGeoPos(GeoPos geoPos) {
                geoCoding.getPixelPos(geoPos, pixelPos);
                final Point2D modelPos = levelZeroToModel.transform(pixelPos, null);
                final Point2D currentPos = modelToCurrentLevel.transform(modelPos, null);
                return new TimeSeriesGraphUpdater.Position((int) currentPos.getX(), (int) currentPos.getY(), currentLevel);
            }
        };
    }

    private void initPlot() {
        final ValueAxis domainAxis = timeSeriesPlot.getDomainAxis();
        domainAxis.setAutoRange(true);
        XYLineAndShapeRenderer xyRenderer = new XYLineAndShapeRenderer(true, true);
        xyRenderer.setBaseLegendTextPaint(DEFAULT_FOREGROUND_COLOR);
        timeSeriesPlot.setRenderer(xyRenderer);
        timeSeriesPlot.setBackgroundPaint(DEFAULT_BACKGROUND_COLOR);
        timeSeriesPlot.setNoDataMessage(NO_DATA_MESSAGE);
        timeSeriesPlot.setDrawingSupplier(null);
    }

    private void updatePlot(boolean hasData, AbstractTimeSeries timeSeries) {
        for (int i = 0; i < timeSeriesPlot.getDatasetCount(); i++) {
            timeSeriesPlot.setDataset(i, null);
        }
        timeSeriesPlot.clearRangeAxes();

        if (!hasData) {
            return;
        }

        paintMap.clear();
        displayAxisMapping = createDisplayAxisMapping(timeSeries);
        final Set<String> aliasNamesSet = displayAxisMapping.getAliasNames();
        final String[] aliasNames = aliasNamesSet.toArray(new String[aliasNamesSet.size()]);

        for (String aliasName : aliasNamesSet) {
            consumeColors(aliasName, displayAxisMapping.getRasterNames(aliasName), IDENTIFIER_RASTER);
            consumeColors(aliasName, displayAxisMapping.getInsituNames(aliasName), IDENTIFIER_INSITU);
        }

        for (int aliasIdx = 0; aliasIdx < aliasNames.length; aliasIdx++) {
            String aliasName = aliasNames[aliasIdx];

            timeSeriesPlot.setRangeAxis(aliasIdx, createValueAxis(aliasName));

            final int aliasIndexOffset = aliasIdx * 3;
            final int cursorCollectionIndex = aliasIndexOffset + CURSOR_COLLECTION_INDEX_OFFSET;
            final int pinCollectionIndex = aliasIndexOffset + PIN_COLLECTION_INDEX_OFFSET;
            final int insituCollectionIndex = aliasIndexOffset + INSITU_COLLECTION_INDEX_OFFSET;

            TimeSeriesCollection cursorDataset = new TimeSeriesCollection();
            timeSeriesPlot.setDataset(cursorCollectionIndex, cursorDataset);

            TimeSeriesCollection pinDataset = new TimeSeriesCollection();
            timeSeriesPlot.setDataset(pinCollectionIndex, pinDataset);

            TimeSeriesCollection insituDataset = new TimeSeriesCollection();
            timeSeriesPlot.setDataset(insituCollectionIndex, insituDataset);

            timeSeriesPlot.mapDatasetToRangeAxis(cursorCollectionIndex, aliasIdx);
            timeSeriesPlot.mapDatasetToRangeAxis(pinCollectionIndex, aliasIdx);
            timeSeriesPlot.mapDatasetToRangeAxis(insituCollectionIndex, aliasIdx);

            final XYErrorRenderer pinRenderer = createXYErrorRenderer();
            final XYErrorRenderer cursorRenderer = createXYErrorRenderer();
            final XYErrorRenderer insituRenderer = createXYErrorRenderer();

            pinRenderer.setBaseStroke(PIN_STROKE);
            cursorRenderer.setBaseStroke(CURSOR_STROKE);

            insituRenderer.setBaseLinesVisible(false);
            insituRenderer.setBaseShapesFilled(false);


            final List<String> rasterNamesSet = displayAxisMapping.getRasterNames(aliasName);
            final String[] rasterNames = rasterNamesSet.toArray(new String[rasterNamesSet.size()]);

            for (int i = 0; i < rasterNames.length; i++) {
                final String paintKey = aliasName + IDENTIFIER_RASTER + rasterNames[i];
                final Paint paint = paintMap.get(paintKey);
                cursorRenderer.setSeriesPaint(i, paint);
                pinRenderer.setSeriesPaint(i, paint);
            }

            final List<String> insituNamesSet = displayAxisMapping.getInsituNames(aliasName);
            final String[] insituNames = insituNamesSet.toArray(new String[insituNamesSet.size()]);

            for (int i = 0; i < insituNames.length; i++) {
                final String paintKey = aliasName + IDENTIFIER_INSITU + insituNames[i];
                final Paint paint = paintMap.get(paintKey);
                insituRenderer.setSeriesPaint(i, paint);
            }

            timeSeriesPlot.setRenderer(cursorCollectionIndex, cursorRenderer);
            timeSeriesPlot.setRenderer(pinCollectionIndex, pinRenderer);
            timeSeriesPlot.setRenderer(insituCollectionIndex, insituRenderer);
        }
    }

    private void consumeColors(String aliasName, List<String> names, String identifier) {
        final int registeredPaints = paintMap.size();
        for (int i = 0; i < names.size(); i++) {
            final Paint paint = displayController.getPaint(registeredPaints + i);
            paintMap.put(aliasName + identifier + names.get(i), paint);
        }
    }

    private XYErrorRenderer createXYErrorRenderer() {
        final XYErrorRenderer renderer = new XYErrorRenderer();
        renderer.setDrawXError(false);
        renderer.setDrawYError(false);
        renderer.setBaseLinesVisible(true);
        renderer.setAutoPopulateSeriesStroke(false);
        renderer.setAutoPopulateSeriesPaint(false);
        renderer.setAutoPopulateSeriesFillPaint(false);
        renderer.setAutoPopulateSeriesOutlinePaint(false);
        renderer.setAutoPopulateSeriesOutlineStroke(false);
        renderer.setAutoPopulateSeriesShape(false);
        return renderer;
    }

    private NumberAxis createValueAxis(String aliasName) {
        String unit = getUnit(displayAxisMapping, aliasName);
        String axisLabel = getAxisLabel(aliasName, unit);
        NumberAxis valueAxis = new NumberAxis(axisLabel);
        valueAxis.setAutoRange(true);
        return valueAxis;
    }

    private AxisMappingModel createDisplayAxisMapping(AbstractTimeSeries timeSeries) {
        final List<String> eoVariables = displayController.getEoVariablesToDisplay();
        final List<String> insituVariables = displayController.getInsituVariablesToDisplay();
        final AxisMappingModel axisMappingModel = timeSeries.getAxisMappingModel();
        return createDisplayAxisMapping(eoVariables, insituVariables, axisMappingModel);
    }

    private AxisMappingModel createDisplayAxisMapping(List<String> eoVariables, List<String> insituVariables, AxisMappingModel axisMappingModel) {
        final AxisMappingModel displayAxisMapping = new AxisMappingModel();

        for (String eoVariable : eoVariables) {
            final String aliasName = axisMappingModel.getRasterAlias(eoVariable);
            if (aliasName == null) {
                displayAxisMapping.addRasterName(eoVariable, eoVariable);
            } else {
                displayAxisMapping.addRasterName(aliasName, eoVariable);
            }
        }

        for (String insituVariable : insituVariables) {
            final String aliasName = axisMappingModel.getInsituAlias(insituVariable);
            if (aliasName == null) {
                displayAxisMapping.addInsituName(insituVariable, insituVariable);
            } else {
                displayAxisMapping.addInsituName(aliasName, insituVariable);
            }
        }
        return displayAxisMapping;
    }

    private String getUnit(AxisMappingModel axisMappingModel, String aliasName) {
        final List<String> rasterNames = axisMappingModel.getRasterNames(aliasName);
        for (List<Band> eoVariableBandList : eoVariableBands) {
            for (String rasterName : rasterNames) {
                final Band raster = eoVariableBandList.get(0);
                if (raster.getName().startsWith(rasterName)) {
                    return raster.getUnit();
                }
            }
        }
        return "";
    }

    private static String getAxisLabel(String variableName, String unit) {
        if (StringUtils.isNotNullAndNotEmpty(unit)) {
            return String.format("%s (%s)", variableName, unit);
        } else {
            return variableName;
        }
    }

    private String[] getAliasNames() {
        final Set<String> aliasNamesSet = displayAxisMapping.getAliasNames();
        return aliasNamesSet.toArray(new String[aliasNamesSet.size()]);
    }

    private Shape getShapeForPosition(TimeSeriesType type, int posIdx) {
        final Shape posShape;
        if (TimeSeriesType.CURSOR.equals(type)) {
            posShape = TimeSeriesGraphDisplayController.CURSOR_SHAPE;
        } else {
            posShape = displayController.getShape(posIdx);
        }
        return posShape;
    }

    private int getDataSourceCount(TimeSeriesType type, String aliasName) {
        if (TimeSeriesType.INSITU.equals(type)) {
            return displayAxisMapping.getInsituNames(aliasName).size();
        } else {
            return displayAxisMapping.getRasterNames(aliasName).size();
        }
    }

    private TimeSeriesGraphUpdater.VersionSafeDataSources createVersionSafeDataSources() {
        return new TimeSeriesGraphUpdater.VersionSafeDataSources(
                displayController.getPinPositionsToDisplay(), getVersion().get()) {
            @Override
            public int getCurrentVersion() {
                return version.get();
            }
        };
    }

    private AbstractTimeSeries getTimeSeries() {
        final ProductSceneView sceneView = getCurrentView();
        if(sceneView == null) {
            return null;
        }
        final Product sceneViewProduct = sceneView.getProduct();
        return TimeSeriesMapper.getInstance().getTimeSeries(sceneViewProduct);
    }

    private ProductSceneView getCurrentView() {
        return VisatApp.getApp().getSelectedProductSceneView();
    }
}
