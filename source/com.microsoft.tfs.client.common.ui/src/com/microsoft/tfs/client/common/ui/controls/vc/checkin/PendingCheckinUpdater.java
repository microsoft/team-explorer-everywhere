// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;

import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.NotesCheckinControl.CheckinNoteEvent;
import com.microsoft.tfs.client.common.ui.controls.vc.checkin.NotesCheckinControl.CheckinNoteListener;
import com.microsoft.tfs.client.common.ui.framework.viewer.CheckboxEvent;
import com.microsoft.tfs.client.common.ui.framework.viewer.CheckboxListener;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementEvent;
import com.microsoft.tfs.client.common.ui.framework.viewer.ElementListener;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.pendingcheckin.PendingCheckin;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Implements all the event listener interfaces needed to handle the events from
 * {@link CheckinControl}'s subcontrols that would require a
 * {@link PendingCheckin} to be updated with new control data. For example, when
 * a user types text in the comment field in the source files subcontrol, an
 * instance of this class gets the event and updates a {@link PendingCheckin}.
 * </p>
 * <p>
 * An alternative to this design would be to have the controls directly
 * implement the {@link PendingCheckin} interface (and subcontrols would
 * implement sub-interfaces). One drawback to that alternative is thread-safety:
 * a {@link PendingCheckin} needs to be safe to use from any thread, and UI
 * controls aren't, so they need to marshall all their data when queried.
 * </p>
 * <p>
 * This class is meant to be used exclusively from the UI thread.
 * </p>
 *
 * @threadsafety thread-compatible
 */
class PendingCheckinUpdater {
    private final static Log log = LogFactory.getLog(PendingCheckinUpdater.class);

    private final PendingCheckin pendingCheckin;
    private final CheckinControl checkinControl;

    /**
     * Implements {@link CheckboxListener} and {@link ElementListener} so it can
     * be attached to the source files table and do the same thing for both type
     * of events.
     *
     * @threadsafety thread-compatible
     */
    private class SourceFilesCheckboxAndElementListener implements CheckboxListener, ElementListener {
        @Override
        public void checkedElementsChanged(final CheckboxEvent event) {
            log.trace("updating pending checkin checked source control items"); //$NON-NLS-1$

            updatePendingCheckin();
        }

        @Override
        public void elementsChanged(final ElementEvent event) {
            log.trace("updating pending checkin all source control items"); //$NON-NLS-1$

            updatePendingCheckin();
        }

        private void updatePendingCheckin() {
            /*
             * Update all pending changes first, because updating selected
             * pending changes fires the event and we want the up-to-date info
             * in the pending checkin.
             */
            final PendingChange[] allPendingChanges = ChangeItem.getPendingChanges(
                checkinControl.getSourceFilesSubControl().getChangesTable().getChangeItems());

            pendingCheckin.getPendingChanges().setAllPendingChanges(allPendingChanges);

            final PendingChange[] checkedPendingChanges = ChangeItem.getPendingChanges(
                checkinControl.getSourceFilesSubControl().getChangesTable().getCheckedChangeItems());

            pendingCheckin.getPendingChanges().setCheckedPendingChanges(checkedPendingChanges);
        }
    };

    /**
     * Implements {@link CheckboxListener} and {@link ElementListener} so it can
     * be attached to the work item table and do the same thing for both type of
     * events.
     *
     * @threadsafety thread-compatible
     */
    private class WorkItemCheckboxAndElementListener implements CheckboxListener, ElementListener {
        @Override
        public void checkedElementsChanged(final CheckboxEvent event) {
            updatePendingCheckin();
        }

        @Override
        public void elementsChanged(final ElementEvent event) {
            updatePendingCheckin();
        }

        private void updatePendingCheckin() {
            log.trace("updating pending checkin checked work items"); //$NON-NLS-1$

            pendingCheckin.getWorkItems().setCheckedWorkItems(
                checkinControl.getWorkItemSubControl().getWorkItemTable().getCheckedWorkItems());
        }
    };

    private class CheckinNoteChangedListener implements CheckinNoteListener {
        @Override
        public void onCheckinNoteChanged(final CheckinNoteEvent e) {
            log.trace("updating pending checkin notes"); //$NON-NLS-1$

            pendingCheckin.getCheckinNotes().setCheckinNotes(checkinControl.getNotesSubControl().getCheckinNote());
        }
    }

    /*
     * Named instances of listeners so they can be hooked and unhooked easily.
     */

    private final ModifyListener sourceFilesCommentModifyListener = new ModifyListener() {
        @Override
        public void modifyText(final ModifyEvent e) {
            log.trace("updating pending checkin comment text"); //$NON-NLS-1$

            pendingCheckin.getPendingChanges().setComment(checkinControl.getSourceFilesSubControl().getComment());
        }
    };

    private final SourceFilesCheckboxAndElementListener sourceFilesCheckboxAndElementListener =
        new SourceFilesCheckboxAndElementListener();

    private final WorkItemCheckboxAndElementListener workItemsCheckboxAndElementListener =
        new WorkItemCheckboxAndElementListener();

    private final CheckinNoteChangedListener checkinNoteListener = new CheckinNoteChangedListener();

    /**
     * Creates a {@link PendingCheckinUpdater} which updates the given
     * {@link PendingCheckin} when events from the given {@link CheckinControl}
     * happen.
     *
     * @param pendingCheckin
     *        the {@link PendingCheckin} to update (must not be
     *        <code>null</code>)
     * @param checkinControl
     *        the {@link CheckinControl} to watch (must not be <code>null</code>
     *        )
     */
    public PendingCheckinUpdater(final PendingCheckin pendingCheckin, final CheckinControl checkinControl) {
        Check.notNull(pendingCheckin, "pendingCheckin"); //$NON-NLS-1$
        Check.notNull(checkinControl, "checkinControl"); //$NON-NLS-1$

        this.pendingCheckin = pendingCheckin;
        this.checkinControl = checkinControl;

        this.checkinControl.getSourceFilesSubControl().getCommentText().addModifyListener(
            sourceFilesCommentModifyListener);

        /*
         * A quirk of SWT is that the check box events are only fired for
         * certain user interface actions (mouse clicks and such), and they are
         * not fired when a list/table element is removed. So this class must
         * hook up both checkbox listeners, and element listeners, and always
         * update both the "all" and "checked" pending changes in the pending
         * checkin when either event is fired.
         */
        this.checkinControl.getSourceFilesSubControl().getChangesTable().addCheckboxListener(
            sourceFilesCheckboxAndElementListener);

        this.checkinControl.getSourceFilesSubControl().getChangesTable().addElementListener(
            sourceFilesCheckboxAndElementListener);

        this.checkinControl.getWorkItemSubControl().getWorkItemTable().addElementListener(
            workItemsCheckboxAndElementListener);

        this.checkinControl.getWorkItemSubControl().getWorkItemTable().addCheckboxListener(
            workItemsCheckboxAndElementListener);

        this.checkinControl.getNotesSubControl().addCheckinNoteChangedListener(checkinNoteListener);
    }

    /**
     * Unhooks this object as a listener from all the event sources it
     * originally subscribed to. The instance will not update the
     * {@link PendingCheckin} after this method is called.
     */
    public void unhookListeners() {
        if (!checkinControl.getSourceFilesSubControl().getCommentText().isDisposed()) {
            checkinControl.getSourceFilesSubControl().getCommentText().removeModifyListener(
                sourceFilesCommentModifyListener);
        }

        if (!checkinControl.getSourceFilesSubControl().getChangesTable().isDisposed()) {
            checkinControl.getSourceFilesSubControl().getChangesTable().removeCheckboxListener(
                sourceFilesCheckboxAndElementListener);

            checkinControl.getSourceFilesSubControl().getChangesTable().removeElementListener(
                sourceFilesCheckboxAndElementListener);
        }

        if (!checkinControl.getWorkItemSubControl().getWorkItemTable().isDisposed()) {
            checkinControl.getWorkItemSubControl().getWorkItemTable().removeElementListener(
                workItemsCheckboxAndElementListener);

            checkinControl.getWorkItemSubControl().getWorkItemTable().removeCheckboxListener(
                workItemsCheckboxAndElementListener);
        }

        checkinControl.getNotesSubControl().removeCheckinNoteChangedListener(checkinNoteListener);
    }
}
