package org.esa.beam.glob.ui.graph;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

class TimeSeriesGraphUpdater extends SwingWorker<List<TimeSeries>, Void> {

    private final WorkerChainSupport workerChainSupport;
    private final Position cursorPosition;
    private final PositionSupport positionSupport;
    private final TimeSeriesType type;
    private final int version;
    private final AbstractTimeSeries timeSeries;
    private final TimeSeriesDataHandler dataHandler;
    private final VersionSafeDataSources dataSources;
    private final DisplayAxisMapping displayAxisMapping;

    TimeSeriesGraphUpdater(AbstractTimeSeries timeSeries, VersionSafeDataSources dataSources, TimeSeriesDataHandler dataHandler, DisplayAxisMapping displayAxisMapping, WorkerChainSupport workerChainSupport, Position cursorPosition, PositionSupport positionSupport, TimeSeriesType type, int version) {
        super();
        this.timeSeries = timeSeries;
        this.dataHandler = dataHandler;
        this.dataSources = dataSources;
        this.displayAxisMapping = displayAxisMapping;
        this.workerChainSupport = workerChainSupport;
        this.cursorPosition = cursorPosition;
        this.positionSupport = positionSupport;
        this.type = type;
        this.version = version;
        if (TimeSeriesType.CURSOR.equals(type)) {
            Assert.notNull(cursorPosition);
        } else {
            Assert.argument(cursorPosition == null);
        }
    }

    @Override
    protected List<TimeSeries> doInBackground() throws Exception {
        if (dataSources.getCurrentVersion() != version) {
            return Collections.emptyList();
        }

        switch (type) {
            case INSITU:
                return computeInsituTimeSeries();
            case PIN:
                return computeRasterTimeSeries();
            case CURSOR:
                return computeRasterTimeSeries();
        }
        throw new IllegalStateException("Unknown type '" + type + "'.");
    }

    @Override
    protected void done() {
        try {
            if (dataSources.getCurrentVersion() != version) {
                return;
            }
//            if (type.equals(TimeSeriesType.CURSOR)) {
//                dataHandler.removeCursorTimeSeries();
//            }
            dataHandler.collectTimeSeries(get(), type);
        } catch (InterruptedException ignore) {
            ignore.printStackTrace();
        } catch (ExecutionException ignore) {
            ignore.printStackTrace();
        } finally {
            workerChainSupport.removeWorkerAndStartNext(this);
        }
    }

    private List<TimeSeries> computeRasterTimeSeries() {
        final List<Position> positionsToDisplay = new ArrayList<Position>();
        if (type.equals(TimeSeriesType.PIN)) {
            final List<GeoPos> pinPositionsToDisplay = dataSources.getPinPositionsToDisplay();
            for (GeoPos geoPos : pinPositionsToDisplay) {
                positionsToDisplay.add(positionSupport.transformGeoPos(geoPos));
            }
        } else {
            positionsToDisplay.add(cursorPosition);
        }

        final Set<String> aliasNames = displayAxisMapping.getAliasNames();
        final List<TimeSeries> rasterTimeSeries = new ArrayList<TimeSeries>();

        for (Position position : positionsToDisplay) {
            for (String aliasName : aliasNames) {
                final Set<String> rasterNames = displayAxisMapping.getRasterNames(aliasName);
                for (String rasterName : rasterNames) {
                    final List<Band> bandsForVariable = timeSeries.getBandsForVariable(rasterName);
                    final TimeSeries timeSeries = computeSingleTimeSeries(bandsForVariable, position.pixelX,
                            position.pixelY, position.currentLevel);
                    rasterTimeSeries.add(timeSeries);
                }
            }
        }
        return rasterTimeSeries;
    }

    private List<TimeSeries> computeInsituTimeSeries() {
        final InsituSource insituSource = timeSeries.getInsituSource();
        final List<TimeSeries> insituTimeSeries = new ArrayList<TimeSeries>();

        final Set<String> aliasNames = displayAxisMapping.getAliasNames();
        final List<GeoPos> pinPositionsToDisplay = dataSources.getPinPositionsToDisplay();

        for (GeoPos pinPos : pinPositionsToDisplay) {
            for (String aliasName : aliasNames) {
                final Set<String> insituNames = displayAxisMapping.getInsituNames(aliasName);
                for (String insituName : insituNames) {
                    InsituRecord[] insituRecords = insituSource.getValuesFor(insituName, pinPos);
                    final TimeSeries timeSeries = computeSingleTimeSeries(insituRecords);
                    insituTimeSeries.add(timeSeries);
                }
            }
        }

        return insituTimeSeries;
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

        void collectTimeSeries(List<TimeSeries> data, TimeSeriesType type);
    }

    static interface WorkerChainSupport {

        void removeWorkerAndStartNext(TimeSeriesGraphUpdater worker);
    }

    static abstract class VersionSafeDataSources {

        private final List<GeoPos> pinPositionsToDisplay;
        private final int version;

        protected VersionSafeDataSources(List<GeoPos> pinPositionsToDisplay, final int version) {
            this.pinPositionsToDisplay = pinPositionsToDisplay;
            this.version = version;
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

    static interface PositionSupport {
        Position transformGeoPos(GeoPos geoPos);
    }

}
