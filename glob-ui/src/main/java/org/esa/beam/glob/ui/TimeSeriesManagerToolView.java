package org.esa.beam.glob.ui;

import org.esa.beam.framework.ui.application.support.AbstractToolView;

import javax.swing.JComponent;

public class TimeSeriesManagerToolView extends AbstractToolView {

    @Override
    protected JComponent createControl() {
        return new TimeSeriesManagerForm();
    }
}
