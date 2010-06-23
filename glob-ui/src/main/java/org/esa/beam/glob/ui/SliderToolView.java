package org.esa.beam.glob.ui;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ImageInfo;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glayer.RasterImageLayerType;
import org.esa.beam.glevel.BandImageMultiLevelSource;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeries;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.TimeZone;

/**
 * User: Thomas Storm
 * Date: 18.06.2010
 * Time: 15:48:31
 */
public class SliderToolView extends AbstractToolView {

    private SceneViewListener sceneViewListener;

    private ProductSceneView currentView;
    private JSlider timeSlider;


    public SliderToolView() {
        sceneViewListener = new SceneViewListener();
    }

    public void setCurrentView(ProductSceneView currentView) {
        if (this.currentView != currentView) {
            this.currentView = currentView;
            configureTimeSlider();
        }
    }

    public ProductSceneView getCurrentView() {
        return currentView;
    }

    @Override
    public void componentShown() {
        VisatApp.getApp().addInternalFrameListener(sceneViewListener);
    }

    @Override
    public void componentHidden() {
        VisatApp.getApp().removeInternalFrameListener(sceneViewListener);
    }

    @Override
    protected JComponent createControl() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setRowFill(0, TableLayout.Fill.HORIZONTAL);
        final JPanel panel = new JPanel(tableLayout);
        timeSlider = new JSlider(JSlider.HORIZONTAL);
        timeSlider.setPaintLabels(true);
        timeSlider.setPaintTicks(true);
        timeSlider.setPaintTrack(true);
        timeSlider.addChangeListener(new SliderChangeListener());
        configureTimeSlider();
        panel.add(timeSlider);
        return panel;
    }

    private void configureTimeSlider() {
        final Product currentProduct = getCurrentProduct();
        if (currentProduct != null) {
            final ProductNodeGroup<Band> bandGroup = currentProduct.getBandGroup();

            timeSlider.setMinimum(0);
            final int nodeCount = bandGroup.getNodeCount();
            timeSlider.setMaximum(nodeCount - 1);
            timeSlider.setSnapToTicks(true);

            final Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
            if (nodeCount > 0) {
                timeSlider.setEnabled(true);
                for (int i = 0; i < nodeCount; i++) {
                    final ProductData.UTC utcStartTime = bandGroup.get(i).getTimeCoding().getStartTime();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    final String dateText = dateFormat.format(utcStartTime.getAsCalendar().getTime());
                    final String timeText = timeFormat.format(utcStartTime.getAsCalendar().getTime());
                    String labelText = String.format("<html><p align=\"center\"> <font size=\"2\">%s<br>%s</font></p>",
                                                     dateText, timeText);
                    labelTable.put(i, new JLabel(labelText));
                }
                timeSlider.setLabelTable(labelTable);
            } else {
                timeSlider.setLabelTable(null);
                timeSlider.setEnabled(false);
            }

            timeSlider.setValue(0);
        } else {
            timeSlider.setLabelTable(null);
            timeSlider.setEnabled(false);
        }
    }

    private Product getCurrentProduct() {
        if (currentView != null && currentView.getRaster() != null) {
            return currentView.getRaster().getProduct();
        }
        return null;
    }

    // todo (mp) - The following should be done on ProdsuctSceneView.setRasters()

    private void exchangeRasterInProductSceneView(Band nextRaster, ProductSceneView sceneView) {
        // todo use a real ProgressMonitor
        final RasterDataNode currentRaster = sceneView.getRaster();
        final ImageInfo imageInfoClone = (ImageInfo) currentRaster.getImageInfo(ProgressMonitor.NULL).clone();
        nextRaster.setImageInfo(imageInfoClone);
        reconfigureBaseImageLayer(nextRaster, currentView);
        sceneView.setRasters(new RasterDataNode[]{nextRaster});
        sceneView.setImageInfo(imageInfoClone.createDeepCopy());
        VisatApp.getApp().getSelectedInternalFrame().setTitle(nextRaster.getDisplayName());
    }

    private void reconfigureBaseImageLayer(RasterDataNode rasterDataNode, ProductSceneView sceneView) {
        final Layer rootLayer = sceneView.getRootLayer();
        final ImageLayer baseImageLayer = (ImageLayer) LayerUtils.getChildLayerById(rootLayer,
                                                                                    ProductSceneView.BASE_IMAGE_LAYER_ID);

        // todo use a real ProgressMonitor
        final BandImageMultiLevelSource multiLevelSource = BandImageMultiLevelSource.create(rasterDataNode,
                                                                                            ProgressMonitor.NULL);
        baseImageLayer.setMultiLevelSource(multiLevelSource);
        baseImageLayer.getConfiguration().setValue(RasterImageLayerType.PROPERTY_NAME_RASTER, rasterDataNode);
        baseImageLayer.getConfiguration().setValue(ImageLayer.PROPERTY_NAME_MULTI_LEVEL_SOURCE, multiLevelSource);
        baseImageLayer.setName(rasterDataNode.getDisplayName());
        try {
            final Field sceneImageField = ProductSceneView.class.getDeclaredField("sceneImage");
            sceneImageField.setAccessible(true);
            final Object sceneImage = sceneImageField.get(sceneView);
            final Field multiLevelSourceField = ProductSceneImage.class.getDeclaredField("bandImageMultiLevelSource");
            multiLevelSourceField.setAccessible(true);
            multiLevelSourceField.set(sceneImage, multiLevelSource);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private class SliderChangeListener implements ChangeListener {

        private int currentBandIndex;

        @Override
        public void stateChanged(ChangeEvent e) {
            final int newBandIndex = timeSlider.getValue();
            if (currentBandIndex != newBandIndex && currentView != null) {
                final Band newRaster = getCurrentProduct().getBandGroup().get(newBandIndex);
                exchangeRasterInProductSceneView(newRaster, currentView);
                currentBandIndex = newBandIndex;
            }
        }

    }

    private class SceneViewListener extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView view = (ProductSceneView) contentPane;
                final RasterDataNode viewRaster = view.getRaster();
                final String viewProductType = viewRaster.getProduct().getProductType();
                if (getCurrentView() != view && viewProductType.equals(
                        TimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
                    setCurrentView(view);
                }
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (currentView == contentPane) {
                setCurrentView(null);
            }
        }
    }
}
