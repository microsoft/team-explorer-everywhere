// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import com.microsoft.tfs.client.clc.commands.Command;

/**
 *         A script command is like a normal command, but is never shown in the
 *         general help.
 */
public abstract class ScriptCommand extends Command {
    public ScriptCommand() {
        super();
    }
}
