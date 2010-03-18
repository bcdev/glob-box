package org.esa.beam.glob.export;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.GlobBox;
import org.esa.beam.util.Debug;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 17.03.2010
 * Time: 15:48:58
 */
public class ExportTimeBasedKmz extends ExecCommand {

    private static final String IMAGE_EXPORT_DIR_PREFERENCES_KEY = "user.image.export.dir";
    BeamFileFilter kmzFileFilter = new BeamFileFilter("KMZ", "kmz", "KMZ - Google Earth File Format");
    private int level = 2;

    @Override
    public void actionPerformed(CommandEvent event) {
        //todo resolve deprecated calls
        final VisatApp app = VisatApp.getApp();
        ProductSceneView view = app.getSelectedProductSceneView();
        final GeoCoding geoCoding = view.getProduct().getGeoCoding();
        boolean isGeographic = false;
        if (geoCoding instanceof MapGeoCoding) {
            MapGeoCoding mapGeoCoding = (MapGeoCoding) geoCoding;
            MapTransformDescriptor transformDescriptor = mapGeoCoding.getMapInfo()
                    .getMapProjection().getMapTransform().getDescriptor();
            String typeID = transformDescriptor.getTypeID();
            if (typeID.equals(IdentityTransformDescriptor.TYPE_ID)) {
                isGeographic = true;
            }
        } else if (geoCoding instanceof CrsGeoCoding) {
            isGeographic = CRS.equalsIgnoreMetadata(geoCoding.getMapCRS(), DefaultGeographicCRS.WGS84);
        }

        if (isGeographic) {
            final List<RasterDataNode> rasterList = GlobBox.getInstance().getRasterList();
            final File output = fetchOutputFile(view);
            final ImageHandler imageHandler = new ImageHandler(app, "Save KMZ", view, output, rasterList, level);
            imageHandler.executeWithBlocking();
        } else {
            String message = "Product must be in ''Geographic Lat/Lon'' projection.";
            app.showInfoDialog(message, null);
        }
    }

    protected File fetchOutputFile(ProductSceneView sceneView) {
        VisatApp visatApp = VisatApp.getApp();
        final String lastDir = visatApp.getPreferences().getPropertyString(
                IMAGE_EXPORT_DIR_PREFERENCES_KEY,
                SystemUtils.getUserHomeDir().getPath());
        final File currentDir = new File(lastDir);

        final BeamFileChooser fileChooser = new BeamFileChooser();
        HelpSys.enableHelpKey(fileChooser, getHelpId());
        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.addChoosableFileFilter(kmzFileFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        fileChooser.setDialogTitle(visatApp.getAppName() + " - " + "Export time series"); /* I18N */
        fileChooser.setCurrentFilename("time_series_" + sceneView.getRaster().getName());

        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        Dimension fileChooserSize = fileChooser.getPreferredSize();
        if (fileChooserSize != null) {
            fileChooser.setPreferredSize(new Dimension(
                    fileChooserSize.width + 120, fileChooserSize.height));
        } else {
            fileChooser.setPreferredSize(new Dimension(512, 256));
        }

        final JPanel regionPanel = new JPanel(new GridLayout(4, 1));
        regionPanel.setBorder(BorderFactory.createTitledBorder("Level")); /*I18N*/
        final JRadioButton levelZero = new JRadioButton("0", false); /*I18N*/
        final JRadioButton levelOne = new JRadioButton("1", false); /*I18N*/
        final JRadioButton levelTwo = new JRadioButton("2", true); /*I18N*/
        final JRadioButton levelThree = new JRadioButton("3", false); /*I18N*/
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(levelZero);
        regionPanel.add(levelZero);
        buttonGroup.add(levelOne);
        regionPanel.add(levelOne);
        buttonGroup.add(levelTwo);
        regionPanel.add(levelTwo);
        buttonGroup.add(levelThree);
        regionPanel.add(levelThree);
        final RadioButtonActionListener radioButtonListener = new RadioButtonActionListener();
        levelZero.addActionListener(radioButtonListener);
        levelOne.addActionListener(radioButtonListener);
        levelTwo.addActionListener(radioButtonListener);
        levelThree.addActionListener(radioButtonListener);
        final JPanel accessory = new JPanel();
        accessory.setLayout(new BoxLayout(accessory, BoxLayout.Y_AXIS));
        accessory.add(regionPanel);
        fileChooser.setAccessory(accessory);

        int result = fileChooser.showSaveDialog(visatApp.getMainFrame());
        File file = fileChooser.getSelectedFile();
        fileChooser.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // @todo never comes here, why?
                Debug.trace(evt.toString());
            }
        });
        final File currentDirectory = fileChooser.getCurrentDirectory();
        if (currentDirectory != null) {
            visatApp.getPreferences().setPropertyString(
                    IMAGE_EXPORT_DIR_PREFERENCES_KEY,
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

    private class RadioButtonActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            final JRadioButton button = (JRadioButton) e.getSource();
            if (button.isSelected()) {
                System.out.println(button.getText());
                level = Integer.parseInt(button.getText());
            }
        }
    }
}
