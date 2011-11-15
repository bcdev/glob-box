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

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.glob.core.insitu.csv.CsvInsituLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;

/**
 * Factory class for creating instances of type {@link InsituLoader}.
 *
 * @author Thomas Storm
 */
public class InsituLoaderFactory {

    public static InsituLoader createInsituLoader(File selectedFile) throws FileNotFoundException {
        final CsvInsituLoader csvInsituLoader = new CsvInsituLoader();
        Reader reader = new RandomAccessFileReader(new RandomAccessFile(selectedFile, "r"));
        csvInsituLoader.setCsvReader(reader);
        // todo - ts - allow user specifying date format
        csvInsituLoader.setDateFormat(ProductData.UTC.createDateFormat("yyyy-MM-dd"));
        return csvInsituLoader;
    }

    private static class RandomAccessFileReader extends Reader {

        RandomAccessFile raf;
        InputStreamReader reader;

        private RandomAccessFileReader(RandomAccessFile raf) {
            super();
            this.raf = raf;
            createReader();
        }

        protected void createReader() {
            reader = new InputStreamReader(new InputStream() {
                @Override
                public int read() throws IOException {
                    return raf.read();
                }
            });
        }

        @Override
        public void reset() throws IOException {
            raf.seek(0);
            createReader();
        }

        @Override
        public void close() throws IOException {
            reader.close();
            raf.close();
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            return reader.read(cbuf, off, len);
        }

        public long size() throws IOException {
            return raf.length();
        }
    }

}
