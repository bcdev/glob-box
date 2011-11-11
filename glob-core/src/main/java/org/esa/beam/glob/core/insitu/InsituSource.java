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
import org.esa.beam.glob.core.insitu.csv.Header;
import org.esa.beam.glob.core.insitu.csv.InsituRecord;
import org.esa.beam.glob.core.insitu.csv.Record;
import org.esa.beam.glob.core.insitu.csv.RecordSource;
import org.esa.beam.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * TODO fill out or delete
 *
 * @author Thomas Storm
 */
public class InsituSource {

    private final InsituLoader insituLoader;
    private RecordSource recordSource;

    public InsituSource(InsituLoader insituLoader) {
        this.insituLoader = insituLoader;
    }

    public InsituRecord[] getValuesFor(String parameterName) throws IOException {
        ensureInsituData();
        final Header header = recordSource.getHeader();
        final String[] columnNames = header.getColumnNames();
        final int columnIndex = StringUtils.indexOf(columnNames, parameterName);

        final Iterable<Record> records = recordSource.getRecords();
        final List<InsituRecord> parameterRecords = new ArrayList<InsituRecord>();
        for (Record record : records) {
            final GeoPos pos = record.getLocation();
            final Date time = record.getTime();
            final Double value = (Double) record.getAttributeValues()[columnIndex];
            final InsituRecord insituRecord = new InsituRecord(pos, time, value);
            parameterRecords.add(insituRecord);
        }

        sortRecordsAscending(parameterRecords);
        return parameterRecords.toArray(new InsituRecord[parameterRecords.size()]);
    }

    public String[] getParameterNames() throws IOException {
        ensureInsituData();
        return recordSource.getHeader().getParameterNames();
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

    private void ensureInsituData() throws IOException {
        if(recordSource == null) {
            recordSource = insituLoader.loadSource();
        }
    }
}
