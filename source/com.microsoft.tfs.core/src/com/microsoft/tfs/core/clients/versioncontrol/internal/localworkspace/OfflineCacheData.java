// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.LocalMetadataTable;
import com.microsoft.tfs.util.GUID;

public class OfflineCacheData {
    private final Map<Class, WeakReference<LocalMetadataTable>> metadataTableCache;
    private final Map<Class, LocalMetadataTable> strongMetadataTableCache;
    private boolean stronglyRootMetadataTables;

    private GUID lastServerPendingChangeSignature = GUID.EMPTY;
    private Calendar lastReconcileTime = Calendar.getInstance();

    public OfflineCacheData() {
        this.metadataTableCache = new HashMap<Class, WeakReference<LocalMetadataTable>>();
        this.strongMetadataTableCache = new HashMap<Class, LocalMetadataTable>();
    }

    public void cacheMetadataTable(final LocalMetadataTable toCache) {
        synchronized (this) {
            final Class c = toCache.getClass();

            metadataTableCache.put(c, new WeakReference<LocalMetadataTable>(toCache));

            if (stronglyRootMetadataTables) {
                strongMetadataTableCache.put(c, toCache);
            }
        }
    }

    public boolean isStronglyRootMetadataTables() {
        return stronglyRootMetadataTables;
    }

    public void setStronglyRootMetadataTables(final boolean value) {
        synchronized (this) {
            if (!value && stronglyRootMetadataTables) {
                strongMetadataTableCache.clear();
                stronglyRootMetadataTables = false;
            } else if (value && !stronglyRootMetadataTables) {
                for (final Class key : metadataTableCache.keySet()) {
                    final LocalMetadataTable table = metadataTableCache.get(key).get();
                    if (null != table) {
                        strongMetadataTableCache.put(key, table);
                    }
                }
                stronglyRootMetadataTables = true;
            }
        }
    }

    public LocalWorkspaceProperties getCachedLocalWorkspaceProperties() {
        return (LocalWorkspaceProperties) getCachedMetadataTable(LocalWorkspaceProperties.class);
    }

    public WorkspaceVersionTable getCachedWorkspaceVersionTable() {
        return (WorkspaceVersionTable) getCachedMetadataTable(WorkspaceVersionTable.class);
    }

    public LocalPendingChangesTable getCachedLocalPendingChangesTable() {
        return (LocalPendingChangesTable) getCachedMetadataTable(LocalPendingChangesTable.class);
    }

    public LocalMetadataTable getCachedMetadataTable(final Class<? extends LocalMetadataTable> c) {
        WeakReference<LocalMetadataTable> weakReference;

        synchronized (this) {
            if (stronglyRootMetadataTables) {
                final LocalMetadataTable table = strongMetadataTableCache.get(c);
                if (table != null) {
                    if (table.isEligibleForCachedLoad()) {
                        return table;
                    } else {
                        strongMetadataTableCache.remove(c);
                        metadataTableCache.remove(c);
                    }
                }
            } else if (metadataTableCache.containsKey(c)) {
                weakReference = metadataTableCache.get(c);
                final LocalMetadataTable table = weakReference.get();
                if (null != table && table.isEligibleForCachedLoad()) {
                    return table;
                } else {
                    metadataTableCache.remove(c);
                }
            }
        }

        return null;
    }

    public GUID getLastServerPendingChangeSignature() {
        return lastServerPendingChangeSignature;
    }

    public void setLastServerPendingChangeSignature(final GUID value) {
        lastServerPendingChangeSignature = value;
    }

    public Calendar getLastReconcileTime() {
        return lastReconcileTime;
    }

    public void setLastReconcileTime(final Calendar value) {
        lastReconcileTime = value;
    }
}
