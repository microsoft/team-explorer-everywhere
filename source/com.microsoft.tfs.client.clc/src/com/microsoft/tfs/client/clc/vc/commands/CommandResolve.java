// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.io.File;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.microsoft.alm.client.utils.StringUtil;
import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.EnvironmentVariables;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.externaltools.CLCTools;
import com.microsoft.tfs.client.clc.options.Option;
import com.microsoft.tfs.client.clc.vc.options.OptionAuto;
import com.microsoft.tfs.client.clc.vc.options.OptionConvertToType;
import com.microsoft.tfs.client.clc.vc.options.OptionForce;
import com.microsoft.tfs.client.clc.vc.options.OptionNewName;
import com.microsoft.tfs.client.clc.vc.options.OptionOverrideType;
import com.microsoft.tfs.client.clc.vc.options.OptionPreview;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.printers.ConflictPrinter;
import com.microsoft.tfs.console.display.Display;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.ResolutionOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedListener;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.path.ItemPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ConflictType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Resolution;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.externaltools.ExternalToolAssociation;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolException;
import com.microsoft.tfs.core.util.FileEncoding;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.process.ProcessFinishedHandler;
import com.microsoft.tfs.util.process.ProcessRunner;

public final class CommandResolve extends Command implements ConflictResolvedListener {
    private Resolution resolution = Resolution.NONE;
    private boolean useExternalTool = false;

    public CommandResolve() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        final WorkspaceInfo cachedWorkspace = determineCachedWorkspace();

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);
        client.getEventEngine().addConflictResolvedListener(this);

        parseResolutionOption();

        final boolean preview = (findOptionType(OptionPreview.class) != null) || resolution == Resolution.NONE;

        if (resolution != Resolution.ACCEPT_MERGE && findOptionType(OptionForce.class) != null) {
            throw new InvalidOptionException(
                Messages.getString("CommandResolve.ForceOnlyValidWithAutoMergeOrExternal")); //$NON-NLS-1$
        }

        Option o = null;
        final ResolutionOptions options = new ResolutionOptions();

        /*
         * Detect if they want to override the encodings with a specific type
         * (which is incompatible with conversion).
         */
        if ((o = findOptionType(OptionOverrideType.class)) != null) {
            reportBadOptionCombinationIfPresent(OptionOverrideType.class, OptionConvertToType.class);

            options.setEncodingStrategy(
                ResolutionOptions.EncodingStrategy.OVERRIDE_EXPLICIT,
                ((OptionOverrideType) o).getValueAsEncoding());

            options.setAcceptMergeEncoding(((OptionOverrideType) o).getValueAsEncoding());
        }

        /*
         * Detect if they want to convert to another encoding.
         */
        if ((o = findOptionType(OptionConvertToType.class)) != null) {
            options.setEncodingStrategy(
                ResolutionOptions.EncodingStrategy.CONVERT_EXPLICIT,
                ((OptionConvertToType) o).getValueAsEncoding());

            options.setAcceptMergeEncoding(((OptionConvertToType) o).getValueAsEncoding());
        }

        options.setUseInternalEngine(useExternalTool == false);
        options.setAcceptMergeWithConflicts(findOptionType(OptionForce.class) != null);

        ExternalToolset mergeToolset = null;
        if (useExternalTool) {
            /*
             * Build a toolset with just one tool: the one the environment
             * variable describes.
             */
            mergeToolset = new ExternalToolset();
            mergeToolset.addAssociation(new ExternalToolAssociation(new String[] {
                ExternalToolset.WILDCARD_EXTENSION
            }, CLCTools.getMergeTool()));
        }

        /*
         * Calculate the path (if the user wants to override).
         */
        String newName = null;

        if ((o = findOptionType(OptionNewName.class)) != null) {
            newName = ((OptionNewName) o).getValue();
        }

        /*
         * Validate the new name against the resolution type and wildcards.
         */
        if (newName != null) {
            if (resolution != Resolution.ACCEPT_YOURS_RENAME_THEIRS && resolution != Resolution.ACCEPT_MERGE) {
                throw new InvalidOptionException(
                    //@formatter:off
                    Messages.getString("CommandResolve.NewnameOnlyValidWithAutoMergeOrExternalOrKeepYoursRenameTheirs")); //$NON-NLS-1$
                    //@formatter:on
            }

            if (getFreeArguments().length > 1 || LocalPath.isWildcard(newName)) {
                throw new InvalidFreeArgumentException(Messages.getString("CommandResolve.OnlyOneConflictWithNewname")); //$NON-NLS-1$
            }

            options.setNewPath(ItemPath.canonicalize(newName));
        } else if (resolution == Resolution.ACCEPT_YOURS_RENAME_THEIRS) {
            throw new InvalidFreeArgumentException(
                Messages.getString("CommandResolve.NewNameRequiredWithKeepYoursRenameTheirs")); //$NON-NLS-1$
        }

        /*
         * Query conflicts for the paths in the free arguments. If no free
         * arguments available, send null to the web service to query the whole
         * workspace.
         */

        String[] resolvePaths = null;
        if (getFreeArguments().length > 0) {
            resolvePaths = new String[getFreeArguments().length];
            for (int i = 0; i < resolvePaths.length; i++) {
                resolvePaths[i] = ItemPath.canonicalize(getFreeArguments()[i]);
            }
        }

        final Workspace workspace = realizeCachedWorkspace(cachedWorkspace, client);

        Conflict[] conflicts = workspace.queryConflicts(resolvePaths, findOptionType(OptionRecursive.class) != null);

        if (conflicts.length == 0) {
            getDisplay().printLine(Messages.getString("CommandResolve.ThereAreNoConflictsToResolve")); //$NON-NLS-1$
            return;
        }

        if (newName != null && conflicts.length != 1) {
            throw new InvalidOptionException(Messages.getString("CommandResolve.OnlyOneConflictWhenNewname")); //$NON-NLS-1$
        }

        if (preview) {
            for (int i = 0; i < conflicts.length; i++) {
                ConflictPrinter.printConflict(conflicts[i], getDisplay(), false);
            }

            setExitCode(ExitCode.PARTIAL_SUCCESS);
            return;
        }

        /*
         * Check symbolic links
         */
        boolean containsSymlink = false;
        for (final Conflict conflict : conflicts) {
            if (!StringUtil.isNullOrEmpty(conflict.getTargetLocalItem())
                && FileSystemUtils.getInstance().getAttributes(conflict.getTargetLocalItem()).isSymbolicLink()) {
                containsSymlink = true;
                break;
            }
        }

        if (containsSymlink
            && !(resolution.equals(Resolution.ACCEPT_THEIRS)
                || resolution.equals(Resolution.ACCEPT_YOURS)
                || resolution.equals(Resolution.OVERWRITE_LOCAL))) {
            getDisplay().printLine(Messages.getString("CommandResolve.SymlinksOnlyAcceptYoursOrTheirs")); //$NON-NLS-1$
            return;
        }

        /*
         * Resolve the conflicts.
         */

        /*
         * Collect the reported and resolved conflicts.
         */
        final Set<Integer> reportedConflictIDs = new HashSet<Integer>();
        final Set<Integer> resolvedConflictIDs = new HashSet<Integer>();

        /*
         * Contains server paths to directories that are conflicts, that were
         * already resolved in the resolution loop, so conflicts underneath
         * these directories can be skipped on that iteration.
         */
        final List<String> resolvedFolders = new ArrayList<String>();

        // Number of conflicts resolved per iteration in the loop.
        int numResolvedConflicts = 0;

        /*
         * We may have to do multiple passes (each outside loop iteration is a
         * pass) if the conflicts are not all resolved in the first pass.
         */
        boolean tryAgain = false;
        do {
            numResolvedConflicts = 0;
            resolvedFolders.clear();

            Conflict[] conflictsResolvedThisIteration = new Conflict[0];

            /*
             * Each pass, iterate over the conflicts starting with the specified
             * resolution and options.
             */
            for (final Conflict conflict : conflicts) {
                conflict.setResolution(resolution);
                conflict.setResolutionOptions(options);

                // There's no point in trying local conflicts without overwrite
                // being specified.
                if (resolution == Resolution.OVERWRITE_LOCAL && conflict.getType() != ConflictType.LOCAL) {
                    continue;
                }

                // If the conflict has already been processed then skip it.
                if (resolvedConflictIDs.contains(conflict.getConflictID())) {
                    continue;
                }

                /**
                 * If this conflict is a child of a folder such that the folder
                 * was both resolved in this pass (sorted top down) and the
                 * folder had a rename or undelete and the resolution is accept
                 * merge and we're likely to do a three-way merge on the child,
                 * we want to wait until we get the updated conflict with the
                 * updated disk location before resolving the conflict.
                 */
                if (conflict.getResolution() == Resolution.ACCEPT_MERGE
                    && conflict.getYourItemType() == ItemType.FILE
                    && conflict.getYourServerItemSource() != null
                    && conflict.canMergeContent()
                    && conflict.getYourEncoding() != FileEncoding.BINARY
                    && resolvedFolders.size() > 0) {
                    boolean affected = false;
                    for (final String directory : resolvedFolders) {
                        if (ServerPath.isChild(directory, conflict.getYourServerItemSource())) {
                            affected = true;
                            break;
                        }
                    }

                    if (affected) {
                        continue;
                    }
                }

                final ProcessFinishedHandler finishedHandler = new MergeToolFinishedHandler(getDisplay());

                /*
                 * Resolve!
                 */
                try {
                    final AtomicReference<Conflict[]> conflictsHolder = new AtomicReference<Conflict[]>();

                    workspace.resolveConflict(
                        conflict,
                        conflictsHolder,
                        null,
                        mergeToolset,
                        finishedHandler,
                        System.out,
                        System.err);

                    conflictsResolvedThisIteration = conflictsHolder.get();
                } catch (final ExternalToolException e) {
                    cleanupAfterFailedMerge(conflict);

                    /*
                     * Rethrow this, because it's fatal.
                     */
                    throw e;
                } catch (final VersionControlException e) {
                    cleanupAfterFailedMerge(conflict);

                    getDisplay().printErrorLine(e.getMessage());
                }

                /*
                 * Add to the lists so we can continue on other conflicts.
                 */
                if (conflict.isResolved()) {
                    /**
                     * Keep track of whether we have successfully resolved
                     * folders with accept merge where it was either rename or
                     * undelete. We don't want to work on the files under of
                     * those folders, because resolving the parent will change
                     * the on-disk location of the files.
                     */
                    if (conflict.getYourItemType() == ItemType.FOLDER
                        && conflict.getYourServerItemSource() != null
                        && conflict.getResolution() == Resolution.ACCEPT_MERGE
                        && (conflict.getBaseChangeType().contains(ChangeType.UNDELETE)
                            || conflict.getBaseChangeType().contains(ChangeType.RENAME))) {
                        resolvedFolders.add(conflict.getYourServerItemSource());
                    }

                    numResolvedConflicts++;
                    for (final Conflict c : conflictsResolvedThisIteration) {
                        resolvedConflictIDs.add(c.getConflictID());
                    }
                } else {
                    if (numResolvedConflicts == 0 && reportedConflictIDs.contains(conflict.getConflictID()) == false) {
                        /*
                         * If we couldn't resolve the conflict then say why.
                         */
                        displayConflictResolveError(conflict);

                        reportedConflictIDs.add(conflict.getConflictID());
                    }
                }
            }

            tryAgain = numResolvedConflicts != 0 && numResolvedConflicts != conflicts.length;

            if (tryAgain) {
                conflicts = workspace.queryConflicts(resolvePaths, findOptionType(OptionRecursive.class) != null);
            }
        } while (tryAgain && conflicts.length != 0);

        final boolean haveUnresolvedLeft = conflicts.length != 0 && conflicts.length != numResolvedConflicts;

        /*
         * Print any conflicts that were not resolved.
         */
        if (haveUnresolvedLeft) {
            for (int i = 0; i < conflicts.length; i++) {
                final Conflict conflict = conflicts[i];

                if (conflict.isResolved() == false && reportedConflictIDs.contains(conflict.getConflictID()) == false) {
                    displayConflictResolveError(conflict);
                }
            }
        }

        if (haveUnresolvedLeft) {
            setExitCode(ExitCode.PARTIAL_SUCCESS);
        }
    }

    private void displayConflictResolveError(final Conflict conflict) {
        final String displayPath = ConflictPrinter.printConflict(conflict, getDisplay(), false);
        getDisplay().printErrorLine(
            MessageFormat.format(Messages.getString("CommandResolve.ConflictWasNotResolvedFormat"), displayPath)); //$NON-NLS-1$
    }

    private void cleanupAfterFailedMerge(final Conflict conflict) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$

        if (conflict.isResolved() == false && conflict.getMergedFileName() != null) {
            new File(conflict.getMergedFileName()).delete();
        }
    }

    private String getResolvedAsStringForResolution(final Resolution resolution) {
        if (resolution == Resolution.ACCEPT_MERGE) {
            if (useExternalTool) {
                return OptionAuto.EXTERNAL_TOOL;
            }

            return OptionAuto.AUTO_MERGE;
        } else if (resolution == Resolution.ACCEPT_YOURS) {
            return OptionAuto.KEEP_YOURS;
        } else if (resolution == Resolution.ACCEPT_THEIRS) {
            return OptionAuto.TAKE_THEIRS;
        } else if (resolution == Resolution.DELETE_CONFLICT) {
            return OptionAuto.DELETE_CONFLICT;
        } else if (resolution == Resolution.ACCEPT_YOURS_RENAME_THEIRS) {
            return OptionAuto.KEEP_YOURS_RENAME_THEIRS;
        } else if (resolution == Resolution.OVERWRITE_LOCAL) {
            return OptionAuto.OVERWRITE_LOCAL;
        }

        log.error(MessageFormat.format("Unknown resolution type {0}", resolution)); //$NON-NLS-1$
        return ""; //$NON-NLS-1$
    }

    private void parseResolutionOption() {
        final OptionAuto o = (OptionAuto) findOptionType(OptionAuto.class);

        resolution = Resolution.NONE;
        useExternalTool = false;

        if (o != null) {
            if (o.getValue().equalsIgnoreCase(OptionAuto.TAKE_THEIRS)) {
                resolution = Resolution.ACCEPT_THEIRS;
            } else if (o.getValue().equalsIgnoreCase(OptionAuto.KEEP_YOURS)) {
                resolution = Resolution.ACCEPT_YOURS;
            } else if (o.getValue().equalsIgnoreCase(OptionAuto.AUTO_MERGE)) {
                resolution = Resolution.ACCEPT_MERGE;
            } else if (o.getValue().equalsIgnoreCase(OptionAuto.EXTERNAL_TOOL)) {
                /*
                 * External tools still use the resolution of AcceptMerge,
                 * because the code path in the merge engine is the same, but
                 * run() will set external tool details too.
                 */
                resolution = Resolution.ACCEPT_MERGE;
                useExternalTool = true;
            } else if (o.getValue().equalsIgnoreCase(OptionAuto.KEEP_YOURS_RENAME_THEIRS)) {
                resolution = Resolution.ACCEPT_YOURS_RENAME_THEIRS;
            } else if (o.getValue().equalsIgnoreCase(OptionAuto.DELETE_CONFLICT)) {
                resolution = Resolution.DELETE_CONFLICT;
            } else if (o.getValue().equalsIgnoreCase(OptionAuto.OVERWRITE_LOCAL)) {
                resolution = Resolution.OVERWRITE_LOCAL;
            }
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[2];

        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionAuto.class,
            OptionPreview.class,
            OptionOverrideType.class,
            OptionRecursive.class,
            OptionNewName.class
        }, "<itemSpec>"); //$NON-NLS-1$

        optionSets[1] = new AcceptedOptionSet(new Class[] {
            OptionAuto.class,
            OptionPreview.class,
            OptionConvertToType.class,
            OptionRecursive.class,
            OptionNewName.class
        }, "<itemSpec>"); //$NON-NLS-1$

        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandResolve.HelpText1"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText2") //$NON-NLS-1$
                + EnvironmentVariables.EXTERNAL_MERGE_COMMAND
                + Messages.getString("CommandResolve.HelpText3") //$NON-NLS-1$
                + EnvironmentVariables.EXTERNAL_MERGE_COMMAND
                + Messages.getString("CommandResolve.HelpText4"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText5"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText6"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText7"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText8"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText9"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText10"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText11"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText12"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText13"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText14"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText15"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText16"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText17"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText18"), //$NON-NLS-1$
            Messages.getString("CommandResolve.HelpText19"), //$NON-NLS-1$
        };
    }

    @Override
    public void onConflictResolved(final ConflictResolvedEvent e) {
        String path = null;

        if (e.getConflict().getType() == ConflictType.MERGE) {
            /*
             * If they didn't accept the change, we should use the existing name
             * and not the name it would have had. The only exceptions are
             * branches and undeletes, which you couldn't have had prior to the
             * merge. Otherwise (including unresolved conflicts), we want the
             * new name.
             */
            if ((e.getConflict().getResolution() == Resolution.ACCEPT_YOURS
                || e.getConflict().getResolution() == Resolution.ACCEPT_YOURS_RENAME_THEIRS)
                && e.getConflict().getSourceLocalItem() != null
                && e.getConflict().getSourceLocalItem().length() > 0) {
                path = e.getConflict().getSourceLocalItem();
            } else {
                path = e.getConflict().getTargetLocalItem();
            }
        } else {
            /*
             * Unfortunately, non-merge and merge conflicts don't follow the
             * same rules.
             */
            if (e.getConflict().getTargetLocalItem() != null) {
                path = e.getConflict().getTargetLocalItem();
            } else if (e.getConflict().getSourceLocalItem() != null) {
                path = e.getConflict().getSourceLocalItem();
            }
        }

        // Make sure we have something to print.
        if (path == null || path.length() == 0) {
            path = e.getConflict().getTheirServerItem();
        }

        if (e.getConflict().getContentMergeSummary() != null) {
            getDisplay().printLine(""); //$NON-NLS-1$
            getDisplay().printLine(
                ConflictPrinter.getContentMergeSummaryLine(path, e.getConflict().getContentMergeSummary()));
        }

        getDisplay().printLine(MessageFormat.format(
            Messages.getString("CommandResolve.ResolvedPathAsFormat"), //$NON-NLS-1$
            path,
            getResolvedAsStringForResolution(e.getConflict().getResolution())));
    }

    /**
     * Handles an external merge tool failing by printing error messages. The
     * merge engine continues to handle the failure, the CLC just wants to print
     * the failure information as soon as it happens.
     */
    private static class MergeToolFinishedHandler implements ProcessFinishedHandler {
        private final Display display;

        public MergeToolFinishedHandler(final Display display) {
            this.display = display;
        }

        @Override
        public void processInterrupted(final ProcessRunner runner) {
            log.warn("External merge tool interrupted", runner.getExecutionError()); //$NON-NLS-1$

            display.printErrorLine(Messages.getString("CommandResolve.ExternalMergeToolInterrupted")); //$NON-NLS-1$
        }

        @Override
        public void processExecFailed(final ProcessRunner runner) {
            log.warn("Couldn't start external merge tool", runner.getExecutionError()); //$NON-NLS-1$

            display.printErrorLine(
                MessageFormat.format(
                    Messages.getString("CommandResolve.ExternalMergeToolExecFailedFormat"), //$NON-NLS-1$
                    EnvironmentVariables.EXTERNAL_MERGE_COMMAND,
                    runner.getExecutionError().getLocalizedMessage()));
        }

        @Override
        public void processCompleted(final ProcessRunner runner) {
            // Nothing to print here, resolve will complete normally.
        }
    };
}
