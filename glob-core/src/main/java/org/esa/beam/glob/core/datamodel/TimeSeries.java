package org.esa.beam.glob.core.datamodel;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.ProductData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 29.03.2010
 * Time: 15:57:55
 */
public class TimeSeries {

    private List<TimedRaster> rasterList;

    private ProductData.UTC startTime;

    private ProductData.UTC endTime;

    private TimedRaster refRaster;

    private GeoCoding geoCoding;

    public TimeSeries(List<TimedRaster> rasterList, TimedRaster refRaster, ProductData.UTC startTime,
                      ProductData.UTC endTime) {
        this.endTime = endTime;
        this.rasterList = rasterList;
        this.refRaster = refRaster;
        this.startTime = startTime;
        validateRasterList();
    }

    public void applyGeoCoding(GeoCoding gc) {
        setGeoCoding(gc);
        for (TimedRaster tr : rasterList) {
            tr.getRaster().setGeoCoding(gc);
        }
    }

    public void setGeoCoding(GeoCoding geoCoding) {
        this.geoCoding = geoCoding;
    }

    public List<TimedRaster> getRasterList() {
        validateRasterList();
        return rasterList;
    }

    private void validateRasterList() {
        List<TimedRaster> result = new ArrayList<TimedRaster>();
        for (TimedRaster tr : rasterList) {
            final Date rasterStartTime = tr.getTimeCoding().getStartTime().getAsDate();
            final Date rasterEndTime = tr.getTimeCoding().getEndTime().getAsDate();
            final boolean isValidStartTime = rasterStartTime.equals(startTime.getAsDate())
                                             || rasterStartTime.after(startTime.getAsDate());
            final boolean isValidEndTime = rasterEndTime.equals(endTime.getAsDate())
                                           || rasterEndTime.before(endTime.getAsDate());
            if (isValidStartTime && isValidEndTime) {
                result.add(tr);
            }
        }
        this.rasterList = result;
    }

}
