package org.esa.beam.glob.export.text;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.GlobBox;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.JFileChooser;
import java.awt.Dimension;
import java.io.File;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 18.03.2010
 * Time: 16:51:49
 */
public class
        ExportTimeBasedText extends ExecCommand {

    private static final String EXPORT_DIR_PREFERENCES_KEY = "user.export.dir";
    BeamFileFilter kmzFileFilter = new BeamFileFilter("CSV", "csv", "Comma separated values");

    @Override
    public void actionPerformed(CommandEvent event) {
        File outputFile = fetchOutputFile();
        List<RasterDataNode> rasterList = GlobBox.getInstance().getModel().getCurrentRasterList();
        CsvExporter exporter = new CsvExporter(rasterList, outputFile, 2);
        exporter.exportCsv();
    }

    private File fetchOutputFile() {
        VisatApp visatApp = VisatApp.getApp();
        ProductSceneView sceneView = visatApp.getSelectedProductSceneView();
        final String lastDir = visatApp.getPreferences().getPropertyString(
                EXPORT_DIR_PREFERENCES_KEY,
                SystemUtils.getUserHomeDir().getPath());
        final File currentDir = new File(lastDir);

        final BeamFileChooser fileChooser = new BeamFileChooser();
        HelpSys.enableHelpKey(fileChooser, getHelpId());
        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.addChoosableFileFilter(kmzFileFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        fileChooser.setDialogTitle(visatApp.getAppName() + " - " + "Export time series as CSV file..."); /* I18N */
        final RasterDataNode refRaster = sceneView.getRaster();
        fileChooser.setCurrentFilename("time_series_" + refRaster.getName());

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

    @Override
    public void updateState(CommandEvent event) {
        ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        setEnabled(view != null);
    }
}
