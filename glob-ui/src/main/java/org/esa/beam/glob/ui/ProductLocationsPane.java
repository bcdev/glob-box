package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.FolderChooser;
import org.esa.beam.framework.dataio.ProductIOPlugIn;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.glob.core.timeseries.datamodel.ProductLocationType;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;

class ProductLocationsPane extends JPanel {

    private ProductLocationsPaneModel model;
    private JList sourceList;

    ProductLocationsPane() {
        this(new DefaultProductLocationsPaneModel());
    }

    ProductLocationsPane(ProductLocationsPaneModel model) {
        this.model = model;
        createPane();
    }

    public void setModel(ProductLocationsPaneModel model) {
        this.model = model;
        updatePane();
    }

    private void createPane() {
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setColumnFill(0, TableLayout.Fill.BOTH);
        tableLayout.setColumnFill(1, TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableWeightY(1.0);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setColumnWeightX(1, 0.0);
        tableLayout.setColumnWeightY(1, 0.0);
        tableLayout.setCellRowspan(0, 0, 2);
        setLayout(tableLayout);
        sourceList = new JList(model);
        sourceList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                                                                                       cellHasFocus);
                if (value instanceof ProductLocation) {
                    final ProductLocation location = (ProductLocation) value;
                    String path = location.getPath();
                    if(location.getProductLocationType() != ProductLocationType.FILE) {
                        if (!path.endsWith(File.separator)) {
                            path += File.separator;
                        }
                    }
                    if (location.getProductLocationType() == ProductLocationType.DIRECTORY) {
                        path += "*";
                    }
                    if (location.getProductLocationType() == ProductLocationType.DIRECTORY_REC) {
                        path += "**";
                    }

                    label.setText(path);
                }
                return label;

            }
        });


        final JButton addButton = new JButton("Add");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JPopupMenu popup = new JPopupMenu("Add");
                final Rectangle buttonBounds = addButton.getBounds();
                popup.add(new AddDirectoryAction(false));
                popup.add(new AddDirectoryAction(true));
                popup.add(new AddFileAction());
                popup.show(addButton, 1, buttonBounds.height + 1);
            }
        });

        final JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.remove(sourceList.getSelectedIndices());
            }
        });
        add(new JScrollPane(sourceList));
        add(addButton);
        add(removeButton, new TableLayout.Cell(1,1));
    }

    private void updatePane() {
        sourceList.setModel(model);
    }

    private class AddDirectoryAction extends AbstractAction {

        private boolean recursive;

        private AddDirectoryAction(boolean recursive) {
            this("Add Directory" + (recursive ? " Recursive": ""));
            this.recursive = recursive;
        }

        protected AddDirectoryAction(String title) {
            super(title);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final FolderChooser folderChooser = new FolderChooser();
            final VisatApp visatApp = VisatApp.getApp();
            final PropertyMap preferences = visatApp.getPreferences();
            String lastDir = preferences.getPropertyString(VisatApp.PROPERTY_KEY_APP_LAST_OPEN_DIR,
                                                                SystemUtils.getUserHomeDir().getPath());

            folderChooser.setCurrentDirectory(new File(lastDir));

            final int result = folderChooser.showOpenDialog(ProductLocationsPane.this);
            if(result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            File currentDir = folderChooser.getSelectedFolder();
            model.addDirectory(currentDir, recursive);
            if (currentDir != null) {
                preferences.setPropertyString(VisatApp.PROPERTY_KEY_APP_LAST_OPEN_DIR, currentDir.getAbsolutePath());
            }

        }
    }

    private class AddFileAction extends AbstractAction {

        private AddFileAction() {
            super("Add Product(s)");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final VisatApp visatApp = VisatApp.getApp();
            final PropertyMap preferences = visatApp.getPreferences();
            String lastDir = preferences.getPropertyString(VisatApp.PROPERTY_KEY_APP_LAST_OPEN_DIR,
                                                                SystemUtils.getUserHomeDir().getPath());
            String lastFormat = preferences.getPropertyString(VisatApp.PROPERTY_KEY_APP_LAST_OPEN_FORMAT,
                                                                   VisatApp.ALL_FILES_IDENTIFIER);
            BeamFileChooser fileChooser = new BeamFileChooser();
            fileChooser.setCurrentDirectory(new File(lastDir));
            fileChooser.setAcceptAllFileFilterUsed(true);
            fileChooser.setDialogTitle("Select Product(s)");
            fileChooser.setMultiSelectionEnabled(true);

            FileFilter actualFileFilter = fileChooser.getAcceptAllFileFilter();
            Iterator allReaderPlugIns = ProductIOPlugInManager.getInstance().getAllReaderPlugIns();
            while (allReaderPlugIns.hasNext()) {
                final ProductIOPlugIn plugIn = (ProductIOPlugIn) allReaderPlugIns.next();
                BeamFileFilter productFileFilter = plugIn.getProductFileFilter();
                fileChooser.addChoosableFileFilter(productFileFilter);
                if (!VisatApp.ALL_FILES_IDENTIFIER.equals(lastFormat) &&
                    productFileFilter.getFormatName().equals(lastFormat)) {
                    actualFileFilter = productFileFilter;
                }
            }
            fileChooser.setFileFilter(actualFileFilter);

            int result = fileChooser.showDialog(visatApp.getMainFrame(), "Open Product");    /*I18N*/
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }

            String currentDir = fileChooser.getCurrentDirectory().getAbsolutePath();
            if (currentDir != null) {
                preferences.setPropertyString(VisatApp.PROPERTY_KEY_APP_LAST_OPEN_DIR, currentDir);
            }

            if (fileChooser.getFileFilter() instanceof BeamFileFilter) {
                String currentFormat = ((BeamFileFilter) fileChooser.getFileFilter()).getFormatName();
                if (currentFormat != null) {
                    preferences.setPropertyString(VisatApp.PROPERTY_KEY_APP_LAST_OPEN_FORMAT, currentFormat);
                }
            } else {
                preferences.setPropertyString(VisatApp.PROPERTY_KEY_APP_LAST_OPEN_FORMAT, VisatApp.ALL_FILES_IDENTIFIER);
            }
            final File[] selectedFiles = fileChooser.getSelectedFiles();
            model.addFiles(selectedFiles);
        }
    }
}
