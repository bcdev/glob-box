package org.esa.beam.glob.export.kmz;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import org.esa.beam.framework.datamodel.ImageLegend;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.visat.VisatApp;

import java.awt.Cursor;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * User: Thomas Storm
 * Date: 17.03.2010
 * Time: 17:15:37
 */
public class ImageHandler extends ProgressMonitorSwingWorker {

    private static final String OVERLAY_KML = "overlay.kml";
    private static final String IMAGE_TYPE = "PNG";

    private final VisatApp visatApp;
    private final ProductSceneView view;
    private final File file;
    private final RasterDataNode[] rasterList;
    private final int level;

    public ImageHandler(final VisatApp visatApp, final String message, final ProductSceneView view, final File file,
                        final List<RasterDataNode> rasterList, final int level) {
        super(visatApp.getMainFrame(), message);
        this.visatApp = visatApp;
        this.view = view;
        this.file = file;
        this.rasterList = rasterList.toArray(new RasterDataNode[rasterList.size()]);
        this.level = level;
    }

    @Override
    protected Object doInBackground(ProgressMonitor pm) throws Exception {
        try {
            if (file != null) {
                visatApp.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                final String message = String.format("Saving image as %s...", file.getPath());
                pm.beginTask(message, rasterList.length + 2);
                visatApp.setStatusBarMessage(message);


                RasterDataNode refRaster = view.getRaster();

                ZipOutputStream outStream = new ZipOutputStream(new FileOutputStream(file));
                try {
                    for (RasterDataNode raster : rasterList) {

                        if (raster.getImageInfo() == null) {
                            raster.setImageInfo(refRaster.getImageInfo().createDeepCopy());
                        }

                        final RenderedImage image = ImageManager.getInstance().createColoredBandImage(
                                new RasterDataNode[]{raster}, raster.getImageInfo(), level);

                        outStream.putNextEntry(new ZipEntry(raster.getDisplayName() + ".png"));
                        ImageEncoder encoder = ImageCodec.createImageEncoder(IMAGE_TYPE, outStream, null);
                        encoder.encode(image);
                        pm.worked(1);

                    }
                    final String legendName = refRaster.getName() + "_legend";
                    if (!view.isRGB()) {
                        outStream.putNextEntry(new ZipEntry(legendName + ".png"));
                        ImageEncoder encoder = ImageCodec.createImageEncoder(IMAGE_TYPE, outStream, null);
                        encoder.encode(createImageLegend(refRaster));
                        pm.worked(1);
                    }


                    outStream.putNextEntry(new ZipEntry(OVERLAY_KML));
//                    final String kmlContent = KmlFormatter.formatKML(refRaster, rasterList, legendName);
//                    outStream.write(kmlContent.getBytes());
                    pm.worked(1);

                } finally {
                    outStream.close();
                }
            }
        } catch (OutOfMemoryError ignored) {
            visatApp.showOutOfMemoryErrorDialog("The image could not be exported."); /*I18N*/
        } catch (Throwable e) {
            e.printStackTrace();
            visatApp.handleUnknownException(e);
        } finally {
            visatApp.getMainFrame().setCursor(Cursor.getDefaultCursor());
            visatApp.clearStatusBarMessage();
            pm.done();
        }
        return null;
    }

    private RenderedImage createImageLegend(RasterDataNode raster) {
        ImageLegend imageLegend = initImageLegend(raster);
        return imageLegend.createImage();
    }

    private ImageLegend initImageLegend(RasterDataNode raster) {
        ImageLegend imageLegend = new ImageLegend(raster.getImageInfo(), raster);

        imageLegend.setHeaderText(getLegendHeaderText(raster));
        imageLegend.setOrientation(ImageLegend.VERTICAL);
        imageLegend.setBackgroundTransparency(0.0f);
        imageLegend.setBackgroundTransparencyEnabled(true);
        imageLegend.setAntialiasing(true);

        return imageLegend;
    }

    private String getLegendHeaderText(RasterDataNode raster) {
        String unit = raster.getUnit() != null ? raster.getUnit() : "-";
        unit = unit.replace('*', ' ');
        return "(" + unit + ")";
    }

}
