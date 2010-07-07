package org.esa.beam.glob.export.text;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.visat.VisatApp;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 07.07.2010
 * Time: 10:13:43
 */
public class TimeCsvExporter extends CsvExporter {


    public TimeCsvExporter(List<RasterDataNode> rasterList, List<PixelPos> positions, File outputFile) {
        super(rasterList, positions, outputFile);
        forExcel = true;
    }

    @Override
    void setUpColumns() {
        columns.add("Pin");
        if (exportImageCoords) {
            columns.add("Image position (x | y)");
        }
        if (exportLatLon) {
            columns.add("Geo position (lat | lon)");
        }
        if (exportUnit) {
            columns.add("Unit");
        }
        for (RasterDataNode raster : rasterList) {
            final Date date = raster.getTimeCoding().getStartTime().getAsDate();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            columns.add(sdf.format(date));
        }
    }

    @Override
    void setUpRows() {
        final RasterDataNode refRaster = VisatApp.getApp().getSelectedProductSceneView().getRaster();
        int index = 1;
        for (PixelPos pixelPos : positions) {
            final StringBuilder row = new StringBuilder();
            row.append("Pin").append(index++);
            row.append(getSeparator());
            if (exportImageCoords) {
                DecimalFormat formatter = new DecimalFormat("0.00");
                row.append(formatter.format(pixelPos.getX()));
                row.append(" | ");
                row.append(formatter.format(pixelPos.getY()));
                row.append(getSeparator());
            }
            if (exportLatLon) {
                final GeoPos geoPos = new GeoPos();
                refRaster.getGeoCoding().getGeoPos(pixelPos, geoPos);
                row.append(geoPos.getLatString());
                row.append(" | ");
                row.append(geoPos.getLonString());
                row.append(getSeparator());

            }
            if (exportUnit) {
                row.append(" (").append(refRaster.getUnit()).append(")");
                row.append(getSeparator());
            }
            for (int i = 0; i < rasterList.size(); i++) {
                RasterDataNode raster = rasterList.get(i);
                row.append(getValue(raster, (int) pixelPos.x, (int) pixelPos.y, level));
                if (i < rasterList.size() - 1) {
                    row.append(getSeparator());
                }
            }
            rows.add(row.toString());
        }
    }

}
