package org.esa.beam.glob.ui;

import org.jfree.data.time.TimeSeries;

import java.util.ArrayList;
import java.util.List;

public class TimeSeriesCollection extends org.jfree.data.time.TimeSeriesCollection {

    @Override
    public List<TimeSeries> getSeries() {
        final ArrayList<TimeSeries> result = new ArrayList<TimeSeries>();
        final List seriesList = super.getSeries();
        for (Object oneSeries : seriesList) {
            result.add((TimeSeries) oneSeries);
        }
        return result;
    }

}
