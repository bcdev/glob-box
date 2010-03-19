package org.esa.beam.glob;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.glob.ui.GlobToolboxManagerFormModel;

import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import java.awt.Container;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class GlobBox {

    public static final String CURRENT_VIEW_PROPERTY = "currentView";
    public static final String RASTER_LIST_PROPERTY = "rasterList";

    private static GlobBox instance;

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

    private final GlobToolboxManagerFormModel model;

    public static GlobBox getInstance() {
        if (instance == null) {
            instance = new GlobBox();
        }
        return instance;
    }

    private GlobBox() {
        productList = new ArrayList<Product>();
        productManagerListener = new ProductManagerListener();
        sceneViewListener = new SceneViewListener();
        refRaster = new AtomicReference<RasterDataNode>();
        rasterList = Collections.emptyList();
        propertyChangeSupport = new PropertyChangeSupport(this);
        this.model = new GlobToolboxManagerFormModel(this);
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

    public List<RasterDataNode> getRasterList() {
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
            for (Product product : productManager.getProducts()) {
                if (isAddableProduct(product)) {
                    productList.add(product);
                }
            }
            rasterList = createRasterList(getReferenceRaster());
            propertyChangeSupport.firePropertyChange(RASTER_LIST_PROPERTY, null, rasterList);
        }
    }

    private List<RasterDataNode> createRasterList(RasterDataNode referenceRaster) {
        final List<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        if (referenceRaster != null) {
            final String rasterName = referenceRaster.getName();
            for (Product p : getCurrentProductList()) {
                rasterList.add(p.getRasterDataNode(rasterName));
            }
        }
        sortRasterList(rasterList);
        return rasterList;
    }

    private void sortRasterList(List<RasterDataNode> rasterList) {
        Collections.sort(rasterList, new Comparator<RasterDataNode>() {
            @Override
            public int compare(RasterDataNode raster1, RasterDataNode raster2) {
                final Date raster1Date = raster1.getProduct().getStartTime().getAsDate();
                final Date raster2Date = raster2.getProduct().getStartTime().getAsDate();
                return raster1Date.compareTo(raster2Date) * -1;
            }
        });
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
                addToRasterList(newProduct);
            }
        }

        @Override
        public void productRemoved(ProductManager.Event event) {
            final Product oldProduct = event.getProduct();
            productList.remove(oldProduct);
            removeFromRasterList(oldProduct);
        }

    }

    private void addToRasterList(Product newProduct) {
        final RasterDataNode raster = getReferenceRaster();
        if (raster != null) {
            final String rasterName = raster.getName();
            final RasterDataNode newRaster = newProduct.getRasterDataNode(rasterName);
            rasterList.add(newRaster);
            sortRasterList(rasterList);
            propertyChangeSupport.firePropertyChange(RASTER_LIST_PROPERTY, null, newRaster);
        }
    }

    private void removeFromRasterList(Product oldProduct) {
        final RasterDataNode raster = getReferenceRaster();
        if (raster != null) {
            final String rasterName = raster.getName();
            final RasterDataNode removedRaster = oldProduct.getRasterDataNode(rasterName);
            rasterList.remove(removedRaster);
            sortRasterList(rasterList);
            propertyChangeSupport.firePropertyChange(RASTER_LIST_PROPERTY, removedRaster, null);
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

    public GlobToolboxManagerFormModel getModel() {
        return model;
    }
}