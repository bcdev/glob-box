package org.esa.beam.glob.export;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.GlobBox;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.BoundingBox;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private boolean exportTimed = false;
    private ProductSceneView view;

    @Override
    public void actionPerformed(CommandEvent event) {
        //todo resolve deprecated calls
        final VisatApp app = VisatApp.getApp();
        view = app.getSelectedProductSceneView();
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
            final KmzExporter exporter = new KmzExporter("description", "name");
            exporter.setTimeSeries(false);
            final BoundingBox boundBox = new ReferencedEnvelope(0, 20, 70, 30, DefaultGeographicCRS.WGS84);
            List<KmlLayer> layers = createLayers();
            for (KmlLayer layer : layers) {
                exporter.addLayer(layer);
            }
            try {
                exporter.export(new BufferedOutputStream(new ByteArrayOutputStream()),
                                com.bc.ceres.core.ProgressMonitor.NULL);
            } catch (IOException e) {
                e.printStackTrace();
            }

//            final ImageHandler imageHandler = new ImageHandler(app, "Save KMZ", view, output, rasterList, level);
//            imageHandler.executeWithBlocking();
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

        fileChooser.setDialogTitle(visatApp.getAppName() + " - " + "Export time series as time based KMZ"); /* I18N */
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

        int maxLevel = refRaster.getSourceImage().getModel().getLevelCount() - 1;
        maxLevel = maxLevel > 10 ? 10 : maxLevel;

        final JPanel levelPanel = new JPanel(new GridLayout(maxLevel, 1));
        levelPanel.setBorder(BorderFactory.createTitledBorder("Level"));
        ButtonGroup buttonGroup = new ButtonGroup();
        final RadioButtonActionListener radioButtonListener = new RadioButtonActionListener();
        for (int i = 0; i < maxLevel; i++) {
            final JRadioButton button = new JRadioButton(Integer.toString(i), true);
            buttonGroup.add(button);
            levelPanel.add(button);
            button.addActionListener(radioButtonListener);
        }

        final JPanel timedPanel = new JPanel(new GridLayout(1, 1));
        timedPanel.setBorder(BorderFactory.createTitledBorder("Timed"));
        final JCheckBox timedButton = new JCheckBox("Export Timed KMZ");
        timedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportTimed = timedButton.isSelected();
            }
        });
        timedPanel.add(timedButton);

        final JPanel accessory = new JPanel();
        accessory.setLayout(new BoxLayout(accessory, BoxLayout.Y_AXIS));
        accessory.add(levelPanel);
        accessory.add(timedPanel);
        fileChooser.setAccessory(accessory);

        int result = fileChooser.showSaveDialog(visatApp.getMainFrame());
        File file = fileChooser.getSelectedFile();

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

    private List<KmlLayer> createLayers() {
        List<KmlLayer> layers = new ArrayList<KmlLayer>();
        for (RasterDataNode raster : GlobBox.getInstance().getRasterList()) {
            KmlLayer layer;
            String name = raster.getDisplayName();
            if (raster.getImageInfo() == null) {
                raster.setImageInfo(view.getRaster().getImageInfo().createDeepCopy());
            }
            RenderedImage image = ImageManager.getInstance().createColoredBandImage(
                    new RasterDataNode[]{raster}, raster.getImageInfo(), level);
            final BoundingBox boundBox = new ReferencedEnvelope(0, 20, 70, 30, DefaultGeographicCRS.WGS84);

            final Product product = raster.getProduct();
            final ProductData.UTC startTime = product.getStartTime();
            final ProductData.UTC endTime = product.getEndTime();
            if (exportTimed && startTime != null && product.getEndTime() != null) {
                layer = new TimedKmlLayer(name, image, boundBox, startTime, endTime);
            } else {
                layer = new KmlLayer(name, image, boundBox);
            }
            layers.add(layer);
        }
        return layers;
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
                level = Integer.parseInt(button.getText());
            }
        }
    }
}
