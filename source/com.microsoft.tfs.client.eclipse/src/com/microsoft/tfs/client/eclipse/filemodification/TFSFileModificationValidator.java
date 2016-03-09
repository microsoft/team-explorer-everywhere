// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.filemodification;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileModificationValidator;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.microsoft.tfs.client.common.commands.vc.EditCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.framework.command.JobCommandAdapter;
import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.framework.resources.filter.CompositeResourceFilter.Builder;
import com.microsoft.tfs.client.common.framework.resources.filter.CompositeResourceFilter.CompositeResourceFilterType;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilter;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilterResult;
import com.microsoft.tfs.client.common.framework.resources.filter.ResourceFilters;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.util.ExtensionLoader;
import com.microsoft.tfs.client.eclipse.Messages;
import com.microsoft.tfs.client.eclipse.TFSEclipseClientPlugin;
import com.microsoft.tfs.client.eclipse.TFSRepositoryProvider;
import com.microsoft.tfs.client.eclipse.commands.eclipse.IgnoreResourceRefreshesEditCommand;
import com.microsoft.tfs.client.eclipse.project.ProjectRepositoryStatus;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.resourcedata.ResourceDataManager;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.SupportedFeatures;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

/**
 * This is the actual file modification validator for the Eclipse plugin. It
 * does NOT implement {@link IFileModificationValidator} or extend
 * {@link org.eclipse.core.resources.team.FileModificationValidator} so that
 * other classes (which do) may proxy to this class safely. (
 * {@link org.eclipse.core.resources.team.FileModificationValidator} is new for
 * Eclipse 3.3.)
 *
 * @threadsafety unknown
 */
public final class TFSFileModificationValidator {
    private final static Log log = LogFactory.getLog(TFSFileModificationValidator.class);

    public static final String ADVISOR_EXTENSION_POINT_ID = "com.microsoft.tfs.client.eclipse.fileModificationAdvisor"; //$NON-NLS-1$

    /**
     * A property to disable the ignoring of events fired from the workspace
     * undo manager.
     */
    private static final String IGNORE_UNDO_MANAGER_PROPERTY_NAME =
        "com.microsoft.tfs.client.eclipse.filemodification.ignoreUndoManager"; //$NON-NLS-1$

    /**
     * Number of seconds to block waiting for checkouts to finish from the
     * server.
     */
    private final static long CHECKOUT_WAIT_TIME = 30;

    /**
     * Items which match this filter are made writable immediately in response
     * to an edit validation (no echanges are pended). This behavior allows
     * "ignored" resources (from .tpignore or Eclipse's team ignored resources)
     * to be edited by editors or other plug-ins without causing pending
     * changes.
     *
     * The linked resource filter is not part of this filter because linked
     * resources are not {@link IFile}s and won't be passed to
     * {@link #validateEdit(IFile[], boolean, Object)}.
     *
     * {@link PluginResourceFilters#TFS_IGNORE_FILTER} should not be used here
     * because it's only for local workspaces, where files are writable by
     * default and won't be validated by this class.
     */
    private final static ResourceFilter IGNORED_RESOURCES_FILTER =
        new Builder(CompositeResourceFilterType.ALL_MUST_ACCEPT).addFilter(
            PluginResourceFilters.TEAM_IGNORED_RESOURCES_FILTER).addFilter(
                ResourceFilters.TEAM_PRIVATE_RESOURCES_FILTER).addFilter(PluginResourceFilters.TPIGNORE_FILTER).build();

    private final TFSRepositoryProvider repositoryProvider;

    /**
     * The {@link backgroundFiles} map contains all files which are currently
     * being checked out on background threads, as well as their modification
     * stamp (before checkout). This allows us to rollback to this modification
     * stamp if the checkout fails.
     */
    private final Map<IFile, TFSFileModificationStatusData> backgroundFiles =
        new HashMap<IFile, TFSFileModificationStatusData>();

    private final Object advisorLock = new Object();
    private TFSFileModificationAdvisor advisor = null;

    public TFSFileModificationValidator(final TFSRepositoryProvider repositoryProvider) {
        this.repositoryProvider = repositoryProvider;
    }

    /*
     * Validate edit is the commonly called operation for when Eclipse notifies
     * us of resource changes.
     */
    public IStatus validateEdit(final IFile[] files, final boolean attemptUi, final Object shell) {
        final ResourceDataManager resourceDataManager = TFSEclipseClientPlugin.getDefault().getResourceDataManager();
        final TFSRepository repository = repositoryProvider.getRepository();

        if (repositoryProvider.getRepositoryStatus() == ProjectRepositoryStatus.CONNECTING) {
            getStatusReporter(attemptUi, shell).reportError(
                Messages.getString("TFSFileModificationValidator.ErrorConnectionInProgress"), //$NON-NLS-1$
                new Status(
                    IStatus.ERROR,
                    TFSEclipseClientPlugin.PLUGIN_ID,
                    0,
                    Messages.getString("TFSFileModificationValidator.ErrorConnectionInProgressDescription"), //$NON-NLS-1$
                    null));

            return Status.CANCEL_STATUS;
        }

        /*
         * Offline server workspace. Simply mark files as writable and continue.
         */
        if (repository == null) {
            for (int i = 0; i < files.length; i++) {
                log.info(MessageFormat.format("Setting {0} writable, project is offline from TFS server", files[i])); //$NON-NLS-1$

                files[i].setReadOnly(false);
            }

            return Status.OK_STATUS;
        }

        /*
         * Local workspace: ignore this entirely. This method is only called for
         * read-only files. A read only file in a local workspace was not placed
         * by us and we should not set them writable.
         */
        if (WorkspaceLocation.LOCAL.equals(repository.getWorkspace().getLocation())) {
            for (int i = 0; i < files.length; i++) {
                log.info(MessageFormat.format("Ignoring read-only file {0} in local TFS workspace", files[i])); //$NON-NLS-1$
            }

            return Status.OK_STATUS;
        }

        /*
         * HACK: avoid "phantom pending changes" that arise from the undo
         * manager. If we have no undoable context (ie, no Shell), then check to
         * see if we're being called from the undo manager and simply defer this
         * operation.
         *
         * This bug is hard to reproduce, so details are thus far limited. The
         * best guess is that the Eclipse undo manager watches edits using a
         * resource changed listener (for post-change and post-build
         * notifications.) It then realizes that the current contents of a file
         * are identical to a previous undo state, and therefore needs to
         * resynchronize the file history. It then, for some reason, calls
         * validateEdit on that file. This causes the file to be checked out
         * (here.)
         */
        if (attemptUi == false && shell == null) {
            /*
             * Since this is a terrible hack, we may need to have users disable
             * this functionality with a sysprop.
             */
            if ("false".equalsIgnoreCase(System.getProperty(IGNORE_UNDO_MANAGER_PROPERTY_NAME)) == false) //$NON-NLS-1$
            {
                /* Build an exception to get our stack trace. */
                final Exception e = new Exception(""); //$NON-NLS-1$
                e.fillInStackTrace();

                final StackTraceElement[] stackTrace = e.getStackTrace();

                if (stackTrace != null) {
                    for (int i = 0; i < stackTrace.length; i++) {
                        if (stackTrace[i].getClassName().equals(
                            "org.eclipse.ui.internal.ide.undo.WorkspaceUndoMonitor")) //$NON-NLS-1$
                        {
                            log.info("Ignoring file modification request from WorkspaceUndoMonitor"); //$NON-NLS-1$
                            return Status.OK_STATUS;
                        }
                    }
                }
            }
        }

        /* Pend edits for these files. */
        final List<String> pathList = new ArrayList<String>();
        final Set<String> projectSet = new HashSet<String>();

        for (int i = 0; i < files.length; i++) {
            if (IGNORED_RESOURCES_FILTER.filter(files[i]) == ResourceFilterResult.REJECT) {
                log.info(
                    MessageFormat.format("Setting {0} writable, file matches automatic validation filter", files[i])); //$NON-NLS-1$

                files[i].setReadOnly(false);

                continue;
            }

            /* Make sure that this file exists on the server. */
            if (!resourceDataManager.hasResourceData(files[i])
                && resourceDataManager.hasCompletedRefresh(files[i].getProject())) {
                continue;
            }

            final String path = Resources.getLocation(files[i], LocationUnavailablePolicy.IGNORE_RESOURCE);
            final String serverPath = repository.getWorkspace().getMappedServerPath(path);

            if (path == null) {
                continue;
            }

            final PendingChange pendingChange = repository.getPendingChangeCache().getPendingChangeByLocalPath(path);

            /* Don't pend changes when there's already an add or edit pended. */
            if (pendingChange != null
                && (pendingChange.getChangeType().contains(ChangeType.ADD)
                    || pendingChange.getChangeType().contains(ChangeType.EDIT))) {
                log.debug(MessageFormat.format(
                    "File {0} has pending change {1}, ignoring", //$NON-NLS-1$
                    files[i],
                    pendingChange.getChangeType().toUIString(true, pendingChange)));

                continue;
            }

            pathList.add(path);

            log.info(MessageFormat.format("File {0} is being modified, checking out", files[i])); //$NON-NLS-1$

            if (serverPath != null) {
                projectSet.add(ServerPath.getTeamProject(serverPath));
            }
        }

        if (pathList.size() == 0) {
            return Status.OK_STATUS;
        }

        LockLevel forcedLockLevel = null;
        boolean forcedGetLatest = false;

        /*
         * Query the server's default checkout lock and get latest on checkout
         * setting
         */
        for (final Iterator<String> i = projectSet.iterator(); i.hasNext();) {
            final String teamProject = i.next();

            final String exclusiveCheckoutAnnotation = repository.getAnnotationCache().getAnnotationValue(
                VersionControlConstants.EXCLUSIVE_CHECKOUT_ANNOTATION,
                teamProject,
                0);
            final String getLatestAnnotation = repository.getAnnotationCache().getAnnotationValue(
                VersionControlConstants.GET_LATEST_ON_CHECKOUT_ANNOTATION,
                teamProject,
                0);

            if ("true".equalsIgnoreCase(exclusiveCheckoutAnnotation)) //$NON-NLS-1$
            {
                forcedLockLevel = LockLevel.CHECKOUT;
                break;
            }

            /* Server get latest on checkout forces us to work synchronously */
            if ("true".equalsIgnoreCase(getLatestAnnotation)) //$NON-NLS-1$
            {
                forcedGetLatest = true;
            }
        }

        /* Allow UI hooks to handle prompt before checkout. */

        final TFSFileModificationOptions checkoutOptions =
            getOptions(attemptUi, shell, pathList.toArray(new String[pathList.size()]), forcedLockLevel);

        if (!checkoutOptions.getStatus().isOK()) {
            return checkoutOptions.getStatus();
        }

        final String[] paths = checkoutOptions.getFiles();
        final LockLevel lockLevel = checkoutOptions.getLockLevel();
        final boolean getLatest = checkoutOptions.isGetLatest();
        final boolean synchronousCheckout = checkoutOptions.isSynchronous();
        final boolean foregroundCheckout = checkoutOptions.isForeground();

        if (paths.length == 0) {
            return Status.OK_STATUS;
        }

        final ItemSpec[] itemSpecs = new ItemSpec[paths.length];
        for (int i = 0; i < paths.length; i++) {
            itemSpecs[i] = new ItemSpec(paths[i], RecursionType.NONE);
        }

        /*
         * Query get latest on checkout preference (and ensure server supports
         * the feature)
         */
        GetOptions getOptions = GetOptions.NO_DISK_UPDATE;
        PendChangesOptions pendChangesOptions = PendChangesOptions.NONE;

        if (repository.getWorkspace().getClient().getServerSupportedFeatures().contains(
            SupportedFeatures.GET_LATEST_ON_CHECKOUT) && getLatest) {
            /*
             * If we're doing get latest on checkout, we need add the overwrite
             * flag: we need to set the file writable before this method exits
             * (in order for Eclipse to pick up the change, but we need to do
             * the get in another thread (so that we can clear the resource lock
             * on this file.) Thus we need to set the file writable, then fire a
             * synchronous worker to overwrite it. This is safe as this method
             * will ONLY be called when the file is readonly.
             */
            pendChangesOptions = PendChangesOptions.GET_LATEST_ON_CHECKOUT;
            getOptions = GetOptions.NONE;
        }

        /*
         * Build the checkout command - no need to query conflicts here, the
         * only conflicts that can arise from a pend edit are writable file
         * conflicts (when get latest on checkout is true.) This method is never
         * called for writable files.
         */
        final EditCommand editCommand =
            new EditCommand(repository, itemSpecs, lockLevel, null, getOptions, pendChangesOptions, false);

        /*
         * Pend changes in the foreground if get latest on checkout is
         * requested. A disk update may be required, so we want to block user
         * input.
         */
        if (synchronousCheckout
            || pendChangesOptions.contains(PendChangesOptions.GET_LATEST_ON_CHECKOUT)
            || forcedGetLatest) {
            /*
             * Wrap this edit command in one that disables the plugin's
             * automatic resource refresh behavior. This is required to avoid
             * deadlocks: the calling thread has taken a resource lock on the
             * resource it wishes to check out - the plugin will also require a
             * resource lock to do the refresh in another thread.
             */
            final ICommand wrappedEditCommand = new IgnoreResourceRefreshesEditCommand(editCommand);

            final IStatus editStatus = getSynchronousCommandExecutor(attemptUi, shell).execute(wrappedEditCommand);

            /* Refresh files on this thread, since it has the resource lock. */
            for (int i = 0; i < files.length; i++) {
                try {
                    files[i].refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
                } catch (final Throwable e) {
                    log.warn(MessageFormat.format("Could not refresh {0}", files[i].getName()), e); //$NON-NLS-1$
                }
            }

            return editStatus;
        }
        /* Pend changes in the background */
        else {
            synchronized (backgroundFiles) {
                for (int i = 0; i < files.length; i++) {
                    files[i].setReadOnly(false);
                    backgroundFiles.put(files[i], new TFSFileModificationStatusData(files[i]));
                }
            }

            final JobCommandAdapter editJob = new JobCommandAdapter(editCommand);
            editJob.setPriority(Job.INTERACTIVE);
            editJob.setUser(foregroundCheckout);
            editJob.schedule();

            final Thread editThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    IStatus editStatus;

                    try {
                        /*
                         * We don't need to safe-wait with
                         * ExtensionPointAsyncObjectWaiter because we're
                         * guaranteed not on the UI thread.
                         */
                        editJob.join();
                        editStatus = editJob.getResult();
                    } catch (final Exception e) {
                        editStatus = new Status(IStatus.ERROR, TFSEclipseClientPlugin.PLUGIN_ID, 0, null, e);
                    }

                    if (editStatus.isOK()) {
                        synchronized (backgroundFiles) {
                            for (int i = 0; i < files.length; i++) {
                                final TFSFileModificationStatusData statusData = backgroundFiles.remove(files[i]);

                                if (statusData != null) {
                                    log.info(MessageFormat.format(
                                        "File {0} checked out in {1} seconds", //$NON-NLS-1$
                                        files[i],
                                        (int) ((System.currentTimeMillis() - statusData.getStartTime()) / 1000)));
                                }
                            }
                        }
                    } else {
                        final List<TFSFileModificationStatusData> statusDataList =
                            new ArrayList<TFSFileModificationStatusData>();

                        synchronized (backgroundFiles) {
                            for (int i = 0; i < files.length; i++) {
                                final TFSFileModificationStatusData statusData = backgroundFiles.remove(files[i]);

                                if (statusData != null) {
                                    log.info(MessageFormat.format(
                                        "File {0} failed to check out in {1} seconds", //$NON-NLS-1$
                                        files[i],
                                        (int) ((System.currentTimeMillis() - statusData.getStartTime()) / 1000)));

                                    statusDataList.add(statusData);
                                }
                            }
                        }

                        /*
                         * Unfortunately, we have to roll back ALL FILES when an
                         * edit fails. We could (in theory) be better about this
                         * and use the non fatal listener in EditCommand to give
                         * us the paths that failed, but at the moment, the use
                         * case is only for one file at a time, so this is okay.
                         */
                        final TFSFileModificationStatusData[] statusData =
                            statusDataList.toArray(new TFSFileModificationStatusData[statusDataList.size()]);
                        getStatusReporter(attemptUi, shell).reportStatus(repository, statusData, editStatus);
                    }
                }
            });

            editThread.start();

            return Status.OK_STATUS;
        }
    }

    /*
     * Disallow save events while we're still checking out. With our revert /
     * accept writable conflict logic in the edit hook
     * (TFSFileModificationUiStatusReporter), this is probably unnecessary and
     * should be revisited.
     */
    public IStatus validateSave(final IFile file) {
        final TFSRepository repository = repositoryProvider.getRepository();

        /*
         * Local workspaces: ignore this save. (Modifications will get picked up
         * by the scanner, hinted by file modification validator.)
         */
        if (repository != null && WorkspaceLocation.LOCAL.equals(repository.getWorkspace().getLocation())) {
            return Status.OK_STATUS;
        }

        /*
         * Server workspace: ensure that the background checkout task completes
         * before we allow the save to proceed. (Block on the checkout.)
         */

        boolean inBackground = false;

        synchronized (backgroundFiles) {
            inBackground = (backgroundFiles.get(file) != null);
        }

        /* Wait for the background checkout to complete */
        final long startTime = System.currentTimeMillis();
        long lastNotifyTime = startTime;
        while (inBackground) {
            if (startTime == lastNotifyTime) {
                log.debug(
                    MessageFormat.format(
                        "File {0} still being checked out from TFS server, waiting for checkout to succeed before allowing save event", //$NON-NLS-1$
                        file.getLocation().toOSString()));
            }

            if ((System.currentTimeMillis() - lastNotifyTime) > (CHECKOUT_WAIT_TIME * 1000)) {
                log.warn(MessageFormat.format(
                    "File {0} still being checked out from TFS server after {1} seconds.", //$NON-NLS-1$
                    file.getLocation().toOSString(),
                    (int) ((System.currentTimeMillis() - startTime) / 1000)));
                lastNotifyTime = System.currentTimeMillis();
            }

            try {
                Thread.sleep(500);
            } catch (final InterruptedException e) {
                return Status.CANCEL_STATUS;
            }

            synchronized (backgroundFiles) {
                inBackground = (backgroundFiles.get(file) != null);
            }
        }

        return Status.OK_STATUS;
    }

    /**
     * Tests whether the specified file is currenlty being checked out by a
     * background thread.
     *
     * @param file
     *        the file to test (must not be <code>null</code>)
     * @return <code>true</code> if the file is being checked out, false if it
     *         is not
     */
    public boolean isCheckingOutFileInBackground(final IFile file) {
        synchronized (backgroundFiles) {
            return backgroundFiles.get(file) != null;
        }
    }

    /**
     * @return the {@link TFSFileModificationAdvisor} contributed via extension
     *         point, or <code>null</code> if none found
     */
    private final TFSFileModificationAdvisor getFileModificationAdvisor() {
        synchronized (advisorLock) {
            if (advisor == null) {
                try {
                    advisor = (TFSFileModificationAdvisor) ExtensionLoader.loadSingleExtensionClass(
                        ADVISOR_EXTENSION_POINT_ID);
                } catch (final Exception e) {
                    log.error("Could not load file modification advisor for the product", e); //$NON-NLS-1$
                    advisor = null;
                }
            }

            return advisor;
        }
    }

    private final TFSFileModificationOptions getOptions(
        final boolean attemptUi,
        final Object shell,
        final String[] files,
        final LockLevel forcedLockLevel) {
        final TFSFileModificationAdvisor advisor = getFileModificationAdvisor();

        if (advisor != null) {
            final TFSFileModificationOptionsProvider optionsProvider = advisor.getOptionsProvider(attemptUi, shell);

            if (optionsProvider != null) {
                return optionsProvider.getOptions(files, forcedLockLevel);
            }
        }

        log.warn("File modification advisor not present or did not provide options, using default"); //$NON-NLS-1$
        return new TFSFileModificationOptions(Status.OK_STATUS, files, LockLevel.UNCHANGED, true, true, false);
    }

    private final CommandExecutor getSynchronousCommandExecutor(final boolean attemptUi, final Object shell) {
        final TFSFileModificationAdvisor advisor = getFileModificationAdvisor();

        if (advisor != null) {
            final CommandExecutor executor = advisor.getSynchronousCommandExecutor(attemptUi, shell);

            if (executor != null) {
                return executor;
            }
        }

        log.warn("File modification advisor not present or did not provide a command executor, using default"); //$NON-NLS-1$
        return new CommandExecutor();
    }

    private final TFSFileModificationStatusReporter getStatusReporter(final boolean attemptUi, final Object shell) {
        final TFSFileModificationAdvisor advisor = getFileModificationAdvisor();

        if (advisor != null) {
            final TFSFileModificationStatusReporter statusReporter = advisor.getStatusReporter(attemptUi, shell);

            if (statusReporter != null) {
                return statusReporter;
            }
        }

        log.warn("File modification advisor not present or did not provide a status reporter, using default"); //$NON-NLS-1$
        return new TFSFileModificationStatusReporter();
    }
}