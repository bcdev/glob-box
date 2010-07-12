package org.esa.beam.glob.export.animations;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;
import org.w3c.dom.Node;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: Thomas Storm
 * Date: 09.07.2010
 * Time: 15:48:46
 */
public class AnimatedGifExport extends ProgressMonitorSwingWorker<Void, Void> {

    private File outputFile;
    private static final String EXPORT_DIR_PREFERENCES_KEY = "user.export.dir";
    private RenderedImage[] frames;
    private int level;

    public AnimatedGifExport(Component parentComponent, String title) {
        super(parentComponent, title);
        this.outputFile = fetchOutputFile();
    }

    @Override
    protected Void doInBackground(ProgressMonitor pm) throws Exception {
        exportAnimation("50", outputFile, pm);
        return null;
    }

    public void createFrames(List<Band> bandsForVariable) {
        List<RenderedImage> images = new ArrayList<RenderedImage>();
        for (Band band : bandsForVariable) {
            images.add(band.getGeophysicalImage().getImage(level));
        }

        frames = images.toArray(new RenderedImage[images.size()]);
    }

    private void exportAnimation(String delayTime, File file, ProgressMonitor pm) {

        ImageWriter imageWriter = ImageIO.getImageWritersByFormatName("gif").next();

        try {
            ImageOutputStream outputStream = ImageIO.createImageOutputStream(file);
            imageWriter.setOutput(outputStream);
            imageWriter.prepareWriteSequence(null);

            pm.beginTask("Exporting time series as animated gif", frames.length);

            for (int i = 0; i < frames.length; i++) {
                RenderedImage currentImage = frames[i];
                ImageWriteParam writeParameters = imageWriter.getDefaultWriteParam();
                IIOMetadata metadata = imageWriter.getDefaultImageMetadata(new ImageTypeSpecifier(currentImage),
                                                                           writeParameters);

                configure(metadata, delayTime, i);
                IIOImage image = new IIOImage(currentImage, null, metadata);
                imageWriter.writeToSequence(image, null);
                pm.worked(1);
            }
            imageWriter.endWriteSequence();
            outputStream.close();
            pm.done();
        } catch (IOException e) {
            VisatApp.getApp().handleError("Unable to create animated gif", e);
        }
    }

    private static void configure(IIOMetadata meta, String delayTime, int imageIndex) {
        String metaFormat = meta.getNativeMetadataFormatName();

        if (!"javax_imageio_gif_image_1.0".equals(metaFormat)) {
            throw new IllegalArgumentException(
                    "Unfamiliar gif metadata format: " + metaFormat);
        }

        Node root = meta.getAsTree(metaFormat);

        //find the GraphicControlExtension node
        Node child = root.getFirstChild();
        while (child != null) {
            if ("GraphicControlExtension".equals(child.getNodeName())) {
                break;
            }
            child = child.getNextSibling();
        }

        IIOMetadataNode gce = (IIOMetadataNode) child;
        gce.setAttribute("userInputFlag", "FALSE");
        gce.setAttribute("delayTime", delayTime);

        //only the first node needs the ApplicationExtensions node
        if (imageIndex == 0) {
            IIOMetadataNode parentNode = new IIOMetadataNode("ApplicationExtensions");
            IIOMetadataNode childNode = new IIOMetadataNode("ApplicationExtension");
            childNode.setAttribute("applicationID", "NETSCAPE");
            childNode.setAttribute("authenticationCode", "2.0");
            byte[] userObject = new byte[]{
                    //last two bytes is an unsigned short (little endian) that
                    //indicates the number of times to loop.
                    //0 means loop forever.
                    0x1, 0x0, 0x0
            };
            childNode.setUserObject(userObject);
            parentNode.appendChild(childNode);
            root.appendChild(parentNode);
        }

        try {
            meta.setFromTree(metaFormat, root);
        } catch (IIOInvalidTreeException e) {
            VisatApp.getApp().handleError(e);
        }
    }

    private File fetchOutputFile() {
        VisatApp visatApp = VisatApp.getApp();
        final String lastDir = visatApp.getPreferences().getPropertyString(EXPORT_DIR_PREFERENCES_KEY,
                                                                           SystemUtils.getUserHomeDir().getPath());
        final File currentDir = new File(lastDir);

        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setCurrentDirectory(currentDir);
        fileChooser.addChoosableFileFilter(new BeamFileFilter("gif", "gif", "Animated GIF"));
        fileChooser.setAcceptAllFileFilterUsed(false);

        fileChooser.setDialogTitle(visatApp.getAppName() + " - " + "Export time series as animated GIF..."); /* I18N */
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        Dimension fileChooserSize = fileChooser.getPreferredSize();
        if (fileChooserSize != null) {
            fileChooser.setPreferredSize(new Dimension(
                    fileChooserSize.width + 120, fileChooserSize.height));
        } else {
            fileChooser.setPreferredSize(new Dimension(512, 256));
        }

        final RasterDataNode currentRaster = VisatApp.getApp().getSelectedProductSceneView().getRaster();
        int maxLevel = currentRaster.getSourceImage().getModel().getLevelCount() - 1;
        maxLevel = maxLevel > 10 ? 10 : maxLevel;

        final JPanel levelPanel = new JPanel(new GridLayout(maxLevel, 1));
        levelPanel.setBorder(BorderFactory.createTitledBorder("Resolution Level"));
        ButtonGroup buttonGroup = new ButtonGroup();
        final RadioButtonActionListener radioButtonListener = new RadioButtonActionListener();
        for (int i = 0; i < maxLevel; i++) {
            String level = Integer.toString(i);
            if (i == 0) {
                level += " (high, very slow)";
            } else if (i == maxLevel - 1) {
                level += " (low, fast)";
            }
            final JRadioButton button = new JRadioButton(level, true);
            buttonGroup.add(button);
            levelPanel.add(button);
            button.addActionListener(radioButtonListener);
            button.setSelected(true);
        }

        final JPanel accessory = new JPanel();
        accessory.setLayout(new BoxLayout(accessory, BoxLayout.Y_AXIS));
        accessory.add(levelPanel);
        fileChooser.setAccessory(accessory);

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

    private class RadioButtonActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            final JRadioButton button = (JRadioButton) e.getSource();
            if (button.isSelected()) {
                String buttonText = button.getText();
                final int index = buttonText.indexOf(" (");
                if (index != -1) {
                    buttonText = buttonText.substring(index);
                }
                level = Integer.parseInt(buttonText);
                if (level < 2) {

                }
            }
        }
    }

}