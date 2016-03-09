// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs.version;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;

import ms.tfs.versioncontrol.clientservices._03._WorkspaceVersionSpec;

/**
 * Describes a version of an item as it exists in a given {@link Workspace}.
 *
 * @threadsafety immutable
 * @since TEE-SDK-10.1
 */
public final class WorkspaceVersionSpec extends VersionSpec {
    /**
     * The single character identifier for the type of VersionSpec implemented
     * by this class.
     */
    protected static final char IDENTIFIER = 'W';

    public WorkspaceVersionSpec(final _WorkspaceVersionSpec spec) {
        super(spec);

        final String displayName = spec.getOwnerDisp();
        if (displayName == null || displayName.length() == 0) {
            spec.setOwnerDisp(spec.getOwner());
        }
    }

    public WorkspaceVersionSpec(final WorkspaceSpec spec) {
        this(spec.getName(), spec.getOwner(), spec.getOwner());
    }

    public WorkspaceVersionSpec(final Workspace workspace) {
        this(workspace.getName(), workspace.getOwnerName(), workspace.getOwnerDisplayName());
    }

    public WorkspaceVersionSpec(final String name, final String owner, final String ownerDisplayName) {
        this(new _WorkspaceVersionSpec(name, owner, owner, ownerDisplayName));
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.soapextensions.IVersionSpec#toString()
     */
    @Override
    public String toString() {
        /*
         * Returns something like "Wworkspacename;domain\\user"
         */
        return IDENTIFIER
            + new WorkspaceSpec(
                ((_WorkspaceVersionSpec) getWebServiceObject()).getName(),
                ((_WorkspaceVersionSpec) getWebServiceObject()).getOwner()).toString();
    }

    public String getName() {
        return ((_WorkspaceVersionSpec) getWebServiceObject()).getName();
    }

    public String getOwner() {
        return ((_WorkspaceVersionSpec) getWebServiceObject()).getOwner();
    }

    public String getOwnerDisplayName() {
        return ((_WorkspaceVersionSpec) getWebServiceObject()).getOwnerDisp();
    }
}
