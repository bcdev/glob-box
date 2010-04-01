package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.util.List;
import java.util.Map;

/**
 * User: Thomas Storm
 * Date: 31.03.2010
 * Time: 10:00:13
 */
public class TimeCoding {

    private final TimedRaster raster;

    private final ProductData.UTC startTime;

    private final ProductData.UTC endTime;

    private Map<PixelPos, ProductData.UTC> pixelToDate;

    private final boolean hasTimePerPixel;

    public TimeCoding(final RasterDataNode raster, ProductData.UTC startTime, ProductData.UTC endTime,
                      boolean hasTimePerPixel) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.raster = new TimedRaster(raster, this);
        this.hasTimePerPixel = hasTimePerPixel;
    }

    /**
     * Constructor for products which only have a start time; using this assumes that the end time is equal to the
     * start time
     *
     * @param raster    the raster the TimeCoding is associated with
     * @param startTime the time sensoring of the raster has started
     */
    public TimeCoding(final RasterDataNode raster, final ProductData.UTC startTime) {
        this(raster, startTime, startTime, false);
    }

    public List<PixelPos> getPixelsAtDate(final ProductData.UTC date) {
// TODO ts implement
        return null;
    }

    public ProductData.UTC getDateAtPixel(final PixelPos pos) {
        if (!hasTimePerPixel) { // no time per pixel set: all pixels have same time information
            return startTime;
        } else {
            return pixelToDate.get(pos);
        }
    }

    public void setPixelToDateMap(Map<PixelPos, ProductData.UTC> pixelToDate) {
        this.pixelToDate = pixelToDate;
    }

    public ProductData.UTC getEndTime() {
        return endTime;
    }

    public ProductData.UTC getStartTime() {
        return startTime;
    }
}
