// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.util.ConnectionHelper;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.core.pendingcheckin.events.AffectedTeamProjectsChangedEvent;
import com.microsoft.tfs.core.pendingcheckin.events.AffectedTeamProjectsChangedListener;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class NotesCheckinControl extends AbstractCheckinSubControl {
    private static final Log log = LogFactory.getLog(NotesCheckinControl.class);

    private static final String BUSY_MESSAGE = Messages.getString("NotesCheckinControl.BusyMessage"); //$NON-NLS-1$
    private static final String NO_NOTES_MESSAGE = Messages.getString("NotesCheckinControl.NoNotesMessage"); //$NON-NLS-1$
    private static final String NO_REPOSITORY_MESSAGE = Messages.getString("NotesCheckinControl.NoRepositoryMessage"); //$NON-NLS-1$
    private static final String NO_SAVED_NOTES_MESSAGE = Messages.getString("NotesCheckinControl.NoSavedNotesMessage"); //$NON-NLS-1$
    private static final String NO_SAVED_NOTES_SHELVESET_MESSAGE =
        Messages.getString("NotesCheckinControl.NoSavedNotesShelvesetMessage"); //$NON-NLS-1$

    private static final String REQUIRED_MESSAGE = Messages.getString("NotesCheckinControl.RequiredMessage"); //$NON-NLS-1$

    private static final int REQUIRED_EMPTY_COLOR = SWT.COLOR_DARK_GRAY;

    /*
     * Use a scrolled composite to hold children - this may be either a
     * composite containing the checkin notes, or a composite containing a text
     * label (loading or no notes.)
     */
    private final ScrolledComposite rootComposite;

    private final CheckinControlOptions options;

    private TFSRepository repository;
    private PendingCheckin pendingCheckin;

    private String[] projects = new String[0];
    private boolean dirty = true;
    private boolean updating = false;

    private CheckinNote checkinNote = new CheckinNote();

    private final SingleListenerFacade listeners = new SingleListenerFacade(CheckinNoteListener.class);

    private final TeamProjectsListener teamProjectsListener = new TeamProjectsListener();

    private CheckinNoteFieldControl[] fieldControls = new CheckinNoteFieldControl[0];

    protected NotesCheckinControl(final Composite parent, final int style, final CheckinControlOptions options) {
        super(parent, style, Messages.getString("NotesCheckinControl.Title"), CheckinSubControlType.CHECKIN_NOTES); //$NON-NLS-1$

        this.options = options;

        final FillLayout layout = new FillLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        final int rootStyle = options.isForDialog() ? SWT.BORDER : SWT.NONE;
        rootComposite = new ScrolledComposite(this, SWT.V_SCROLL | rootStyle);
        rootComposite.setLayout(new FillLayout());
        rootComposite.setExpandHorizontal(true);
        rootComposite.setExpandVertical(true);

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                if (pendingCheckin != null) {
                    pendingCheckin.getPendingChanges().removeAffectedTeamProjectsChangedListener(teamProjectsListener);
                }
            }
        });
    }

    @Override
    public void addContributions(final IContributionManager contributionManager, final String groupName) {
    }

    @Override
    public void removeContributions(final IContributionManager contributionManager, final String groupname) {
    }

    public void addCheckinNoteChangedListener(final CheckinNoteListener checkinNoteListener) {
        listeners.addListener(checkinNoteListener);
    }

    public void removeCheckinNoteChangedListener(final CheckinNoteListener checkinNoteListener) {
        listeners.removeListener(checkinNoteListener);
    }

    public CheckinNote getCheckinNote() {
        return checkinNote;
    }

    public void setCheckinNote(final CheckinNote checkinNote) {
        this.checkinNote = (checkinNote != null) ? checkinNote : new CheckinNote();

        dirty = true;
        updateControls();
    }

    /**
     * Merges the values of a checkin note into this one. Used when unshelving
     * and preserving an existing checkin note.
     *
     * @param checkinNote
     */
    public void mergeCheckinNote(final CheckinNote newCheckinNote) {
        if (newCheckinNote == null || newCheckinNote.getValues() == null) {
            return;
        }

        final Map notesMap = new HashMap();

        final CheckinNoteFieldValue[] oldValues = checkinNote.getValues();
        final CheckinNoteFieldValue[] newValues = newCheckinNote.getValues();

        for (int i = 0; i < oldValues.length; i++) {
            if (oldValues[i].getValue() != null && oldValues[i].getValue().length() > 0) {
                notesMap.put(CheckinNote.canonicalizeName(oldValues[i].getName()), oldValues[i]);
            }
        }

        for (int i = 0; i < newValues.length; i++) {
            if (newValues[i].getValue() != null && newValues[i].getValue().length() > 0) {
                notesMap.put(CheckinNote.canonicalizeName(newValues[i].getName()), newValues[i]);
            }
        }

        final CheckinNoteFieldValue[] mergedValues =
            (CheckinNoteFieldValue[]) notesMap.values().toArray(new CheckinNoteFieldValue[notesMap.values().size()]);
        checkinNote = new CheckinNote(mergedValues);

        dirty = true;
        updateControls();
    }

    public void setPendingCheckin(final TFSRepository repository, final PendingCheckin pendingCheckin) {
        if (options.getCheckinNotesReadOnly()) {
            return;
        }

        if (this.pendingCheckin != null) {
            this.pendingCheckin.getPendingChanges().removeAffectedTeamProjectsChangedListener(teamProjectsListener);
        }

        this.repository = repository;
        this.pendingCheckin = pendingCheckin;

        if (pendingCheckin != null) {
            pendingCheckin.getPendingChanges().addAffectedTeamProjectsChangedListener(teamProjectsListener);
            projects = pendingCheckin.getPendingChanges().getAffectedTeamProjectPaths();
            checkinNote = pendingCheckin.getCheckinNotes().getCheckinNotes();
        } else {
            projects = new String[0];
        }

        dirty = true;

        updateControls();
    }

    public void afterCheckin() {
        checkinNote = new CheckinNote();

        projects = pendingCheckin.getPendingChanges().getAffectedTeamProjectPaths();
        dirty = true;

        updateControls();
    }

    public void refresh() {
        dirty = true;
        updateControls();
    }

    private void updateControls() {
        if (dirty == false || updating == true) {
            return;
        }

        if (options.getCheckinNotesReadOnly() && (checkinNote == null || checkinNote.getValues().length == 0)) {
            dirty = false;
            updateEmptyControl();
            return;
        } else if (options.getCheckinNotesReadOnly()) {
            dirty = false;
            updateReadOnlyControl();
            return;
        } else if (options.getCheckinNotesHistoric()) {
            /* Set updating to true to suppress modify events */
            dirty = false;
            updating = true;
            fieldControls = updateHistoricControl();
            updating = false;
            return;
        } else if (projects == null || projects.length == 0) {
            dirty = false;
            updateEmptyControl();
            return;
        } else if (repository == null || !ConnectionHelper.isConnected(repository.getConnection())) {
            dirty = false;
            updateEmptyControl();
            return;
        }

        updateEmptyControl();

        updating = true;
        fieldControls = new CheckinNoteFieldControl[0];

        updateBusyControl();

        /*
         * Set dirty now so that we can detect if we need to (again) requery.
         * Another thread may set us dirty.
         */
        dirty = false;

        /*
         * Run a background job that queries the check-in notes for the
         * currently selected projects, and build the note fields on the UI
         * thread.
         */
        final Display display = getDisplay();

        final TFSRepository repository = this.repository;
        final String[] projects = this.projects;

        final Job updateJob = new Job(BUSY_MESSAGE) {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                try {
                    if (projects.length == 1) {
                        log.info(MessageFormat.format("Querying checkin notes for project {0}", projects[0])); //$NON-NLS-1$
                    } else {
                        log.info("Querying checkin notes for team projects"); //$NON-NLS-1$
                    }

                    final SortedSet definitionSet =
                        repository.getVersionControlClient().queryCheckinNoteFieldDefinitionsForServerPaths(projects);
                    final CheckinNoteFieldDefinition[] definitions =
                        (CheckinNoteFieldDefinition[]) definitionSet.toArray(
                            new CheckinNoteFieldDefinition[definitionSet.size()]);

                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (definitions == null || definitions.length == 0) {
                                    updateEmptyControl();
                                } else {
                                    fieldControls = updateFieldControl(definitions);
                                }

                                updateCheckinNote();
                            } finally {
                                updating = false;

                                if (dirty == true) {
                                    updateControls();
                                }
                            }
                        }
                    });
                } catch (final Exception e) {
                    return new Status(
                        IStatus.ERROR,
                        TFSCommonUIClientPlugin.PLUGIN_ID,
                        0,
                        Messages.getString("NotesCheckinControl.CouldNotQueryNotes"), //$NON-NLS-1$
                        e);
                }

                return Status.OK_STATUS;
            }
        };

        updateJob.schedule();
    }

    private void updateEmptyControl() {
        final Composite emptyControl = new Composite(rootComposite, SWT.NONE);

        final GridLayout emptyLayout = new GridLayout(1, false);
        emptyLayout.marginWidth = getHorizontalMargin();
        emptyLayout.marginHeight = getVerticalMargin();
        emptyLayout.horizontalSpacing = getHorizontalSpacing();
        emptyLayout.verticalSpacing = getVerticalSpacing();
        emptyControl.setLayout(emptyLayout);

        final Label noneLabel = new Label(emptyControl, SWT.NONE);

        if (options.getCheckinNotesReadOnly() && (checkinNote == null || checkinNote.getValues().length == 0)) {
            if (options.isForShelveset()) {
                noneLabel.setText(NO_SAVED_NOTES_SHELVESET_MESSAGE);
            } else {
                noneLabel.setText(NO_SAVED_NOTES_MESSAGE);
            }
        } else if (repository == null) {
            noneLabel.setText(NO_REPOSITORY_MESSAGE);
        } else {
            noneLabel.setText(NO_NOTES_MESSAGE);
        }

        GridDataBuilder.newInstance().hFill().vFill().applyTo(noneLabel);

        setRootCompositeContent(emptyControl);
    }

    private void updateBusyControl() {
        final Composite busyControl = new Composite(rootComposite, SWT.NONE);

        final GridLayout busyLayout = new GridLayout(1, false);
        busyLayout.marginWidth = getHorizontalMargin();
        busyLayout.marginHeight = getVerticalMargin();
        busyLayout.horizontalSpacing = getHorizontalSpacing();
        busyLayout.verticalSpacing = getVerticalSpacing();
        busyControl.setLayout(busyLayout);

        final Label busyLabel = new Label(busyControl, SWT.NONE);
        busyLabel.setText(BUSY_MESSAGE + Messages.getString("NotesCheckinControl.BusyLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().vFill().applyTo(busyLabel);

        setRootCompositeContent(busyControl);
    }

    private void updateReadOnlyControl() {
        final Composite noteListControl = new Composite(rootComposite, SWT.NONE);

        final GridLayout noteListLayout = new GridLayout(1, false);
        noteListLayout.marginWidth = getHorizontalMargin();
        noteListLayout.marginHeight = getVerticalMargin();
        noteListLayout.horizontalSpacing = getHorizontalSpacing();
        noteListLayout.verticalSpacing = getVerticalSpacing();
        noteListControl.setLayout(noteListLayout);

        final CheckinNoteFieldValue[] values = checkinNote.getValues();

        for (int i = 0; i < values.length; i++) {
            final Composite noteComposite = new Composite(noteListControl, SWT.NONE);

            final GridLayout noteCompositeLayout = new GridLayout(1, false);
            noteCompositeLayout.marginWidth = 0;
            noteCompositeLayout.marginHeight = 0;
            noteCompositeLayout.horizontalSpacing = 0;
            noteCompositeLayout.verticalSpacing = 0;
            noteComposite.setLayout(noteCompositeLayout);

            GridDataBuilder.newInstance().hGrab().hFill().applyTo(noteComposite);

            final String messageFormat = Messages.getString("NotesCheckinControl.NameLabelTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, values[i].getName());

            final Label nameLabel = new Label(noteComposite, SWT.NONE);
            nameLabel.setText(message);
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(nameLabel);

            final Text valueText = new Text(noteComposite, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.READ_ONLY);
            valueText.setText(values[i].getValue());
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(valueText);
        }

        setRootCompositeContent(noteListControl);
        rootComposite.setMinHeight(noteListControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
    }

    private CheckinNoteFieldControl[] updateHistoricControl() {
        final Composite noteListControl = new Composite(rootComposite, SWT.NONE);

        final GridLayout noteListLayout = new GridLayout(1, false);
        noteListLayout.marginWidth = getHorizontalMargin();
        noteListLayout.marginHeight = getVerticalMargin();
        noteListLayout.horizontalSpacing = getHorizontalSpacing();
        noteListLayout.verticalSpacing = getVerticalSpacing();
        noteListControl.setLayout(noteListLayout);

        final CheckinNoteFieldValue[] currentValues = checkinNote.getValues();

        fieldControls = new CheckinNoteFieldControl[currentValues.length];

        for (int i = 0; i < currentValues.length; i++) {
            fieldControls[i] = new CheckinNoteFieldControl(noteListControl, SWT.MULTI | SWT.WRAP);
            fieldControls[i].setName(currentValues[i].getName());
            fieldControls[i].setText(currentValues[i].getValue());

            GridDataBuilder.newInstance().hFill().hGrab().applyTo(fieldControls[i]);
        }

        setRootCompositeContent(noteListControl);
        rootComposite.setMinHeight(noteListControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);

        return fieldControls;
    }

    private CheckinNoteFieldControl[] updateFieldControl(final CheckinNoteFieldDefinition[] definitions) {
        final Composite noteListControl = new Composite(rootComposite, SWT.NONE);

        final GridLayout noteListLayout = new GridLayout(1, false);
        noteListLayout.marginWidth = getHorizontalMargin();
        noteListLayout.marginHeight = getVerticalMargin();
        noteListLayout.horizontalSpacing = getHorizontalSpacing();
        noteListLayout.verticalSpacing = getVerticalSpacing();
        noteListControl.setLayout(noteListLayout);

        fieldControls = new CheckinNoteFieldControl[definitions.length];

        final CheckinNoteFieldValue[] currentValues = checkinNote.getValues();

        for (int i = 0; i < definitions.length; i++) {
            fieldControls[i] = new CheckinNoteFieldControl(noteListControl, SWT.MULTI | SWT.WRAP);
            fieldControls[i].setCheckinNoteFieldDefinition(definitions[i]);
            fieldControls[i].setAutomationID(definitions[i].getName());

            /* Restore previously entered data */
            for (int j = 0; j < currentValues.length; j++) {
                if (CheckinNote.canonicalizeName(currentValues[j].getName()).equals(
                    CheckinNote.canonicalizeName(definitions[i].getName()))) {
                    fieldControls[i].setText(currentValues[j].getValue());
                    break;
                }
            }

            GridDataBuilder.newInstance().hFill().hGrab().applyTo(fieldControls[i]);
        }

        setRootCompositeContent(noteListControl);
        rootComposite.setMinHeight(noteListControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);

        return fieldControls;
    }

    /**
     * Sets new content on {@link #rootComposite}, disposing of old content.
     */
    private void setRootCompositeContent(final Control newContent) {
        final Control oldContent = rootComposite.getContent();

        rootComposite.setContent(newContent);

        if (oldContent != null && !oldContent.isDisposed()) {
            oldContent.dispose();
        }
    }

    private class TeamProjectsListener implements AffectedTeamProjectsChangedListener {
        @Override
        public void onAffectedTeamProjectsChanged(final AffectedTeamProjectsChangedEvent e) {
            final Runnable updater = new Runnable() {
                @Override
                public void run() {
                    final String[] oldProjects = projects;
                    projects = e.getNewTeamProjects();

                    dirty = !Arrays.equals(projects, oldProjects);

                    if (dirty) {
                        updateControls();
                    }
                }
            };

            /*
             * TODO: is this necessary? i think this event is always fired on
             * the ui thread.
             */
            if (getDisplay().getThread() != Thread.currentThread()) {
                getDisplay().asyncExec(updater);
            } else {
                updater.run();
            }
        }
    }

    /*
     * update the internal checkin note with the user's input
     */
    private void updateCheckinNote() {
        if (fieldControls == null || fieldControls.length == 0) {
            checkinNote = new CheckinNote();
        } else {
            final CheckinNoteFieldValue[] values = new CheckinNoteFieldValue[fieldControls.length];

            for (int i = 0; i < fieldControls.length; i++) {
                values[i] = fieldControls[i].getCheckinNoteFieldValue();
            }

            checkinNote = new CheckinNote(values);
        }

        ((CheckinNoteListener) listeners.getListener()).onCheckinNoteChanged(new CheckinNoteEvent(this));
    }

    private class CheckinNoteFieldControl extends BaseControl {
        private final int style;

        private CheckinNoteFieldDefinition definition;
        private String name;

        private final Label nameLabel;
        private final Text valueText;

        public CheckinNoteFieldControl(final Composite parent, final int style) {
            super(parent, SWT.NONE);

            this.style = style;

            final GridLayout layout = new GridLayout(1, false);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.horizontalSpacing = 0;
            layout.verticalSpacing = 0;
            setLayout(layout);

            nameLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().hFill().hGrab().applyTo(nameLabel);

            valueText = new Text(this, SWT.BORDER | SWT.WRAP | SWT.MULTI | (style & (SWT.READ_ONLY)));

            /*
             * MSFT displays "<Required>" in empty required fields. Do this
             * until the control gets focus.
             */
            valueText.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(final FocusEvent e) {
                    if (definition != null && definition.isRequired() && valueText.getData("empty") != null) //$NON-NLS-1$
                    {
                        valueText.setData("empty", null); //$NON-NLS-1$
                        valueText.setText(""); //$NON-NLS-1$
                        valueText.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
                    }
                }

                @Override
                public void focusLost(final FocusEvent e) {
                    if (definition != null && definition.isRequired() && valueText.getText().trim().length() == 0) {
                        valueText.setData("empty", Boolean.TRUE); //$NON-NLS-1$
                        valueText.setText(REQUIRED_MESSAGE);
                        valueText.setForeground(getDisplay().getSystemColor(REQUIRED_EMPTY_COLOR));
                    }
                }
            });

            /*
             * Allow tab keys to traverse to the next fields.
             */
            valueText.addTraverseListener(new TraverseListener() {
                @Override
                public void keyTraversed(final TraverseEvent e) {
                    if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                        e.doit = true;
                    }
                }
            });

            /*
             * We want to relayout the parent on modify events so that we can
             * dynamically grow the text box.
             */
            valueText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    if (rootComposite.getContent() != null && !rootComposite.getContent().isDisposed()) {
                        ((Composite) rootComposite.getContent()).layout(true);
                        rootComposite.setMinHeight(rootComposite.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
                        rootComposite.layout(true);
                    }

                    if (!updating) {
                        updateCheckinNote();
                    }
                }
            });

            GridDataBuilder.newInstance().hFill().hGrab().applyTo(valueText);
        }

        public void setName(final String name) {
            this.name = name;

            final String messageFormat = Messages.getString("NotesCheckinControl.NameLabelTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, name);
            nameLabel.setText(message);

        }

        public void setAutomationID(final String automationId) {
            AutomationIDHelper.setWidgetID(valueText, automationId);
        }

        public void setCheckinNoteFieldDefinition(final CheckinNoteFieldDefinition definition) {
            Check.notNull(definition, "definition"); //$NON-NLS-1$

            this.definition = definition;

            final String messageFormat = Messages.getString("NotesCheckinControl.NameLabelTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, definition.getName());
            nameLabel.setText(message);

            if (definition.isRequired() && (style & SWT.READ_ONLY) == 0) {
                valueText.setBackground(getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
                valueText.setForeground(getDisplay().getSystemColor(REQUIRED_EMPTY_COLOR));
                valueText.setData("empty", Boolean.TRUE); //$NON-NLS-1$
                valueText.setText(REQUIRED_MESSAGE);
            }
        }

        public void setText(final String text) {
            Check.notNull(text, "text"); //$NON-NLS-1$

            if (definition != null && definition.isRequired() && text.trim().length() == 0) {
                valueText.setData("empty", Boolean.TRUE); //$NON-NLS-1$
                valueText.setForeground(getDisplay().getSystemColor(REQUIRED_EMPTY_COLOR));
                valueText.setText(REQUIRED_MESSAGE);
            } else if (definition != null && definition.isRequired()) {
                valueText.setData("empty", null); //$NON-NLS-1$
                valueText.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
                valueText.setText(text);
            } else {
                valueText.setData("empty", Boolean.FALSE); //$NON-NLS-1$
                valueText.setText(text);
            }
        }

        public String getText() {
            if (valueText.getData("empty") != null) //$NON-NLS-1$
            {
                final Boolean empty = (Boolean) valueText.getData("empty"); //$NON-NLS-1$

                if (Boolean.TRUE.equals(empty)) {
                    return ""; //$NON-NLS-1$
                }
            }

            return valueText.getText().trim();
        }

        public CheckinNoteFieldValue getCheckinNoteFieldValue() {
            if (definition != null) {
                return new CheckinNoteFieldValue(definition.getName(), getText());
            } else if (name != null) {
                return new CheckinNoteFieldValue(name, getText());
            } else {
                throw new NullPointerException(
                    Messages.getString("NotesCheckinControl.NoteFieldNotConfiguredWithName")); //$NON-NLS-1$
            }
        }
    }

    public interface CheckinNoteListener {
        public void onCheckinNoteChanged(CheckinNoteEvent e);
    }

    public class CheckinNoteEvent {
        private final NotesCheckinControl control;

        public CheckinNoteEvent(final NotesCheckinControl notesCheckinControl) {
            control = notesCheckinControl;
        }

        public NotesCheckinControl getControl() {
            return control;
        }
    }
}
