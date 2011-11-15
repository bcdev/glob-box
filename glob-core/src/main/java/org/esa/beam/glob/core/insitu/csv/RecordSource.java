package org.esa.beam.glob.core.insitu.csv;

public interface RecordSource {
    /**
     * @return The header of the record source.
     */
    Header getHeader();

    /**
     * Gets the records.
     *
     * @return The records.
     */
    Iterable<Record> getRecords();

    /**
     * Closes the sources.
     */
    void close();
}
