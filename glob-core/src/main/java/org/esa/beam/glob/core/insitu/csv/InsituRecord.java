package org.esa.beam.glob.core.insitu.csv;

import org.esa.beam.framework.datamodel.GeoPos;
import java.util.Date;

public class InsituRecord {

    public final GeoPos pos;
    public final Date time;
    public final double value;

    public InsituRecord(GeoPos pos, Date time, double value) {
        this.pos = pos;
        this.time = time;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final InsituRecord that = (InsituRecord) o;

        if (Double.compare(that.value, value) != 0) {
            return false;
        }
        if (pos != null ? !pos.equals(that.pos) : that.pos != null) {
            return false;
        }
        if (time != null ? !time.equals(that.time) : that.time != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = pos != null ? pos.hashCode() : 0;
        result = 31 * result + (time != null ? time.hashCode() : 0);
        long temp = value != +0.0d ? Double.doubleToLongBits(value) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
