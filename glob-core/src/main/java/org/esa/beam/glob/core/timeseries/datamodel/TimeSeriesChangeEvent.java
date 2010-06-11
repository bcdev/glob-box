package org.esa.beam.glob.core.timeseries.datamodel;

/**
 * User: Thomas Storm
 * Date: 11.06.2010
 * Time: 17:17:38
 */
public class TimeSeriesChangeEvent {

    private TimeSeriesProperty property;
    private Object newValue;
    private Object oldValue;

    public TimeSeriesChangeEvent(TimeSeriesProperty property, Object oldValue, Object newValue) {
        this.property = property;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public TimeSeriesProperty getProperty() {
        return property;
    }
}
