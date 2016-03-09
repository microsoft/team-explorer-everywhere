// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.events.ConflictResolvedEvent;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.versioncontrol.events.NonFatalErrorEvent;
import com.microsoft.tfs.core.clients.versioncontrol.internal.conflict.ConflictResolveErrorHandler;
import com.microsoft.tfs.core.clients.versioncontrol.internal.conflict.ConflictResolvedHandler;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetOperation;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

public class ResolveLocalConflictHandler implements ConflictResolvedHandler, ConflictResolveErrorHandler {
    private final VersionControlClient client;
    private final Workspace workspace;

    private final List<GetRequest> getRequests = new ArrayList<GetRequest>();

    private ChangePendedFlags flags;

    public ResolveLocalConflictHandler(final VersionControlClient client, final Workspace workspace) {
        this.client = client;
        this.workspace = workspace;
    }

    public List<GetRequest> getGetRequests() {
        return getRequests;
    }

    public ChangePendedFlags getFlags() {
        return flags;
    }

    @Override
    public void conflictResolved(
        final Conflict conflict,
        final GetOperation[] getOps,
        final GetOperation[] undoOps,
        final Conflict[] resolvedConflicts,
        final ChangePendedFlags changePendedFlags) {
        conflict.setResolved(true);

        client.getEventEngine().fireConflictResolved(
            new ConflictResolvedEvent(EventSource.newFromHere(), workspace, conflict, changePendedFlags));

        /*
         * We prefer to retry the get using the target local item, but when that
         * is null we use the source path (deletes have null targets, for
         * example). This is because the SourceIsWritable also includes the case
         * where the target is remapped, so we're really trying to get the
         * target if it's non-null.
         */
        String path = conflict.getTargetLocalItem();
        if (path == null) {
            path = conflict.getSourceLocalItem();
        }

        // If TheirVersion is non-zero, use it. Otherwise, ask for the earliest
        // version,
        // which will either delete the item or complete an unfinished unshelve.
        VersionSpec version;
        if (conflict.getTheirVersion() != 0) {
            version = new ChangesetVersionSpec(conflict.getTheirVersion());
        } else {
            version = new ChangesetVersionSpec(1);
        }

        getRequests.add(new GetRequest(new ItemSpec(path, RecursionType.NONE), version));
    }

    @Override
    public void conflictResolveError(final Conflict conflict, final Exception exception) {
        client.getEventEngine().fireNonFatalError(
            new NonFatalErrorEvent(EventSource.newFromHere(), workspace, exception));
    }
}