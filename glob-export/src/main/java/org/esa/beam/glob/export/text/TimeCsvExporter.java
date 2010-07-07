package org.esa.beam.glob.export.text;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;

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

    public TimeCsvExporter(List<List<Band>> rasterList, List<PixelPos> positions, File outputFile) {
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
        columns.add("Variable");
        if (exportUnit) {
            columns.add("Unit");
        }
        // we assume all bandlists to contain the same time information, so the columns are built on the first
        // non-empty bandlist.
        for (List<Band> bandList : variablesList) {
            if (!bandList.isEmpty()) {
                for (Band band : bandList) {
                    final Date date = band.getTimeCoding().getStartTime().getAsDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    columns.add(sdf.format(date));
                }
                break;
            }
        }
    }

    @Override
    void setUpRows(ProgressMonitor pm) {
        int index = 0;
        pm.beginTask("Exporting pin data as csv-file...", positions.size());
        for (PixelPos pixelPos : positions) {
            index++;
            for (List<Band> bandList : variablesList) {
                if (!bandList.isEmpty()) {
                    rows.add(setUpRow(pixelPos, bandList, index));
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    private String setUpRow(PixelPos pixelPos, List<Band> bandList, int index) {
        Band refBand = bandList.get(0);
        final StringBuilder row = new StringBuilder();
        row.append("Pin").append(index);
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
            refBand.getGeoCoding().getGeoPos(pixelPos, geoPos);
            row.append(geoPos.getLatString()).append(" (").append(geoPos.getLat()).append(") ");
            row.append(" | ");
            row.append(geoPos.getLonString()).append(" (").append(geoPos.getLon()).append(") ");
            row.append(getSeparator());

        }
        row.append(AbstractTimeSeries.rasterToVariableName(refBand.getName()));
        row.append(getSeparator());
        if (exportUnit) {
            row.append(" (").append(refBand.getUnit()).append(")");
            row.append(getSeparator());
        }
        for (int i = 0; i < bandList.size(); i++) {
            Band band = bandList.get(i);
            row.append(getValue(band, (int) pixelPos.x, (int) pixelPos.y, level));
            if (i < bandList.size() - 1) {
                row.append(getSeparator());
            }
        }
        return row.toString();
    }

}
