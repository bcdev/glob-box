package org.esa.beam.dataio.globcover.geotiff;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

class XlsLegendParser implements Parser {
    private static final String GLOBAL_LEGEND_SHEET = "Global";
    private static final String REGIONAL_LEGEND_SHEET = "Regional";
    private static final String VALUE = "Value";
    private static final String LABEL = "Label";
    private static final String RED = "Red";
    private static final String BLUE = "Blue";
    private static final String GREEN = "Green";

    @Override
    public LegendClass[] parse(InputStream inputStream, boolean isRegional) {
        Workbook workbook = null;
        LegendClass[] classes = new LegendClass[0];
        try {
            workbook = Workbook.getWorkbook(inputStream);
            Sheet sheet;
            if( isRegional ) {
                sheet = workbook.getSheet(REGIONAL_LEGEND_SHEET);
                if(sheet == null) {
                    throw new IllegalArgumentException("Given xls-legend is not regional.");
                }
            } else {
                sheet = workbook.getSheet(GLOBAL_LEGEND_SHEET);
            }

            final int valueCol = sheet.findCell(VALUE).getColumn();
            final int labelCol = sheet.findCell(LABEL).getColumn();
            final int redCol = sheet.findCell(RED).getColumn();
            final int blueCol = sheet.findCell(BLUE).getColumn();
            final int greenCol = sheet.findCell(GREEN).getColumn();

            classes = new LegendClass[ sheet.getRows() - 1 ];

            for (int i = 1; i < sheet.getRows(); i++) {
                final Cell valueCell = sheet.getCell(valueCol, i);
                if(StringUtils.isNullOrEmpty(valueCell.getContents())) {
                    continue;
                }
                final Cell labelCell = sheet.getCell(labelCol, i);
                final Cell redCell = sheet.getCell(redCol, i);
                final Cell greenCell = sheet.getCell(greenCol, i);
                final Cell blueCell = sheet.getCell(blueCol, i);

                final int value = Integer.parseInt(valueCell.getContents());
                final String name = "Class_" + i;
                final String descr = labelCell.getContents().trim();
                final Color color = new Color(Integer.parseInt(redCell.getContents()),
                                              Integer.parseInt(greenCell.getContents()),
                                              Integer.parseInt(blueCell.getContents()));

                final LegendClass legendClass = new LegendClass(value, descr, name, color);
                classes[ i - 1 ] = legendClass;
            }

        } catch (BiffException e) {
            Debug.trace(e);
        } catch (IOException e) {
            Debug.trace(e);
        } finally {
            if( workbook != null ) {
                workbook.close();
            }
        }
        return classes;
    }
}
