// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc;

import java.io.File;
import java.io.FileFilter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryItemsCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.BaselineFolder;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.Platform;

public class FilterItemsExistOnServerCommand extends TFSCommand {
    private File[] localItems;
    private final String filterPath;
    private final AddFilesFilter filesFilter;

    /*
     * Used for recursive disk walking
     */
    private List<File> fileList;

    /*
     * true if filter folder recursively, false only filter one level
     */
    private final boolean recursion;

    private static final Log log = LogFactory.getLog(FilterItemsExistOnServerCommand.class);

    public FilterItemsExistOnServerCommand(final String filterPath, final boolean recursion) {
        this.filterPath = LocalPath.canonicalize(filterPath);
        this.recursion = recursion;
        filesFilter = new AddFilesFilter();
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        if (recursion) {
            localItems = listAllFiles(filterPath);
        } else {
            localItems = new File(filterPath).listFiles(filesFilter);
        }

        final TFSRepository repository = getRepository();

        if (localItems == null || repository == null) {
            localItems = new File[0];
            return Status.OK_STATUS;
        }

        final VersionControlClient vcClient = repository.getVersionControlClient();

        final HashSet<String> excludePaths = new HashSet<String>();
        final List<File> filteredFiles = new ArrayList<File>();

        /*
         * Query the pending changes cache to filter files already added.
         */
        for (final File f : localItems) {
            final PendingChange pendingChange =
                repository.getPendingChangeCache().getPendingChangeByLocalPath(f.getPath());
            if (pendingChange != null && pendingChange.getChangeType().contains(ChangeType.ADD)) {
                excludePaths.add(f.getPath());
            }
        }

        final Workspace workspace = repository.getWorkspace();
        final String serverPath = workspace.getMappedServerPath(filterPath);

        if (serverPath != null) {
            final RecursionType recursionType = recursion ? RecursionType.FULL : RecursionType.ONE_LEVEL;
            final QueryItemsCommand queryCommand = new QueryItemsCommand(vcClient, new ItemSpec[] {
                new ItemSpec(serverPath, recursionType)
            }, LatestVersionSpec.INSTANCE, DeletedState.NON_DELETED, ItemType.ANY, GetItemsOptions.INCLUDE_BRANCH_INFO);

            final IStatus status = new CommandExecutor().execute(queryCommand);

            if (!status.isOK()) {
                return status;
            }

            if (queryCommand.getItemSets() != null && queryCommand.getItemSets().length > 0) {
                final ItemSet itemSet = queryCommand.getItemSets()[0];

                if (itemSet != null) {
                    final Item[] items = itemSet.getItems();

                    if (items != null) {
                        for (final Item item : items) {
                            final String localPath = workspace.getMappedLocalPath(item.getServerItem());
                            excludePaths.add(localPath);
                        }
                    }
                }
            }
        }

        for (final File f : localItems) {
            if (recursion) {
                if (!excludePaths.contains(f.getPath())) {
                    filteredFiles.add(f);
                }
            }
            // always show sub-folders when user browse a folder to add
            else if (f.isDirectory() || !excludePaths.contains(f.getPath())) {
                filteredFiles.add(f);
            }
        }
        localItems = filteredFiles.toArray(new File[filteredFiles.size()]);

        return Status.OK_STATUS;
    }

    private File[] listAllFiles(final String path) {
        fileList = new ArrayList<File>();
        fileList.clear();

        walk(path);
        return fileList.toArray(new File[fileList.size()]);
    }

    private void walk(final String path) {
        final File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (isSymlink(file)) {
            fileList.add(file);
            return;
        }

        final File[] list = file.listFiles(filesFilter);
        if (list == null) {
            return;
        }

        for (final File f : list) {
            if (f.isDirectory() && !isSymlink(f)) {
                walk(f.getAbsolutePath());
            } else {
                fileList.add(f);
            }
        }
    }

    private boolean isSymlink(final File file) {
        final FileSystemAttributes attr = FileSystemUtils.getInstance().getAttributes(file);
        if (attr.isSymbolicLink()) {
            return true;
        } else {
            return false;
        }
    }

    public File[] getLocalItems() {
        return localItems;
    }

    private TFSRepository getRepository() {
        return TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();
    }

    @Override
    public String getName() {
        return ""; //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("AddFilesControl.FilterItemsExistOnServerCommandErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("AddFilesControl.FilterItemsExistOnServerCommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    private class AddFilesFilter implements FileFilter {
        @Override
        public boolean accept(final File file) {
            if (BaselineFolder.isPotentialBaselineFolderName(file.getName())) {
                return false;
            }

            if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                final FileSystemAttributes attributes;

                try {
                    attributes = FileSystemUtils.getInstance().getAttributes(file.getAbsolutePath());
                } catch (final Exception e) {
                    log.debug(
                        MessageFormat.format(
                            Messages.getString("FilterItemsExistOnServerCommand.ErrorGetFileAttributeTextFormat"), //$NON-NLS-1$
                            file.getAbsolutePath()));

                    /* Page file / hibernation file on Windows */
                    return false;
                }

                if (attributes.isHidden() || attributes.isSystem()) {
                    return false;
                }
            }

            return true;
        }
    }
}
