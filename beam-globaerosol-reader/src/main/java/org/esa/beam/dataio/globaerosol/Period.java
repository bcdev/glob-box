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

package org.esa.beam.dataio.globaerosol;

import java.util.Calendar;

enum Period {

    DAILY(Calendar.DAY_OF_YEAR, 1),
    WEEKLY(Calendar.DAY_OF_YEAR, 7),
    MONTHLY(Calendar.MONTH, 1),
    YEAR(Calendar.YEAR, 1);

    private int calendarFieldIndex;
    private int amount;

    Period(int calendarFieldIndex, int amount) {
        this.calendarFieldIndex = calendarFieldIndex;
        this.amount = amount;
    }

    public int getCalendarFieldIndex() {
        return calendarFieldIndex;
    }

    public int getAmount() {
        return amount;
    }

}
