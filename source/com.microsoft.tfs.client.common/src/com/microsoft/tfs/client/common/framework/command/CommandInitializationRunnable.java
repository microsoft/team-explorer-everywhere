// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.command;

import org.eclipse.core.runtime.IProgressMonitor;

public interface CommandInitializationRunnable {
    public void initialize(IProgressMonitor progressMonitor) throws Exception;

    public void complete(IProgressMonitor progressMonitor);
}
