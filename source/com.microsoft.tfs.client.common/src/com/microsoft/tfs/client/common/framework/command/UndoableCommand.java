// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

public interface UndoableCommand {
    /**
     * Multi command helpers and executors may need to roll back previously
     * executed commands when one further down the execution chain fails.
     * Subclasses may override to provide rollback functionality.
     *
     * @throws Exception
     */
    public IStatus rollback(IProgressMonitor progressMonitor) throws Exception;
}
