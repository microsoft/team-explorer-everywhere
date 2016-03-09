// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.QualifiedItem;
import com.microsoft.tfs.client.clc.vc.options.OptionChangeset;
import com.microsoft.tfs.client.clc.vc.options.OptionKeepMergeHistory;
import com.microsoft.tfs.client.clc.vc.options.OptionLock;
import com.microsoft.tfs.client.clc.vc.options.OptionNoAutoResolve;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionSilent;
import com.microsoft.tfs.client.clc.vc.options.OptionToVersion;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.RollbackOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LabelVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;

public final class CommandRollback extends Command {
    public CommandRollback() {
        super();
    }

    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        // Parse arguments as qualified items to ensure validity

        final QualifiedItem[] qualifiedItems = parseQualifiedItems(null, true, 0);
        final String[] qualifiedItemPaths = new String[qualifiedItems.length];
        for (int i = 0; i < qualifiedItems.length; i++) {
            qualifiedItemPaths[i] = qualifiedItems[i].getPath();
        }

        // Connect to server and workspace

        final TFSTeamProjectCollection connection = createConnection(qualifiedItemPaths);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        client.getEventEngine().addConflictResolvedListener(this);

        final WorkspaceInfo wsInfo = determineCachedWorkspace(qualifiedItemPaths);
        final Workspace workspace = realizeCachedWorkspace(wsInfo, client);

        // Parse general options

        final boolean recursive = findOptionType(OptionRecursive.class) != null;
        LockLevel lockLevel = LockLevel.UNCHANGED;

        final Option o = findOptionType(OptionLock.class);
        if (o != null) {
            lockLevel = ((OptionLock) o).getValueAsLockLevel();
        }

        // Examine paths for versions

        VersionSpec[] itemSpecVersions = null;
        final String[] paths = getFreeArguments();

        // if the user provided a /version:
        final OptionVersion optionVersion = (OptionVersion) findOptionType(OptionVersion.class);
        if (optionVersion != null && paths.length > 0) {
            itemSpecVersions = optionVersion.getParsedVersionSpecs();

            // if the user specified more than one version spec, this is an
            // error
            if (itemSpecVersions.length > 1) {
                throw new CLCException(Messages.getString("CommandRollback.AmbiguousVersion")); //$NON-NLS-1$
            }
        }

        // Get our rollback options
        final RollbackOptions options = getRollbackOptions();
        final VersionSpec[] versions = getVersionRange(paths);

        // call to OM to perform the rollback.
        final GetStatus getStatus = workspace.rollback(
            ItemSpec.fromStrings(paths, recursive ? RecursionType.FULL : RecursionType.NONE),
            itemSpecVersions == null || itemSpecVersions.length == 0 ? null : itemSpecVersions[0],
            versions[0],
            versions[1],
            lockLevel,
            options,
            null);

        // if the server returned failures and no get ops, set exit code to
        // failure
        if (getStatus.getNumFailures() > 0 && getStatus.getNumOperations() == 0) {
            setExitCode(ExitCode.FAILURE);
        }

        if (getStatus.isNoActionNeeded()) {
            if (findOptionType(OptionSilent.class) == null) {
                getDisplay().printLine(Messages.getString("CommandRollback.NoChangesToRollback")); //$NON-NLS-1$
            }
        } else {
            displayMergeSummary(getStatus, true);

            // VS opens the conflict UI here if there were conflicts, but we
            // will have already printed them above (possibly again in the
            // summary).
        }
    }

    private RollbackOptions getRollbackOptions() {
        RollbackOptions options = RollbackOptions.NONE;

        if (findOptionType(OptionSilent.class) != null) {
            options = options.combine(RollbackOptions.SILENT);
        }

        if (findOptionType(OptionToVersion.class) != null) {
            options = options.combine(RollbackOptions.TO_VERSION);
        }

        if (findOptionType(OptionKeepMergeHistory.class) != null) {
            options = options.combine(RollbackOptions.KEEP_MERGE_HISTORY);
        }

        if (findOptionType(OptionNoAutoResolve.class) != null) {
            options = options.combine(RollbackOptions.NO_AUTO_RESOLVE);
        }

        return options;
    }

    /**
     * Extracts out the from, to ranges of the rollback
     */
    private VersionSpec[] getVersionRange(final String[] paths)
        throws InvalidOptionException,
            InvalidOptionValueException {
        // if the user specified both changeset and ToVersion this is an error
        reportBadOptionCombinationIfPresent(OptionChangeset.class, OptionToVersion.class);

        final OptionChangeset optionChangeset = (OptionChangeset) findOptionType(OptionChangeset.class);
        final OptionToVersion optionToVersion = (OptionToVersion) findOptionType(OptionToVersion.class);

        final VersionSpec[] versionRange = new VersionSpec[2];

        if (optionToVersion != null) {
            versionRange[0] = optionToVersion.getParsedVersionSpecs()[0];
            // end range is latest
            versionRange[1] = LatestVersionSpec.INSTANCE;
        } else if (optionChangeset != null) {
            // user specified changeset.
            final VersionSpec[] changesets = optionChangeset.getParsedVersionSpecs();

            // validate the version specs
            for (int i = 0; i < changesets.length; i++) {
                if (changesets[i] instanceof WorkspaceVersionSpec || changesets[i] instanceof LabelVersionSpec) {
                    throw new InvalidOptionValueException(MessageFormat.format(
                        //@formatter:off
                        Messages.getString("CommandRollback.CannotProvideWorkspaceOrLabelWithChangesetOptionFormat"), //$NON-NLS-1$
                        //@formatter:on
                        OptionsMap.getPreferredOptionPrefix(),
                        optionChangeset.getMatchedAlias()));
                }
            }

            // if the user provided just one changeset, set both from and to to
            // it
            if (changesets.length == 1) {
                versionRange[0] = changesets[0];
                versionRange[1] = changesets[0];
            } else if (changesets.length == 2) {
                versionRange[0] = changesets[0];
                versionRange[1] = changesets[1];
            }
        } else {
            throw new InvalidOptionException(Messages.getString("CommandRollback.RollbackNoVersionsSpecified")); //$NON-NLS-1$
        }

        return versionRange;
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[2];

        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class,
            OptionLock.class,
            OptionVersion.class,
            OptionKeepMergeHistory.class,
            OptionNoAutoResolve.class,
        }, "[<itemSpec>...]", new Class[] { //$NON-NLS-1$
            OptionChangeset.class
        });

        optionSets[1] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class,
            OptionLock.class,
            OptionVersion.class,
            OptionKeepMergeHistory.class,
            OptionNoAutoResolve.class,
        }, "<itemSpec>...", new Class[] { //$NON-NLS-1$
            OptionToVersion.class
        });

        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandRollback.HelpText1") //$NON-NLS-1$
        };
    }
}
