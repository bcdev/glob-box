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

import com.bc.ceres.glayer.support.*;
import com.bc.ceres.grender.*;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.product.*;
import org.esa.beam.glob.core.*;
import org.esa.beam.glob.core.insitu.*;
import org.esa.beam.glob.core.insitu.csv.*;
import org.esa.beam.glob.core.timeseries.datamodel.*;
import org.esa.beam.util.*;
import org.esa.beam.visat.*;
import org.jfree.chart.annotations.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.*;
import org.jfree.data.time.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;


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

    private final AtomicInteger version = new AtomicInteger(0);
    private DisplayController displayController;

    private final List<SwingWorker> synchronizedWorkerChain;
    private SwingWorker unchainedWorker;
    private boolean workerIsRunning = false;
    private boolean isShowingSelectedPins;
    private boolean isShowingAllPins;

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
    }

    private void initPlot() {
        final ValueAxis domainAxis = timeSeriesPlot.getDomainAxis();
        domainAxis.setAutoRange(true);
        XYLineAndShapeRenderer xyRenderer = new XYLineAndShapeRenderer(true, true);
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

    private void updatePlot(boolean hasData, AbstractTimeSeries timeSeries) {
        for (int i = 0; i < timeSeriesPlot.getDatasetCount(); i++) {
            timeSeriesPlot.setDataset(i, null);
        }
        timeSeriesPlot.clearRangeAxes();
        pinDatasets.clear();
        cursorDatasets.clear();
        insituDatasets.clear();

        if (hasData) {
            final AxisMappingModel displayAxisModel = createDisplayAxisModel(timeSeries);
            final Set<String> aliasNamesSet = displayAxisModel.getAliasNames();
            final String[] aliasNames = aliasNamesSet.toArray(new String[aliasNamesSet.size()]);
            for (int i = 0; i < aliasNames.length; i++) {
                String aliasName = aliasNames[i];

                Paint paint = displayController.getPaint(i);
                String unit = getUnit(displayAxisModel, aliasName);
                String axisLabel = getAxisLabel(aliasName, unit);
                NumberAxis valueAxis = new NumberAxis(axisLabel);
                valueAxis.setAutoRange(true);
                valueAxis.setAxisLinePaint(paint);
                valueAxis.setLabelPaint(paint);
                valueAxis.setTickLabelPaint(paint);
                valueAxis.setTickMarkPaint(paint);
                timeSeriesPlot.setRangeAxis(i, valueAxis);

                XYLineAndShapeRenderer pinRenderer = new XYLineAndShapeRenderer(true, true);
                pinRenderer.setBasePaint(paint);
                pinRenderer.setBaseStroke(PIN_STROKE);
                pinRenderer.setAutoPopulateSeriesPaint(true);
                pinRenderer.setAutoPopulateSeriesStroke(false);

                final int cursorIndex = i * 3;
                final int pinIndex = 1 + i * 3;
                final int insituIndex = 2 + i * 3;

                TimeSeriesCollection cursorDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(cursorIndex, cursorDataset);
                cursorDatasets.add(cursorDataset);

                TimeSeriesCollection pinDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(pinIndex, pinDataset);
                pinDatasets.add(pinDataset);

                TimeSeriesCollection insituDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(insituIndex, insituDataset);
                insituDatasets.add(insituDataset);

                timeSeriesPlot.setRenderer(cursorIndex, pinRenderer, true);
                timeSeriesPlot.setRenderer(pinIndex, pinRenderer, true);
                timeSeriesPlot.setRenderer(insituIndex, pinRenderer, true);

                timeSeriesPlot.mapDatasetToRangeAxis(cursorIndex, i);
                timeSeriesPlot.mapDatasetToRangeAxis(pinIndex, i);
                timeSeriesPlot.mapDatasetToRangeAxis(insituIndex, i);
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

    private AxisMappingModel createDisplayAxisModel(AbstractTimeSeries timeSeries) {
        final List<String> eoVariables = displayController.getEoVariablesToDisplay();
        final List<String> insituVariables = displayController.getInsituVariablesToDisplay();
        final AxisMappingModel axisMappingModel = timeSeries.getAxisMappingModel();
        return createDisplayAxisModel(eoVariables, insituVariables, axisMappingModel);
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

    private AxisMappingModel createDisplayAxisModel(List<String> eoVariables, List<String> insituVariables, AxisMappingModel axisMappingModel) {
        final AxisMappingModel displayAxisModel = new AxisMappingModel();

        for (String eoVariable : eoVariables) {
            final String aliasName = axisMappingModel.getRasterAlias(eoVariable);
            if(aliasName == null) {
                displayAxisModel.addAlias(eoVariable);
                displayAxisModel.addRasterName(eoVariable, eoVariable);
            } else {
                displayAxisModel.addAlias(aliasName);
                displayAxisModel.addRasterName(aliasName, eoVariable);
            }
        }

        for (String insituVariable : insituVariables) {
            final String aliasName = axisMappingModel.getInsituAlias(insituVariable);
            if(aliasName == null) {
                displayAxisModel.addAlias(insituVariable);
                displayAxisModel.addInsituName(insituVariable, insituVariable);
            } else {
                displayAxisModel.addAlias(aliasName);
                displayAxisModel.addInsituName(aliasName, insituVariable);
            }
        }

        return displayAxisModel;
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

    private void addTimeSeries(Map<String, List<TimeSeries>> timeSeries, TimeSeriesType type) {
        final AxisMappingModel displayAxisModel = createDisplayAxisModel(getTimeSeries());
        List<TimeSeriesCollection> datasets = getDatasets(type);
        for (String alias : displayAxisModel.getAliasNames()) {
            TimeSeriesCollection aliasDataset = getDatasetForAlias(alias, datasets);
            final List<TimeSeries> aliasTimeSerieses = timeSeries.get(alias);
            for (TimeSeries aliasTimeSeriese : aliasTimeSerieses) {
                aliasDataset.addSeries(aliasTimeSeriese);
            }
        }
    }

    private TimeSeriesCollection getDatasetForAlias(String alias, List<TimeSeriesCollection> datasets) {
        final AxisMappingModel displayAxisModel = createDisplayAxisModel(getTimeSeries());
        int index = 0;
        for (String aliasName : displayAxisModel.getAliasNames()) {
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
        updateTimeSeries(-1, -1, -1, TimeSeriesType.INSITU, true);
    }

    void setIsShowingSelectedPins(boolean isShowingSelectedPins) {
        this.isShowingSelectedPins = isShowingSelectedPins;
        updatePins();
    }

    void setIsShowingAllPins(boolean isShowingAllPins) {
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
        if(pins == null) {
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
                    currentLevel, TimeSeriesType.PIN, true);
        }
    }

    synchronized void updateTimeSeries(int pixelX, int pixelY, int currentLevel, TimeSeriesType type, boolean chained) {
        final TimeSeriesUpdater w = new TimeSeriesUpdater(pixelX, pixelY, currentLevel, type, version.get());
        setOrExecuteNextWorker(w, chained);
    }

    synchronized private void setOrExecuteNextWorker(SwingWorker w, boolean chained) {
        if (w == null) {
            return;
        }
        if (workerIsRunning) {
            if (chained) {
                synchronizedWorkerChain.add(w);
//                System.out.println("add worker to chain");
            } else {
                unchainedWorker = w;
//                System.out.println("replace unchained worker");
            }
        } else {
//            System.out.println("===================================================");
            if (chained) {
                synchronizedWorkerChain.add(w);
//                System.out.println("start working chained");
                executeFirstWorkerInChain();
            } else {
                unchainedWorker = w;
//                System.out.println("start working unchained");
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
//            System.out.println("execute unchained worker");
            return;
        }
        workerIsRunning = false;
//        System.out.println("stop working");
//        System.out.println("");
    }

    private void executeFirstWorkerInChain() {
        synchronizedWorkerChain.get(0).execute();
//        System.out.println("excecute first worker in chain");
    }

    private AbstractTimeSeries getTimeSeries() {
        final ProductSceneView sceneView = getCurrentView();
        final Product sceneViewProduct = sceneView.getProduct();
        return TimeSeriesMapper.getInstance().getTimeSeries(sceneViewProduct);
    }

    private Placemark[] filterInsituPins(Placemark[] pins) {
        if(pins == null) {
            return new Placemark[0];
        }
        AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(getCurrentView().getProduct());
        final List<Placemark> insituPins = timeSeries.getInsituPlacemarks();
        final List<Placemark> result = new ArrayList<Placemark>();
        for (Placemark pin : pins) {
            if(!insituPins.contains(pin)) {
                result.add(pin);
            }
        }
        return result.toArray(new Placemark[result.size()]);
    }

    private class TimeSeriesUpdater extends SwingWorker<Map<String, List<TimeSeries>>, Void> {

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
        protected Map<String, List<TimeSeries>> doInBackground() throws Exception {
            if (version.get() != myVersion) {
                return Collections.emptyMap();
            }

            final AxisMappingModel axisMappingModel = createDisplayAxisModel(getTimeSeries());
            switch (type) {
                case INSITU: {
                    return computeInsituTimeSeries(axisMappingModel);
                }
                default: {
                    return computeRasterTimeSeries(axisMappingModel);
                }
            }
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
                removeCurrentWorkerAndExecuteNext(this);
            }
        }

        private Map<String, List<TimeSeries>> computeRasterTimeSeries(AxisMappingModel axisMappingModel) {
            final Set<String> aliasNames = axisMappingModel.getAliasNames();
            final Map<String, List<TimeSeries>> rasterTimeSeriesForAlias = new HashMap<String, List<TimeSeries>>();
            for (String aliasName : aliasNames) {
                final List<List<Band>> bandList = getListOfBandLists(aliasName, axisMappingModel);
                final List<TimeSeries> tsList = new ArrayList<TimeSeries>();
                for (List<Band> bands : bandList) {
                    final TimeSeries timeSeries = computeSingleTimeSeries(bands, pixelX, pixelY, currentLevel);
                    tsList.add(timeSeries);
                }
                rasterTimeSeriesForAlias.put(aliasName, tsList);
            }
            return rasterTimeSeriesForAlias;
        }

        private Map<String, List<TimeSeries>> computeInsituTimeSeries(AxisMappingModel axisMappingModel) {
            AbstractTimeSeries globTimeSeries = getTimeSeries();
            final InsituSource insituSource = globTimeSeries.getInsituSource();
            final Product timeSeriesProduct = globTimeSeries.getTsProduct();
            final GeoCoding geoCoding = timeSeriesProduct.getGeoCoding();
            final Map<String, List<TimeSeries>> insituTimeSeriesForAlias = new HashMap<String, List<TimeSeries>>();
            final Set<String> aliasNames = axisMappingModel.getAliasNames();
            for (String aliasName : aliasNames) {
                final List<TimeSeries> timeSerieses = new ArrayList<TimeSeries>();
                final Set<String> insituNames = axisMappingModel.getInsituNames(aliasName);
                final Set<String> insituVariablesForAlias = new HashSet<String>();
                for (String insituName : insituNames) {
                    if (insituVariables.contains(insituName)) {
                        insituVariablesForAlias.add(insituName);
                    }
                }
                for (String insituVariable : insituVariablesForAlias) {
                    final GeoPos[] insituPositions = insituSource.getInsituPositionsFor(insituVariable);
                    final List<GeoPos> pinPositionsToDisplay = displayController.getPinPositionsToDisplay();
                    PixelPos pixelPos = new PixelPos();
                    for (GeoPos insituPosition : insituPositions) {
                        if (!contains(pinPositionsToDisplay, insituPosition)) {
                            continue;
                        }
                        geoCoding.getPixelPos(insituPosition, pixelPos);
                        if (!AbstractTimeSeries.isPixelValid(timeSeriesProduct, pixelPos)) {
                            continue;
                        }
                        InsituRecord[] insituRecords = insituSource.getValuesFor(insituVariable, insituPosition);
                        final TimeSeries timeSeries = computeSingleTimeSeries(insituRecords);
                        timeSerieses.add(timeSeries);
                    }
                }
                insituTimeSeriesForAlias.put(aliasName, timeSerieses);
            }
            return insituTimeSeriesForAlias;
        }

        private boolean contains(List<GeoPos> pinPositionsToDisplay, GeoPos insituPosition) {
            for (GeoPos geoPos : pinPositionsToDisplay) {
                if (Math.abs(geoPos.lat - insituPosition.lat) < 0.001 &&
                        Math.abs(geoPos.lon - insituPosition.lon) < 0.001) {
                    return true;
                }
            }
            return false;
        }

        private List<List<Band>> getListOfBandLists(String aliasName, AxisMappingModel axisMappingModel) {
            List<List<Band>> bandList = new ArrayList<List<Band>>();
            final Set<String> rasterNames = axisMappingModel.getRasterNames(aliasName);
            for (List<Band> eoVariableBandList : eoVariableBands) {
                for (String rasterName : rasterNames) {
                    final Band raster = eoVariableBandList.get(0);
                    if (raster.getName().startsWith(rasterName)) {
                        bandList.add(eoVariableBandList);
                    }
                }
            }
            return bandList;
        }

        private TimeSeries computeSingleTimeSeries(InsituRecord[] insituRecords) {
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

        private TimeSeries computeSingleTimeSeries(final List<Band> bandList, int pixelX, int pixelY, int currentLevel) {
            final Band firstBand = bandList.get(0);
            final String firstBandName = firstBand.getName();
            final int lastUnderscore = firstBandName.lastIndexOf("_");
            final String timeSeriesName = firstBandName.substring(0, lastUnderscore);
            final TimeSeries timeSeries = new TimeSeries(timeSeriesName);
            // @todo se ... find a better solution to ensure only valid entries in time series
            final double noDataValue = firstBand.getNoDataValue();
            final AbstractTimeSeries globTimeSeries = getTimeSeries();
            for (Band band : bandList) {
                final TimeCoding timeCoding = globTimeSeries.getRasterTimeMap().get(band);
                if (timeCoding != null) {
                    final ProductData.UTC startTime = timeCoding.getStartTime();
                    final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                            ProductData.UTC.UTC_TIME_ZONE,
                            Locale.getDefault());
                    final double value = getValue(band, pixelX, pixelY, currentLevel);
                    if (value != noDataValue) {
                        timeSeries.add(new TimeSeriesDataItem(timePeriod, value));
                    }
                }
            }
            return timeSeries;
        }
    }

    private class DisplayController {

        private final List<String> eoVariablesToDisplay;
        private final List<String> insituVariablesToDisplay;
        private static final int ALPHA = 200;

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
                for(int i = 0; i < pinCount; i++) {
                    final Placemark pin = pinGroup.get(i);
                    pinPositionsToDisplay.add(pin.getGeoPos());
                }
            } else if (isShowingSelectedPins) {
                final ProductSceneView sceneView = getCurrentView();
                final List<Placemark> selectedPlacemarks = Arrays.asList(sceneView.getSelectedPins());
                final PlacemarkGroup pinGroup = getTimeSeries().getTsProduct().getPinGroup();
                final int pinCount = pinGroup.getNodeCount();
                for(int i = 0; i < pinCount; i++) {
                    final Placemark pin = pinGroup.get(i);
                    if(selectedPlacemarks.contains(pin)) {
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
