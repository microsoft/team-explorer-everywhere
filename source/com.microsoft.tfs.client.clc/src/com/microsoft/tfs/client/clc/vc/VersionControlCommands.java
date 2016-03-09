// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc;

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
import com.microsoft.tfs.client.clc.vc.commands.CommandAdd;
import com.microsoft.tfs.client.clc.vc.commands.CommandBranch;
import com.microsoft.tfs.client.clc.vc.commands.CommandBranches;
import com.microsoft.tfs.client.clc.vc.commands.CommandChangeset;
import com.microsoft.tfs.client.clc.vc.commands.CommandCheckin;
import com.microsoft.tfs.client.clc.vc.commands.CommandCheckout;
import com.microsoft.tfs.client.clc.vc.commands.CommandDelete;
import com.microsoft.tfs.client.clc.vc.commands.CommandDestroy;
import com.microsoft.tfs.client.clc.vc.commands.CommandDifference;
import com.microsoft.tfs.client.clc.vc.commands.CommandDir;
import com.microsoft.tfs.client.clc.vc.commands.CommandGet;
import com.microsoft.tfs.client.clc.vc.commands.CommandGetChangeset;
import com.microsoft.tfs.client.clc.vc.commands.CommandHistory;
import com.microsoft.tfs.client.clc.vc.commands.CommandLabel;
import com.microsoft.tfs.client.clc.vc.commands.CommandLabels;
import com.microsoft.tfs.client.clc.vc.commands.CommandLock;
import com.microsoft.tfs.client.clc.vc.commands.CommandMerge;
import com.microsoft.tfs.client.clc.vc.commands.CommandMerges;
import com.microsoft.tfs.client.clc.vc.commands.CommandOnline;
import com.microsoft.tfs.client.clc.vc.commands.CommandPrint;
import com.microsoft.tfs.client.clc.vc.commands.CommandProperties;
import com.microsoft.tfs.client.clc.vc.commands.CommandProperty;
import com.microsoft.tfs.client.clc.vc.commands.CommandReconcile;
import com.microsoft.tfs.client.clc.vc.commands.CommandRename;
import com.microsoft.tfs.client.clc.vc.commands.CommandResolve;
import com.microsoft.tfs.client.clc.vc.commands.CommandResolvePath;
import com.microsoft.tfs.client.clc.vc.commands.CommandRollback;
import com.microsoft.tfs.client.clc.vc.commands.CommandShelve;
import com.microsoft.tfs.client.clc.vc.commands.CommandShelvesets;
import com.microsoft.tfs.client.clc.vc.commands.CommandStatus;
import com.microsoft.tfs.client.clc.vc.commands.CommandUndelete;
import com.microsoft.tfs.client.clc.vc.commands.CommandUndo;
import com.microsoft.tfs.client.clc.vc.commands.CommandUndoUnchanged;
import com.microsoft.tfs.client.clc.vc.commands.CommandUnlabel;
import com.microsoft.tfs.client.clc.vc.commands.CommandUnshelve;
import com.microsoft.tfs.client.clc.vc.commands.CommandWorkFold;
import com.microsoft.tfs.client.clc.vc.commands.CommandWorkspace;
import com.microsoft.tfs.client.clc.vc.commands.CommandWorkspaces;
import com.microsoft.tfs.client.clc.vc.commands.ScriptCommandChangeDirectory;
import com.microsoft.tfs.client.clc.vc.commands.ScriptCommandExit;
import com.microsoft.tfs.client.clc.vc.commands.ScriptCommandSetNoPrompt;
import com.microsoft.tfs.client.clc.vc.options.OptionNoPrompt;
import com.microsoft.tfs.client.clc.vc.options.OptionNoSummary;
import com.microsoft.tfs.client.clc.vc.options.OptionProxy;
import com.microsoft.tfs.client.clc.vc.options.OptionWorkspace;

/**
 *         Contains all known commands.
 *
 *         This class is thread-safe.
 */
public class VersionControlCommands extends CommandsMap {
    public VersionControlCommands() {
        /*
         * IMPORTANT: The first string in the names array is the command's
         * canonical name. Put other aliases or abbreviations after this name,
         * because it will be shown in help text.
         *
         * These are kept in alphabetical order of class name for ease of
         * maintenance.
         */
        putCommand(CommandAdd.class, new String[] {
            "add" //$NON-NLS-1$
        });
        putCommand(CommandBranch.class, new String[] {
            "branch" //$NON-NLS-1$
        });
        putCommand(CommandBranches.class, new String[] {
            "branches" //$NON-NLS-1$
        });
        putCommand(CommandChangeset.class, new String[] {
            "changeset" //$NON-NLS-1$
        });
        putCommand(CommandCheckin.class, new String[] {
            "checkin", //$NON-NLS-1$
            "ci", //$NON-NLS-1$
            "commit", //$NON-NLS-1$
            "submit" //$NON-NLS-1$
        });
        putCommand(CommandCheckout.class, new String[] {
            "checkout", //$NON-NLS-1$
            "co", //$NON-NLS-1$
            "edit" //$NON-NLS-1$
        });
        putCommand(CommandDelete.class, new String[] {
            "delete", //$NON-NLS-1$
            "rm" //$NON-NLS-1$
        });
        putCommand(CommandDifference.class, new String[] {
            "difference", //$NON-NLS-1$
            "diff", //$NON-NLS-1$
            "compare" //$NON-NLS-1$
        });
        putCommand(CommandDestroy.class, new String[] {
            "destroy" //$NON-NLS-1$
        });
        putCommand(CommandDir.class, new String[] {
            "dir", //$NON-NLS-1$
            "ls" //$NON-NLS-1$
        });
        putCommand(CommandEULA.class, new String[] {
            "eula" //$NON-NLS-1$
        });
        putCommand(CommandGet.class, new String[] {
            "get" //$NON-NLS-1$
        });
        putCommand(CommandGetChangeset.class, new String[] {
            "getcs", //$NON-NLS-1$
            "getchangeset" //$NON-NLS-1$
        });
        putCommand(CommandHelp.class, new String[] {
            "help" //$NON-NLS-1$
        });
        putCommand(CommandHistory.class, new String[] {
            "history" //$NON-NLS-1$
        });
        putCommand(CommandProperties.class, new String[] {
            "info", //$NON-NLS-1$
            "properties", //$NON-NLS-1$
            "props" //$NON-NLS-1$
        });
        putCommand(CommandLabel.class, new String[] {
            "label" //$NON-NLS-1$
        });
        putCommand(CommandLabels.class, new String[] {
            "labels" //$NON-NLS-1$
        });
        putCommand(CommandLock.class, new String[] {
            "lock" //$NON-NLS-1$
        });
        putCommand(CommandMerge.class, new String[] {
            "merge" //$NON-NLS-1$
        });
        putCommand(CommandMerges.class, new String[] {
            "merges" //$NON-NLS-1$
        });
        putCommand(CommandOnline.class, new String[] {
            "online" //$NON-NLS-1$
        });
        putCommand(CommandPrint.class, new String[] {
            "print", //$NON-NLS-1$
            "cat" //$NON-NLS-1$
        });
        putCommand(CommandProperty.class, new String[] {
            "property" //$NON-NLS-1$
        });
        putCommand(CommandReconcile.class, new String[] {
            "reconcile", //$NON-NLS-1$
        });
        putCommand(CommandRename.class, new String[] {
            "rename", //$NON-NLS-1$
            "move", //$NON-NLS-1$
            "mv" //$NON-NLS-1$
        });
        putCommand(CommandResolve.class, new String[] {
            "resolve" //$NON-NLS-1$
        });
        putCommand(CommandResolvePath.class, new String[] {
            "resolvePath" //$NON-NLS-1$
        });
        putCommand(CommandRollback.class, new String[] {
            "rollback" //$NON-NLS-1$
        });
        putCommand(CommandShelve.class, new String[] {
            "shelve" //$NON-NLS-1$
        });
        putCommand(CommandShelvesets.class, new String[] {
            "shelvesets" //$NON-NLS-1$
        });
        putCommand(CommandStatus.class, new String[] {
            "status" //$NON-NLS-1$
        });
        putCommand(CommandUndelete.class, new String[] {
            "undelete" //$NON-NLS-1$
        });
        putCommand(CommandUndo.class, new String[] {
            "undo" //$NON-NLS-1$
        });
        putCommand(CommandUndoUnchanged.class, new String[] {
            "uu", //$NON-NLS-1$
            "undounchanged" //$NON-NLS-1$
        });
        putCommand(CommandUnlabel.class, new String[] {
            "unlabel" //$NON-NLS-1$
        });
        putCommand(CommandUnshelve.class, new String[] {
            "unshelve" //$NON-NLS-1$
        });
        putCommand(CommandWorkFold.class, new String[] {
            "workfold" //$NON-NLS-1$
        });
        putCommand(CommandWorkspace.class, new String[] {
            "workspace" //$NON-NLS-1$
        });
        putCommand(CommandWorkspaces.class, new String[] {
            "workspaces" //$NON-NLS-1$
        });
        putCommand(ScriptCommandChangeDirectory.class, new String[] {
            "cd", //$NON-NLS-1$
            "chdir" //$NON-NLS-1$
        });
        putCommand(ScriptCommandExit.class, new String[] {
            "exit", //$NON-NLS-1$
            "quit" //$NON-NLS-1$
        });
        putCommand(ScriptCommandSetNoPrompt.class, new String[] {
            "setnoprompt" //$NON-NLS-1$
        });
    }

    /**
     * These options are valid for any command.
     *
     * @return an array of options that are valid for all commands.
     */
    @Override
    public Class[] getGlobalOptions() {
        /*
         * Arrange them in the order they should display to the user in help
         * text.
         */
        return new Class[] {
            OptionContinueOnError.class,
            OptionOutputSeparator.class,
            OptionLogin.class,
            OptionHelp.class,
            OptionCollection.class,
            OptionServer.class,
            OptionWorkspace.class,
            OptionNoPrompt.class,
            OptionNoSummary.class,
            OptionProxy.class,
            OptionExitCode.class
        };
    }
}
