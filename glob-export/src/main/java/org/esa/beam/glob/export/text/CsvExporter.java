package org.esa.beam.glob.export.text;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 23.03.2010
 * Time: 14:40:45
 */
public abstract class CsvExporter {

    List<String> columns;
    List<String> rows;
    List<List<Band>> variablesList;
    File outputFile;
    int level;

    boolean forExcel = false;
    boolean exportImageCoords = true;
    boolean exportLatLon = true;
    boolean exportUnit = true;
    List<PixelPos> positions;

    public CsvExporter(List<List<Band>> variablesList, List<PixelPos> positions, File outputFile) {
        this(variablesList, positions, outputFile, 0);
    }

    public CsvExporter(List<List<Band>> variablesList, List<PixelPos> positions, File outputFile, int level) {
        this.variablesList = variablesList;
        this.positions = positions;
        this.outputFile = outputFile;
        this.level = level;
        this.columns = new ArrayList<String>();
        this.rows = new ArrayList<String>();
    }

    void exportCsv() {
        setUpColumns();
        setUpRows();
        StringBuilder builder = new StringBuilder();
        String sep = getSeparator();
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            builder.append(column);
            if (i < columns.size() - 1) {
                builder.append(sep);
            }
        }
        builder.append("\n");
        FileOutputStream outStream = null;
        PrintStream printStream = null;
        try {
            outStream = new FileOutputStream(outputFile);
            printStream = new PrintStream(outStream);
            printStream.print(builder.toString());
            for (String row : rows) {
                printStream.print(row);
                printStream.print("\n");
            }
        } catch (FileNotFoundException exc) {
            VisatApp.getApp().handleError(exc);
            Debug.trace(exc);
        } catch (IOException exc) {
            VisatApp.getApp().handleError(exc);
            Debug.trace(exc);
        } finally {
            try {
                outStream.close();
                printStream.close();
            } catch (IOException ignore) {
            }
        }
    }

    abstract void setUpColumns();

    abstract void setUpRows();

    String getSeparator() {
        return forExcel ? ";" : ",";
    }

    double getValue(RasterDataNode raster, int pixelX, int pixelY, int currentLevel) {
        final RenderedImage image = raster.getGeophysicalImage().getImage(currentLevel);
        final Rectangle pixelRect = new Rectangle(pixelX, pixelY, 1, 1);
        final Raster data = image.getData(pixelRect);
        final RenderedImage validMask = raster.getValidMaskImage().getImage(currentLevel);
        final Raster validMaskData = validMask.getData(pixelRect);
        final double value;
        if (validMaskData.getSample(pixelX, pixelY, 0) > 0) {
            value = data.getSampleDouble(pixelX, pixelY, 0);
        } else {
            value = Double.NaN;
        }
        return value;
    }
}
