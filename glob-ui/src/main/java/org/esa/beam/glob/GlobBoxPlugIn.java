package org.esa.beam.glob;

import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.VisatPlugIn;

import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;

public class GlobBoxPlugIn implements VisatPlugIn {

    private SceneViewListener sceneViewListener;

    @Override
    public void start(VisatApp visatApp) {
        sceneViewListener = new SceneViewListener();
        visatApp.addInternalFrameListener(sceneViewListener);
    }

    @Override
    public void stop(VisatApp visatApp) {
        visatApp.removeInternalFrameListener(sceneViewListener);
    }

    @Override
    public void updateComponentTreeUI() {
    }

    private class SceneViewListener extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView sceneView = (ProductSceneView) contentPane;

            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
            }
        }
    }

}
