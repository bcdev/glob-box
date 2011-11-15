/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.glob.ui.manager;

import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.glob.core.insitu.InsituLoader;
import org.esa.beam.glob.core.insitu.InsituLoaderFactory;
import org.esa.beam.glob.core.insitu.InsituSource;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.beam.glob.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Action for loading in situ data.
 *
 * @author Thomas Storm
 * @author Sabine Embacher
 */
public class LoadInsituAction extends AbstractAction {

    private static final String PROPERTY_KEY_LAST_OPEN_INSITU_DIR = "glob.file.lastInsituOpenDir";

    private final AbstractTimeSeries currentTimeSeries;

    public LoadInsituAction(AbstractTimeSeries currentTimeSeries) {
        putValue(SHORT_DESCRIPTION, "Import in situ source file");
        putValue(LARGE_ICON_KEY, UIUtils.loadImageIcon("icons/Import24.gif"));
        this.currentTimeSeries = currentTimeSeries;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final VisatApp visatApp = VisatApp.getApp();
        final PropertyMap preferences = visatApp.getPreferences();
        String lastDir = preferences.getPropertyString(PROPERTY_KEY_LAST_OPEN_INSITU_DIR, SystemUtils.getUserHomeDir().getPath());
        final BeamFileChooser fileChooser = new BeamFileChooser(new File(lastDir));
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setDialogTitle("Select insitu source file");
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter actualFileFilter = fileChooser.getAcceptAllFileFilter();
        fileChooser.setFileFilter(actualFileFilter);

        int result = fileChooser.showDialog(visatApp.getMainFrame(), "Select in situ source file");    /*I18N*/
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        final File selectedFile = fileChooser.getSelectedFile();
        try {
            final InsituLoader insituLoader = InsituLoaderFactory.createInsituLoader(selectedFile);
            final InsituSource insituSource = new InsituSource(insituLoader);
            currentTimeSeries.setInsituSource(insituSource);
            currentTimeSeries.addTimeSeriesListener(new TimeSeriesListener() {
                @Override
                public void nodeAdded(ProductNodeEvent event) {
                    System.out.println("nodeAdded = " + event.getType());
                }

                @Override
                public void nodeChanged(ProductNodeEvent event) {
                    System.out.println("nodeChanged = " + event.getType());
                }

                @Override
                public void nodeDataChanged(ProductNodeEvent event) {
                    System.out.println("nodeDataChanged = " + event.getType());
                }

                @Override
                public void nodeRemoved(ProductNodeEvent event) {
                    System.out.println("nodeRemoved = " + event.getType());
                }

                @Override
                public void timeSeriesChanged(TimeSeriesChangeEvent event) {
                    System.out.println("event = " + event.getType());
                }
            });
        } catch (IOException exception) {
            BeamLogManager.getSystemLogger().log(Level.WARNING, "Unable to load in situ data from '" + selectedFile + "'.", exception);
            return;
        }

        String currentDir = fileChooser.getCurrentDirectory().getAbsolutePath();
        if (currentDir != null) {
            preferences.setPropertyString(PROPERTY_KEY_LAST_OPEN_INSITU_DIR, currentDir);
        }
    }


}