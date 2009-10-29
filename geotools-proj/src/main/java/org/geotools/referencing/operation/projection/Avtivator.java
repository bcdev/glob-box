/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.geotools.referencing.operation.projection;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;

import org.geotools.factory.FactoryIteratorProvider;
import org.geotools.factory.GeoTools;
import org.geotools.referencing.operation.MathTransformProvider;

import java.util.Iterator;

public class Avtivator implements Activator{

    private CeresFactoryIteratorProvider factoryIteratorProvider;

    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        final ServiceRegistry<MathTransformProvider> serviceRegistry = ServiceRegistryManager.getInstance().getServiceRegistry(MathTransformProvider.class);
        factoryIteratorProvider = new CeresFactoryIteratorProvider(serviceRegistry);
        GeoTools.addFactoryIteratorProvider(factoryIteratorProvider);
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
        if (factoryIteratorProvider != null) {
            GeoTools.removeFactoryIteratorProvider(factoryIteratorProvider);
        }
    }

    private final class CeresFactoryIteratorProvider implements FactoryIteratorProvider {
        private final ServiceRegistry<MathTransformProvider> serviceRegistry;

        private CeresFactoryIteratorProvider(ServiceRegistry<MathTransformProvider> serviceRegistry) {
            this.serviceRegistry = serviceRegistry;
        }

        @Override
        public <T> Iterator<T> iterator(Class<T> category) {
            if (category.equals(serviceRegistry.getServiceType()) ) {
                return (Iterator<T>) serviceRegistry.getServices().iterator();
            } else {
                return null;
            }
        }
    }

}
