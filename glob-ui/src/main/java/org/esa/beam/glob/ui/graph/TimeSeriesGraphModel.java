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
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.AxisMappingModel;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.util.StringUtils;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYErrorRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.SwingWorker;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


class TimeSeriesGraphModel {

    private static final Color DEFAULT_FOREGROUND_COLOR = Color.BLACK;
    private static final Color DEFAULT_BACKGROUND_COLOR = new Color(180, 180, 180);
    private static final String NO_DATA_MESSAGE = "No data to display";
    private static final Stroke PIN_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
                                                             new float[]{10.0f}, 0.0f);

    private final Map<AbstractTimeSeries, DisplayController> displayControllerMap;
    private final XYPlot timeSeriesPlot;
    private final List<List<Band>> eoVariableBands;
    private final List<String> insituVariables;
    private final List<TimeSeriesCollection> pinDatasets;
    private final List<TimeSeriesCollection> cursorDatasets;
    private final List<TimeSeriesCollection> insituDatasets;

    final private AtomicInteger version = new AtomicInteger(0);
    private DisplayController displayController;

    private final List<SwingWorker> synchronizedWorkerChain;
    private SwingWorker unchainedWorker;
    private boolean workerIsRunning = false;
    private boolean isShowingSelectedPins;
    private boolean isShowingAllPins;
    private DisplayAxisMapping displayAxisMapping;
    private final TimeSeriesGraphUpdater.WorkerChainSupport workerChainSupport;
    private final TimeSeriesGraphUpdater.TimeSeriesDataHandler dataTarget;

    TimeSeriesGraphModel(XYPlot plot) {
        timeSeriesPlot = plot;
        eoVariableBands = new ArrayList<List<Band>>();
        insituVariables = new ArrayList<String>();
        displayControllerMap = new WeakHashMap<AbstractTimeSeries, DisplayController>();
        pinDatasets = new ArrayList<TimeSeriesCollection>();
        cursorDatasets = new ArrayList<TimeSeriesCollection>();
        insituDatasets = new ArrayList<TimeSeriesCollection>();
        synchronizedWorkerChain = Collections.synchronizedList(new ArrayList<SwingWorker>());
        initPlot();
        workerChainSupport = new TimeSeriesGraphUpdater.WorkerChainSupport() {
            @Override
            public void removeWorkerAndStartNext(TimeSeriesGraphUpdater worker) {
                removeCurrentWorkerAndExecuteNext(worker);
            }
        };
        dataTarget = new TimeSeriesGraphUpdater.TimeSeriesDataHandler() {
            @Override
            public void collectTimeSeries(Map<String, List<TimeSeries>> data, TimeSeriesType type) {
                addTimeSeries(data, type);
            }

            @Override
            public void removeCursorTimeSeries() {
                TimeSeriesGraphModel.this.removeCursorTimeSeries();
            }
        };
    }

    private void initPlot() {
        final ValueAxis domainAxis = timeSeriesPlot.getDomainAxis();
        domainAxis.setAutoRange(true);
//        XYLineAndShapeRenderer xyRenderer = new XYSplineRenderer();
        XYLineAndShapeRenderer xyRenderer = new XYLineAndShapeRenderer(true, true);
//        xyRenderer.setBaseShapesVisible(true);
//        xyRenderer.setBaseShapesFilled(true);
//        xyRenderer.setAutoPopulateSeriesPaint(true);
//        xyRenderer.setBaseLegendTextFont(Font.getFont(DEFAULT_FONT_NAME));
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
            displayController = displayControllerMap.get(timeSeries);
            if (displayController == null) {
                displayController = new DisplayController(timeSeries);
                displayControllerMap.put(timeSeries, displayController);
            } else {
                displayController.adaptTo(timeSeries);
            }
            for (String eoVariableName : displayController.eoVariablesToDisplay) {
                eoVariableBands.add(timeSeries.getBandsForVariable(eoVariableName));
            }
            for (String insituVariableName : displayController.getInsituVariablesToDisplay()) {
                insituVariables.add(insituVariableName);
            }
        } else {
            displayController = null;
        }
        updatePlot(hasData, timeSeries);
    }

    AtomicInteger getVersion() {
        return version;
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
            displayAxisMapping = createDisplayAxisMapping(timeSeries);
            final Set<String> aliasNamesSet = displayAxisMapping.getAliasNames();
            final String[] aliasNames = aliasNamesSet.toArray(new String[aliasNamesSet.size()]);
            final Map<String, Paint[]> aliasPaintMap = new HashMap<String, Paint[]>();

            for (Iterator<String> iterator = aliasNamesSet.iterator(); iterator.hasNext(); ) {
                String aliasName = iterator.next();
                final Set<String> rasterNames = displayAxisMapping.getRasterNames(aliasName);
                final Set<String> insituNames = displayAxisMapping.getInsituNames(aliasName);
                int numColors = Math.max(rasterNames.size(), insituNames.size());
                int registeredPaints = displayAxisMapping.getNumRegisteredPaints();
                for (int i = 0; i < numColors; i++) {
                    final Paint paint = displayController.getPaint(registeredPaints + i);
                    displayAxisMapping.addPaintForAlias(aliasName, paint);
                }
            }

            for (int aliasIdx = 0; aliasIdx < aliasNames.length; aliasIdx++) {
                String aliasName = aliasNames[aliasIdx];

                String unit = getUnit(displayAxisMapping, aliasName);
                String axisLabel = getAxisLabel(aliasName, unit);
                NumberAxis valueAxis = new NumberAxis(axisLabel);
                valueAxis.setAutoRange(true);
                timeSeriesPlot.setRangeAxis(aliasIdx, valueAxis);

//                Paint paint = displayController.getPaint(aliasIdx);
                final XYErrorRenderer pinRenderer = new XYErrorRenderer();
                pinRenderer.setBaseLinesVisible(true);
                pinRenderer.setDrawXError(false);

//                pinRenderer.setBasePaint(paint);
                pinRenderer.setBaseStroke(PIN_STROKE);
                pinRenderer.setAutoPopulateSeriesPaint(true);
                pinRenderer.setAutoPopulateSeriesStroke(false);

                final int cursorIndex = aliasIdx * 3;
                final int rasterIndex = 1 + aliasIdx * 3;
                final int insituIndex = 2 + aliasIdx * 3;

                TimeSeriesCollection cursorDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(cursorIndex, cursorDataset);
                cursorDatasets.add(cursorDataset);

                TimeSeriesCollection pinDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(rasterIndex, pinDataset);
                pinDatasets.add(pinDataset);

                TimeSeriesCollection insituDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(insituIndex, insituDataset);
                insituDatasets.add(insituDataset);

                timeSeriesPlot.setRenderer(cursorIndex, pinRenderer, true);
                timeSeriesPlot.setRenderer(rasterIndex, pinRenderer, true);
                timeSeriesPlot.setRenderer(insituIndex, pinRenderer, true);

                timeSeriesPlot.mapDatasetToRangeAxis(cursorIndex, aliasIdx);
                timeSeriesPlot.mapDatasetToRangeAxis(rasterIndex, aliasIdx);
                timeSeriesPlot.mapDatasetToRangeAxis(insituIndex, aliasIdx);
            }


            /*
            List<String> eoVariablesToDisplay = displayController.getEoVariablesToDisplay();
            int numEoVariables = eoVariablesToDisplay.size();
            for (int i = 0; i < numEoVariables; i++) {
                String eoVariableName = eoVariablesToDisplay.get(i);
                final List<Band> bandList = eoVariableBands.get(i);

                Paint paint = displayController.getVariablename2colorMap().get(eoVariableName);
                String axisLabel = getAxisLabel(eoVariableName, bandList.get(0).getUnit());
                NumberAxis valueAxis = new NumberAxis(axisLabel);
                valueAxis.setAutoRange(true);
                valueAxis.setRange(computeYAxisRange(bandList));
                valueAxis.setAxisLinePaint(paint);
                valueAxis.setLabelPaint(paint);
                valueAxis.setTickLabelPaint(paint);
                valueAxis.setTickMarkPaint(paint);
                timeSeriesPlot.setRangeAxis(i, valueAxis);

                TimeSeriesCollection cursorDatasetCollection = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(i, cursorDatasetCollection);
                cursorDatasets.add(cursorDatasetCollection);

                TimeSeriesCollection pinDatasetCollection = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(i + numEoVariables, pinDatasetCollection);
                pinDatasets.add(pinDatasetCollection);

                timeSeriesPlot.mapDatasetToRangeAxis(i, i);
                timeSeriesPlot.mapDatasetToRangeAxis(i + numEoVariables, i);

//                XYLineAndShapeRenderer cursorRenderer = new XYLineAndShapeRenderer(true, true);
//                cursorRenderer.setSeriesPaint(0, paint);
//                cursorRenderer.setSeriesStroke(0, CURSOR_STROKE);

                XYLineAndShapeRenderer pinRenderer = new XYLineAndShapeRenderer(true, true);
                pinRenderer.setBasePaint(paint);
                pinRenderer.setBaseStroke(PIN_STROKE);
                pinRenderer.setAutoPopulateSeriesPaint(true);
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
                TimeSeriesCollection insituDatasetCollection = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(i + numEoVariables * 2, insituDatasetCollection);
                insituDatasets.add(insituDatasetCollection);

                timeSeriesPlot.mapDatasetToRangeAxis(i + numEoVariables * 2, i);

                XYLineAndShapeRenderer insituRenderer = new XYLineAndShapeRenderer(false, true);
                insituRenderer.setBasePaint(paint);
                // todo - ts - set better stroke
                insituRenderer.setBaseStroke(PIN_STROKE);
                insituRenderer.setAutoPopulateSeriesPaint(true);
                insituRenderer.setAutoPopulateSeriesStroke(false);

                timeSeriesPlot.setRenderer(i + numEoVariables * 2, insituRenderer, true);
            }
            */
        }
    }

    private DisplayAxisMapping createDisplayAxisMapping(AbstractTimeSeries timeSeries) {
        final List<String> eoVariables = displayController.getEoVariablesToDisplay();
        final List<String> insituVariables = displayController.getInsituVariablesToDisplay();
        final AxisMappingModel axisMappingModel = timeSeries.getAxisMappingModel();
        return createDisplayAxisMapping(eoVariables, insituVariables, axisMappingModel);
    }

    private String getUnit(AxisMappingModel axisMappingModel, String aliasName) {
        final Set<String> rasterNames = axisMappingModel.getRasterNames(aliasName);
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

    private DisplayAxisMapping createDisplayAxisMapping(List<String> eoVariables, List<String> insituVariables, AxisMappingModel axisMappingModel) {
        final DisplayAxisMapping displayAxisMapping = new DisplayAxisMapping();

        for (String eoVariable : eoVariables) {
            final String aliasName = axisMappingModel.getRasterAlias(eoVariable);
            if (aliasName == null) {
                displayAxisMapping.addAlias(eoVariable);
                displayAxisMapping.addRasterName(eoVariable, eoVariable);
            } else {
                displayAxisMapping.addAlias(aliasName);
                displayAxisMapping.addRasterName(aliasName, eoVariable);
            }
        }

        for (String insituVariable : insituVariables) {
            final String aliasName = axisMappingModel.getInsituAlias(insituVariable);
            if (aliasName == null) {
                displayAxisMapping.addAlias(insituVariable);
                displayAxisMapping.addRasterName(insituVariable, insituVariable);
            } else {
                displayAxisMapping.addAlias(aliasName);
                displayAxisMapping.addRasterName(aliasName, insituVariable);
            }
        }

        return displayAxisMapping;
    }

    public class DisplayAxisMapping extends AxisMappingModel {

        private final Map<String, List<Paint>> alias_paintList_Map;

        private DisplayAxisMapping() {
            alias_paintList_Map = new HashMap<String, List<Paint>>();
        }

        public void addPaintForAlias(String aliasName, Paint paint) {
            if (!getAliasNames().contains(aliasName)) {
                throw new IllegalStateException("alias '" + aliasName + "' must be already registered");
            }
            if (!alias_paintList_Map.containsKey(aliasName)) {
                alias_paintList_Map.put(aliasName, new ArrayList<Paint>());
            }
            alias_paintList_Map.get(aliasName).add(paint);
        }

        public List<Paint> getPaintListForAlias(String aliasName) {
            return Collections.unmodifiableList(alias_paintList_Map.get(aliasName));
        }

        public int getNumRegisteredPaints() {
            int numRegisteredPaints = 0;
            for (List<Paint> paintList : alias_paintList_Map.values()) {
                numRegisteredPaints += paintList.size();
            }
            return numRegisteredPaints;
        }
    }

    private static String getAxisLabel(String variableName, String unit) {
        if (StringUtils.isNotNullAndNotEmpty(unit)) {
            return String.format("%s (%s)", variableName, unit);
        } else {
            return variableName;
        }
    }

    private void addTimeSeries(Map<String, List<TimeSeries>> timeSeries, TimeSeriesType type) {
//        final DisplayAxisMapping displayAxisMapping = createDisplayAxisMapping(getTimeSeries());
        List<TimeSeriesCollection> datasets = getDatasets(type);
        for (String alias : displayAxisMapping.getAliasNames()) {
            TimeSeriesCollection aliasDataset = getDatasetForAlias(alias, datasets);
            final List<TimeSeries> aliasTimeSerieses = timeSeries.get(alias);
            for (TimeSeries aliasTimeSeriese : aliasTimeSerieses) {
                aliasDataset.addSeries(aliasTimeSeriese);
            }
        }
    }

    private TimeSeriesCollection getDatasetForAlias(String alias, List<TimeSeriesCollection> datasets) {
//        final DisplayAxisMapping displayAxisMapping = createDisplayAxisMapping(getTimeSeries());
        int index = 0;
        for (String aliasName : displayAxisMapping.getAliasNames()) {
            if (alias.equals(aliasName)) {
                return datasets.get(index);
            }
            index++;
        }
        throw new IllegalStateException(MessageFormat.format("No dataset found for alias ''{0}''.", alias));
    }

    synchronized void removeCursorTimeSeriesInWorkerThread() {
        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                removeCursorTimeSeries();
                return null;
            }

            @Override
            protected void done() {
                removeCurrentWorkerAndExecuteNext(this);
            }
        };
        setOrExecuteNextWorker(worker, false);
    }

    void removeCursorTimeSeries() {
        removeTimeSeries(TimeSeriesType.CURSOR);
    }

    void removePinTimeSeries() {
        removeTimeSeries(TimeSeriesType.PIN);
    }

    synchronized void removeInsituTimeSeriesInWorkerThread() {
        final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                removeTimeSeries(TimeSeriesType.INSITU);
                return null;
            }

            @Override
            protected void done() {
                removeCurrentWorkerAndExecuteNext(this);
            }
        };
        setOrExecuteNextWorker(worker, true);
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

        ProductSceneView sceneView = getCurrentView();
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

    void updateInsituTimeSeries() {
        updateTimeSeries(-1, -1, -1, TimeSeriesType.INSITU);
    }

    void setIsShowingSelectedPins(boolean isShowingSelectedPins) {
        if (isShowingSelectedPins && isShowingAllPins) {
            throw new IllegalStateException("isShowingSelectedPins && isShowingAllPins");
        }
        this.isShowingSelectedPins = isShowingSelectedPins;
        updatePins();
    }

    void setIsShowingAllPins(boolean isShowingAllPins) {
        if (isShowingAllPins && isShowingSelectedPins) {
            throw new IllegalStateException("isShowingAllPins && isShowingSelectedPins");
        }
        this.isShowingAllPins = isShowingAllPins;
        updatePins();
    }

    boolean isShowingSelectedPins() {
        return isShowingSelectedPins;
    }

    boolean isShowingAllPins() {
        return isShowingAllPins;
    }


    // todo - method needed which differentiates between pin and insitu time series
    void updatePins() {
        removePinTimeSeries();
        Placemark[] pins = null;
        final ProductSceneView currentView = getCurrentView();
        if (isShowingAllPins()) {
            PlacemarkGroup pinGroup = currentView.getProduct().getPinGroup();
            pins = pinGroup.toArray(new Placemark[pinGroup.getNodeCount()]);
        } else if (isShowingSelectedPins()) {
            pins = currentView.getSelectedPins();
        }
//        pins = filterInsituPins(pins);
        if (pins == null) {
            return;
        }
        for (Placemark pin : pins) {
            final Viewport viewport = currentView.getViewport();
            final ImageLayer baseLayer = currentView.getBaseImageLayer();
            final int currentLevel = baseLayer.getLevel(viewport);
            final AffineTransform levelZeroToModel = baseLayer.getImageToModelTransform();
            final AffineTransform modelToCurrentLevel = baseLayer.getModelToImageTransform(currentLevel);
            final Point2D modelPos = levelZeroToModel.transform(pin.getPixelPos(), null);
            final Point2D currentPos = modelToCurrentLevel.transform(modelPos, null);
            updateTimeSeries((int) currentPos.getX(), (int) currentPos.getY(),
                             currentLevel, TimeSeriesType.PIN);
        }
    }

    synchronized void updateTimeSeries(int pixelX, int pixelY, int currentLevel, TimeSeriesType type) {

        final TimeSeriesGraphUpdater w = new TimeSeriesGraphUpdater(getTimeSeries(),
                                                                    createVersionSafeDataSources(), dataTarget,
                                                                    displayAxisMapping, workerChainSupport,
                                                                    pixelX, pixelY, currentLevel,
                                                                    type, version.get());
        final boolean chained = type != TimeSeriesType.CURSOR;
        setOrExecuteNextWorker(w, chained);
    }

    private TimeSeriesGraphUpdater.VersionSafeDataSources createVersionSafeDataSources() {
        final List<String> insituVariablesClone = new ArrayList<String>();
        Collections.copy(insituVariablesClone, insituVariables);

        final List<List<Band>> eoVariableBandsClone = new ArrayList<List<Band>>();
        Collections.copy(eoVariableBandsClone, eoVariableBands);

        return new TimeSeriesGraphUpdater.VersionSafeDataSources
                    (insituVariablesClone, eoVariableBandsClone, displayController.getPinPositionsToDisplay(), getVersion().get()) {
            @Override
            public int getCurrentVersion() {
                return version.get();
            }
        };
    }

    synchronized private void setOrExecuteNextWorker(SwingWorker w, boolean chained) {
        if (w == null) {
            return;
        }
        if (workerIsRunning) {
            if (chained) {
                synchronizedWorkerChain.add(w);
            } else {
                unchainedWorker = w;
            }
        } else {
            if (chained) {
                synchronizedWorkerChain.add(w);
                executeFirstWorkerInChain();
            } else {
                unchainedWorker = w;
                w.execute();
            }
            workerIsRunning = true;
        }
    }

    synchronized void removeCurrentWorkerAndExecuteNext(SwingWorker currentWorker) {
        synchronizedWorkerChain.remove(currentWorker);
        if (unchainedWorker == currentWorker) {
            unchainedWorker = null;
        }
        if (synchronizedWorkerChain.size() > 0) {
            executeFirstWorkerInChain();
            return;
        }
        if (unchainedWorker != null) {
            unchainedWorker.execute();
            return;
        }
        workerIsRunning = false;
    }

    private void executeFirstWorkerInChain() {
        synchronizedWorkerChain.get(0).execute();
    }

    private AbstractTimeSeries getTimeSeries() {
        final ProductSceneView sceneView = getCurrentView();
        final Product sceneViewProduct = sceneView.getProduct();
        return TimeSeriesMapper.getInstance().getTimeSeries(sceneViewProduct);
    }

    private Placemark[] filterInsituPins(Placemark[] pins) {
        if (pins == null) {
            return new Placemark[0];
        }
        AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(getCurrentView().getProduct());
        final List<Placemark> insituPins = timeSeries.getInsituPlacemarks();
        final List<Placemark> result = new ArrayList<Placemark>();
        for (Placemark pin : pins) {
            if (!insituPins.contains(pin)) {
                result.add(pin);
            }
        }
        return result.toArray(new Placemark[result.size()]);
    }

    private class DisplayController {

        private final List<String> eoVariablesToDisplay;
        private final List<String> insituVariablesToDisplay;
        private static final int ALPHA = 200;

        private Color[] colors = {
                    Color.red,
                    Color.green,
                    Color.blue,
                    Color.magenta,
                    Color.orange,
                    Color.darkGray,
                    Color.pink,
                    Color.cyan,
                    Color.yellow,
                    Color.red.brighter(),
                    Color.green.brighter(),
                    Color.blue.brighter(),
                    Color.blue.brighter(),
                    Color.magenta.brighter(),
                    Color.orange.darker(),
//                    new Color(0, 0, 60, ALPHA),
//                    new Color(0, 60, 0, ALPHA),
//                    new Color(60, 0, 0, ALPHA),
//                    new Color(60, 60, 60, ALPHA),
//                    new Color(0, 0, 120, ALPHA),
//                    new Color(0, 120, 0, ALPHA),
//                    new Color(120, 0, 0, ALPHA),
//                    new Color(120, 120, 120, ALPHA)
        };

        private Paint getPaint(int i) {
            return colors[i % colors.length];
        }

        private DisplayController(AbstractTimeSeries timeSeries) {
            eoVariablesToDisplay = new ArrayList<String>();
            insituVariablesToDisplay = new ArrayList<String>();
            adaptTo(timeSeries);
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
                    } else {
                        insituVariablesToDisplay.remove(insituVariableName);
                    }
                }
            }
        }

        private List<GeoPos> getPinPositionsToDisplay() {
            final ArrayList<GeoPos> pinPositionsToDisplay = new ArrayList<GeoPos>(0);
            if (isShowingAllPins) {
                final PlacemarkGroup pinGroup = getTimeSeries().getTsProduct().getPinGroup();
                final int pinCount = pinGroup.getNodeCount();
                for (int i = 0; i < pinCount; i++) {
                    final Placemark pin = pinGroup.get(i);
                    pinPositionsToDisplay.add(pin.getGeoPos());
                }
            } else if (isShowingSelectedPins) {
                final ProductSceneView sceneView = getCurrentView();
                final List<Placemark> selectedPlacemarks = Arrays.asList(sceneView.getSelectedPins());
                final PlacemarkGroup pinGroup = getTimeSeries().getTsProduct().getPinGroup();
                final int pinCount = pinGroup.getNodeCount();
                for (int i = 0; i < pinCount; i++) {
                    final Placemark pin = pinGroup.get(i);
                    if (selectedPlacemarks.contains(pin)) {
                        pinPositionsToDisplay.add(pin.getGeoPos());
                    }
                }
            }
            return pinPositionsToDisplay;
        }
    }

    private ProductSceneView getCurrentView() {
        return VisatApp.getApp().getSelectedProductSceneView();
    }
}
