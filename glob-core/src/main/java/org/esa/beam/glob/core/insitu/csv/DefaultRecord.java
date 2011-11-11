package org.esa.beam.glob.core.insitu.csv;

import org.esa.beam.framework.datamodel.GeoPos;

import java.util.Arrays;
import java.util.Date;

/**
 * A default implementation of a {@link Record}.
 *
 * @author MarcoZ
 * @author Norman
 */
class DefaultRecord implements Record {
    private final GeoPos location;
    private final Date time;
    private final Object[] values;

    DefaultRecord(GeoPos location, Date time, Object[] values) {
        this.location = location;
        this.time = time;
        this.values = values;
    }

    @Override
    public GeoPos getLocation() {
        return location;
    }

    @Override
    public Date getTime() {
        return time;
    }

    @Override
    public Object[] getAttributeValues() {
        return values;
    }

    @Override
    public String toString() {
        return "DefaultRecord{" +
                "location=" + location +
                ", time=" + time +
                ", values=" + Arrays.asList(values) +
                '}';
    }
}
