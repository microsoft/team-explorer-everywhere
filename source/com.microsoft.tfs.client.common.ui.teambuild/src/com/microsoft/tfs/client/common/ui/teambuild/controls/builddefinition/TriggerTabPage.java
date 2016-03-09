// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.BuildSourceProviders;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.ISchedule;
import com.microsoft.tfs.core.clients.build.flags.ScheduleDays;
import com.microsoft.tfs.core.clients.build.soapextensions.ContinuousIntegrationType;
import com.microsoft.tfs.util.datetime.CalendarUtils;

public class TriggerTabPage extends BuildDefinitionTabPage {
    private TriggerControl control;

    public TriggerTabPage(final IBuildDefinition buildDefinition) {
        super(buildDefinition);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.teambuild.controls.ToolStripTabPage
     * #createControl(org .eclipse.swt.widgets.Composite)
     */
    @Override
    public Control createControl(final Composite parent) {
        control = new TriggerControl(parent, SWT.NONE);
        populateControl();
        control.validate();
        return control;
    }

    private void populateControl() {
        final ContinuousIntegrationType ciType = getBuildDefinition().getContinuousIntegrationType();

        getControl().getNoCIButton().setSelection(ciType.equals(ContinuousIntegrationType.NONE));

        getControl().getEveryCheckInButton().setSelection(ciType.equals(ContinuousIntegrationType.INDIVIDUAL));

        // Accumulate Check-ins Section
        getControl().getAccumulateButton().setSelection(ciType.equals(ContinuousIntegrationType.BATCH));
        getControl().getMinimumWaitButton().setSelection(
            getControl().getAccumulateButton().getSelection()
                && (getBuildDefinition().getContinuousIntegrationQuietPeriod() > 0));
        if (getControl().getMinimumWaitButton().getSelection()) {
            getControl().getMinimumWaitMinutesText().setText(
                Integer.toString(getBuildDefinition().getContinuousIntegrationQuietPeriod()));
        } else {
            getControl().getMinimumWaitMinutesText().setText(""); //$NON-NLS-1$
        }

        // Gated section
        getControl().getGatedButton().setSelection(ciType.equals(ContinuousIntegrationType.GATED));
        if (!BuildSourceProviders.isTfVersionControl(getBuildDefinition().getDefaultSourceProvider())
            || getBuildDefinition().getBuildServer().getBuildServerVersion().isV2()) {
            getControl().getGatedButton().setEnabled(false);
        }

        // Schedule Section
        getControl().getScheduleButton().setSelection(
            ciType.equals(ContinuousIntegrationType.SCHEDULE)
                || ciType.equals(ContinuousIntegrationType.SCHEDULE_FORCED));

        if (getControl().getScheduleButton().getSelection()) {
            final ISchedule[] schedules = getBuildDefinition().getSchedules();
            if (schedules.length > 0) {
                // Load the schedule data
                final ISchedule schedule = schedules[0];
                getControl().getSundayButton().setSelection(schedule.getDaysToBuild().contains(ScheduleDays.SUNDAY));
                getControl().getMondayButton().setSelection(schedule.getDaysToBuild().contains(ScheduleDays.MONDAY));
                getControl().getTuesdayButton().setSelection(schedule.getDaysToBuild().contains(ScheduleDays.TUESDAY));
                getControl().getWednesdayButton().setSelection(
                    schedule.getDaysToBuild().contains(ScheduleDays.WEDNESDAY));
                getControl().getThursdayButton().setSelection(
                    schedule.getDaysToBuild().contains(ScheduleDays.THURSDAY));
                getControl().getFridayButton().setSelection(schedule.getDaysToBuild().contains(ScheduleDays.FRIDAY));
                getControl().getSaturdayButton().setSelection(
                    schedule.getDaysToBuild().contains(ScheduleDays.SATURDAY));
                getControl().getForcedSchedule().setSelection(ciType.equals(ContinuousIntegrationType.SCHEDULE_FORCED));
                getControl().setScheduleTimeAsSecondsAfterMidnight(schedule.getStartTime());
            } else {
                getControl().getNoCIButton().setSelection(true);
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.teambuild.controls.ToolStripTabPage
     * #getName()
     */
    @Override
    public String getName() {
        return Messages.getString("TriggerTabPage.TabLabelText"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.teambuild.controls.ToolStripTabPage
     * #isValid()
     */
    @Override
    public boolean isValid() {
        return true;
    }

    public TriggerControl getControl() {
        return control;
    }

    public class TriggerControl extends BaseControl {
        private static final int RADIO_INDENT = 10;
        private static final int SECTION_INDENT = 25;
        private final DateFormat TIME_FORMAT = DateFormat.getTimeInstance(DateFormat.SHORT);
        private int[] secondsAfterMidnight;
        private Button noCIButton;
        private Button accumulateButton;
        private Button everyCheckInButton;
        private Button scheduleButton;
        private Button gatedButton;

        private Composite accumulateSection;
        private Button minimumWaitButton;
        private Text minimumWaitMinutesText;

        private Composite scheduleSection;
        private Button mondayButton;
        private Button tuesdayButton;
        private Button wednesdayButton;
        private Button thursdayButton;
        private Button fridayButton;
        private Button saturdayButton;
        private Button sundayButton;
        private Combo scheduleTimeCombo;
        private Button forcedSchedule;

        public TriggerControl(final Composite parent, final int style) {
            super(parent, style);
            createControls(this);
        }

        private void createControls(final Composite composite) {
            final GridLayout layout = SWTUtil.gridLayout(composite, 1);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.horizontalSpacing = getHorizontalSpacing();
            layout.verticalSpacing = getVerticalSpacing() * 2;

            final Label summary =
                SWTUtil.createLabel(composite, SWT.WRAP, Messages.getString("TriggerTabPage.SummaryLabelText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().fill().hGrab().applyTo(summary);
            ControlSize.setCharWidthHint(summary, 42);

            noCIButton = createNoCI(composite);
            everyCheckInButton = createEveryCheckIn(composite);
            accumulateButton = createAccumulate(composite);
            gatedButton = createGated(composite);
            scheduleButton = createSchedule(composite);

        }

        private Button createNoCI(final Composite parent) {
            final Button button =
                SWTUtil.createButton(parent, SWT.RADIO, Messages.getString("TriggerTabPage.ManualButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hIndent(RADIO_INDENT).applyTo(button);
            attachSelectionListener(button);
            return button;
        }

        private Button createEveryCheckIn(final Composite parent) {
            final Button button = SWTUtil.createButton(
                parent,
                SWT.RADIO,
                Messages.getString("TriggerTabPage.ContinuousIntegrationButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hIndent(RADIO_INDENT).applyTo(button);
            attachSelectionListener(button);
            return button;
        }

        private Button createAccumulate(final Composite parent) {
            final Button button =
                SWTUtil.createButton(parent, SWT.RADIO, Messages.getString("TriggerTabPage.RollingBuildsButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hIndent(RADIO_INDENT).applyTo(button);
            attachSelectionListener(button);

            accumulateSection = new Composite(parent, SWT.NONE);
            GridDataBuilder.newInstance().hIndent(SECTION_INDENT).applyTo(accumulateSection);

            final GridLayout layout = new GridLayout(3, false);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.horizontalSpacing = getHorizontalSpacing();
            accumulateSection.setLayout(layout);

            minimumWaitButton = SWTUtil.createButton(
                accumulateSection,
                SWT.CHECK,
                Messages.getString("TriggerTabPage.MinWaitButtonText")); //$NON-NLS-1$
            attachSelectionListener(minimumWaitButton);
            minimumWaitMinutesText = new Text(accumulateSection, SWT.BORDER);
            ControlSize.setCharWidthHint(minimumWaitMinutesText, 8);
            // Text label = new Text(accumulateSection, SWT.READ_ONLY);
            // label.setText("minutes");
            // label.setEditable(false);
            SWTUtil.createLabel(accumulateSection, Messages.getString("TriggerTabPage.MinutesLabelText")); //$NON-NLS-1$

            return button;
        }

        private Button createGated(final Composite parent) {
            final Button button = SWTUtil.createButton(
                parent,
                SWT.RADIO | SWT.WRAP,
                Messages.getString("TriggerTabPage.GatedCheckinButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hIndent(RADIO_INDENT).applyTo(button);
            attachSelectionListener(button);

            return button;
        }

        private Button createSchedule(final Composite parent) {
            final Button button =
                SWTUtil.createButton(parent, SWT.RADIO, Messages.getString("TriggerTabPage.ScheduledBuildButtonText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().hIndent(RADIO_INDENT).applyTo(button);
            attachSelectionListener(button);

            scheduleSection = new Composite(parent, SWT.NONE);
            GridDataBuilder.newInstance().fill().hGrab().hIndent(SECTION_INDENT).applyTo(scheduleSection);
            SWTUtil.gridLayout(scheduleSection, 1);

            final Composite weekdaySection = createWeekdaySection(scheduleSection);
            GridDataBuilder.newInstance().fill().hGrab().applyTo(weekdaySection);
            ControlSize.setCharWidthHint(weekdaySection, 62);

            SWTUtil.createLabel(scheduleSection, Messages.getString("TriggerTabPage.QueueOnDefaultAgentButtonText")); //$NON-NLS-1$
            createScheduleTimeSection(scheduleSection);

            forcedSchedule = SWTUtil.createButton(
                scheduleSection,
                SWT.CHECK,
                Messages.getString("TriggerTabPage.ForceBuildButtonText")); //$NON-NLS-1$

            return button;
        }

        private Composite createScheduleTimeSection(final Composite parent) {
            final Composite timeSection = new Composite(parent, SWT.NONE);
            SWTUtil.gridLayout(timeSection, 2);

            final TimeZone tz = TimeZone.getDefault();
            final Locale locale = Locale.getDefault();
            final Calendar now = new GregorianCalendar(tz, locale);

            scheduleTimeCombo = new Combo(timeSection, SWT.READ_ONLY);
            // List the day in 30 minute internals.
            final Calendar time = Calendar.getInstance();
            CalendarUtils.removeTime(time);
            secondsAfterMidnight = new int[48];
            for (int i = 0; i < 48; i++) {
                scheduleTimeCombo.add(TIME_FORMAT.format(time.getTime()));
                secondsAfterMidnight[i] = CalendarUtils.getSecondsSinceMidnight(time);
                time.add(Calendar.MINUTE, 30);
            }
            // Select a default time (3am) using the same algorithm as
            // Microsoft's client
            setScheduleTimeAsSecondsAfterMidnight(10800);

            scheduleTimeCombo.pack();

            SWTUtil.createLabel(timeSection, tz.getDisplayName(tz.inDaylightTime(now.getTime()), TimeZone.LONG));

            return timeSection;
        }

        private Composite createWeekdaySection(final Composite parent) {
            final Composite weekdaySection = new Composite(parent, SWT.NONE);

            final RowLayout layout = new RowLayout(SWT.HORIZONTAL);
            layout.spacing = 10;
            layout.pack = false;
            weekdaySection.setLayout(layout);

            // Microsoft's UI is not clever enough to present the days using the
            // appropriate locales first day of the
            // week no neither do we. We always display it as Monday-Sunday and
            // always have monday-friday checked as
            // weekdays by default.

            final DateFormatSymbols dateSymbols = new DateFormatSymbols();
            final String[] weekdayNames = dateSymbols.getWeekdays();

            mondayButton = SWTUtil.createButton(weekdaySection, SWT.CHECK, weekdayNames[Calendar.MONDAY]);
            tuesdayButton = SWTUtil.createButton(weekdaySection, SWT.CHECK, weekdayNames[Calendar.TUESDAY]);
            wednesdayButton = SWTUtil.createButton(weekdaySection, SWT.CHECK, weekdayNames[Calendar.WEDNESDAY]);
            thursdayButton = SWTUtil.createButton(weekdaySection, SWT.CHECK, weekdayNames[Calendar.THURSDAY]);
            fridayButton = SWTUtil.createButton(weekdaySection, SWT.CHECK, weekdayNames[Calendar.FRIDAY]);
            saturdayButton = SWTUtil.createButton(weekdaySection, SWT.CHECK, weekdayNames[Calendar.SATURDAY]);
            sundayButton = SWTUtil.createButton(weekdaySection, SWT.CHECK, weekdayNames[Calendar.SUNDAY]);

            mondayButton.setSelection(true);
            tuesdayButton.setSelection(true);
            wednesdayButton.setSelection(true);
            thursdayButton.setSelection(true);
            fridayButton.setSelection(true);
            saturdayButton.setSelection(false);
            sundayButton.setSelection(false);

            // Force it so that at least one check box must be selected in the
            // schedule days.
            final SelectionAdapter forceOneSelectedAdapter = new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    if (!mondayButton.getSelection()
                        && !tuesdayButton.getSelection()
                        && !wednesdayButton.getSelection()
                        && !thursdayButton.getSelection()
                        && !fridayButton.getSelection()
                        && !saturdayButton.getSelection()
                        && !sundayButton.getSelection()) {
                        ((Button) e.getSource()).setSelection(true);
                    }
                }
            };

            mondayButton.addSelectionListener(forceOneSelectedAdapter);
            tuesdayButton.addSelectionListener(forceOneSelectedAdapter);
            wednesdayButton.addSelectionListener(forceOneSelectedAdapter);
            thursdayButton.addSelectionListener(forceOneSelectedAdapter);
            fridayButton.addSelectionListener(forceOneSelectedAdapter);
            saturdayButton.addSelectionListener(forceOneSelectedAdapter);
            sundayButton.addSelectionListener(forceOneSelectedAdapter);

            return weekdaySection;
        }

        private void attachSelectionListener(final Button button) {
            button.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    validate();
                }
            });
        }

        protected void validate() {
            setSecionEnabled(accumulateSection, accumulateButton.getSelection());
            minimumWaitMinutesText.setEnabled(accumulateButton.getSelection() && minimumWaitButton.getSelection());
            setSecionEnabled(scheduleSection, scheduleButton.getSelection());
        }

        protected void setSecionEnabled(final Composite section, final boolean isEnabled) {
            final Control[] children = section.getChildren();

            for (int i = 0; i < children.length; i++) {
                if (children[i] instanceof Button) {
                    ((Button) children[i]).setEnabled(isEnabled);
                } else if (children[i] instanceof Combo) {
                    ((Combo) children[i]).setEnabled(isEnabled);
                }
                // else if (children[i] instanceof Text)
                // {
                // ((Text) children[i]).setEnabled(isEnabled);
                // }
                else if (children[i] instanceof Label) {
                    ((Label) children[i]).setEnabled(isEnabled);
                } else if (children[i] instanceof Composite) {
                    // Recurse into any child composites.
                    setSecionEnabled((Composite) children[i], isEnabled);
                }
            }

        }

        public void setScheduleTimeAsSecondsAfterMidnight(final int startTime) {
            final Calendar time = Calendar.getInstance();

            // Set the time to be midnight.
            CalendarUtils.removeTime(time);

            // Add the specified number of seconds.
            time.add(Calendar.SECOND, startTime);
            scheduleTimeCombo.setText(TIME_FORMAT.format(time.getTime()));
        }

        public int getScheduleTimeAsSecondsAfterMidnight() {
            return secondsAfterMidnight[scheduleTimeCombo.getSelectionIndex()];
        }

        public ScheduleDays getScheduleDays() {
            final ScheduleDays days = new ScheduleDays();
            if (sundayButton.getSelection()) {
                days.add(ScheduleDays.SUNDAY);
            }
            if (mondayButton.getSelection()) {
                days.add(ScheduleDays.MONDAY);
            }
            if (tuesdayButton.getSelection()) {
                days.add(ScheduleDays.TUESDAY);
            }
            if (wednesdayButton.getSelection()) {
                days.add(ScheduleDays.WEDNESDAY);
            }
            if (thursdayButton.getSelection()) {
                days.add(ScheduleDays.THURSDAY);
            }
            if (fridayButton.getSelection()) {
                days.add(ScheduleDays.FRIDAY);
            }
            if (saturdayButton.getSelection()) {
                days.add(ScheduleDays.SATURDAY);
            }
            return days;
        }

        public Button getNoCIButton() {
            return noCIButton;
        }

        public Button getAccumulateButton() {
            return accumulateButton;
        }

        public Button getEveryCheckInButton() {
            return everyCheckInButton;
        }

        public Button getScheduleButton() {
            return scheduleButton;
        }

        public Composite getAccumulateSection() {
            return accumulateSection;
        }

        public Button getMinimumWaitButton() {
            return minimumWaitButton;
        }

        public Text getMinimumWaitMinutesText() {
            return minimumWaitMinutesText;
        }

        public Composite getScheduleSection() {
            return scheduleSection;
        }

        public Button getMondayButton() {
            return mondayButton;
        }

        public Button getTuesdayButton() {
            return tuesdayButton;
        }

        public Button getWednesdayButton() {
            return wednesdayButton;
        }

        public Button getThursdayButton() {
            return thursdayButton;
        }

        public Button getFridayButton() {
            return fridayButton;
        }

        public Button getSaturdayButton() {
            return saturdayButton;
        }

        public Button getSundayButton() {
            return sundayButton;
        }

        public Combo getScheduleTimeCombo() {
            return scheduleTimeCombo;
        }

        public Button getForcedSchedule() {
            return forcedSchedule;
        }

        public Button getGatedButton() {
            return gatedButton;
        }

    }

}
