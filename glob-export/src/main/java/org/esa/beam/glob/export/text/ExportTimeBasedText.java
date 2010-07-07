package org.esa.beam.glob.export.text;

import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.JFileChooser;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 18.03.2010
 * Time: 16:51:49
 */
public class ExportTimeBasedText implements ActionListener {

    private static final String EXPORT_DIR_PREFERENCES_KEY = "user.export.dir";
    BeamFileFilter kmzFileFilter = new BeamFileFilter("CSV", "csv", "Comma separated values");

    @Override
    public void actionPerformed(ActionEvent event) {

        final VisatApp app = VisatApp.getApp();
        final ProductSceneView view = app.getSelectedProductSceneView();
        if (view != null && view.getProduct() != null &&
            view.getProduct().getProductType().equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
            RasterDataNode[] rasterList = view.getProduct().getBands();
            List<PixelPos> positions = getPositions();
            if (positions.isEmpty()) {
                app.showErrorDialog("No pins specified", "There are no pins, which could be exported.");
            } else {
                CsvExporter exporter = new TimeCsvExporter(Arrays.asList(rasterList), positions, fetchOutputFile());
                exporter.exportCsv();
            }
        } else {
            app.showErrorDialog("No time series specified", "There is no time series view open in VISAT, " +
                                                            "which could be exported.");
        }
    }

    private List<PixelPos> getPositions() {
        final ArrayList<PixelPos> positions = new ArrayList<PixelPos>();
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        final PlacemarkGroup pinGroup = view.getProduct().getPinGroup();
        for (int i = 0; i < pinGroup.getNodeCount(); i++) {
            positions.add(pinGroup.get(i).getPixelPos());
        }
        return positions;
    }

    private File fetchOutputFile() {
        VisatApp visatApp = VisatApp.getApp();
        final String lastDir = visatApp.getPreferences().getPropertyString(
                EXPORT_DIR_PREFERENCES_KEY,
                SystemUtils.getUserHomeDir().getPath());
        final File currentDir = new File(lastDir);

        final BeamFileChooser fileChooser = new BeamFileChooser();
//        HelpSys.enableHelpKey(fileChooser, getHelpId());
        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.addChoosableFileFilter(kmzFileFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        fileChooser.setDialogTitle(visatApp.getAppName() + " - " + "Export time series as CSV file..."); /* I18N */
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        Dimension fileChooserSize = fileChooser.getPreferredSize();
        if (fileChooserSize != null) {
            fileChooser.setPreferredSize(new Dimension(
                    fileChooserSize.width + 120, fileChooserSize.height));
        } else {
            fileChooser.setPreferredSize(new Dimension(512, 256));
        }

        int result = fileChooser.showSaveDialog(visatApp.getMainFrame());
        File file = fileChooser.getSelectedFile();

        final File currentDirectory = fileChooser.getCurrentDirectory();
        if (currentDirectory != null) {
            visatApp.getPreferences().setPropertyString(
                    EXPORT_DIR_PREFERENCES_KEY,
                    currentDirectory.getPath());
        }
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        if (file == null || file.getName().isEmpty()) {
            return null;
        }

        if (!visatApp.promptForOverwrite(file)) {
            return null;
        }

        return file;
    }
}
