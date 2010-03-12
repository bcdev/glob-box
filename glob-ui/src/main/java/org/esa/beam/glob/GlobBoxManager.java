package org.esa.beam.glob;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.Container;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class GlobBoxManager {

    public static final String CURRENT_VIEW_PROPERTY = "currentView";

    private static GlobBoxManager instance;

    private final ArrayList<Product> productList;
    private final SceneViewListener sceneViewListener;
    private final AtomicReference<RasterDataNode> refRaster;
    private final ProductManager.Listener productManagerListener;
    private final PropertyChangeSupport propertyChangeSupport;

    public ProductSceneView getCurrentView() {
        return currentView;
    }

    private ProductSceneView currentView;
    private ProductManager productManager;
    private List<RasterDataNode> rasterList;

    public static GlobBoxManager getInstance() {
        if (instance == null) {
            instance = new GlobBoxManager();
        }
        return instance;
    }

    private GlobBoxManager() {
        productList = new ArrayList<Product>();
        productManagerListener = new ProductManagerListener();
        sceneViewListener = new SceneViewListener();
        refRaster = new AtomicReference<RasterDataNode>();
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public InternalFrameListener getSceneViewListener() {
        return sceneViewListener;
    }

    public void setProductManager(ProductManager productManager) {
        if (this.productManager != null) {
            this.productManager.removeListener(this.productManagerListener);
        }
        this.productManager = productManager;
        if (this.productManager != null) {
            this.productManager.addListener(this.productManagerListener);
        }
    }

    public List<RasterDataNode> getCurrentRasterList() {
        if (rasterList == null) {
            rasterList = createRasterList(getReferenceRaster());
        }
        return rasterList;
    }

    public List<Product> getCurrentProductList() {
        return Collections.unmodifiableList(productList);
    }

    void setReferenceRaster(final RasterDataNode newRefRaster) {
        final RasterDataNode currentRefRaster = refRaster.get();
        if (currentRefRaster != newRefRaster) {
            refRaster.set(newRefRaster);
            productList.clear();
            resetRasterList();
            for (Product product : productManager.getProducts()) {
                if (isAddableProduct(product)) {
                    productList.add(product);
                }
            }
        }
    }

    private void resetRasterList() {
        rasterList = null;
    }

    private List<RasterDataNode> createRasterList(RasterDataNode referenceRaster) {
        final List<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        if (referenceRaster != null) {
            final String rasterName = referenceRaster.getName();
            for (Product p : getCurrentProductList()) {
                rasterList.add(p.getRasterDataNode(rasterName));
            }
        }
        return rasterList;
    }

    private RasterDataNode getReferenceRaster() {
        return refRaster.get();
    }

    private boolean isAddableProduct(Product newProduct) {
        RasterDataNode referenceRaster = getReferenceRaster();
        return referenceRaster != null &&
               newProduct.getProductType().equals(referenceRaster.getProduct().getProductType()) &&
               newProduct.getStartTime() != null &&
               referenceRaster.getProduct().isCompatibleProduct(newProduct, 1.0e-6f);
    }

    public void addPropertyChangeListener(String property, PropertyChangeListener pcl) {
        this.propertyChangeSupport.addPropertyChangeListener(property, pcl);
    }

    public void removePropertyChangeListener(String property, PropertyChangeListener pcl) {
        this.propertyChangeSupport.removePropertyChangeListener(property, pcl);
    }

    private void setCurrentView(ProductSceneView sceneView) {
        ProductSceneView oldView = currentView;
        currentView = sceneView;
        propertyChangeSupport.firePropertyChange(CURRENT_VIEW_PROPERTY, oldView, currentView);
    }


    private class ProductManagerListener implements ProductManager.Listener {

        @Override
        public void productAdded(ProductManager.Event event) {
            final Product newProduct = event.getProduct();
            if (isAddableProduct(newProduct)) {
                productList.add(newProduct);
                resetRasterList();
            }
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            final Product oldProduct = event.getProduct();
            productList.remove(oldProduct);
            resetRasterList();
        }

    }

    private class SceneViewListener extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView sceneView = (ProductSceneView) contentPane;
                setReferenceRaster(sceneView.getRaster());
                setCurrentView(sceneView);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                setReferenceRaster(null);
                setCurrentView(null);
            }
        }
    }
}