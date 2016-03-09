// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.project;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.util.Check;

final class ProjectConnectionManagerResult {
    private final TFSRepository repository;
    private final IStatus status;

    ProjectConnectionManagerResult(final TFSRepository repository) {
        this(repository, Status.OK_STATUS);
    }

    ProjectConnectionManagerResult(final IStatus status) {
        this(null, status);
    }

    ProjectConnectionManagerResult(final TFSRepository repository, final IStatus status) {
        Check.notNull(status, "status"); //$NON-NLS-1$
        Check.isTrue(repository != null || !status.isOK(), "repository != null || ! status.isOK()"); //$NON-NLS-1$

        this.repository = repository;
        this.status = status;
    }

    public TFSRepository getRepository() {
        return repository;
    }

    public IStatus getStatus() {
        return status;
    }
}