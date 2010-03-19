package org.esa.beam.glob.ui;

import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.JComponent;

public class GlobToolboxManagerToolView extends AbstractToolView {

    @Override
    protected JComponent createControl() {
        return new GlobToolboxManagerForm();
    }
}
