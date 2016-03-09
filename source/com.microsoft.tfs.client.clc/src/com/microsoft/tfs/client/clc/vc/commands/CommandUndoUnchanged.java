// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.CannotFindWorkspaceException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.prompt.Prompt;
import com.microsoft.tfs.client.clc.prompt.QuestionResponse;
import com.microsoft.tfs.client.clc.prompt.QuestionType;
import com.microsoft.tfs.client.clc.vc.options.OptionNoPrompt;
import com.microsoft.tfs.client.clc.vc.options.OptionRecursive;
import com.microsoft.tfs.client.clc.vc.options.OptionWorkspace;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.WorkspaceSpec;

public final class CommandUndoUnchanged extends Command {
    public CommandUndoUnchanged() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * Undo redundant pending changes (unchanged pending changes).
     *
     * Usage: tf uu [/noprompt] [/recursive] [<itemSpec>...]
     */
    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {

        final TFSTeamProjectCollection connection = createConnection(false, true);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        final String[] args = getFreeArguments();

        /*
         * If there was a workspace option specified, use that workspace for the
         * undo (it may be on a different computer). Otherwise, determine a
         * local cached workspace from the free arguments.
         */
        final OptionWorkspace option = (OptionWorkspace) findOptionType(OptionWorkspace.class);
        Workspace workspace = null;
        if (option != null) {
            final WorkspaceSpec spec =
                WorkspaceSpec.parse(option.getValue(), VersionControlConstants.AUTHENTICATED_USER);
            workspace = client.queryWorkspace(spec.getName(), spec.getOwner());
            if (workspace == null) {
                final String messageFormat = Messages.getString("CommandUndoUnchanged.WorkspaceNotFoundOnServerFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, spec);

                throw new CannotFindWorkspaceException(message);
            }
        } else {
            workspace = realizeCachedWorkspace(determineCachedWorkspace(args), client);
        }

        RecursionType recursion = RecursionType.ONE_LEVEL;

        if (findOptionType(OptionRecursive.class) != null) {
            recursion = RecursionType.FULL;
        }

        final boolean noPrompt = findOptionType(OptionNoPrompt.class) != null;

        getDisplay().printLine(Messages.getString("CommandUndoUnchanged.GettingPendingChanges")); //$NON-NLS-1$
        final PendingSet set;

        /*
         * If user specifies itemSpec, then undo unchanged based on item specs.
         */
        if (args.length > 0) {
            final ItemSpec[] specs = ItemSpec.fromStrings(args, recursion);
            set = workspace.getPendingChanges(specs, false);
        }
        /*
         * Otherwise get all the pending changes for the workspace and undo
         * unchanged pending changes.
         */
        else {
            set = workspace.getPendingChanges();
        }

        final PendingChange[] allchanges = (set != null) ? set.getPendingChanges() : null;

        final List<ItemSpec> specList = new ArrayList<ItemSpec>();

        if (allchanges != null) {
            getDisplay().printLine(Messages.getString("CommandUndoUnchanged.BuildUnchangedPendingChanges")); //$NON-NLS-1$
            for (final PendingChange change : allchanges) {
                if (change.isUnchanged(workspace)) {
                    specList.add(new ItemSpec(change.getServerItem(), RecursionType.NONE));
                    getDisplay().printLine(
                        MessageFormat.format(
                            Messages.getString("CommandUndoUnchanged.EditTypeChangeFormat"), //$NON-NLS-1$
                            change.getLocalItem()));
                }
            }

            if (specList.isEmpty()) {
                getDisplay().printLine(Messages.getString("CommandUndoUnchanged.AllChangesModifiedSinceCheckout")); //$NON-NLS-1$
            } else {
                if (noPrompt
                    || Prompt.askQuestion(
                        getDisplay(),
                        getInput(),
                        QuestionType.YES_NO,
                        Messages.getString("CommandUndoUnchanged.ConfirmUndoUnchanged")) == QuestionResponse.YES) //$NON-NLS-1$
                {
                    getDisplay().printLine(Messages.getString("CommandUndoUnchanged.UndoingPendingChanges")); //$NON-NLS-1$
                    final ItemSpec[] itemSpecs = specList.toArray(new ItemSpec[specList.size()]);
                    if (workspace.undo(itemSpecs) == 0) {
                        setExitCode(ExitCode.FAILURE);
                    }
                }
            }
        } else {
            getDisplay().printLine(Messages.getString("CommandUndoUnchanged.NoPendingChanges")); //$NON-NLS-1$
        }
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionRecursive.class,
            OptionNoPrompt.class
        }, "<itemSpec>..."); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandUndoUnchanged.HelpText1"), //$NON-NLS-1$
            Messages.getString("CommandUndoUnchanged.HelpText2") //$NON-NLS-1$
        };
    }

}
