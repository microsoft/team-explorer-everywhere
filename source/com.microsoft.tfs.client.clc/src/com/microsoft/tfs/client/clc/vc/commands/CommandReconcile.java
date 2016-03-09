// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.shared.OptionBuildName;
import com.microsoft.tfs.client.clc.options.shared.OptionTeamProject;
import com.microsoft.tfs.client.clc.vc.CLCTaskMonitor;
import com.microsoft.tfs.client.clc.vc.options.OptionChangeset;
import com.microsoft.tfs.client.clc.vc.options.OptionForgetBuild;
import com.microsoft.tfs.client.clc.vc.options.OptionNoSummary;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.printers.BuildPrinter;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.BuildConstants;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildDetailSpec;
import com.microsoft.tfs.core.clients.build.IBuildQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.InformationNodeConverters;
import com.microsoft.tfs.core.clients.build.buildstatus.BuildStatusCache;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.InformationTypes;
import com.microsoft.tfs.core.clients.build.flags.QueryOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.GetStatus;
import com.microsoft.tfs.core.clients.versioncontrol.ReconcilePendingChangesStatus;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkspaceInfo;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

public final class CommandReconcile extends Command {
    private final DateFormat defaultFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

    private final TaskMonitor taskMonitor = new CLCTaskMonitor();

    public CommandReconcile() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {
        final boolean recursive = findOptionType(OptionRecursive.class) != null;

        final OptionBuildName optionBuildName = (OptionBuildName) findOptionType(OptionBuildName.class);
        final OptionChangeset optionChangeset = (OptionChangeset) findOptionType(OptionChangeset.class);
        final OptionForgetBuild optionForgetBuild = (OptionForgetBuild) findOptionType(OptionForgetBuild.class);

        if ((optionBuildName != null && optionChangeset != null)
            || (optionChangeset != null && optionForgetBuild != null)
            || (optionForgetBuild != null && optionBuildName != null)) {
            throw new InvalidOptionException(Messages.getString("CommandReconcile.OnlyOneOfBuildNameChangesetForget")); //$NON-NLS-1$
        }

        final TFSTeamProjectCollection connection = createConnection();
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        final WorkspaceInfo cw = determineCachedWorkspace(getFreeArguments());
        final Workspace workspace = realizeCachedWorkspace(cw, client);

        /*
         * Default to all team projects for displaying builds and resolving
         * builds to changesets.
         */
        String teamProject = BuildConstants.STAR;
        if (findOptionType(OptionTeamProject.class) != null) {
            teamProject = ((OptionTeamProject) findOptionType(OptionTeamProject.class)).getValue();
        }

        if (optionChangeset != null) {
            final VersionSpec[] versions = optionChangeset.getParsedVersionSpecs();
            if (versions.length != 1) {
                throw new InvalidOptionValueException(
                    Messages.getString("CommandReconcile.ExactlyOneChangesetMustBeSupplied")); //$NON-NLS-1$
            } else if (versions[0] instanceof ChangesetVersionSpec == false) {
                throw new InvalidOptionValueException(
                    Messages.getString("CommandReconcile.OnlyChangesetVersionSpecsAllowed")); //$NON-NLS-1$
            }

            reconcileChangeset(workspace, ((ChangesetVersionSpec) versions[0]).getChangeset(), recursive);
        } else if (optionBuildName != null) {
            reconcileBuildByName(workspace, optionBuildName.getValue(), teamProject, recursive);
        } else if (optionForgetBuild != null) {
            forgetBuildByName(connection, optionForgetBuild.getValue(), teamProject);
        } else {
            displayBriefWatchedBuilds(connection, teamProject);
        }

    }

    /**
     * Displays the watched builds in a table.
     *
     * @param connection
     *        the {@link TFSTeamProjectCollection} to use (must not be
     *        <code>null</code>)
     * @param teamProject
     *        the team project name (supports wildcards) by which to constrain
     *        the query (must not be <code>null</code> or empty)
     */
    private void displayBriefWatchedBuilds(final TFSTeamProjectCollection connection, final String teamProject) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNullOrEmpty(teamProject, "teamProject"); //$NON-NLS-1$

        /*
         * Load the watched builds list from disk.
         */
        final BuildStatusCache cache = BuildStatusCache.load(connection);
        final List<Integer> watchedBuildIDs = cache.getBuilds();

        if (watchedBuildIDs.size() == 0) {
            getDisplay().printLine(Messages.getString("CommandReconcile.NoWatchedCheckins")); //$NON-NLS-1$
            return;
        }

        /*
         * Load information about the watched builds via the build server.
         */

        final int[] queryIDs = new int[watchedBuildIDs.size()];
        for (int i = 0; i < watchedBuildIDs.size(); i++) {
            queryIDs[i] = watchedBuildIDs.get(i);
        }

        final IQueuedBuild[] queuedBuilds = connection.getBuildServer().getQueuedBuild(queryIDs, QueryOptions.NONE);

        /*
         * It's possible some of the watched IDs are no longer in the system
         * (deleted from the server after they got too old). Collect those and
         * remove them from the cache.
         */
        final List<Integer> missingBuildsIDs = new ArrayList<Integer>();
        for (int i = 0; i < queuedBuilds.length; i++) {
            if (queuedBuilds[i] == null || queuedBuilds[i].getID() == 0) {
                missingBuildsIDs.add(queryIDs[i]);
            }
        }

        /*
         * Remove from the watched build cache any build IDs which we didn't get
         * back in our query.
         */
        if (missingBuildsIDs.size() > 0) {
            for (int i = 0; i < missingBuildsIDs.size(); i++) {
                cache.removeBuild(missingBuildsIDs.get(i));
            }
            cache.save(connection);
        }

        Arrays.sort(queuedBuilds);

        /*
         * There may be some queued builds which have not been started yet (no
         * build detail, so can't print a row), so test for 0 printed.
         */
        if (BuildPrinter.printQueuedBuilds(
            queuedBuilds,
            connection.getBuildServer(),
            defaultFormat,
            getDisplay()) == 0) {
            getDisplay().printLine(Messages.getString("CommandReconcile.NoBuildsForWatchedCheckins")); //$NON-NLS-1$
        }
    }

    /**
     * Does the reconcile work for a single changeset ID using the free
     * arguments as filter paths.
     *
     * @param workspace
     *        the {@link Workspace} to reconcile (must not be <code>null</code>)
     * @param changesetID
     *        the changeset ID (must be > 0)
     * @param recursive
     *        <code>true</code> to match free arguments against pending changes
     *        recursively
     */
    private void reconcileChangeset(final Workspace workspace, final int changesetID, final boolean recursive) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$

        /*
         * Get the commited changeset info.
         */

        final Changeset changeset = workspace.getClient().getChangeset(changesetID);

        /*
         * Get the current pending changes which match the free arguments.
         */
        final PendingSet set;

        if (getFreeArguments().length == 0) {
            set = workspace.getPendingChanges();
        } else {
            // Canonicalize local paths
            final String[] paths = getFreeArguments();
            for (int i = 0; i < paths.length; i++) {
                if (ServerPath.isServerPath(paths[i]) == false) {
                    paths[i] = LocalPath.canonicalize(paths[i]);
                }
            }

            // Create item specs
            final ItemSpec[] specs = new ItemSpec[paths.length];
            for (int i = 0; i < paths.length; i++) {
                specs[i] = new ItemSpec(paths[i], recursive ? RecursionType.FULL : RecursionType.NONE);
            }

            set = workspace.getPendingChanges(specs, false);
        }

        final PendingChange[] pendingChanges = (set != null) ? set.getPendingChanges() : null;

        if (pendingChanges == null || pendingChanges.length == 0) {
            getDisplay().printLine(Messages.getString("CommandReconcile.ThereAreNoLocalPendingChangesToReconcile")); //$NON-NLS-1$
            return;
        }

        /*
         * Filter the changes by the changeset.
         */

        final ReconcilePendingChangesStatus findStatus =
            workspace.findReconcilablePendingChangesForChangeset(changeset, pendingChanges);

        if (findStatus.matchedAtLeastOnePendingChange() == false) {
            getDisplay().printLine(Messages.getString("CommandReconcile.NoPendingChangesFoundInChangeset")); //$NON-NLS-1$
            return;
        }

        final PendingChange[] reconcilablePendingChanges = findStatus.getReconcilablePendingChanges();

        /*
         * Get the local items which are not null.
         */
        final String[] localItems = PendingChange.toLocalItems(reconcilablePendingChanges);

        if (reconcilablePendingChanges.length == 0 || localItems.length == 0) {
            /*
             * At least one path may have appeared to match, but after querying
             * for rename info and testing for things like encodings, it turns
             * out there's nothing we can automatically reconcile.
             */
            getDisplay().printLine(Messages.getString("CommandReconcile.NoChangesCanBeAutomaticallyReconciled")); //$NON-NLS-1$
            return;
        }

        /*
         * Create item specs for undo.
         */
        final ItemSpec[] getItemSpecs = new ItemSpec[localItems.length];
        for (int i = 0; i < localItems.length; i++) {
            getItemSpecs[i] = new ItemSpec(localItems[i], RecursionType.NONE);
        }

        /*
         * Do not update the local disk when we undo these files. We'll do an
         * explicit get later to overwrite them.
         */
        if (workspace.undo(getItemSpecs, GetOptions.NO_DISK_UPDATE) == 0) {
            setExitCode(ExitCode.FAILURE);
            return;
        }

        /*
         * Build the get requests for the specific changeset. This "optimizing"
         * method generates for parent items with some recursion to minimize the
         * request data sent to the server. This widening of scope (through
         * limited recursion) has the happy effect of covering both sides of a
         * rename of an item that didn't move directories.
         */
        final GetRequest[] getRequests = GetRequest.createOptimizedRequests(
            workspace.getClient(),
            localItems,
            new ChangesetVersionSpec(changeset.getChangesetID()));

        GetStatus status = null;
        try {
            TaskMonitorService.pushTaskMonitor(taskMonitor);

            status = workspace.get(getRequests, GetOptions.OVERWRITE);
        } finally {
            TaskMonitorService.popTaskMonitor();
        }

        if (status != null) {
            if (status.isNoActionNeeded()) {
                /*
                 * We downloaded all files without any problems.
                 */
            } else {
                if (findOptionType(OptionNoSummary.class) == null) {
                    displayGetSummary(status);
                }

                // VS opens the conflict UI here if there were conflicts, but we
                // will have already printed them above (possibly again in the
                // summary).
            }

            setExitCode(ExitCode.SUCCESS);
        }
    }

    /**
     * Removes one or more builds (wildcards) from the local build status cache.
     *
     * @param connection
     *        the connection (must not be <code>null</code>)
     * @param buildName
     *        the name of the build detail to forget, which may include
     *        wildcards (must not be <code>null</code> or empty)
     * @param teamProject
     *        the team project name (supports wildcards) to constrain the query
     *        by (must not be <code>null</code> or empty)
     */
    private void forgetBuildByName(
        final TFSTeamProjectCollection connection,
        final String buildName,
        final String teamProject) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNullOrEmpty(buildName, "buildName"); //$NON-NLS-1$
        Check.notNullOrEmpty(teamProject, "teamProject"); //$NON-NLS-1$

        /*
         * Check to see if there are any in the cache to begin with and fail
         * fast.
         */
        BuildStatusCache cache = BuildStatusCache.load(connection);
        final List<Integer> cachedBuilds = cache.getBuilds();

        if (cachedBuilds.size() == 0) {
            getDisplay().printErrorLine(Messages.getString("CommandReconcile.NoWatchedCheckins")); //$NON-NLS-1$
            setExitCode(ExitCode.FAILURE);
            return;
        }

        /*
         * Query for details which match the specified name and team project.
         * Match builds of all status.
         */
        final IBuildQueryResult result = queryBuilds(connection, buildName, teamProject, null);

        if (result == null || result.getBuilds() == null || result.getBuilds().length == 0) {
            getDisplay().printErrorLine(
                MessageFormat.format(
                    Messages.getString("CommandReconcile.NoBuildsMatchingNameAndTeamProjectFormat"), //$NON-NLS-1$
                    buildName,
                    teamProject));

            setExitCode(ExitCode.FAILURE);
            return;
        }

        final IBuildDetail[] details = result.getBuilds();

        /*
         * We queried for the build detail by build name, but the IBuildDetail
         * object doesn't yield the ID of the queued build(s) which caused it,
         * which we need to remove from the cache. Query all queued builds which
         * in the cache and find the ones with matching build details.
         */
        final int[] queryIDs = new int[cachedBuilds.size()];
        for (int i = 0; i < cachedBuilds.size(); i++) {
            queryIDs[i] = cachedBuilds.get(i);
        }
        final IQueuedBuild[] queuedBuilds = connection.getBuildServer().getQueuedBuild(queryIDs, QueryOptions.NONE);

        if (queuedBuilds == null || queuedBuilds.length == 0) {
            getDisplay().printErrorLine(Messages.getString("CommandReconcile.NoBuildsForWatchedCheckins")); //$NON-NLS-1$
            setExitCode(ExitCode.FAILURE);
            return;
        }

        /*
         * Map the details we got back to the queued builds which created them.
         */
        final List<IQueuedBuild> forgetQueuedBuilds = new ArrayList<IQueuedBuild>();
        for (final IBuildDetail detail : details) {
            if (detail == null) {
                continue;
            }

            for (final IQueuedBuild queuedBuild : queuedBuilds) {
                if (queuedBuild == null) {
                    continue;
                }

                if (queuedBuild.getBuild() != null && detailsEqual(queuedBuild.getBuild(), detail)) {
                    forgetQueuedBuilds.add(queuedBuild);
                    break;
                }
            }
        }

        /*
         * Forget the ones which we found a match for. Ignore the ones we didn't
         * find a match for; they'll be removed when the user runs
         * "tf reconcile" (which removes IDs not returned).
         */
        if (forgetQueuedBuilds.size() > 0) {
            // Refresh the cache
            cache = BuildStatusCache.load(connection);

            for (final IQueuedBuild queuedBuild : forgetQueuedBuilds) {
                getDisplay().printLine(
                    MessageFormat.format(
                        Messages.getString("CommandReconcile.ForgettingBuildNameFormat"), //$NON-NLS-1$
                        queuedBuild.getBuild().getBuildNumber()));

                cache.removeBuild(queuedBuild.getID());
            }

            cache.save(connection);
        }
    }

    /**
     * Reconciles the changeset associated with a gated check-in build. If more
     * than one build detail results from the given name query, an error message
     * is printed to the user.
     * <p>
     * Sets exit code to {@link ExitCode#SUCCESS} if a single build was
     * reconciled and removed from the cache, {@link ExitCode#PARTIAL_SUCCESS}
     * if a single build was found but did not have a changeset ID (removed from
     * the cache, too), {@link ExitCode#FAILURE} if there were multiple builds
     * that matched the build name and team project filters or the single build
     * could not be reconciled.
     *
     * @param workspace
     *        the {@link Workspace} to reconcile changes in (must not be
     *        <code>null</code>)
     * @param buildName
     *        the name of the build detail to resolve, which may include
     *        wildcards (must not be <code>null</code> or empty)
     * @param teamProject
     *        the team project name (supports wildcards) to constrain the query
     *        by (must not be <code>null</code> or empty)
     * @param recursive
     *        <code>true</code> to match free arguments against pending changes
     *        recursively
     * @throws CLCException
     *         if no matching builds were found or a single build was found but
     *         did not contain a changeset
     */
    private void reconcileBuildByName(
        final Workspace workspace,
        final String buildName,
        final String teamProject,
        final boolean recursive) {
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        Check.notNullOrEmpty(buildName, "buildName"); //$NON-NLS-1$
        Check.notNullOrEmpty(teamProject, "teamProject"); //$NON-NLS-1$

        final IBuildQueryResult result = queryBuilds(
            workspace.getClient().getConnection(),
            buildName,
            teamProject,
            BuildStatus.SUCCEEDED.combine(BuildStatus.PARTIALLY_SUCCEEDED));

        if (result == null || result.getBuilds() == null || result.getBuilds().length == 0) {
            getDisplay().printErrorLine(
                MessageFormat.format(
                    Messages.getString("CommandReconcile.NoBuildsMatchingNameAndTeamProjectFormat"), //$NON-NLS-1$
                    buildName,
                    teamProject));

            setExitCode(ExitCode.FAILURE);
            return;
        }

        if (result.getBuilds().length > 1) {
            final IBuildDetail[] sortedBuilds = result.getBuilds();
            Arrays.sort(sortedBuilds, new Comparator<IBuildDetail>() {
                @Override
                public int compare(final IBuildDetail o1, final IBuildDetail o2) {
                    if (o1.getFinishTime() != null && o2.getFinishTime() != null) {
                        return o1.getFinishTime().compareTo(o2.getFinishTime());
                    }

                    if (o1.getFinishTime() != null) {
                        // First has finished, second hasn't, so first sorts
                        // earlier
                        return -1;
                    } else if (o2.getFinishTime() != null) {
                        // Second has finished, first hasn't, so second sorts
                        // later
                        return 1;
                    }

                    // Both have not finished
                    return 0;
                }
            });

            getDisplay().printLine(
                MessageFormat.format(
                    Messages.getString("CommandReconcile.MoreThanOneBuildMatchedNameProjectFormat"), //$NON-NLS-1$
                    buildName,
                    teamProject));
            getDisplay().printLine(""); //$NON-NLS-1$

            BuildPrinter.printBuildDetails(
                sortedBuilds,
                workspace.getClient().getConnection().getBuildServer(),
                defaultFormat,
                getDisplay());

            setExitCode(ExitCode.FAILURE);
            return;
        }

        final IBuildDetail reconciledBuild = result.getBuilds()[0];

        // Just one build.
        final int changesetID = InformationNodeConverters.getChangesetID(reconciledBuild.getInformation());

        if (changesetID <= 0) {
            getDisplay().printErrorLine(
                MessageFormat.format(
                    Messages.getString("CommandReconcile.BuildNameDoesNotContainChangesetFormat"), //$NON-NLS-1$
                    reconciledBuild.getBuildNumber()));

            /*
             * This build should be removed from the cache.
             */
            setExitCode(ExitCode.PARTIAL_SUCCESS);
        } else {
            getDisplay().printLine(
                MessageFormat.format(
                    Messages.getString("CommandReconcile.ReconcilingBuildNameWithChangesetFormat"), //$NON-NLS-1$
                    reconciledBuild.getBuildNumber(),
                    Integer.toString(changesetID)));

            reconcileChangeset(workspace, changesetID, recursive);
        }

        if (getExitCode() == ExitCode.PARTIAL_SUCCESS || getExitCode() == ExitCode.SUCCESS) {
            /*
             * We queried for the build detail by build name, but the
             * IBuildDetail object doesn't yield the ID of the queued build(s)
             * which caused it, which we need to remove from the cache. Query
             * all queued builds which in the cache and find the one with the
             * matching build detail.
             */

            final BuildStatusCache cache = BuildStatusCache.load(workspace.getClient().getConnection());

            final List<Integer> builds = cache.getBuilds();

            if (builds.size() > 0) {
                final int[] queryIDs = new int[builds.size()];

                for (int i = 0; i < builds.size(); i++) {
                    queryIDs[i] = builds.get(i);
                }

                final IQueuedBuild[] queuedBuilds =
                    workspace.getClient().getConnection().getBuildServer().getQueuedBuild(queryIDs, QueryOptions.NONE);

                if (queuedBuilds != null && queuedBuilds.length > 0) {
                    for (int i = 0; i < queuedBuilds.length; i++) {
                        if (queuedBuilds[i].getBuild() != null
                            && detailsEqual(queuedBuilds[i].getBuild(), reconciledBuild)) {
                            cache.removeBuild(queuedBuilds[i].getID());
                            cache.save(workspace.getClient().getConnection());

                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Queries the server for succeeded or partially succeeded builds which
     * match the given build name and team project.
     *
     * @param connection
     *        the connection (must not be <code>null</code>)
     * @param buildName
     *        the build name (must not be <code>null</code> or empty)
     * @param teamProject
     *        the team project to filter by (must not be <code>null</code> or
     *        empty)
     * @param status
     *        the statuses to match (may be <code>null</code>)
     * @return the result
     */
    private IBuildQueryResult queryBuilds(
        final TFSTeamProjectCollection connection,
        final String buildName,
        final String teamProject,
        final BuildStatus status) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNullOrEmpty(buildName, "buildName"); //$NON-NLS-1$
        Check.notNullOrEmpty(teamProject, "teamProject"); //$NON-NLS-1$

        final IBuildServer buildServer = connection.getBuildServer();

        /*
         * Query by name string and status. Include check in outcome info
         * fields.
         */
        final IBuildDetailSpec buildDetailSpec = buildServer.createBuildDetailSpec(teamProject);
        buildDetailSpec.setBuildNumber(buildName);
        buildDetailSpec.setInformationTypes(new String[] {
            InformationTypes.CHECK_IN_OUTCOME
        });
        if (status != null) {
            buildDetailSpec.setStatus(status);
        }

        return buildServer.queryBuilds(buildDetailSpec);
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[4];

        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionTeamProject.class
        }, ""); //$NON-NLS-1$

        optionSets[1] = new AcceptedOptionSet(new Class[] {
            OptionTeamProject.class,
            OptionRecursive.class
        }, "[<itemSpec>...]", new Class[] { //$NON-NLS-1$
            OptionBuildName.class
        });

        optionSets[2] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class,
        }, "[<itemSpec>...]", new Class[] { //$NON-NLS-1$
            OptionChangeset.class
        });

        optionSets[3] = new AcceptedOptionSet(new Class[] {
            OptionTeamProject.class
        }, "", new Class[] { //$NON-NLS-1$
            OptionForgetBuild.class
        });

        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandReconcile.HelpText1"), //$NON-NLS-1$
            Messages.getString("CommandReconcile.HelpText2"), //$NON-NLS-1$
            Messages.getString("CommandReconcile.HelpText3") //$NON-NLS-1$
        };
    }

    private static boolean detailsEqual(final IBuildDetail a, final IBuildDetail b) {
        if (a == b) {
            return true;
        }
        if (a == null && b != null || a != null && b == null) {
            return false;
        }

        /*
         * TODO Remove this method when we use the TFS 2012 web services and
         * just use .equals().
         *
         * Why would we want to ignore the query parameters?
         *
         * To support batched gated builds, a TFS 2012 server speaking to
         * clients over the TFS 2010 API appends query parameters like
         * "?queueId=146&latest" to the end of the URI string. This out-of-band
         * data is round-tripped by TFS 2010 clients so the server can
         * distinguish which build caused the detail.
         *
         * When we start using TFS 2012 build APIs, we won't see the parameter
         * and the URL is opaque. TFS 2010 servers will never use this trick.
         */

        final String uriStringA = a.getURI();
        final String uriStringB = b.getURI();

        if (uriStringA == uriStringB) {
            return true;
        }
        if (uriStringA == null && uriStringB != null || uriStringA != null && uriStringB == null) {
            return false;
        }

        final URI uriA = URIUtils.removeQueryParts(URIUtils.newURI(a.getURI()));
        final URI uriB = URIUtils.removeQueryParts(URIUtils.newURI(b.getURI()));

        return uriA.equals(uriB);
    }
}
