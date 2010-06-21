package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
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
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.TimeZone;

import static org.esa.beam.glob.ui.CreateTimeSeriesAction.*;

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

    private class SliderChangeListener implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            final int selectedBandIndex = timeSlider.getValue();
            System.out.println("selectedBandIndex = " + selectedBandIndex);
            final Band band = getCurrentProduct().getBandGroup().get(selectedBandIndex);
            currentView.getRaster().setSourceImage(band.getGeophysicalImage());
            currentView.updateImage();
        }
    }

    private class SceneViewListener extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView view = (ProductSceneView) contentPane;
                final RasterDataNode currentRaster = view.getRaster();
                if (currentRaster.getProduct().getMetadataRoot().containsElement(
                        TIME_SERIES_METADATA_ELEMENT)) {
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
