package org.esa.beam.dataio.arcbin;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;
import org.geotools.data.shapefile.dbf.DbaseFileReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LegendFile {

    private LegendFile() {
    }

    static Map<Integer, String> createDescriptionMap( final File dir ) throws IOException {
        File file = findDbfLegendFile(dir);
        if (file != null) {
            return createDbfDescriptionMap(file);
        }

        file = findXlsLegendFile( dir );
        if( file != null ) {
            return createXlsDescriptionMap( file );
        }

        return Collections.emptyMap();
    }

    private static Map<Integer, String> createXlsDescriptionMap(final File file) {
        Workbook workbook = null;
        final Map<Integer, String> map = new HashMap<Integer, String>();
        try {
            workbook = Workbook.getWorkbook(file);
            Sheet sheet = workbook.getSheet(0);

            final Cell[] firstRow = sheet.getRow(0);
            int descColIndex = -1;
            for (int i = 0; i < firstRow.length; i++) {
                Cell cell = firstRow[i];
                final String columnName = cell.getContents().toLowerCase();
                if (columnName.contains("class") && columnName.contains("name")) {
                    descColIndex = i;
                }
            }

            if (descColIndex == -1) {
                return Collections.emptyMap();
            }

            final Cell[] valueCol = sheet.getColumn(0);
            final Cell[] descCol = sheet.getColumn(descColIndex);
            for (int i = 1; i < valueCol.length; i++) {
                Cell valueCell = valueCol[i];
                final String value = valueCell.getContents();
                if (StringUtils.isNullOrEmpty(value)) {
                    continue;
                }
                Cell descCell = descCol[i];
                final int intValue = Integer.parseInt(value.trim());

                map.put(intValue, descCell.getContents());
            }

        } catch (BiffException e) {
            Debug.trace(e);
            return Collections.emptyMap();
        } catch (IOException e) {
            Debug.trace(e);
            return Collections.emptyMap();
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
        return map;
    }

    private static Map<Integer, String> createDbfDescriptionMap(final File file) throws IOException {

        final DbaseFileReader reader = new DbaseFileReader(createChannel(file), true, Charset.defaultCharset());
        Map<Integer, String> map = new HashMap<Integer, String>();

        try {
            while (reader.hasNext()) {
                DbaseFileReader.Row row = reader.readRow();
                map.put(((Double) row.read(0)).intValue(), row.read(1).toString());
            }
        } finally {
            reader.close();
        }
        return map;
    }

    private static File findXlsLegendFile(final File dir) {
        final File parentDir = dir.getParentFile();

        final File[] legendFiles = parentDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.startsWith(".") && name.toLowerCase().endsWith("_legend.xls");
            }
        });

        if (legendFiles.length > 0) {
            return legendFiles[0];
        } else {
            return null;
        }
    }

    private static File findDbfLegendFile(final File dir) {
        final File parentDir = dir.getParentFile();

        final File[] legendFiles = parentDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.startsWith(".") && name.toLowerCase().endsWith("_legend.dbf");
            }
        });

        if (legendFiles.length > 0) {
            return legendFiles[0];
        } else {
            return null;
        }
    }

    private static ReadableByteChannel createChannel(File legendFile) throws FileNotFoundException {
        return new FileInputStream(legendFile).getChannel();
    }

}
