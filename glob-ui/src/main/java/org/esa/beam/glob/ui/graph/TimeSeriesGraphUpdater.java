package org.esa.beam.glob.ui.graph;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.glob.core.insitu.InsituSource;
import org.esa.beam.glob.core.insitu.csv.InsituRecord;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.util.ProductUtils;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;

import javax.swing.SwingWorker;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

class TimeSeriesGraphUpdater extends SwingWorker<Map<String, List<TimeSeries>>, Void> {

    static final Position NULL_POSITION = new Position(-1, -1, -1);

    private final WorkerChainSupport workerChainSupport;
    private final Position position;
    private final TimeSeriesType type;
    private final int version;
    private final AbstractTimeSeries timeSeries;
    private final TimeSeriesDataHandler dataHandler;
    private final VersionSafeDataSources dataSources;
    private final DisplayAxisMapping displayAxisMapping;

    TimeSeriesGraphUpdater(AbstractTimeSeries timeSeries, VersionSafeDataSources dataSources, TimeSeriesDataHandler dataHandler, DisplayAxisMapping displayAxisMapping, WorkerChainSupport workerChainSupport, Position position, TimeSeriesType type, int version) {
        super();
        this.timeSeries = timeSeries;
        this.dataHandler = dataHandler;
        this.dataSources = dataSources;
        this.displayAxisMapping = displayAxisMapping;
        this.workerChainSupport = workerChainSupport;
        this.position = position;
        this.type = type;
        this.version = version;
    }

    @Override
    protected Map<String, List<TimeSeries>> doInBackground() throws Exception {
        if (dataSources.getCurrentVersion() != version) {
            return Collections.emptyMap();
        }

        if (type == TimeSeriesType.INSITU) {
            return computeInsituTimeSeries();
        } else {
            return computeRasterTimeSeries();
        }
    }

    @Override
    protected void done() {
        if (dataSources.getCurrentVersion() != version) {
            return;
        }
        if (type.equals(TimeSeriesType.CURSOR)) {
            dataHandler.removeCursorTimeSeries();
        }
        try {

            dataHandler.collectTimeSeries(get(), type);
        } catch (InterruptedException ignore) {
            ignore.printStackTrace();
        } catch (ExecutionException ignore) {
            ignore.printStackTrace();
        } finally {
            workerChainSupport.removeWorkerAndStartNext(this);
        }
    }

    private Map<String, List<TimeSeries>> computeRasterTimeSeries() {
        final Set<String> aliasNames = displayAxisMapping.getAliasNames();
        final Map<String, List<TimeSeries>> rasterTimeSeriesForAlias = new HashMap<String, List<TimeSeries>>();
        for (String aliasName : aliasNames) {
            final List<List<Band>> bandList = getListOfBandLists(aliasName);
            final List<TimeSeries> tsList = new ArrayList<TimeSeries>();
            for (List<Band> bands : bandList) {
                final TimeSeries timeSeries = computeSingleTimeSeries(bands, position.pixelX, position.pixelY, position.currentLevel);
                tsList.add(timeSeries);
            }
            rasterTimeSeriesForAlias.put(aliasName, tsList);
        }
        return rasterTimeSeriesForAlias;
    }

    private Map<String, List<TimeSeries>> computeInsituTimeSeries() {
        final InsituSource insituSource = timeSeries.getInsituSource();
        final Product timeSeriesProduct = timeSeries.getTsProduct();
        final GeoCoding geoCoding = timeSeriesProduct.getGeoCoding();
        final Map<String, List<TimeSeries>> insituTimeSeriesForAlias = new HashMap<String, List<TimeSeries>>();

        final Set<String> aliasNames = displayAxisMapping.getAliasNames();


        for (String aliasName : aliasNames) {
            final List<TimeSeries> timeSerieses = new ArrayList<TimeSeries>();
            final Set<String> insituNames = displayAxisMapping.getInsituNames(aliasName);
            final Set<String> insituVariablesForAlias = new HashSet<String>();
            for (String insituName : insituNames) {
                if (dataSources.getInsituVariables().contains(insituName)) {
                    insituVariablesForAlias.add(insituName);
                }
            }
            for (String insituVariable : insituVariablesForAlias) {
                final GeoPos[] insituPositions = insituSource.getInsituPositionsFor(insituVariable);
                final List<GeoPos> pinPositionsToDisplay = dataSources.getPinPositionsToDisplay();
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

    private List<List<Band>> getListOfBandLists(String aliasName) {
        List<List<Band>> bandList = new ArrayList<List<Band>>();
        final Set<String> rasterNames = displayAxisMapping.getRasterNames(aliasName);
        for (List<Band> eoVariableBandList : dataSources.getRasterSources()) {
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
        for (Band band : bandList) {
            final TimeCoding timeCoding = this.timeSeries.getRasterTimeMap().get(band);
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

    static class Position {

        private final int pixelX;
        private final int pixelY;
        private final int currentLevel;

        Position(int pixelX, int pixelY, int currentLevel) {
            this.currentLevel = currentLevel;
            this.pixelY = pixelY;
            this.pixelX = pixelX;
        }
    }

    static interface TimeSeriesDataHandler {

        void collectTimeSeries(Map<String, List<TimeSeries>> data, TimeSeriesType type);

        void removeCursorTimeSeries();
    }

    static interface WorkerChainSupport {

        void removeWorkerAndStartNext(TimeSeriesGraphUpdater worker);
    }

    static abstract class VersionSafeDataSources {

        private final List<String> insituVariables;
        private final List<List<Band>> rasterSources;
        private final List<GeoPos> pinPositionsToDisplay;
        private final int version;

        protected VersionSafeDataSources(List<String> insituVariables, List<List<Band>> rasterSources, List<GeoPos> pinPositionsToDisplay, final int version) {
            this.insituVariables = insituVariables;
            this.rasterSources = rasterSources;
            this.pinPositionsToDisplay = pinPositionsToDisplay;
            this.version = version;
        }

        public List<String> getInsituVariables() {
            if (canReturnValues()) {
                return insituVariables;
            }
            return Collections.emptyList();
        }

        public List<List<Band>> getRasterSources() {
            if (canReturnValues()) {
                return rasterSources;
            }
            return Collections.emptyList();
        }

        public List<GeoPos> getPinPositionsToDisplay() {
            if (canReturnValues()) {
                return pinPositionsToDisplay;
            }
            return Collections.emptyList();
        }

        protected abstract int getCurrentVersion();

        private boolean canReturnValues() {
            return getCurrentVersion() == version;
        }
    }

}
