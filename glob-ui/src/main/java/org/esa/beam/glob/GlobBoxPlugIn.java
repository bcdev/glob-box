package org.esa.beam.glob;

import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.VisatPlugIn;

import javax.swing.event.InternalFrameListener;

public class GlobBoxPlugIn implements VisatPlugIn {

    private InternalFrameListener sceneViewListener;

    @Override
    public void start(VisatApp visatApp) {
        GlobBox globBox = GlobBox.getInstance();
        sceneViewListener = globBox.getSceneViewListener();
        ProductManager productManager = visatApp.getProductManager();
        globBox.setProductManager(productManager);
        visatApp.addInternalFrameListener(sceneViewListener);
    }

    @Override
    public void stop(VisatApp visatApp) {
        ProductManager productManager = visatApp.getProductManager();
        visatApp.removeInternalFrameListener(sceneViewListener);
    }

    @Override
    public void updateComponentTreeUI() {
    }

}
