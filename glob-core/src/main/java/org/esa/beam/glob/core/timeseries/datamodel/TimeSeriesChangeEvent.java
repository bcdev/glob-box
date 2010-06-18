package org.esa.beam.glob.core.timeseries.datamodel;

/**
 * User: Thomas Storm
 * Date: 11.06.2010
 * Time: 17:17:38
 */
public class TimeSeriesChangeEvent {

    private TimeSeriesEventType eventType;
    private Object newValue;
    private Object oldValue;

    public TimeSeriesChangeEvent(TimeSeriesEventType eventType, Object oldValue, Object newValue) {
        this.eventType = eventType;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public TimeSeriesEventType getEventType() {
        return eventType;
    }
}
