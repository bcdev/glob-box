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

/**
 * @author Thomas Storm
 */
public class TimeSeriesChangeEvent {

    public static final int BAND_TO_BE_REMOVED = 1;
    private final int type;
    private final Object value;

    public TimeSeriesChangeEvent(int type, Object value) {
        this.type = type;
        this.value = value;
    }

    public int getEventType() {
        return type;
    }

    public Object getValue() {
        return value;
    }
}
