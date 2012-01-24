/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.glob.core.insitu;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.glob.core.insitu.csv.InsituRecord;
import org.esa.beam.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a source for in situ data
 *
 * @author Thomas Storm
 * @author Sabine Embacher
 */
public class InsituSource {

    private RecordSource recordSource;

    public InsituSource(RecordSource recordSource) throws IOException {
        this.recordSource = recordSource;
    }

    public Collection<GeoPos> getInsituPositionsFor(String parameterName) {
        Set<GeoPos> result = new HashSet<GeoPos>();
        final int columnIndex = getIndexForParameter(parameterName);
        final Iterable<Record> records = recordSource.getRecords();
        for (Record record : records) {
            final Double value = (Double) record.getAttributeValues()[columnIndex];
            if (value == null) {
                continue;
            }
            result.add(record.getLocation());
        }
        return result;
    }

    public InsituRecord[] getValuesFor(String parameterName, GeoPos position) {
        final int columnIndex = getIndexForParameter(parameterName);
        final Iterable<Record> records = recordSource.getRecords();
        final List<InsituRecord> parameterRecords = new ArrayList<InsituRecord>();
        for (Record record : records) {
            final GeoPos pos = record.getLocation();
            if (position != null && !pos.equals(position)) {
                continue;
            }
            final Date time = record.getTime();
            final Double value = (Double) record.getAttributeValues()[columnIndex];
            if (value == null) {
                continue;
            }
            final String stationName = record.getStationName() == null ? "" : record.getStationName();
            final InsituRecord insituRecord = new InsituRecord(pos, time, stationName, value);
            parameterRecords.add(insituRecord);
        }

        sortRecordsAscending(parameterRecords);
        return parameterRecords.toArray(new InsituRecord[parameterRecords.size()]);
    }

    public boolean hasStationNames() {
        return recordSource.getHeader().hasStationName();
    }

    private int getIndexForParameter(String parameterName) {
        final Header header = recordSource.getHeader();
        final String[] columnNames = header.getColumnNames();
        return StringUtils.indexOf(columnNames, parameterName);
    }

    public String[] getParameterNames() {
        return recordSource.getHeader().getParameterNames();
    }

    public String getNameFor(GeoPos geoPos) {
        return "";
    }

    public void close() {
        recordSource.close();
    }

    private void sortRecordsAscending(List<InsituRecord> parameterRecords) {
        Collections.sort(parameterRecords, new Comparator<InsituRecord>() {
            @Override
            public int compare(InsituRecord o1, InsituRecord o2) {
                if (o1.time.equals(o2.time)) {
                    return 0;
                }
                return o1.time.before(o2.time) ? -1 : 1;
            }
        });
    }

}