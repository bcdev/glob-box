package org.esa.beam.glob.ui;

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.visat.actions.AbstractVisatAction;

public class NewTimeSeriesAction extends AbstractVisatAction {
    public static String ID = "newTimeSeriesAction";
    @Override
    public void actionPerformed(CommandEvent event) {
        super.actionPerformed(event);
        getAppContext().handleError("Not implemented yet.", null);

    }
}
