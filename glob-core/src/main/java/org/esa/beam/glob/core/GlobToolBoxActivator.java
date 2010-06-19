package org.esa.beam.glob.core;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import org.esa.beam.framework.datamodel.ProductNode;

/**
 * User: Marco
 * Date: 19.06.2010
 */
public class GlobToolBoxActivator implements Activator {
    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        ExtensionManager.getInstance().register(ProductNode.class, new TimeCodingExtensionFactory());
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
    }

}
