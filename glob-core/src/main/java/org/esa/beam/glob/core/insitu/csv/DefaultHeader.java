package org.esa.beam.glob.core.insitu.csv;

import java.util.Arrays;
import java.util.List;

/**
 * A default implementation of a {@link Header}.
 *
 * @author Norman
 */
class DefaultHeader implements Header {

    private final boolean hasLocation;
    private final boolean hasTime;
    private final List<String> parameterNames;
    private final List<String> columnNames;

    DefaultHeader(boolean hasLocation, boolean hasTime, String[] columnNames, String[] parameterNames) {
        this.hasLocation = hasLocation;
        this.hasTime = hasTime;
        this.parameterNames = Arrays.asList(parameterNames);
        this.columnNames = Arrays.asList(columnNames);
    }

    @Override
    public boolean hasLocation() {
        return hasLocation;
    }

    @Override
    public boolean hasTime() {
        return hasTime;
    }

    @Override
    public String[] getColumnNames() {
        return columnNames.toArray(new String[columnNames.size()]);
    }

    @Override
    public String[] getParameterNames() {
        return parameterNames.toArray(new String[parameterNames.size()]);
    }
}
