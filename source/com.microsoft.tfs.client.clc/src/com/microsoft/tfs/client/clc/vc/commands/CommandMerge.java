// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.CannotFindWorkspaceException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.options.shared.OptionFormat;
import com.microsoft.tfs.client.clc.vc.options.OptionBaseless;
import com.microsoft.tfs.client.clc.vc.options.OptionCandidate;
import com.microsoft.tfs.client.clc.vc.options.OptionDiscard;
import com.microsoft.tfs.client.clc.vc.options.OptionForce;
import com.microsoft.tfs.client.clc.vc.options.OptionLock;
import com.microsoft.tfs.client.clc.vc.options.OptionNoAutoResolve;
import com.microsoft.tfs.client.clc.vc.options.OptionNoImplicitBaseless;
import com.microsoft.tfs.client.clc.vc.options.OptionNoSummary;
import com.microsoft.tfs.client.clc.vc.options.OptionPreview;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionSilent;
import com.microsoft.tfs.client.clc.vc.options.OptionVersion;
import com.microsoft.tfs.client.clc.vc.printers.MergeCandidatePrinter;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.MergeFlags;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.MergeCandidate;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.VersionedFileSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.util.Check;

public final class CommandMerge extends Command {
    /**
     * The default long format for the current locale used in brief and detailed
     * displays.
     */
    private final DateFormat DEFAULT_DATE_FORMAT = SimpleDateFormat.getDateTimeInstance();

    public CommandMerge() {
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
            throw new InvalidFreeArgumentException(Messages.getString("CommandMerge.TwoItemsAreRequired")); //$NON-NLS-1$
        }

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);
        client.getEventEngine().addMergingListener(this);
        client.getEventEngine().addConflictResolvedListener(this);

        final VersionSpec[] sourceVersions = getSourceVersionRange();

        if (findOptionType(OptionCandidate.class) != null) {
            doMergeCandidateQuery(client);
        } else {
            reportBadOptionCombinationIfPresent(OptionForce.class, OptionDiscard.class);

            final MergeFlags mergeFlags = getMergeFlags();
            final String source = getSource();
            final String target = getTarget();

            final WorkspaceInfo cachedWorkspace = determineCachedWorkspace();
            final Workspace workspace = realizeCachedWorkspace(cachedWorkspace, client);

            Option o = null;

            LockLevel lockLevel = LockLevel.UNCHANGED;
            if ((o = findOptionType(OptionLock.class)) != null) {
                lockLevel = ((OptionLock) o).getValueAsLockLevel();
            }

            RecursionType recursionType = RecursionType.NONE;
            if (findOptionType(OptionRecursive.class) != null) {
                recursionType = RecursionType.FULL;
            }

            String format = OptionFormat.BRIEF;
            if ((o = findOptionType(OptionFormat.class)) != null) {
                format = ((OptionFormat) o).getValue();
            }

            final GetStatus status = workspace.merge(
                source,
                target,
                sourceVersions[0],
                sourceVersions[1],
                lockLevel,
                recursionType,
                mergeFlags);

            if (status.isNoActionNeeded()) {
                if (findOptionType(OptionSilent.class) == null) {
                    getDisplay().printLine(Messages.getString("CommandMerge.ThereAreNoChangesToMerge")); //$NON-NLS-1$
                }
            } else {
                if (findOptionType(OptionNoSummary.class) == null) {
                    displayMergeSummary(status, OptionFormat.DETAILED.equals(format));
                }

                // VS opens the conflict UI here if there were conflicts, but we
                // will have already printed them above (possibly again in the
                // summary).
            }
        }
    }

    private void doMergeCandidateQuery(final VersionControlClient client)
        throws InvalidOptionException,
            CannotFindWorkspaceException,
            InvalidOptionValueException {
        Check.notNull(client, "client"); //$NON-NLS-1$

        // We don't take these options
        reportBadOptionCombinationIfPresent(OptionForce.class, OptionCandidate.class);
        reportBadOptionCombinationIfPresent(OptionDiscard.class, OptionCandidate.class);
        reportBadOptionCombinationIfPresent(OptionLock.class, OptionCandidate.class);
        reportBadOptionCombinationIfPresent(OptionVersion.class, OptionCandidate.class);
        reportBadOptionCombinationIfPresent(OptionNoSummary.class, OptionCandidate.class);

        final String source = getSource();
        final String target = getTarget();

        RecursionType recursionType = RecursionType.NONE;
        if (findOptionType(OptionRecursive.class) != null) {
            recursionType = RecursionType.FULL;
        }

        MergeFlags mergeFlags = MergeFlags.NONE;
        if (findOptionType(OptionBaseless.class) != null) {
            mergeFlags = mergeFlags.combine(MergeFlags.BASELESS);
        }

        final MergeCandidate[] candidates = client.getMergeCandidates(source, target, recursionType, mergeFlags);

        if (candidates.length == 0) {
            if (findOptionType(OptionSilent.class) == null) {
                getDisplay().printLine(Messages.getString("CommandMerge.ThereAreNoChangesToMerge")); //$NON-NLS-1$
            }
        } else {
            MergeCandidatePrinter.printBriefMergeCandidates(candidates, DEFAULT_DATE_FORMAT, getDisplay());
        }
    }

    private MergeFlags getMergeFlags() {
        MergeFlags flags = MergeFlags.NONE;

        if (findOptionType(OptionForce.class) != null) {
            flags = flags.combine(MergeFlags.FORCE_MERGE);
        }
        if (findOptionType(OptionBaseless.class) != null) {
            flags = flags.combine(MergeFlags.BASELESS);
        }
        if (findOptionType(OptionPreview.class) != null) {
            flags = flags.combine(MergeFlags.NO_MERGE);
        }
        if (findOptionType(OptionDiscard.class) != null) {
            flags = flags.combine(MergeFlags.ALWAYS_ACCEPT_MINE);
        }
        if (findOptionType(OptionSilent.class) != null) {
            flags = flags.combine(MergeFlags.SILENT);
        }
        if (findOptionType(OptionNoImplicitBaseless.class) != null) {
            flags = flags.combine(MergeFlags.NO_IMPLICIT_BASELESS);
        }
        if (findOptionType(OptionNoAutoResolve.class) != null) {
            flags = flags.combine(MergeFlags.NO_AUTO_RESOLVE);
        }

        return flags;
    }

    private String getSource() {
        final VersionedFileSpec spec =
            VersionedFileSpec.parse(getFreeArguments()[0], VersionControlConstants.AUTHENTICATED_USER, true);

        String ret = spec.getItem();

        if (ServerPath.isServerPath(ret) == false) {
            ret = LocalPath.canonicalize(ret);
        }

        return ret;
    }

    private String getTarget() {
        String ret = getFreeArguments()[1];

        if (ServerPath.isServerPath(ret) == false) {
            ret = LocalPath.canonicalize(ret);
        }

        return ret;
    }

    private VersionSpec[] getSourceVersionRange() throws InvalidOptionException {
        /*
         * Check the first argument for a version or range.
         */
        final VersionedFileSpec vfs =
            VersionedFileSpec.parse(getFreeArguments()[0], VersionControlConstants.AUTHENTICATED_USER, true);

        VersionSpec[] parsedVersions = vfs.getVersions();
        VersionSpec latest = null;

        /*
         * If we got versions in the argument, make sure the option was not also
         * used.
         */
        if (parsedVersions.length > 0) {
            if (findOptionType(OptionVersion.class) != null) {
                throw new InvalidOptionException(Messages.getString("CommandMerge.InformationAboutVersionIsAmbiguous")); //$NON-NLS-1$
            }
        } else {
            if (findOptionType(OptionForce.class) != null && findOptionType(OptionVersion.class) == null) {
                throw new InvalidOptionException(Messages.getString("CommandMerge.VersionOrRangeRequiredForForce")); //$NON-NLS-1$
            }

            latest = LatestVersionSpec.INSTANCE;

            Option o = null;
            if ((o = findOptionType(OptionVersion.class)) != null) {
                parsedVersions = ((OptionVersion) o).getParsedVersionSpecs();
            }
        }

        final VersionSpec[] versionsToMerge = new VersionSpec[2];

        /*
         * Make sure we got enough versions.
         */
        if (parsedVersions.length == 0) {
            versionsToMerge[0] = null;
            versionsToMerge[1] = latest;
        } else if (parsedVersions.length == 1) {
            versionsToMerge[0] = null;
            versionsToMerge[1] = parsedVersions[0];
        } else {
            versionsToMerge[0] = parsedVersions[0];
            versionsToMerge[1] = parsedVersions[1];
        }

        return versionsToMerge;
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class,
            OptionForce.class,
            OptionCandidate.class,
            OptionDiscard.class,
            OptionVersion.class,
            OptionLock.class,
            OptionPreview.class,
            OptionBaseless.class,
            OptionNoSummary.class,
            OptionNoImplicitBaseless.class,
            OptionFormat.class,
            OptionNoAutoResolve.class,
        }, "<source> <destination>"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandMerge.HelpText1") //$NON-NLS-1$
        };
    }
}
