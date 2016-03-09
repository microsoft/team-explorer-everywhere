// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.events;

import com.microsoft.tfs.core.clients.CoreClientEvent;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Mapping;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PolicyOverrideInfo;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

/**
 * Event fired when a branch object has been successfully created or updated.
 *
 * @since TEE-SDK-10.1
 */
public class BranchCommittedEvent extends CoreClientEvent {
    private final String sourcePath;
    private final String targetPath;

    public BranchCommittedEvent(
        final EventSource source,
        final String sourcePath,
        final String targetPath,
        final VersionSpec version,
        final String owner,
        final String comment,
        final CheckinNote checkinNote,
        final PolicyOverrideInfo policyOverrideInfo,
        final Mapping[] mappings,
        final int changesetID) {
        super(source);

        Check.notNull(sourcePath, "sourcePath"); //$NON-NLS-1$
        Check.notNull(targetPath, "targetPath"); //$NON-NLS-1$
        Check.notNull(version, "version"); //$NON-NLS-1$

        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getTargetPath() {
        return targetPath;
    }
}
