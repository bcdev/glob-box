package org.esa.beam.glob.core.datamodel;

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

    private Map<Integer, ProductData.UTC> pixelToDate;

    public TimeCoding(final RasterDataNode raster, ProductData.UTC startTime, ProductData.UTC endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.raster = new TimedRaster(raster, this);
    }

    public List<PixelPos> getPixelsAtDate(final ProductData.UTC date) {
// todo ts implement
        return null;
    }

    public ProductData.UTC getDateAtPixel(final PixelPos pos) {
// todo ts implement        
        return null;
    }

//    public Map<Integer, ProductData.UTC> getPixelToDate() {
//        if( pixelToDate == null ) {
//
//        }
//    }

    public void setPixelToDateMap(Map<Integer, ProductData.UTC> pixelToDate) {
        this.pixelToDate = pixelToDate;
    }

    public ProductData.UTC getEndTime() {
        return endTime;
    }

    public ProductData.UTC getStartTime() {
        return startTime;
    }
}
