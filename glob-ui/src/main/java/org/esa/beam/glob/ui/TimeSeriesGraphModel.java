package org.esa.beam.glob.ui;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.Histogram;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.List;
import java.util.Locale;

import static org.esa.beam.glob.core.timeseries.datamodel.TimeSeries.rasterToVariableName;


class TimeSeriesGraphModel {
    private static final String NO_DATA_MESSAGE = "No data to display";

    private final TimeSeriesCollection timeSeriesCollection;
    private final XYPlot timeSeriesPlot;

    private TimeSeries cursorTimeSeries;
    private TimeSeries pinTimeSeries;
    private XYLineAnnotation xyla;
    private List<Band> variableBandList;

    TimeSeriesGraphModel(XYPlot timeSeriesPlot) {
        this.timeSeriesPlot = timeSeriesPlot;
        initPlot();
        timeSeriesCollection = new TimeSeriesCollection();
    }

    private void initPlot() {
        final ValueAxis domainAxis = timeSeriesPlot.getDomainAxis();
        domainAxis.setAutoRange(true);
        final ValueAxis rangeAxis = timeSeriesPlot.getRangeAxis();
        rangeAxis.setAutoRange(false);
        rangeAxis.setUpperBound(1.0);
        XYItemRenderer renderer = timeSeriesPlot.getRenderer();
        if (renderer instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer xyRenderer = (XYLineAndShapeRenderer) renderer;
            xyRenderer.setBaseShapesVisible(true);
            xyRenderer.setBaseShapesFilled(true);
        }
        timeSeriesPlot.setNoDataMessage(NO_DATA_MESSAGE);
    }

    void updatePlot(String rangeAxisLabel, boolean hasData) {
        timeSeriesPlot.getRangeAxis().setLabel(rangeAxisLabel);
        if (hasData) {
            timeSeriesPlot.getRangeAxis().setRange(computeYAxisRange(variableBandList));
        } else {
            timeSeriesPlot.setDataset(null);
        }
    }

   private static List<Band> createVariableBandList(RasterDataNode raster) {
       Guardian.assertNotNull("raster", raster);
       org.esa.beam.glob.core.timeseries.datamodel.TimeSeries timeSeries;
       Product product = raster.getProduct();
       timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(product);
       String rasterName = raster.getName();
       String variableName = rasterToVariableName(rasterName);
       return timeSeries.getBandsForVariable(variableName);
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

    private static double getValue(RasterDataNode raster, int pixelX, int pixelY, int currentLevel) {
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

    static TimeSeries computeTimeSeries(String title, final List<Band> bandList, int pixelX, int pixelY,
                                                int currentLevel) {
        TimeSeries timeSeries = new TimeSeries(title);
        for (Band band : bandList) {
            final ProductData.UTC startTime = band.getTimeCoding().getStartTime();
            final Millisecond timePeriod = new Millisecond(startTime.getAsDate(),
                                                           ProductData.UTC.UTC_TIME_ZONE,
                                                           Locale.getDefault());

            final double value = getValue(band, pixelX, pixelY, currentLevel);
            timeSeries.add(new TimeSeriesDataItem(timePeriod, value));
        }
        return timeSeries;
    }

    public List<Band> getVariableBandList() {
        return variableBandList;
    }

    void adaptToVariable(RasterDataNode raster) {
        if (raster != null) {
            variableBandList = createVariableBandList(raster);
        } else {
            variableBandList = null;
        }
    }

    void addSelectedPinSeries(Placemark pin, ProductSceneView view) {
         PixelPos position = pin.getPixelPos();

         final Viewport viewport = view.getViewport();
         final ImageLayer baseLayer = view.getBaseImageLayer();
         final int currentLevel = baseLayer.getLevel(viewport);
         final AffineTransform levelZeroToModel = baseLayer.getImageToModelTransform();
         final AffineTransform modelToCurrentLevel = baseLayer.getModelToImageTransform(currentLevel);
         final Point2D modelPos = levelZeroToModel.transform(position, null);
         final Point2D currentPos = modelToCurrentLevel.transform(modelPos, null);

         pinTimeSeries = computeTimeSeries("pinTimeSeries", variableBandList,
                                           (int) currentPos.getX(), (int) currentPos.getY(), currentLevel);

         timeSeriesCollection.addSeries(pinTimeSeries);
        XYItemRenderer itemRenderer = timeSeriesPlot.getRenderer();
        if (timeSeriesCollection.getSeries(0) == cursorTimeSeries) {
             itemRenderer.setSeriesPaint(0, Color.RED);
             itemRenderer.setSeriesPaint(1, Color.BLUE);
         } else {
             itemRenderer.setSeriesPaint(0, Color.BLUE);
             itemRenderer.setSeriesPaint(1, Color.RED);
         }
         timeSeriesPlot.setDataset(timeSeriesCollection);
     }


    void removePinTimeSeries() {
        if (pinTimeSeries != null) {
            timeSeriesCollection.removeSeries(pinTimeSeries);
            timeSeriesPlot.getRenderer().setSeriesPaint(0, Color.RED);
        }
    }

    void updateTimeAnnotation(RasterDataNode raster) {
        final int index = variableBandList.indexOf(raster);
        if (index != -1) {
            updateTimeAnnotation(index);
        }
    }

    void updateTimeAnnotation(int timePeriodIndex) {
        if (xyla != null) {
            timeSeriesPlot.removeAnnotation(xyla, true);
        }
        if (cursorTimeSeries != null) {
            double millisecond = cursorTimeSeries.getTimePeriod(timePeriodIndex).getFirstMillisecond();
            Range valueRange = timeSeriesPlot.getRangeAxis().getRange();
            xyla = new XYLineAnnotation(millisecond, valueRange.getLowerBound(), millisecond, valueRange.getUpperBound());
            timeSeriesPlot.addAnnotation(xyla, true);
        }
    }

    void removeCursorTimeSeries() {
        if (cursorTimeSeries != null) {
            timeSeriesCollection.removeSeries(cursorTimeSeries);
        }
    }

    void addCursortimeSeries(TimeSeries cSeries) {
        cursorTimeSeries = cSeries;
        timeSeriesCollection.addSeries(cursorTimeSeries);
        XYItemRenderer renderer = timeSeriesPlot.getRenderer();
        if (timeSeriesCollection.getSeries(0) == cursorTimeSeries) {
            renderer.setSeriesPaint(0, Color.RED);
            renderer.setSeriesPaint(1, Color.BLUE);
        } else {
            renderer.setSeriesPaint(0, Color.BLUE);
            renderer.setSeriesPaint(1, Color.RED);
        }
        timeSeriesPlot.setDataset(timeSeriesCollection);
    }


}
