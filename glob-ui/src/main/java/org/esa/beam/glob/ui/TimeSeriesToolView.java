package org.esa.beam.glob.ui;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.diagram.DiagramCanvas;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class TimeSeriesToolView extends AbstractToolView {

    private String titleBase;
    private DiagramCanvas diagramCanvas;
    private TimeSeriesPPL pixelPosListener;
    private ProductSceneView currentView;
    private TimeSeriesDiagram timeSeriesDiagram;

    public TimeSeriesToolView() {
        pixelPosListener = new TimeSeriesPPL();
    }

    @Override
    protected JComponent createControl() {
        titleBase = getDescriptor().getTitle();
        JPanel control = new JPanel(new BorderLayout(4, 4));

        diagramCanvas = new DiagramCanvas();
        diagramCanvas.setPreferredSize(new Dimension(300, 200));
        diagramCanvas.setMessageText("No product selected."); /*I18N*/
        diagramCanvas.setBackground(Color.white);
        diagramCanvas.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));

        control.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        control.add(BorderLayout.CENTER, diagramCanvas);
        control.setPreferredSize(new Dimension(320, 200));

        VisatApp.getApp().addInternalFrameListener(new TimeSeriesIFL());
        VisatApp.getApp().getProductManager().addListener(new TimeSeriesPML());


        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
        setCurrentView(view);
        updateUIState();
        return control;
    }

    private void updateUIState() {
        updateTitle();
    }

    private void updateTitle() {
        if (currentView != null) {
            setTitle(titleBase + " - " + currentView.getRaster().getName());
        } else {
            setTitle(titleBase);
        }
    }


    private void setCurrentView(ProductSceneView view) {
        if (view != null) {
            view.addPixelPositionListener(pixelPosListener);
        }
        currentView = view;
        updateUIState();
    }

    public ProductSceneView getCurrentView() {
        return currentView;
    }

    private void recreateSpectraDiagram() {
        TimeSeriesDiagram timeSeriesDiagram = getTimeSeriesDiagram();
        timeSeriesDiagram.setBands(getAvailableBands());
        updateUIState();
    }

    private ArrayList<RasterDataNode> getAvailableBands() {
        ArrayList<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        final ProductSceneView sceneView = getCurrentView();
        if (sceneView != null) {
            final String rasterName = sceneView.getRaster().getName();
            for (Product p : getAvailableProducts()) {
                rasterList.add(p.getRasterDataNode(rasterName));
            }
        }

        return rasterList;
    }

    private ArrayList<Product> getAvailableProducts() {
        final ProductSceneView sceneView = getCurrentView();
        ArrayList<Product> productList = new ArrayList<Product>();
        if (sceneView != null) {
            final String productType = sceneView.getProduct().getProductType();
            final ProductManager productManager = VisatApp.getApp().getProductManager();

            for (Product p : productManager.getProducts()) {
                if (p.getProductType().equals(productType)) {
                    productList.add(p);
                }
            }
        }

        return productList;
    }

    private void updateTimeSeries(int pixelX, int pixelY, int currentLevel) {
        timeSeriesDiagram.update(pixelX, pixelY, currentLevel);
    }

    public TimeSeriesDiagram getTimeSeriesDiagram() {
        return timeSeriesDiagram;
    }

    private class TimeSeriesIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
           final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                setCurrentView((ProductSceneView) contentPane);
            }

        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane == currentView) {
                setCurrentView(null);
            }
        }
    }

    private class TimeSeriesPML implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
        }
    }

    private class TimeSeriesPPL implements PixelPositionListener {

        @Override
        public void pixelPosChanged(ImageLayer imageLayer,
                                    int pixelX,
                                    int pixelY,
                                    int currentLevel,
                                    boolean pixelPosValid,
                                    MouseEvent e) {
            diagramCanvas.setMessageText(null);
            if (pixelPosValid && isActive()) {
                updateTimeSeries(pixelX, pixelY, currentLevel);
            }
            if (e.isShiftDown()) {
                getTimeSeriesDiagram().adjustAxes(true);
            }
        }

        @Override
        public void pixelPosNotAvailable() {

            if (isActive()) {
                getTimeSeriesDiagram().clearDiagram();
                diagramCanvas.repaint();
            }
        }

        private boolean isActive() {
            return isVisible() && getTimeSeriesDiagram() != null;
        }

    }

}
