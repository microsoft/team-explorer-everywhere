// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.exceptions;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Failure;

public class ReconcileFailedException extends VersionControlException {
    private static final long serialVersionUID = -7392366339565153133L;

    private final Failure[] failures;

    public ReconcileFailedException(final Failure[] failures) {
        super(Messages.getString("ReconcileFailedException.ReconcileFailedException")); //$NON-NLS-1$
        this.failures = failures;
    }

    public Failure[] getFailures() {
        return failures;
    }
}
