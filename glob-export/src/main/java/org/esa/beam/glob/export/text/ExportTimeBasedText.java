package org.esa.beam.glob.export.text;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.core.TimeSeriesMapper;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.JFileChooser;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 18.03.2010
 * Time: 16:51:49
 */
public class ExportTimeBasedText extends ProgressMonitorSwingWorker<Void, Void> {

    private static final String EXPORT_DIR_PREFERENCES_KEY = "user.export.dir";
    BeamFileFilter kmzFileFilter = new BeamFileFilter("CSV", "csv", "Comma separated values");

    public ExportTimeBasedText(Component parentComponent, String title) {
        super(parentComponent, title);
    }

    @Override
    protected Void doInBackground(ProgressMonitor pm) throws Exception {
        final VisatApp app = VisatApp.getApp();
        final ProductSceneView view = app.getSelectedProductSceneView();
        if (view != null && view.getProduct() != null &&
            view.getProduct().getProductType().equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
            AbstractTimeSeries timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct());
            List<List<Band>> bandList = new ArrayList<List<Band>>();
            final List<String> timeVariables = timeSeries.getTimeVariables();
            for (String timeVariable : timeVariables) {
                bandList.add(timeSeries.getBandsForVariable(timeVariable));
            }
            List<PixelPos> positions = getPositions();
            if (positions.isEmpty()) {
                app.showErrorDialog("No pins specified", "There are no pins, which could be exported.");
            } else {
                CsvExporter exporter = new TimeCsvExporter(bandList, positions, fetchOutputFile());
                exporter.exportCsv(pm);
            }
        } else {
            app.showErrorDialog("No time series specified", "There is no time series view open in VISAT, " +
                                                            "which could be exported.");
        }
        return null;
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
