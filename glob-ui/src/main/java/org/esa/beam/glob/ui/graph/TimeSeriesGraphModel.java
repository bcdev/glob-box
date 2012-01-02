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
import org.esa.beam.glob.ui.WorkerChain;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;


class TimeSeriesGraphModel {

    private static final Color DEFAULT_FOREGROUND_COLOR = Color.BLACK;
    private static final Color DEFAULT_BACKGROUND_COLOR = new Color(180, 180, 180);
    private static final String NO_DATA_MESSAGE = "No data to display";
    private static final Stroke PIN_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
                                                             new float[]{10.0f}, 0.0f);

    private final Map<AbstractTimeSeries, TimeSeriesGraphDisplayController> displayControllerMap;
    private final XYPlot timeSeriesPlot;
    private final List<List<Band>> eoVariableBands;
    private final List<String> insituVariables;
    private final List<TimeSeriesCollection> pinDatasets;
    private final List<TimeSeriesCollection> cursorDatasets;
    private final List<TimeSeriesCollection> insituDatasets;

    final private AtomicInteger version = new AtomicInteger(0);
    private TimeSeriesGraphDisplayController displayController;

    private boolean isShowingSelectedPins;
    private boolean isShowingAllPins;
    private DisplayAxisMapping displayAxisMapping;
    private final TimeSeriesGraphUpdater.WorkerChainSupport workerChainSupport;
    private final TimeSeriesGraphUpdater.TimeSeriesDataHandler dataTarget;
    private final TimeSeriesGraphDisplayController.PinSupport pinSupport;
    private final WorkerChain workerChain;

    TimeSeriesGraphModel(XYPlot plot) {
        timeSeriesPlot = plot;
        eoVariableBands = new ArrayList<List<Band>>();
        insituVariables = new ArrayList<String>();
        displayControllerMap = new WeakHashMap<AbstractTimeSeries, TimeSeriesGraphDisplayController>();
        pinDatasets = new ArrayList<TimeSeriesCollection>();
        cursorDatasets = new ArrayList<TimeSeriesCollection>();
        insituDatasets = new ArrayList<TimeSeriesCollection>();
        workerChainSupport = createWorkerChainSupport();
        dataTarget = createDataHandler();
        pinSupport = createPinSupport();
        workerChain = new WorkerChain();
        initPlot();
    }

    private TimeSeriesGraphUpdater.WorkerChainSupport createWorkerChainSupport() {
        return new TimeSeriesGraphUpdater.WorkerChainSupport() {
            @Override
            public void removeWorkerAndStartNext(TimeSeriesGraphUpdater worker) {
                workerChain.removeCurrentWorkerAndExecuteNext(worker);
            }
        };
    }

    private TimeSeriesGraphUpdater.TimeSeriesDataHandler createDataHandler() {
        return new TimeSeriesGraphUpdater.TimeSeriesDataHandler() {
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

    private TimeSeriesGraphDisplayController.PinSupport createPinSupport() {
        return new TimeSeriesGraphDisplayController.PinSupport() {
            @Override
            public boolean isShowingAllPins() {
                return isShowingAllPins;
            }

            @Override
            public boolean isShowingSelectedPins() {
                return isShowingSelectedPins;
            }

            @Override
            public Placemark[] getSelectedPins() {
                return getCurrentView().getSelectedPins();
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
                displayController = new TimeSeriesGraphDisplayController(pinSupport);
                displayControllerMap.put(timeSeries, displayController);
            }
            displayController.adaptTo(timeSeries);
            for (String eoVariableName : displayController.getEoVariablesToDisplay()) {
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

    synchronized void removeCursorTimeSeriesInWorkerThread() {
        final SwingWorker worker = new SwingWorker() {

            @Override
            protected Object doInBackground() throws Exception {
                removeCursorTimeSeries();
                return null;
            }

            @Override
            protected void done() {
                workerChain.removeCurrentWorkerAndExecuteNext(this);
            }
        };
        workerChain.setOrExecuteNextWorker(worker, false);
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
                workerChain.removeCurrentWorkerAndExecuteNext(this);
            }
        };
        workerChain.setOrExecuteNextWorker(worker, true);
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
        workerChain.setOrExecuteNextWorker(w, chained);
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

            for (String aliasName : aliasNamesSet) {
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

                timeSeriesPlot.setRangeAxis(aliasIdx, createValueAxis(aliasName));

                final int collectionIndexOffset = aliasIdx * 3;
                final int cursorCollectionIndex = collectionIndexOffset;
                final int pinCollectionIndex = 1 + collectionIndexOffset;
                final int insituCollectionIndex = 2 + collectionIndexOffset;

                final XYErrorRenderer cursorRenderer = new XYErrorRenderer();
                TimeSeriesCollection cursorDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(cursorCollectionIndex, cursorDataset);
                cursorDatasets.add(cursorDataset);

                final XYErrorRenderer pinRenderer = new XYErrorRenderer();
                TimeSeriesCollection pinDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(pinCollectionIndex, pinDataset);
                pinDatasets.add(pinDataset);
                pinRenderer.setBaseStroke(new BasicStroke());


                final XYErrorRenderer insituRenderer = new XYErrorRenderer();
                TimeSeriesCollection insituDataset = new TimeSeriesCollection();
                timeSeriesPlot.setDataset(insituCollectionIndex, insituDataset);
                insituDatasets.add(insituDataset);
                insituRenderer.setBaseShapesFilled(false);
                insituRenderer.setBaseLinesVisible(false);

                timeSeriesPlot.mapDatasetToRangeAxis(cursorCollectionIndex, aliasIdx);
                timeSeriesPlot.mapDatasetToRangeAxis(pinCollectionIndex, aliasIdx);
                timeSeriesPlot.mapDatasetToRangeAxis(insituCollectionIndex, aliasIdx);

                final Set<String> rasterNamesSet = displayAxisMapping.getRasterNames(aliasName);
                final String[] rasterNames = rasterNamesSet.toArray(new String[rasterNamesSet.size()]);
                final List<Paint> paintListForAlias = displayAxisMapping.getPaintListForAlias(aliasName);
                for (int i = 0; i < rasterNames.length; i++) {
                    cursorRenderer.setSeriesPaint(i, paintListForAlias.get(i));
                    insituRenderer.setSeriesPaint(i, paintListForAlias.get(i));
                    pinRenderer.setSeriesPaint(i, paintListForAlias.get(i));
                }

                pinRenderer.setBaseLinesVisible(true);
                pinRenderer.setDrawXError(false);

                //                pinRenderer.setBasePaint(paint);
                pinRenderer.setBaseStroke(PIN_STROKE);
                pinRenderer.setAutoPopulateSeriesPaint(true);
                pinRenderer.setAutoPopulateSeriesStroke(false);

                timeSeriesPlot.setRenderer(cursorCollectionIndex, pinRenderer, true);
                timeSeriesPlot.setRenderer(pinCollectionIndex, pinRenderer, true);
                timeSeriesPlot.setRenderer(insituCollectionIndex, pinRenderer, true);


//                pinRenderer.setSeriesShape();
//                pinRenderer.setSeriesPaint();
//                pinRenderer.setSeriesFillPaint();
//                pinRenderer.setSeriesLinesVisible();
//                pinRenderer.setSeriesOutlinePaint();
//                pinRenderer.setSeriesOutlineStroke();
//                pinRenderer.setSeriesShapesFilled();
//                pinRenderer.setSeriesShapesVisible();
//                pinRenderer.setSeriesShapesVisible();


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

    private NumberAxis createValueAxis(String aliasName) {
        String unit = getUnit(displayAxisMapping, aliasName);
        String axisLabel = getAxisLabel(aliasName, unit);
        NumberAxis valueAxis = new NumberAxis(axisLabel);
        valueAxis.setAutoRange(true);
        return valueAxis;
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

    private static String getAxisLabel(String variableName, String unit) {
        if (StringUtils.isNotNullAndNotEmpty(unit)) {
            return String.format("%s (%s)", variableName, unit);
        } else {
            return variableName;
        }
    }

    private void addTimeSeries(Map<String, List<TimeSeries>> timeSeries, TimeSeriesType type) {
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
        int index = 0;
        for (String aliasName : displayAxisMapping.getAliasNames()) {
            if (alias.equals(aliasName)) {
                return datasets.get(index);
            }
            index++;
        }
        throw new IllegalStateException(MessageFormat.format("No dataset found for alias ''{0}''.", alias));
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

    private TimeSeriesGraphUpdater.VersionSafeDataSources createVersionSafeDataSources() {
        final List<String> insituVariablesClone = new ArrayList<String>(insituVariables.size());
        insituVariablesClone.addAll(insituVariables);

        final List<List<Band>> eoVariableBandsClone = new ArrayList<List<Band>>(eoVariableBands.size());
        eoVariableBandsClone.addAll(eoVariableBands);

        return new TimeSeriesGraphUpdater.VersionSafeDataSources
                    (insituVariablesClone, eoVariableBandsClone, displayController.getPinPositionsToDisplay(), getVersion().get()) {
            @Override
            public int getCurrentVersion() {
                return version.get();
            }
        };
    }

    private AbstractTimeSeries getTimeSeries() {
        final ProductSceneView sceneView = getCurrentView();
        final Product sceneViewProduct = sceneView.getProduct();
        return TimeSeriesMapper.getInstance().getTimeSeries(sceneViewProduct);
    }

    private ProductSceneView getCurrentView() {
        return VisatApp.getApp().getSelectedProductSceneView();
    }
}
