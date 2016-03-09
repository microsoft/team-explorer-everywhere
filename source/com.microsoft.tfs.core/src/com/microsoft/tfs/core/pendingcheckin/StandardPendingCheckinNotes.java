// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.pendingcheckin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;
import com.microsoft.tfs.core.pendingcheckin.events.NotesChangedEvent;
import com.microsoft.tfs.core.pendingcheckin.events.NotesChangedListener;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * <p>
 * Standard implementation of {@link PendingCheckinNotes}.
 * </p>
 * <p>
 * The {@link AffectedTeamProjects} object given during construction is
 * permitted to change during the life time of a
 * {@link StandardPendingCheckinNotes} object, and each call to
 * {@link #evaluate()} will use the most recent data from the
 * {@link AffectedTeamProjects} object.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class StandardPendingCheckinNotes implements PendingCheckinNotes {
    private final SingleListenerFacade notesChangedListeners = new SingleListenerFacade(NotesChangedListener.class);

    private final AffectedTeamProjects affectedTeamProjects;

    private CheckinNote checkinNotes;
    private CheckinNoteFieldDefinition[] fieldDefinitions;
    private final VersionControlClient client;

    /**
     * Constructs a standard {@link PendingCheckinNotes} object.
     *
     * @param checkinNotes
     *        the notes set for the pending checkin (must not be
     *        <code>null</code>)
     * @param client
     *        the version control client in use (must not be <code>null</code>)
     * @param affectedTeamProjects
     *        team projects affected by this checkin (must not be
     *        <code>null</code>) This object is not modified by this
     *        implementation, but it is allowed to change while this
     *        {@link PendingCheckinNotes} object is alive.
     */
    public StandardPendingCheckinNotes(
        final CheckinNote checkinNotes,
        final VersionControlClient client,
        final AffectedTeamProjects affectedTeamProjects) {
        Check.notNull(checkinNotes, "checkinNotes"); //$NON-NLS-1$
        Check.notNull(client, "client"); //$NON-NLS-1$
        Check.notNull(affectedTeamProjects, "affectedTeamProjects"); //$NON-NLS-1$

        this.checkinNotes = checkinNotes;
        this.client = client;
        this.affectedTeamProjects = affectedTeamProjects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized CheckinNoteFailure[] evaluate() {
        /*
         * Keep at most one failure per checkin note (keyed on name).
         */
        final Map noteNameToNoteFailureMap = new HashMap();

        /*
         * Query the definitions from the server and store them.
         */
        final SortedSet definitions =
            client.queryCheckinNoteFieldDefinitionsForServerPaths(affectedTeamProjects.getTeamProjectPaths());

        if (definitions == null) {
            fieldDefinitions = new CheckinNoteFieldDefinition[0];
        } else {
            final CheckinNoteFieldDefinition[] array = new CheckinNoteFieldDefinition[definitions.size()];
            int index = 0;
            for (final Iterator i = definitions.iterator(); i.hasNext();) {
                array[index++] = (CheckinNoteFieldDefinition) i.next();

            }
            fieldDefinitions = array;
        }

        for (int i = 0; i < fieldDefinitions.length; i++) {
            final CheckinNoteFieldDefinition definition = fieldDefinitions[i];

            if (definition.isRequired()) {
                boolean found = false;

                /*
                 * Look at the user's notes and make sure he supplied a value
                 * for this definition.
                 */
                if (checkinNotes != null && checkinNotes.getValues() != null) {
                    for (int j = 0; j < checkinNotes.getValues().length; j++) {
                        final CheckinNoteFieldValue thisNoteValue = checkinNotes.getValues()[j];

                        if (thisNoteValue.getValue() != null
                            && thisNoteValue.getValue().length() > 0
                            && thisNoteValue.getName().trim().equalsIgnoreCase(definition.getName().trim())) {
                            found = true;
                            break;
                        }
                    }
                }

                /*
                 * Add a failure if there was not already one for this note.
                 */
                if (found == false && noteNameToNoteFailureMap.containsKey(definition.getName()) == false) {
                    noteNameToNoteFailureMap.put(
                        definition.getName(),
                        new CheckinNoteFailure(
                            definition,
                            //@formatter:off
                            Messages.getString("StandardPendingCheckinNotes.AValueMustBeSpecifiedForTheCheckInNoteFormat"))); //$NON-NLS-1$
                            //@formatter:on
                }
            }
        }

        return (CheckinNoteFailure[]) noteNameToNoteFailureMap.values().toArray(
            new CheckinNoteFailure[noteNameToNoteFailureMap.values().size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized CheckinNote getCheckinNotes() {
        return checkinNotes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void setCheckinNotes(final CheckinNote checkinNotes) {
        Check.notNull(checkinNotes, "checkinNotes"); //$NON-NLS-1$
        this.checkinNotes = checkinNotes;

        ((NotesChangedListener) notesChangedListeners.getListener()).onNotesChanged(
            new NotesChangedEvent(EventSource.newFromHere(), checkinNotes));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized CheckinNoteFieldDefinition[] getFieldDefinitions() {
        return fieldDefinitions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNotesChangedListener(final NotesChangedListener listener) {
        notesChangedListeners.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotesChangedListener(final NotesChangedListener listener) {
        notesChangedListeners.removeListener(listener);
    }
}