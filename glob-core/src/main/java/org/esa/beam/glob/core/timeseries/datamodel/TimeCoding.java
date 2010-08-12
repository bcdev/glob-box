/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.glob.core.timeseries.datamodel;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;

/**
 * <p><i>Note that this class is not yet public API. Interface may chhange in future releases.</i></p>
 * <p/>
 * Abstract class representing a time-coding. A time-coding is defined by a start and an end time and thus represents
 * a time span. It maps time information to pixel-positions.
 */
public abstract class TimeCoding {

    private ProductData.UTC startTime;

    private ProductData.UTC endTime;

    /**
     * Constructor creates a new TimeCoding-instance with a given start and end time.
     *
     * @param startTime the start time of the time span represented by the time-coding
     * @param endTime   the end time of the time span represented by the time-coding
     */
    protected TimeCoding(ProductData.UTC startTime, ProductData.UTC endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * Allows to retrieve time information for a given pixel.
     *
     * @param pos the pixel position to retrieve time information for
     *
     * @return the time at the given pixel position, can be {@code null} if time can not be determined.
     */
    public abstract ProductData.UTC getTime(final PixelPos pos);

    /**
     * Getter for the start time
     *
     * @return the start time, may be {@code null}
     */
    public ProductData.UTC getStartTime() {
        return startTime;
    }

    /**
     * Getter for the end time
     *
     * @return the end time, may be {@code null}
     */
    public ProductData.UTC getEndTime() {
        return endTime;
    }

    /**
     * Setter for the start time
     *
     * @param startTime the start time to set
     */
    public void setStartTime(ProductData.UTC startTime) {
        this.startTime = startTime;
    }

    /**
     * Setter for the end time
     *
     * @param endTime the end time to set
     */
    public void setEndTime(ProductData.UTC endTime) {
        this.endTime = endTime;
    }

    /**
     * Checks if the given {@code time} is within the start and end time of this {@link TimeCoding}.
     *
     * @param time the time to check if it is within this {@code TimeCoding}
     * @return whether this {@code TimeCoding} contains the given time
     */
    public boolean isWithin(ProductData.UTC time) {
        if (getStartTime() == null || time.getAsCalendar().compareTo(getStartTime().getAsCalendar()) < 0) {
            return false;
        }
        if (getEndTime() == null || time.getAsCalendar().compareTo(getEndTime().getAsCalendar()) > 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TimeCoding that = (TimeCoding) o;

        boolean startEqual = areEqual(startTime, that.startTime);
        boolean endEqual = areEqual(endTime, that.endTime);
        return startEqual && endEqual;
    }

    private boolean areEqual(ProductData.UTC time1, ProductData.UTC time2) {
        if (time1 == null && time2 == null) {
            return true;
        }

        if (time1 == null || time2 == null) {
            return false;
        }

        return time1.getAsDate().getTime() == time2.getAsDate().getTime();

    }

    @Override
    public int hashCode() {
        int result = startTime != null ? startTime.hashCode() : 0;
        result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
        return result;
    }
}