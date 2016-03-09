// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.file;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.repository.RepositoryManagerEvent;
import com.microsoft.tfs.client.common.repository.RepositoryManagerListener;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSFolder;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;

public class FileControlPendingChangesCache {
    /**
     * Updated by event handlers for {@link RepositoryManagerEvent}.
     */
    private TFSRepository repository;

    private final Map<TFSFolder, PendingSet[]> mapFoldersToPendingSetArrays = new HashMap<TFSFolder, PendingSet[]>();
    private TFSFolder currentFolder;
    private PendingSet[] currentFolderPendingSets;

    private static final Log log = LogFactory.getLog(FileControlPendingChangesCache.class);

    public FileControlPendingChangesCache() {
        repository =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository();

        /*
         * TODO: Fix this event handler. It only works when there are zero or
         * one repositories in the manager, not two or more.
         */
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().addListener(
            new RepositoryManagerListener() {
                @Override
                public void onRepositoryRemoved(final RepositoryManagerEvent event) {
                    repository = null;
                }

                @Override
                public void onRepositoryAdded(final RepositoryManagerEvent event) {
                    repository = event.getRepository();
                }

                @Override
                public void onDefaultRepositoryChanged(final RepositoryManagerEvent event) {
                    repository = event.getRepository();
                }
            });
    }

    public void clear() {
        mapFoldersToPendingSetArrays.clear();
    }

    public void setCurrentFolder(final TFSFolder folder, final boolean includeDeletedItems) {
        /*
         * If there is no repository yet (a control is shown before connection,
         * or we got disconnected), do not update the cache.
         */
        if (repository == null) {
            return;
        }

        currentFolder = folder;
        if (!mapFoldersToPendingSetArrays.containsKey(currentFolder)) {
            final Set<TFSItem> children = currentFolder.getChildren(includeDeletedItems);
            boolean needToQueryPendingSets = false;
            final Iterator<TFSItem> it = children.iterator();
            while (!needToQueryPendingSets && it.hasNext()) {
                final TFSItem item = it.next();
                needToQueryPendingSets =
                    item.getExtendedItem() == null || item.getExtendedItem().hasOtherPendingChange();
            }

            if (needToQueryPendingSets) {
                try {
                    final PendingSet[] pendingSets = repository.getWorkspace().queryPendingSets(new String[] {
                        folder.getFullPath()
                    }, RecursionType.ONE_LEVEL, null, null, false);
                    mapFoldersToPendingSetArrays.put(currentFolder, pendingSets);
                } catch (final Exception e) {
                    final String messageFormat = "Error refreshing pending changes for {0}"; //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, folder.getFullPath());

                    log.error(message, e);
                    mapFoldersToPendingSetArrays.put(currentFolder, null);
                }
            } else {
                mapFoldersToPendingSetArrays.put(currentFolder, null);
            }
        }
        currentFolderPendingSets = mapFoldersToPendingSetArrays.get(currentFolder);
    }

    public PendingChange[] getChangesForItem(final TFSItem item) {
        final List<PendingChange> pendingChanges = new ArrayList<PendingChange>();

        if (currentFolderPendingSets != null) {
            for (int i = 0; i < currentFolderPendingSets.length; i++) {
                final PendingSet currentSet = currentFolderPendingSets[i];
                final PendingChange[] changes = currentSet.getPendingChanges();
                for (int j = 0; j < changes.length; j++) {
                    if (item.changeApplies(changes[j])) {
                        pendingChanges.add(changes[j]);
                    }
                }
            }
        }

        return pendingChanges.toArray(new PendingChange[pendingChanges.size()]);
    }

    public PendingSet[] getPendingSetsForItem(final TFSItem item) {
        final List<PendingSet> pendingSets = new ArrayList<PendingSet>();

        if (currentFolderPendingSets != null) {
            for (int i = 0; i < currentFolderPendingSets.length; i++) {
                final PendingSet currentSet = currentFolderPendingSets[i];
                final PendingChange[] changes = currentSet.getPendingChanges();
                boolean matched = false;
                int j = 0;
                while (!matched && j < changes.length) {
                    if (item.changeApplies(changes[j])) {
                        pendingSets.add(currentSet);
                        matched = true;
                    }
                    ++j;
                }
            }
        }

        return pendingSets.toArray(new PendingSet[pendingSets.size()]);
    }
}
