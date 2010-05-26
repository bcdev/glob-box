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
