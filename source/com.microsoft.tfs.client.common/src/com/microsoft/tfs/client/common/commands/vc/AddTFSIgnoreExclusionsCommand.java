// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.vc;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.client.common.TFSCommonClientPlugin;
import com.microsoft.tfs.client.common.framework.command.Command;
import com.microsoft.tfs.client.common.framework.resources.LocationUnavailablePolicy;
import com.microsoft.tfs.client.common.framework.resources.Resources;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleUtil;

/**
 * Adds new exclusions for specified paths or resources to the closest TFS 2012
 * .tfignore file (creates one if necessary).
 * <p>
 * Files and directories are supported. The exclusions are all of the
 * "this item" variety, where a partial path item is added to the file (not a
 * wildcard, or parent folder, or short file name).
 */
public class AddTFSIgnoreExclusionsCommand extends Command {
    private final static Log log = LogFactory.getLog(AddTFSIgnoreExclusionsCommand.class);

    private final TFSRepository repository;
    private final IResource[] resources;
    private final String[] localItems;
    private final String exclusion;

    // Resource constructors

    public AddTFSIgnoreExclusionsCommand(final TFSRepository repository, final IResource[] resources) {
        this(repository, resources, null);
    }

    public AddTFSIgnoreExclusionsCommand(
        final TFSRepository repository,
        final IResource[] resources,
        final String exclusion) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(resources, "resources"); //$NON-NLS-1$

        this.repository = repository;
        this.resources = resources;
        this.localItems = null;
        this.exclusion = exclusion;
    }

    // String path constructors

    public AddTFSIgnoreExclusionsCommand(final TFSRepository repository, final String localItem) {
        this(repository, localItem, null);
    }

    public AddTFSIgnoreExclusionsCommand(
        final TFSRepository repository,
        final String localItem,
        final String exclusion) {
        this(repository, new String[] {
            localItem
        }, exclusion);
    }

    public AddTFSIgnoreExclusionsCommand(final TFSRepository repository, final String[] localItems) {
        this(repository, localItems, null);
    }

    public AddTFSIgnoreExclusionsCommand(
        final TFSRepository repository,
        final String[] localItems,
        final String exclusion) {
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(localItems, "localItems"); //$NON-NLS-1$

        this.repository = repository;
        this.resources = null;
        this.localItems = localItems;
        this.exclusion = exclusion;
    }

    @Override
    public String getName() {
        return Messages.getString("AddTFSIgnoreExclusionsCommand.CommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("AddTFSIgnoreExclusionsCommand.CommandErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("AddTFSIgnoreExclusionsCommand.CommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        final String[] paths = resources != null
            ? Resources.getLocations(resources, LocationUnavailablePolicy.IGNORE_RESOURCE) : localItems;

        // Sort them so they appear nicely in the file
        Arrays.sort(paths, ServerPath.TOP_DOWN_COMPARATOR);

        final List<IStatus> errorStatuses = new ArrayList<IStatus>();

        progressMonitor.beginTask("", paths.length + 1); //$NON-NLS-1$

        final Set<String> modifiedIgnoreFiles = new TreeSet<String>(LocalPath.TOP_DOWN_COMPARATOR);

        try {
            for (final String path : paths) {
                progressMonitor.subTask(path);

                try {
                    // exclusion may be null here
                    modifiedIgnoreFiles.add(repository.getWorkspace().addIgnoreFileExclusionAuto(path, exclusion));
                } catch (final Exception e) {
                    log.warn("Error adding exclusion", e); //$NON-NLS-1$

                    errorStatuses.add(
                        new Status(
                            Status.ERROR,
                            TFSCommonClientPlugin.PLUGIN_ID,
                            MessageFormat.format(
                                Messages.getString("AddTFSIgnoreExclusionsCommand.ErrorAddingExclusionFormat"), //$NON-NLS-1$
                                path),
                            e));
                }

                progressMonitor.worked(1);
            }
        } finally {
            progressMonitor.done();
        }

        // Refresh the .tfignore files that are resources to ensure label
        // decoration, etc.
        progressMonitor.subTask(Messages.getString("AddTFSIgnoreExclusionsCommand.RefreshingResources")); //$NON-NLS-1$
        for (final String ignoreFile : modifiedIgnoreFiles) {
            if (ignoreFile != null) {
                final IResource resource = Resources.getFileForLocation(ignoreFile, true);
                if (resource != null) {
                    resource.refreshLocal(IResource.DEPTH_ONE, null);
                }
            }
        }
        progressMonitor.worked(1);

        if (errorStatuses.size() > 0) {
            return new MultiStatus(
                TFSCommonClientPlugin.PLUGIN_ID,
                Status.ERROR,
                errorStatuses.toArray(new IStatus[errorStatuses.size()]),
                Messages.getString("AddTFSIgnoreExclusionsCommand.MultistatusMessageText"), //$NON-NLS-1$
                null);
        }

        return Status.OK_STATUS;
    }
}
