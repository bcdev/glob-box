package org.esa.beam.glob.core.timeseries.parser;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import static org.esa.beam.framework.datamodel.ProductData.*;

/**
 * User: Thomas Storm
 * Date: 01.04.2010
 * Time: 14:26:27
 */
public class GlobColourTimeHandler implements TimeDataHandler {

    @Override
    public TimeCoding generateTimeCoding(RasterDataNode raster) {
        final File file = raster.getProduct().getFileLocation();
        UTC[] dates = null;
        try {
            dates = getTimeInformation(NetcdfFile.open(file.getAbsolutePath()));
        } catch (IOException e) {
            return null;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (dates != null) {
            if (dates.length > 1) {
                return new TimeCoding(raster, dates[0], dates[1], false); // in any case we do not use time per pixel
            } else {
                return new TimeCoding(raster, dates[0]);
            }
        } else {
            return null;
        }
    }

    UTC[] getTimeInformation(NetcdfFile file) throws ParseException {
        String startTimeString = "";
        String endTimeString = "";
        for (Attribute att : file.getGlobalAttributes()) {
            final String attributeName = att.getName();
            if (attributeName.equals("start_time")) {
                startTimeString = att.getStringValue();
            } else if (attributeName.equals("end_time")) {
                endTimeString = att.getStringValue();
            }
        }

        UTC startTime = UTC.parse(startTimeString, "yyyyMMddHHmmss");
        UTC endTime = UTC.parse(endTimeString, "yyyyMMddHHmmss");
        return new UTC[]{startTime, endTime};
    }
}
