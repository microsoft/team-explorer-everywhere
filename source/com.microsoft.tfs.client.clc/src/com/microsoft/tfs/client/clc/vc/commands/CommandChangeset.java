// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.options.OptionComment;
import com.microsoft.tfs.client.clc.vc.options.OptionLatest;
import com.microsoft.tfs.client.clc.vc.options.OptionNotes;
import com.microsoft.tfs.client.clc.vc.printers.ChangesetPrinter;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.util.Check;

public final class CommandChangeset extends Command {
    /**
     * The default long format for the current locale used for display.
     */
    private final DateFormat DEFAULT_DATE_FORMAT = SimpleDateFormat.getDateTimeInstance();

    public CommandChangeset() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        if (findOptionType(OptionLatest.class) != null) {
            if (getFreeArguments().length > 0) {
                throw new InvalidOptionException(
                    Messages.getString("CommandChangeset.ChangesetNumberCannotBeSpecifiedWithLatest")); //$NON-NLS-1$
            }
        } else if (getFreeArguments().length != 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandChangeset.SpecifyOneChangeset")); //$NON-NLS-1$
        }

        /*
         * We don't need a workspace for this command, so ignore the failure.
         */
        final TFSTeamProjectCollection connection = createConnection(true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * This is the object we'll query, and optionally modify and save to the
         * server.
         */
        Changeset changeset = null;

        /*
         * Query the correct changeset (latest or given number).
         */
        if (findOptionType(OptionLatest.class) != null) {
            changeset = client.getChangeset(client.getLatestChangesetID());
        } else {
            int changesetNumber = 0;

            try {
                changesetNumber = Integer.parseInt(getFreeArguments()[0]);
            } catch (final NumberFormatException e) {
                changesetNumber = -1;
            }

            if (changesetNumber < 1) {
                final String messageFormat =
                    Messages.getString("CommandChangeset.NotAValidChangesetNumberSpecifyToMaxFormat"); //$NON-NLS-1$
                final String message =
                    MessageFormat.format(messageFormat, getFreeArguments()[0], Integer.toString(Changeset.MAX));
                throw new InvalidFreeArgumentException(message);
            }

            changeset = client.getChangeset(changesetNumber);
        }

        /*
         * Perform any modifications.
         */
        boolean modified = false;

        Option o = null;

        if ((o = findOptionType(OptionComment.class)) != null) {
            final String value = ((OptionComment) o).getValue();

            if (value != null && value.length() > 0) {
                changeset.setComment(value);
                modified = true;
            }
        }

        if ((o = findOptionType(OptionNotes.class)) != null) {
            final CheckinNote notes = ((OptionNotes) o).getNotes();

            /*
             * Put the new notes into a map. Compare keys (note names)
             * case-insensitive.
             */
            final Map newNoteNameToACheckinNoteFieldValueMap = new java.util.TreeMap(String.CASE_INSENSITIVE_ORDER);

            final CheckinNoteFieldValue[] newNoteValues = notes.getValues();
            for (int i = 0; i < newNoteValues.length; i++) {
                final CheckinNoteFieldValue newNoteValue = newNoteValues[i];
                Check.notNull(newNoteValue, "newNoteValue"); //$NON-NLS-1$

                newNoteNameToACheckinNoteFieldValueMap.put(newNoteValue.getName(), newNoteValue);
            }

            /*
             * If the new set of notes contains a value for an old note, use the
             * new value and in the new notes list and remove the value from the
             * map. Otherwise, use the new value.
             */
            final List newNoteList = new ArrayList();
            if (changeset.getCheckinNote() != null) {
                final CheckinNoteFieldValue[] existingNoteValues = changeset.getCheckinNote().getValues();
                for (int i = 0; i < existingNoteValues.length; i++) {
                    final CheckinNoteFieldValue existingNoteValue = existingNoteValues[i];
                    Check.notNull(existingNoteValue, "existingNoteValue"); //$NON-NLS-1$

                    if (newNoteNameToACheckinNoteFieldValueMap.containsKey(existingNoteValue.getName())) {
                        newNoteList.add(newNoteNameToACheckinNoteFieldValueMap.get(existingNoteValue.getName()));
                        newNoteNameToACheckinNoteFieldValueMap.remove(existingNoteValue.getName());
                    } else {
                        newNoteList.add(existingNoteValue);
                    }
                }
            }

            /*
             * Any notes remaining in the map were new notes that do not
             * correspond to a note field for the changeset.
             */
            for (final Iterator i = newNoteNameToACheckinNoteFieldValueMap.values().iterator(); i.hasNext();) {
                final CheckinNoteFieldValue invalidNoteValue = (CheckinNoteFieldValue) i.next();

                final String messageFormat =
                    Messages.getString("CommandChangeset.NoteFieldNotSupportedForChangesetFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    invalidNoteValue.getName(),
                    Integer.toString(changeset.getChangesetID()));

                getDisplay().printErrorLine(message);
            }

            if (newNoteNameToACheckinNoteFieldValueMap.values().size() > 0) {
                setExitCode(ExitCode.PARTIAL_SUCCESS);
            }

            if (newNoteList.size() > 0) {
                changeset.setCheckinNote(
                    new CheckinNote(
                        (CheckinNoteFieldValue[]) newNoteList.toArray(new CheckinNoteFieldValue[newNoteList.size()])));
                modified = true;
            }
        }

        if (modified) {
            client.updateChangeset(changeset);
        }

        displayChangesetInfo(changeset, connection.getWorkItemClient());

        if (modified) {
            final String messageFormat = Messages.getString("CommandChangeset.ChangesetHasBeenModifiedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(changeset.getChangesetID()));
            getDisplay().printLine(message);
        }
    }

    private void displayChangesetInfo(final Changeset changeset, final WorkItemClient workItemClient) {
        Check.notNull(changeset, "changeset"); //$NON-NLS-1$
        Check.notNull(workItemClient, "workItemClient"); //$NON-NLS-1$

        ChangesetPrinter.printDetailedChangesets(new Changeset[] {
            changeset
        }, DEFAULT_DATE_FORMAT, getDisplay(), workItemClient);
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionLatest.class,
            OptionComment.class,
            OptionNotes.class,
        }, "[changenumber]"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandChangeset.HelpText1") //$NON-NLS-1$
        };
    }
}
