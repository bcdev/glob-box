package org.esa.beam.glob.core.timeseries.datamodel;

/**
 * User: Thomas Storm
 * Date: 11.06.2010
 * Time: 17:21:10
 */
public interface TimeSeriesListener {

    void timeSeriesChanged(TimeSeriesChangeEvent timeSeriesChangeEvent);
}
