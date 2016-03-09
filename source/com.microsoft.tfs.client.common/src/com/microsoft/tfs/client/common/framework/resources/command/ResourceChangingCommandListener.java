// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.framework.resources.command;

public interface ResourceChangingCommandListener {
    /**
     * Called when a {@link ResourceChangingCommand} is starting.
     */
    public void commandStarted();

    /**
     * Called when a {@link ResourceChangingCommand} has finished.
     */
    public void commandFinished();
}