package org.esa.beam.glob.export.text;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.visat.VisatApp;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Thomas Storm
 */
public class TimeCsvExporter extends CsvExporter {

    List<List<Band>> variablesList;
    List<Placemark> pins;
    int level;
    boolean exportImageCoords = true;
    boolean exportLonLat = true;
    boolean exportUnit = true;

    public TimeCsvExporter(List<List<Band>> rasterList, List<Placemark> pins, File outputFile) {
        super(outputFile);
        this.variablesList = rasterList;
        this.pins = pins;
        this.level = 0;
    }

    void setUpHeader() {
        if (!variablesList.isEmpty()) {

        }
        header.add("GlobToolbox pin time series export table");
        header.add("");
        header.add("Product:\t" + resolveProductName());
        header.add("Created on:\t" + new Date());
    }

    private String resolveProductName() {
        for (List<Band> bandList : variablesList) {
            if (!bandList.isEmpty()) {
                for (Band band : bandList) {
                    if (band != null) {
                        return band.getProduct().getName();
                    }
                }
            }
        }
        return "Time Series Product";
    }

    @Override
    void setUpColumns() {
        columns.add("Name");
        if (exportImageCoords) {
            columns.add("X");
            columns.add("Y");
        }
        if (exportLonLat) {
            columns.add("Lon");
            columns.add("Lat");
        }
        columns.add("Variable");
        if (exportUnit) {
            columns.add("Unit");
        }
        // we assume all bandlists to contain the same time information, so the columns are built on the first
        // non-empty bandlist.
        ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(sceneView.getProduct());

        for (List<Band> bandList : variablesList) {
            if (!bandList.isEmpty()) {
                for (Band band : bandList) {
                    TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(band);
                    final Date date = timeCoding.getStartTime().getAsDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    columns.add(sdf.format(date));
                }
                break;
            }
        }
    }

    @Override
    void setUpRows(ProgressMonitor pm) {
        pm.beginTask("Exporting pin data as csv-file...", pins.size());
        for (Placemark pin : pins) {
            for (List<Band> bandList : variablesList) {
                if (!bandList.isEmpty()) {
                    rows.add(setUpRow(pin, bandList));
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    private String setUpRow(Placemark pin, List<Band> bandList) {
        Band refBand = bandList.get(0);
        final StringBuilder row = new StringBuilder();
        row.append(pin.getLabel());
        row.append(getSeparator());
        PixelPos pixelPos = pin.getPixelPos();
        if (exportImageCoords) {
            exportImageCoords(row, pixelPos);
        }
        if (exportLonLat) {
            exportLatLon(refBand, row, pixelPos);
        }
        row.append(AbstractTimeSeries.rasterToVariableName(refBand.getName()));
        row.append(getSeparator());
        if (exportUnit) {
            row.append(refBand.getUnit());
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

    private void exportLatLon(Band refBand, StringBuilder row, PixelPos pixelPos) {
        final GeoPos geoPos = new GeoPos();
        refBand.getGeoCoding().getGeoPos(pixelPos, geoPos);
        row.append(geoPos.getLon());
        row.append(getSeparator());
        row.append(geoPos.getLat());
        row.append(getSeparator());
    }

    private void exportImageCoords(StringBuilder row, PixelPos pixelPos) {
        DecimalFormat formatter = new DecimalFormat("0.000");
        row.append(formatter.format(pixelPos.getX()));
        row.append(getSeparator());
        row.append(formatter.format(pixelPos.getY()));
        row.append(getSeparator());
    }

    @Override
    String getSeparator() {
        return "\t";
    }
}
