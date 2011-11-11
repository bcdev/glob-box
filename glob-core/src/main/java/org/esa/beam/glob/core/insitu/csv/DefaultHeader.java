package org.esa.beam.glob.core.insitu.csv;

import java.util.Arrays;
import java.util.List;

/**
 * A default implementation of a {@link RecordSource}.
 *
 * @author Norman
 */
class DefaultHeader implements Header {

    private final boolean hasLocation;
    private final boolean hasTime;
    private final List<String> attributeNames;

    DefaultHeader(boolean hasLocation, boolean hasTime, String... attributeNames) {
        this.hasLocation = hasLocation;
        this.hasTime = hasTime;
        this.attributeNames = Arrays.asList(attributeNames);
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
    public String[] getAttributeNames() {
        return attributeNames.toArray(new String[attributeNames.size()]);
    }
}
