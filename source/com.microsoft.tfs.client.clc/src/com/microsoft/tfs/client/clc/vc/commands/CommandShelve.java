// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.Calendar;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.shared.OptionCollection;
import com.microsoft.tfs.client.clc.options.shared.OptionDelete;
import com.microsoft.tfs.client.clc.vc.options.OptionComment;
import com.microsoft.tfs.client.clc.vc.options.OptionMove;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionReplace;
import com.microsoft.tfs.client.clc.vc.options.OptionSaved;
import com.microsoft.tfs.client.clc.vc.options.OptionValidate;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.ShelveException;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpecParseException;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.SavedCheckin;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.exceptions.InputValidationException;
import com.microsoft.tfs.util.Check;

public final class CommandShelve extends Command {
    public CommandShelve() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        /*
         * We have to major modes of operation: delete and shelve.
         */
        if (findOptionType(OptionDelete.class) != null) {
            deleteShelveset();
        } else {
            shelve();
        }
    }

    private void deleteShelveset() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        reportBadOptionCombinationIfPresent(OptionComment.class, OptionDelete.class);
        reportBadOptionCombinationIfPresent(OptionMove.class, OptionDelete.class);
        reportBadOptionCombinationIfPresent(OptionReplace.class, OptionDelete.class);
        reportBadOptionCombinationIfPresent(OptionRecursive.class, OptionDelete.class);
        reportBadOptionCombinationIfPresent(OptionValidate.class, OptionDelete.class);

        if (getFreeArguments().length != 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandShelve.ExactlyOneShelvesetForDeletion")); //$NON-NLS-1$
        }

        final String arg = getFreeArguments()[0];
        final WorkspaceSpec spec;
        try {
            spec = WorkspaceSpec.parse(arg, VersionControlConstants.AUTHENTICATED_USER);
        } catch (final WorkspaceSpecParseException e) {
            final String messageFormat = Messages.getString("CommandShelve.ShelvesetSpecCouldNotBeParsedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, arg, e.getLocalizedMessage());

            throw new InvalidFreeArgumentException(message);
        }

        /*
         * Pass an empty array for the free arguments to search for local paths
         * because none of the free arguments is a path.
         */
        final TFSTeamProjectCollection connection = createConnection(new String[0], true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        client.deleteShelveset(spec.getName(), spec.getOwner());
    }

    private void shelve()
        throws ArgumentException,
            InputValidationException,
            MalformedURLException,
            CLCException,
            LicenseException {
        final OptionCollection collectionOption = getCollectionOption();
        if (collectionOption != null) {
            final String messageFormat = Messages.getString("CommandShelve.OptionNotValidForOptionSetFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, collectionOption.getMatchedAlias());

            throw new InvalidOptionException(message);
        }

        final String[] freeArguments = getFreeArguments();

        /*
         * Microsoft's client doesn't allow validate with noprompt, because of
         * the requirement of the UI being present to validate the shelveset
         * (validation includes checking comments, check-in notes, and running
         * the check-in policies). Since we don't have a graphical UI, we can do
         * all this all the time.
         */

        if (freeArguments.length == 0) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandShelve.PleaseSpecifyAShelvesetName")); //$NON-NLS-1$
        }

        final String shelvesetArgument = freeArguments[0];

        // Save off the rest of the free arguments so we can ignore the
        // shelveset name.
        final String[] fileSpecs = new String[freeArguments.length - 1];
        for (int i = 1; i < freeArguments.length; i++) {
            fileSpecs[i - 1] = freeArguments[i];
        }

        /*
         * Determine the workspace from the file specs, if available, else use
         * normal default.
         */
        final WorkspaceInfo cachedWorkspace;
        if (fileSpecs.length == 0) {
            /*
             * Even though fileSpecs is empty, passing it makes sure
             * determineCachedWorkspace() doesn't use the shelveset name as a
             * path.
             */
            cachedWorkspace = determineCachedWorkspace(fileSpecs);
        } else {
            cachedWorkspace = findSingleCachedWorkspace(fileSpecs);
            Check.notNull(cachedWorkspace, "Workspace should have been found because there was at least one filespec."); //$NON-NLS-1$
        }

        /*
         * Parse out the shelveset name and options.
         */
        final WorkspaceSpec shelvesetSpec =
            WorkspaceSpec.parse(shelvesetArgument, VersionControlConstants.AUTHENTICATED_USER);

        final boolean move = findOptionType(OptionMove.class) != null;
        final boolean replace = findOptionType(OptionReplace.class) != null;
        final boolean recursive = findOptionType(OptionRecursive.class) != null;
        final boolean useSavedCheckinInfo = findOptionType(OptionSaved.class) != null;

        final TFSTeamProjectCollection connection = createConnection(fileSpecs, true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        final Workspace workspace = realizeCachedWorkspace(cachedWorkspace, client);

        final SavedCheckin checkinInfo = getCheckinInformation(workspace, useSavedCheckinInfo);

        /*
         * Create the shelveset with the changes specified by the user.
         */
        final Shelveset shelveset = new Shelveset(
            shelvesetSpec.getName(),
            shelvesetSpec.getOwner(),
            shelvesetSpec.getOwner(),
            checkinInfo.getComment(),
            checkinInfo.getPolicyOverrideComment(),
            checkinInfo.getCheckinNotes(),
            checkinInfo.getWorkItemsCheckedInfo(),
            Calendar.getInstance(),
            false,
            null);

        final PendingChange[] changesToShelve;
        if (fileSpecs.length > 0) {
            changesToShelve = getPendingChangesMatchingLocalPaths(workspace, recursive, fileSpecs);
        } else {
            final PendingSet set = workspace.getPendingChanges();
            if (set != null) {
                changesToShelve = set.getPendingChanges();
            } else {
                changesToShelve = null;
            }
        }

        if (changesToShelve == null || changesToShelve.length == 0) {
            getDisplay().printErrorLine(Messages.getString("CommandShelve.NoMatchingPendingChangesNothingShelved")); //$NON-NLS-1$
            setExitCode(ExitCode.FAILURE);
            return;
        }

        try {
            workspace.shelve(shelveset, changesToShelve, replace, move);

            final String messageFormat = Messages.getString("CommandShelve.ShelvesetCreatedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, shelvesetSpec.getName());

            getDisplay().printLine(""); //$NON-NLS-1$
            getDisplay().printLine(message);
            getDisplay().printLine(""); //$NON-NLS-1$
        } catch (final ShelveException e) {
            getDisplay().printErrorLine(Messages.getString("CommandShelve.NoChangesShelved")); //$NON-NLS-1$
            setExitCode(ExitCode.FAILURE);
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[3];

        // Create/replace a shelveset with all changes in the current workspace.
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionReplace.class,
            OptionComment.class,
            OptionValidate.class,
            OptionSaved.class,
        }, "<shelvesetName[;owner]>"); //$NON-NLS-1$

        // Create/replace a shelveset for the given file specs.
        optionSets[1] = new AcceptedOptionSet(new Class[] {
            OptionMove.class,
            OptionReplace.class,
            OptionComment.class,
            OptionRecursive.class,
            OptionValidate.class,
            OptionSaved.class,
        }, "<shelvesetName[;owner]> <fileSpec>..."); //$NON-NLS-1$

        // Delete a shelveset.
        optionSets[2] = new AcceptedOptionSet(new Class[] {
            OptionCollection.class,
            OptionValidate.class,
            OptionSaved.class,
        }, "<shelvesetName[;owner]>", new Class[] //$NON-NLS-1$
        {
            OptionDelete.class
        });
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandShelve.HelpText1") //$NON-NLS-1$
        };
    }

    /**
     * Builds a {@link SavedCheckin} from (optionally) the last saved info plus
     * the info specified in command line options.
     *
     * @param workspace
     *        the workspace to load the saved checkin from (must not be
     *        <code>null</code>)
     * @param useSaved
     *        if <code>true</code> the last saved information is loaded and
     *        command line options override those values, if <code>false</code>
     *        only command line info is used
     * @return the {@link SavedCheckin} with option data merged into it
     * @throws InvalidOptionValueException
     *         if one of the option values was invalid
     */
    private SavedCheckin getCheckinInformation(final Workspace workspace, final boolean useSaved)
        throws InvalidOptionValueException {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        SavedCheckin checkin = useSaved ? workspace.getLastSavedCheckin() : null;

        // The saved one could have been null
        if (checkin == null) {
            checkin = new SavedCheckin();
        }

        final OptionComment optionComment = (OptionComment) findOptionType(OptionComment.class);

        if (optionComment != null) {
            checkin.setComment(optionComment.getValue());
        }

        return checkin;
    }
}
