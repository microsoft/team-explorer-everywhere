// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;

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
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.options.OptionAuthor;
import com.microsoft.tfs.client.clc.vc.options.OptionCheckin;
import com.microsoft.tfs.client.clc.vc.options.OptionComment;
import com.microsoft.tfs.client.clc.vc.options.OptionLock;
import com.microsoft.tfs.client.clc.vc.options.OptionNoGet;
import com.microsoft.tfs.client.clc.vc.options.OptionNotes;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.events.CheckinEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.exceptions.InputValidationException;
import com.microsoft.tfs.core.pendingcheckin.AffectedTeamProjects;
import com.microsoft.tfs.core.pendingcheckin.CheckinNoteFailure;
import com.microsoft.tfs.core.pendingcheckin.StandardPendingCheckinNotes;
import com.microsoft.tfs.util.Check;

public final class CommandBranch extends Command {
    public CommandBranch() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        if (getFreeArguments().length != 2) {
            final String messageFormat = Messages.getString("CommandBranch.BranchRequiresTwoPathsFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());
            throw new InvalidFreeArgumentException(message);
        }

        Option o = null;
        boolean noGet = false;
        boolean recursive = false;
        boolean checkin = false;

        VersionSpec[] optionVersions = null;
        LockLevel lockLevel = LockLevel.UNCHANGED;

        if ((o = findOptionType(OptionNoGet.class)) != null) {
            noGet = true;
        }

        if ((o = findOptionType(OptionRecursive.class)) != null) {
            recursive = true;
        }

        if ((o = findOptionType(OptionVersion.class)) != null) {
            optionVersions = ((OptionVersion) o).getParsedVersionSpecs();

            if (optionVersions == null || optionVersions.length != 1) {
                final String messageFormat = Messages.getString("CommandBranch.OnlyOneVersionMayBeSuppliedFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, o.getMatchedAlias());
                throw new InvalidOptionValueException(message);
            }
        }

        if ((o = findOptionType(OptionLock.class)) != null) {
            lockLevel = ((OptionLock) o).getValueAsLockLevel();
        }

        if ((o = findOptionType(OptionCheckin.class)) != null) {
            checkin = true;
        }
        /*
         * Target item is simply the second free argument.
         */
        final String targetItem = ItemPath.canonicalize(getFreeArguments()[1]);

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);
        final Workspace workspace = realizeCachedWorkspace(determineCachedWorkspace(), client);

        /*
         * Determine which source item and version to branch from.
         */
        String sourceItem = null;
        VersionSpec sourceVersion = null;
        VersionSpec[] versionSpecs = null;
        try {
            /*
             * Check the free argument for a version spec, permitting ranges so
             * we can give a better error below.
             */

            final VersionedFileSpec versionedFileSpec =
                VersionedFileSpec.parse(getFreeArguments()[0], connection.getAuthorizedTFSUser().toString(), false);

            versionSpecs = versionedFileSpec.getVersions();
            Check.notNull(versionSpecs, "versionSpecs"); //$NON-NLS-1$

            sourceItem = versionedFileSpec.getItem();
            Check.notNull(sourceItem, "sourceItem"); //$NON-NLS-1$
        } catch (final InputValidationException e) {
            throw new CLCException(e.getMessage());
        }

        if (versionSpecs.length > 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandBranch.VersionRangesNotSupported")); //$NON-NLS-1$
        } else if (versionSpecs.length == 1) {
            if (optionVersions != null) {
                throw new InvalidOptionException(Messages.getString("CommandBranch.VersionOptionNotPermitted")); //$NON-NLS-1$
            }

            // Use the version in the spec.
            sourceVersion = versionSpecs[0];
        } else {
            /*
             * No version spec was parsed. If the version option was supplied,
             * use it, otherwise use the workspace spec appropriate for the free
             * argument.
             */
            if (optionVersions != null) {
                sourceVersion = optionVersions[0];
            } else {
                // See what kind of path they passed and use a workspace version
                // spec or a latest version spec.
                sourceVersion = getVersionSpecForPath(
                    getFreeArguments()[0],
                    workspace.getName(),
                    workspace.getOwnerName(),
                    workspace.getOwnerDisplayName());
            }
        }

        Check.notNull(sourceVersion, "sourceVersion"); //$NON-NLS-1$

        GetOptions options = GetOptions.NONE;
        if (noGet == true) {
            options = options.combine(GetOptions.PREVIEW);
        }

        if (lockLevel != LockLevel.UNCHANGED && checkin) {
            throw new InvalidOptionException(Messages.getString("CommandBranch.LockOptionCannotBeUsedWithCheckin")); //$NON-NLS-1$
        }

        if (checkin) {
            String comment = null;
            if ((o = findOptionType(OptionComment.class)) != null) {
                comment = ((OptionComment) o).getValue();
            }

            String author = null;
            if ((o = findOptionType(OptionAuthor.class)) != null) {
                author = ((OptionAuthor) o).getValue();
            }

            /*
             * Create branch only works with server paths.
             */
            if (ServerPath.isServerPath(sourceItem) == false || ServerPath.isServerPath(targetItem) == false) {
                throw new InvalidFreeArgumentException(
                    Messages.getString("CommandBranch.SourceAndTargetMustBeServerPathsWithCheckinOption")); //$NON-NLS-1$
            }

            // Evaluate checkin notes.

            CheckinNote checkinNotes = null;
            if ((o = findOptionType(OptionNotes.class)) != null) {
                checkinNotes = ((OptionNotes) o).getNotes();
            } else {
                checkinNotes = new CheckinNote(new CheckinNoteFieldValue[0]);
            }

            final AffectedTeamProjects affectedTeamProjects = new AffectedTeamProjects(new String[] {
                ServerPath.getTeamProject(targetItem)
            });

            final StandardPendingCheckinNotes evaluator =
                new StandardPendingCheckinNotes(checkinNotes, client, affectedTeamProjects);

            final CheckinNoteFailure[] noteFailures = evaluator.evaluate();

            if (!displayCheckinNoteFailures(noteFailures)) {
                final int changeset = client.createBranch(
                    sourceItem,
                    targetItem,
                    sourceVersion,
                    author,
                    comment,
                    checkinNotes,
                    null,
                    null);

                /*
                 * createBranch() doesn't cause an actual checkin event, so we
                 * fake one.
                 */
                workspace.getClient().getEventEngine().fireCheckin(
                    new CheckinEvent(
                        EventSource.newFromHere(),
                        workspace,
                        changeset,
                        new PendingChange[0],
                        new PendingChange[0]));
            }
        } else {
            if (findOptionType(OptionComment.class) != null) {
                throw new InvalidOptionException(Messages.getString("CommandBranch.CommentOnlyWithCheckin")); //$NON-NLS-1$
            }
            if (findOptionType(OptionAuthor.class) != null) {
                throw new InvalidOptionException(Messages.getString("CommandBranch.AuthorOnlyWithCheckin")); //$NON-NLS-1$
            }
            if (findOptionType(OptionNotes.class) != null) {
                throw new InvalidOptionException(Messages.getString("CommandBranch.NotesOnlyWithCheckin")); //$NON-NLS-1$
            }

            /*
             * As of TFS RC, pendBranch seems to interpret
             * RecursionType.OneLevel as RecursionType.Full when branching
             * directories, so we just use a bool on the command line and choose
             * between the two.
             */
            if (workspace.pendBranch(
                sourceItem,
                targetItem,
                sourceVersion,
                lockLevel,
                recursive ? RecursionType.FULL : RecursionType.NONE,
                options,
                PendChangesOptions.NONE) == 0) {
                setExitCode(ExitCode.FAILURE);
            }
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionVersion.class,
            OptionNoGet.class,
            OptionLock.class,
            OptionRecursive.class,
            OptionCheckin.class,
            OptionComment.class,
            OptionAuthor.class,
            OptionNotes.class
        }, "<oldItemSpec> <newLocalItem>"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandBranch.HelpText1") //$NON-NLS-1$
        };
    }
}
