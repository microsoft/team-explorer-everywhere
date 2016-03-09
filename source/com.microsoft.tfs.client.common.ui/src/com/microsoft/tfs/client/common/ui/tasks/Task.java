// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.tasks;

import org.eclipse.core.runtime.IStatus;

import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;

public interface Task {
    public void setCommandExecutor(ICommandExecutor commandExecutor);

    public ICommandExecutor getCommandExecutor();

    public IStatus run();
}
