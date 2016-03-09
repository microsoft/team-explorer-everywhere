// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.actions.sync;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;

import com.microsoft.tfs.client.common.framework.resources.ResourceType;
import com.microsoft.tfs.client.common.ui.compare.TFSItemContentComparator;
import com.microsoft.tfs.client.common.ui.compare.UserPreferenceExternalCompareHandler;
import com.microsoft.tfs.client.common.ui.framework.compare.Compare;
import com.microsoft.tfs.client.common.ui.framework.compare.CompareUtils;
import com.microsoft.tfs.client.common.ui.prefs.ExternalToolPreferenceKey;
import com.microsoft.tfs.client.eclipse.resource.PluginResourceFilters;
import com.microsoft.tfs.client.eclipse.sync.SynchronizeSubscriber;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.client.eclipse.ui.actions.ActionHelpers;
import com.microsoft.tfs.client.eclipse.ui.actions.AdaptedSelectionInfo;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.core.externaltools.ExternalToolset;
import com.microsoft.tfs.core.util.MementoRepository;
import com.microsoft.tfs.util.temp.TempStorageService;

public class ExternalCompareAction extends SynchronizeAction {
    private static final Log log = LogFactory.getLog(ExternalCompareAction.class);

    public ExternalCompareAction(final Shell shell) {
        super(shell);

        setText(Messages.getString("ExternalCompareAction.ActionText")); //$NON-NLS-1$
    }

    protected ExternalTool getExternalToolForSelection() {
        final IStructuredSelection selection = getStructuredSelection();

        if (selection.size() != 1) {
            return null;
        }

        final IResource element = (IResource) selection.getFirstElement();

        final ExternalToolset compareToolset = ExternalToolset.loadFromMemento(
            new MementoRepository(DefaultPersistenceStoreProvider.INSTANCE.getConfigurationPersistenceStore()).load(
                ExternalToolPreferenceKey.COMPARE_KEY));

        return compareToolset.findTool(element.getName());
    }

    public boolean hasExternalToolForSelection() {
        return getExternalToolForSelection() != null;
    }

    @Override
    public void addToContextMenu(final IMenuManager manager, final IResource[] selected) {
        super.addToContextMenu(manager, selected);

        manager.add(this);

        setEnabled(
            ActionHelpers.filterAcceptsAnyResource(selected, PluginResourceFilters.IN_REPOSITORY_FILTER)
                && hasExternalToolForSelection());
    }

    @Override
    public void run() {
        final AdaptedSelectionInfo selectionInfo = ActionHelpers.adaptSelectionToStandardResources(
            getStructuredSelection(),
            PluginResourceFilters.IN_REPOSITORY_FILTER,
            false);

        if (ActionHelpers.ensureNonZeroResourceCountAndSingleRepository(selectionInfo, getShell()) == false) {
            return;
        }

        final IResource localResource = selectionInfo.getResources()[0];

        String localFile = localResource.exists() ? localResource.getLocation().toOSString() : null;
        String remoteTempFile = null;

        /*
         * Open a progress dialog because we'll be fetching content from the
         * server.
         */
        final ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog(getShell());
        monitorDialog.open();

        final IProgressMonitor progressMonitor = monitorDialog.getProgressMonitor();
        progressMonitor.beginTask(Messages.getString("ExternalCompareAction.ProgressText"), 100); //$NON-NLS-1$

        try {
            /*
             * If the local file doesn't exist (due to deletion or incoming
             * addition, etc) then we need to make a dummy blank file for it so
             * that the external compare tool doesn't choke.
             */
            if (localFile == null) {
                try {
                    final File tempDir = TempStorageService.getInstance().createTempDirectory();

                    localFile = File.createTempFile(
                        "LocalNonexistant", //$NON-NLS-1$
                        "." + localResource.getFileExtension(), //$NON-NLS-1$
                        tempDir).getAbsolutePath();
                } catch (final IOException e) {
                    log.error("Error creating an empty local file as substitute for missing resource", e); //$NON-NLS-1$
                    // let the localFile == null test show errors
                }
            }

            /*
             * Get the remote file as a temp file.
             */
            try {
                final SyncInfo syncInfo = SynchronizeSubscriber.getInstance().getSyncInfo(localResource);

                if (syncInfo != null) {
                    final IResourceVariant variant = syncInfo.getRemote();

                    // variant is non-null normally...
                    if (variant != null) {
                        final SubProgressMonitor getMonitor = new SubProgressMonitor(progressMonitor, 75);
                        final IStorage storage = variant.getStorage(getMonitor);

                        // if there's a path to the storage, then the remote
                        // item
                        // was found normally
                        if (storage != null && storage.getFullPath() != null) {
                            remoteTempFile = storage.getFullPath().toOSString();
                        }

                        // otherwise, the remote item is a deletion or a rename,
                        // create a blank temp file for the external compare
                        // tool
                        else {
                            final File tempDir = TempStorageService.getInstance().createTempDirectory();

                            remoteTempFile = File.createTempFile(
                                "RemoteNonexistant", //$NON-NLS-1$
                                "." + localResource.getFileExtension(), //$NON-NLS-1$
                                tempDir).getAbsolutePath();
                        }
                    }
                }
            } catch (final Exception e) {
                log.error("Error getting the remote file contents", e); //$NON-NLS-1$
                // suppress, fall-through to remoteFile == null test
            }
        } finally {
            progressMonitor.done();
            monitorDialog.close();
        }

        // SynchronizeSusbcriber.getSyncInfo().getRemote().getStorage()
        // should always return (even when in-sync, it will hit the
        // server intelligently.)
        if (remoteTempFile == null) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("ExternalCompareAction.CompareErrorDialogTitle"), //$NON-NLS-1$
                Messages.getString("ExternalCompareAction.CompareErrorDialogText")); //$NON-NLS-1$
            return;
        }

        final Compare compare = new Compare();

        compare.setModified(CompareUtils.createCompareElementForLocalPath(localFile, ResourceType.FILE));

        compare.setOriginal(CompareUtils.createCompareElementForLocalPath(remoteTempFile, ResourceType.FILE));

        compare.addComparator(TFSItemContentComparator.INSTANCE);

        compare.setExternalCompareHandler(new UserPreferenceExternalCompareHandler(getShell()));
        compare.open();
    }
}
