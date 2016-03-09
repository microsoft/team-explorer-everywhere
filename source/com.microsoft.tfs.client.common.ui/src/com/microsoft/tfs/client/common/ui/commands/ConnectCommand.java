// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.core.TFSConnection;

/**
 * Base connect command interface.
 *
 * @threadsafety unknown
 */
public interface ConnectCommand extends ICommand {
    TFSConnection getConnection();
}
