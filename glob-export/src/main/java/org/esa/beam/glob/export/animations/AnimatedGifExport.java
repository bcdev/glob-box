package org.esa.beam.glob.export.animations;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Band;
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
import javax.swing.JFileChooser;
import java.awt.Component;
import java.awt.Dimension;
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

    public AnimatedGifExport(Component parentComponent, String title) {
        super(parentComponent, title);
        this.outputFile = fetchOutputFile();
    }

    @Override
    protected Void doInBackground(ProgressMonitor pm) throws Exception {
        exportAnimation("50", outputFile);
        return null;
    }

    public void createFrames(List<Band> bandsForVariable) {
        List<RenderedImage> images = new ArrayList<RenderedImage>();
        for (Band band : bandsForVariable) {
            images.add(band.getGeophysicalImage().getImage(0));
        }

        frames = images.toArray(new RenderedImage[images.size()]);
    }

    private void exportAnimation(String delayTime, File file) {

        ImageWriter imageWriter = ImageIO.getImageWritersByFormatName("gif").next();

        try {
            ImageOutputStream outputStream = ImageIO.createImageOutputStream(file);
            imageWriter.setOutput(outputStream);
            imageWriter.prepareWriteSequence(null);

            for (int i = 0; i < frames.length; i++) {
                RenderedImage currentImage = frames[i];
                ImageWriteParam writeParameters = imageWriter.getDefaultWriteParam();
                IIOMetadata metadata = imageWriter.getDefaultImageMetadata(new ImageTypeSpecifier(currentImage),
                                                                           writeParameters);

                configure(metadata, delayTime, i);
                IIOImage image = new IIOImage(currentImage, null, metadata);
                imageWriter.writeToSequence(image, null);
            }
            imageWriter.endWriteSequence();
            outputStream.close();
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
