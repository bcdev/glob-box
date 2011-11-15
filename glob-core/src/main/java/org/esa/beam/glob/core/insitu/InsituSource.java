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
import org.esa.beam.util.logging.BeamLogManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Represents a source for in situ data
 *
 * @author Thomas Storm
 * @author Sabine Embacher
 */
public class InsituSource {

    private final InsituLoader insituLoader;
    private RecordSource recordSource;
    private Cache cache;

    public InsituSource(InsituLoader insituLoader) throws IOException {
        this.insituLoader = insituLoader;
        ensureInsituData();
        cache = new Cache(50);
    }

    public InsituRecord[] getValuesFor(String parameterName) {
        if (cache.contains(parameterName)) {
            return cache.get(parameterName);
        }
        final Header header = recordSource.getHeader();
        final String[] columnNames = header.getColumnNames();
        final int columnIndex = StringUtils.indexOf(columnNames, parameterName);

        final Iterable<Record> records = recordSource.getRecords();
        final List<InsituRecord> parameterRecords = new ArrayList<InsituRecord>();
        for (Record record : records) {
            final GeoPos pos = record.getLocation();
            final Date time = record.getTime();
            final Double value = (Double) record.getAttributeValues()[columnIndex];
            if (value == null) {
                continue;
            }
            final InsituRecord insituRecord = new InsituRecord(pos, time, value);
            parameterRecords.add(insituRecord);
        }

        sortRecordsAscending(parameterRecords);
        final InsituRecord[] insituRecords = parameterRecords.toArray(new InsituRecord[parameterRecords.size()]);
        cache.add(parameterName, insituRecords);
        return insituRecords;
    }

    public String[] getParameterNames() {
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
        if (recordSource == null) {
            recordSource = insituLoader.loadSource();
        }
    }

    private class Cache {

        private final int capacity;
        private final List<InsituRecord[]> itemList;
        private final Map<String, InsituRecord[]> itemMap;

        private Cache(int capacity) {
            this.capacity = capacity;
            itemList = new ArrayList<InsituRecord[]>(capacity);
            itemMap = new HashMap<String, InsituRecord[]>(capacity);
        }

        private void add(String key, InsituRecord[] item) {
            reload();
            if (itemList.size() >= capacity) {
                clear();
            }
            itemMap.put(key, item);
            itemList.add(item);
        }

        private boolean contains(String key) {
            return itemMap.containsKey(key);
        }

        private InsituRecord[] get(String key) {
            return itemMap.get(key);
        }

        private void clear() {
            itemList.clear();
            itemMap.clear();
        }

        private void reload() {
            InsituSource.this.recordSource = null;
            try {
                InsituSource.this.ensureInsituData();
            } catch (IOException e) {
                BeamLogManager.getSystemLogger().log(Level.WARNING, "Should not come here", e);
            }
        }
    }
}