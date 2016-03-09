// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit;

import com.microsoft.tfs.client.clc.CommandsMap;
import com.microsoft.tfs.client.clc.commands.shared.CommandEULA;
import com.microsoft.tfs.client.clc.commands.shared.CommandHelp;
import com.microsoft.tfs.client.clc.options.shared.OptionCollection;
import com.microsoft.tfs.client.clc.options.shared.OptionContinueOnError;
import com.microsoft.tfs.client.clc.options.shared.OptionExitCode;
import com.microsoft.tfs.client.clc.options.shared.OptionHelp;
import com.microsoft.tfs.client.clc.options.shared.OptionLogin;
import com.microsoft.tfs.client.clc.options.shared.OptionOutputSeparator;
import com.microsoft.tfs.client.clc.options.shared.OptionServer;
import com.microsoft.tfs.client.clc.vc.options.OptionNoPrompt;
import com.microsoft.tfs.client.clc.wit.commands.CommandCreate;
import com.microsoft.tfs.client.clc.wit.commands.CommandEdit;
import com.microsoft.tfs.client.clc.wit.commands.CommandGet;
import com.microsoft.tfs.client.clc.wit.commands.CommandGetFile;
import com.microsoft.tfs.client.clc.wit.commands.CommandInfo;
import com.microsoft.tfs.client.clc.wit.commands.CommandQuery;

/**
 *         Contains all known commands.
 *
 *         This class is thread-safe.
 */
public class WorkItemCommands extends CommandsMap {
    public WorkItemCommands() {
        /*
         * IMPORTANT: The first string in the names array is the command's
         * canonical name. Put other aliases or abbreviations after this name,
         * because it will be shown in help text.
         *
         * These are kept in alphabetical order of class name for ease of
         * maintenance.
         */
        putCommand(CommandCreate.class, new String[] {
            "create" //$NON-NLS-1$
        });
        putCommand(CommandEdit.class, new String[] {
            "edit" //$NON-NLS-1$
        });
        putCommand(CommandEULA.class, new String[] {
            "eula" //$NON-NLS-1$
        });
        putCommand(CommandGet.class, new String[] {
            "get" //$NON-NLS-1$
        });
        putCommand(CommandGetFile.class, new String[] {
            "getfile" //$NON-NLS-1$
        });
        putCommand(CommandHelp.class, new String[] {
            "help" //$NON-NLS-1$
        });
        putCommand(CommandInfo.class, new String[] {
            "info" //$NON-NLS-1$
        });
        putCommand(CommandQuery.class, new String[] {
            "query" //$NON-NLS-1$
        });
    }

    /**
     * These options are valid for any command.
     *
     * @return an array of options that are valid for all commands.
     */
    @Override
    public Class[] getGlobalOptions() {
        return new Class[] {
            OptionContinueOnError.class,
            OptionOutputSeparator.class,
            OptionLogin.class,
            OptionHelp.class,
            OptionCollection.class,
            OptionServer.class,
            OptionNoPrompt.class,
            OptionExitCode.class
        };
    }
}
