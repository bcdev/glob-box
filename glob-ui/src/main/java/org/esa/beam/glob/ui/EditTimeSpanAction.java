package org.esa.beam.glob.ui;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.combobox.DateComboBox;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.glob.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.glob.core.timeseries.datamodel.DefaultTimeCoding;
import org.esa.beam.glob.core.timeseries.datamodel.TimeCoding;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

class EditTimeSpanAction extends AbstractAction {

    private final AbstractTimeSeries timeSeries;

    EditTimeSpanAction(AbstractTimeSeries timeSeries) {
        this.timeSeries = timeSeries;
        setEnabled(timeSeries != null);
        putValue(NAME, "[?]"); // todo set name
//        putValue(LARGE_ICON_KEY, UIUtils.loadImageIcon("icons/ICON_NAME.gif")); // todo set icon
        putValue(ACTION_COMMAND_KEY, getClass().getName());
        putValue(SHORT_DESCRIPTION, "Edit time span");
        putValue("componentName", "EditTimeSpan");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object source = e.getSource();
        Window window = null;
        if (source instanceof Component) {
            window = SwingUtilities.getWindowAncestor((Component) source);
        }

        final ModalDialog dialog = new EditTimeSpanDialog(window, timeSeries);
        dialog.show();
    }

    private static class EditTimeSpanDialog extends ModalDialog {

        private final Calendar dateTimePrototype;
        private final SimpleDateFormat dateFormat;
        private AbstractTimeSeries timeSeries;
        private DateComboBox startTimeBox;
        private DateComboBox endTimeBox;

        private EditTimeSpanDialog(Window window, AbstractTimeSeries timeSeries) {
            super(window, "Edit Time Span", ModalDialog.ID_OK_CANCEL, null);
            dateTimePrototype = createCalendarPrototype();
            dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
            this.timeSeries = timeSeries;
            createUserInterface();
        }

        @Override
        protected void onOK() {
            final ProductData.UTC startTime = ProductData.UTC.create(startTimeBox.getDate(), 0);
            final ProductData.UTC endTime = ProductData.UTC.create(endTimeBox.getDate(), 0);
            timeSeries.setTimeCoding(new DefaultTimeCoding(startTime, endTime,
                                                           timeSeries.getTsProduct().getSceneRasterHeight()));

            super.onOK();
        }

        @Override
        protected boolean verifyUserInput() {
            if (endTimeBox.getCalendar().compareTo(startTimeBox.getCalendar()) < 0) {
                showErrorDialog("End time is before start time."); 
                return false;
            }
            return true;
        }

        private void createUserInterface() {
            final TableLayout tableLayout = new TableLayout(2);
            tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
            tableLayout.setTableWeightX(1.0);
            tableLayout.setTableFill(TableLayout.Fill.BOTH);
            tableLayout.setTablePadding(4, 4);
            JPanel content = new JPanel(tableLayout);
            final JLabel startTimeLabel = new JLabel("Start time:");
            startTimeBox = createDateComboBox();
            final TimeCoding timeCoding = timeSeries.getTimeCoding();
            startTimeBox.setCalendar(timeCoding.getStartTime().getAsCalendar());
            final JLabel endTimeLabel = new JLabel("End time:");
            endTimeBox = createDateComboBox();
            endTimeBox.setCalendar(timeCoding.getEndTime().getAsCalendar());
            content.add(startTimeLabel);
            content.add(startTimeBox);
            content.add(endTimeLabel);
            content.add(endTimeBox);
            setContent(content);
        }

        private static Calendar createCalendarPrototype() {
            final Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, 2000);
            cal.set(Calendar.MONDAY, 8);
            cal.set(Calendar.DAY_OF_MONTH, 30);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            return cal;
        }

        private DateComboBox createDateComboBox() {
            final DateComboBox box = new DateComboBox();
            box.setPrototypeDisplayValue(dateTimePrototype);
            box.setTimeDisplayed(true);
            box.setFormat(dateFormat);
            box.setShowNoneButton(false);
            box.setShowTodayButton(false);
            box.setShowOKButton(true);
            box.setEditable(false);
            return box;
        }


    }

}
