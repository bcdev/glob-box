package org.esa.beam.glob.export.text;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
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
public class CsvExporter {

    private List<String> columns;
    private List<String> rows;
    private List<RasterDataNode> rasterList;
    private File outputFile;

    private boolean forExcel = false;
    private boolean exportImageCoords = true;
    private boolean exportLatLon = true;
    private boolean exportUnit = true;
    private boolean exportTime = true;
    private int level = 2;
    private String sep;

    public CsvExporter(List<RasterDataNode> rasterList, File outputFile, int level) {
        this.rasterList = rasterList;
        this.outputFile = outputFile;
        this.columns = new ArrayList<String>();
        this.rows = new ArrayList<String>();
        this.level = level;
    }

    public CsvExporter(RasterDataNode raster, File outputFile, int level) {
        this((List<RasterDataNode>) null, outputFile, level);
        List<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        rasterList.add(raster);
        this.rasterList = rasterList;
    }

    void exportCsv() {
        setUpColumns();
        setUpRows();
        StringBuilder builder = new StringBuilder();
        if (isForExcel()) {
            sep = ";";
        } else {
            sep = ",";
        }
        for (String column : columns) {
            builder.append(column).append(sep);
        }
        builder.append("\n");
        FileOutputStream outStream = null;
        try {
            outStream = new FileOutputStream(outputFile);
            PrintStream print = new PrintStream(outStream);
            print.print(builder.toString());
            for (String row : rows) {
                print.print(row);
                print.print("\n");
            }
        } catch (FileNotFoundException exc) {
            VisatApp.getApp().handleError(exc);
            Debug.trace(exc);
        } catch (IOException exc) {
            VisatApp.getApp().handleError(exc);
            Debug.trace(exc);
        }
    }

    private void setUpColumns() {
        for (RasterDataNode raster : rasterList) {
            columns.add("Value for " + raster.getProductRefString());
            if (exportTime) {
                columns.add("Start-time");
                columns.add("End-time");
            }
        }
        if (exportImageCoords) {
            columns.add("Image-X");
            columns.add("Image-Y");
        }
        if (exportLatLon) {
            columns.add("Latitude");
            columns.add("Longitude");
        }
        if (exportUnit) {
            columns.add("Unit");
        }
    }

    private void setUpRows() {
        final RasterDataNode refRaster = VisatApp.getApp().getSelectedProductSceneView().getRaster();
        final RenderedImage image = refRaster.getGeophysicalImage().getImage(level);
        final int width = image.getWidth();
        final int height = image.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                PixelPos pixelPos = new PixelPos(x, y);
                GeoPos geoPos = refRaster.getGeoCoding().getGeoPos(pixelPos, null);
                final StringBuilder row = new StringBuilder();
                for (RasterDataNode raster : rasterList) {
//                    row.add( getValue( raster, x, y, level ) + "" );
                    row.append("10.5");
                    if (exportTime) {
                        final Product product = raster.getProduct();
                        row.append(product.getStartTime().getAsDate().toString());
                        row.append(product.getEndTime().getAsDate().toString());
                    }
                }
                if (exportImageCoords) {
                    row.append(pixelPos.getX());
                    row.append(pixelPos.getY());
                }
                if (exportLatLon) {
                    row.append(geoPos.getLatString());
                    row.append(geoPos.getLonString());
                }
                if (exportUnit) {
                    row.append(refRaster.getUnit());
                }
                rows.add(row.toString());
            }
        }
    }

    private double getValue(RasterDataNode raster, int pixelX, int pixelY, int currentLevel) {
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

    public List<RasterDataNode> getRasterList() {
        return rasterList;
    }

    public boolean isForExcel() {
        return forExcel;
    }

    public void setForExcel(boolean forExcel) {
        this.forExcel = forExcel;
    }

    public void setExportUnit(boolean exportUnit) {
        this.exportUnit = exportUnit;
    }

    public void setExportTime(boolean exportTime) {
        this.exportTime = exportTime;
    }

    public void setExportLatLon(boolean exportLatLon) {
        this.exportLatLon = exportLatLon;
    }

    public void setExportImageCoords(boolean exportImageCoords) {
        this.exportImageCoords = exportImageCoords;
    }
}
