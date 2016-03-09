// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.offline;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.PendChangesOptions;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.util.FileEncoding;

/**
 * <p>
 * Transforms a given set of {@link OfflineChange}s into pending changes in a
 * TFS workspace.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public class OfflinePender {
    private final Workspace workspace;
    private final OfflineChange[] offlineChanges;

    public OfflinePender(final Workspace workspace, final OfflineChange[] offlineChanges) {
        this.workspace = workspace;
        this.offlineChanges = offlineChanges;
    }

    /**
     * Pends the changes to the server.
     *
     * @return the number of changes which could NOT be pended
     */
    public final int pendChanges() {
        int failures = 0;

        final List undos = new ArrayList();
        final List adds = new ArrayList();
        final List edits = new ArrayList();
        final List deletes = new ArrayList();

        for (int i = 0; i < offlineChanges.length; i++) {
            final OfflineChange change = offlineChanges[i];

            final String sourcePath = change.getSourceLocalPath();
            final String targetPath = change.getLocalPath();

            if (change.hasChangeType(OfflineChangeType.UNDO)) {
                undos.add(new ItemSpec(targetPath, RecursionType.NONE));
            }
            if (change.hasChangeType(OfflineChangeType.ADD)) {
                adds.add(sourcePath);
            }
            if (change.hasChangeType(OfflineChangeType.EDIT)) {
                edits.add(sourcePath);
            }
            if (change.hasChangeType(OfflineChangeType.DELETE)) {
                deletes.add(sourcePath);
            }
        }

        if (undos.size() > 0) {
            failures += pendUndos(undos);
        }

        if (adds.size() > 0) {
            failures += pendAdds(adds);
        }

        if (edits.size() > 0) {
            failures += pendEdits(edits);
        }

        if (deletes.size() > 0) {
            failures += pendDeletes(deletes);
        }

        // now pend properties separately
        if (workspace.getClient().getServiceLevel().getValue() >= WebServiceLevel.TFS_2012.getValue()) {
            for (int i = 0; i < offlineChanges.length; i++) {
                final OfflineChange change = offlineChanges[i];
                final String sourcePath = change.getSourceLocalPath();
                if (change.hasPropertyChange()) {
                    failures += pendProperty(sourcePath, change.getPropertyValue());
                }
            }
        }

        return failures;
    }

    private int pendUndos(final List undos) {
        /*
         * We need to pass the Overwrite GetOption. This suppresses local
         * TargetWritable conflicts. TODO: revisit this in core, as it seems
         * unnecessary.
         */
        final ItemSpec[] items = (ItemSpec[]) undos.toArray(new ItemSpec[undos.size()]);
        final int undone = workspace.undo(items, GetOptions.combine(new GetOptions[] {
            GetOptions.NO_DISK_UPDATE,
            GetOptions.OVERWRITE
        }));

        return (items.length - undone);
    }

    private int pendAdds(final List adds) {
        final String[] paths = (String[]) adds.toArray(new String[adds.size()]);
        final int pended = workspace.pendAdd(
            paths,
            false,
            FileEncoding.AUTOMATICALLY_DETECT,
            LockLevel.UNCHANGED,
            GetOptions.NONE,
            PendChangesOptions.NONE);

        return (paths.length - pended);
    }

    private int pendEdits(final List edits) {
        final String[] paths = (String[]) edits.toArray(new String[edits.size()]);

        /*
         * Note that we force checkout of local version here -- we would not
         * want the server to offer us the latest versionever in this case.
         */
        final int pended = workspace.pendEdit(
            paths,
            RecursionType.NONE,
            LockLevel.UNCHANGED,
            null,
            GetOptions.NONE,
            PendChangesOptions.FORCE_CHECK_OUT_LOCAL_VERSION);

        return (paths.length - pended);
    }

    private int pendDeletes(final List deletes) {
        final String[] paths = (String[]) deletes.toArray(new String[deletes.size()]);

        final int pended = workspace.pendDelete(
            paths,
            RecursionType.NONE,
            LockLevel.UNCHANGED,
            GetOptions.NO_DISK_UPDATE,
            PendChangesOptions.NONE);

        return (paths.length - pended);
    }

    private int pendProperty(final String path, final PropertyValue[] property) {
        final int pended = workspace.pendPropertyChange(path, property, RecursionType.NONE, LockLevel.UNCHANGED);
        return 1 - pended;
    }
}
