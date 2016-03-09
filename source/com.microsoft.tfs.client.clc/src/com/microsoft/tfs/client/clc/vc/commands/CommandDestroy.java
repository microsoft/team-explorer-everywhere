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
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionException;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.prompt.Prompt;
import com.microsoft.tfs.client.clc.prompt.QuestionResponse;
import com.microsoft.tfs.client.clc.prompt.QuestionType;
import com.microsoft.tfs.client.clc.vc.QualifiedItem;
import com.microsoft.tfs.client.clc.vc.options.OptionKeepHistory;
import com.microsoft.tfs.client.clc.vc.options.OptionNoPrompt;
import com.microsoft.tfs.client.clc.vc.options.OptionPreview;
import com.microsoft.tfs.client.clc.vc.options.OptionSilent;
import com.microsoft.tfs.client.clc.vc.options.OptionStartCleanup;
import com.microsoft.tfs.client.clc.vc.options.OptionStopAt;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.DestroyFlags;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.WorkspaceVersionSpec;

public class CommandDestroy extends Command {

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandDestroy.HelpText1") //$NON-NLS-1$
        };
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        return new AcceptedOptionSet[] {
            new AcceptedOptionSet(new Class[] {
                OptionKeepHistory.class,
                OptionStartCleanup.class,
                OptionPreview.class,
                OptionSilent.class,
                OptionStopAt.class,
                OptionNoPrompt.class
            }, "itemspec1[;versionspec][;XdeletionID] [itemspec2...itemspecN]") //$NON-NLS-1$
        };
    }

    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException, LicenseException {

        if (getFreeArguments().length == 0) {
            final String messageFormat = Messages.getString("CommandDestroy.DestroyRequiresOneOrMoreItemsFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        final QualifiedItem[] qualifiedItems = parseQualifiedItems(null, false, 0);

        if (qualifiedItems.length == 0) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandDestroy.SpecifyOneOrMoreItemsToDestroy")); //$NON-NLS-1$
        }

        if ((findOptionType(OptionStopAt.class) != null) && (findOptionType(OptionKeepHistory.class) == null)) {
            throw new InvalidOptionException(Messages.getString("CommandDestroy.KeepHistoryRequiredWithStopAt")); //$NON-NLS-1$
        }

        DestroyFlags destroyFlags = DestroyFlags.NONE;

        if (findOptionType(OptionKeepHistory.class) != null) {
            destroyFlags = destroyFlags.combine(DestroyFlags.KEEP_HISTORY);
        }

        if (findOptionType(OptionStartCleanup.class) != null) {
            destroyFlags = destroyFlags.combine(DestroyFlags.START_CLEANUP);
        }

        if (findOptionType(OptionPreview.class) != null) {
            destroyFlags = destroyFlags.combine(DestroyFlags.PREVIEW);
        }

        if (findOptionType(OptionSilent.class) != null) {
            destroyFlags = destroyFlags.combine(DestroyFlags.SILENT);
        }

        final boolean noPrompt = findOptionType(OptionNoPrompt.class) != null;

        VersionSpec stopAt = null;

        final OptionStopAt stopAtOption = (OptionStopAt) findOptionType(OptionStopAt.class);
        if (stopAtOption != null) {
            stopAt = stopAtOption.getParsedVersionSpec();

            if (stopAt instanceof LatestVersionSpec || stopAt instanceof WorkspaceVersionSpec) {
                throw new InvalidOptionValueException(
                    Messages.getString("CommandDestroy.StopAtCannotBeLabelOrWorkspace")); //$NON-NLS-1$
            }
        }

        final TFSTeamProjectCollection connection = createConnection(true, false);
        final VersionControlClient client = connection.getVersionControlClient();
        initializeClient(client);

        /*
         * Check each affected item and prompt for confirmation.
         */
        boolean itemsDestroyed = false;
        boolean everSaidAll = false;
        final QuestionType questionType = (qualifiedItems.length == 1) ? QuestionType.YES_NO : QuestionType.YES_NO_ALL;

        for (int i = 0; i < qualifiedItems.length; i++) {
            final QualifiedItem qualifiedItem = qualifiedItems[i];

            boolean affectedChangesThisItem = false;

            /*
             * Verify the item spec is valid and not the root folder.
             */
            if (!ServerPath.isServerPath(qualifiedItem.getPath())) {
                final String messageFormat = Messages.getString("CommandDestroy.ItemMustBeServerItemFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, qualifiedItem.getPath());

                throw new InvalidFreeArgumentException(message);
            }

            if (ServerPath.equals(ServerPath.ROOT, qualifiedItem.getPath())) {
                throw new InvalidFreeArgumentException(Messages.getString("CommandDestroy.CannotDestroyRoot")); //$NON-NLS-1$
            }

            /*
             * Decide on a version spec.
             */
            final ItemSpec itemSpec = qualifiedItem.toItemSpec(RecursionType.FULL);
            VersionSpec versionSpec;

            if (qualifiedItem.getVersions() != null && qualifiedItem.getVersions().length == 1) {
                versionSpec = qualifiedItem.getVersions()[0];
            } else {
                versionSpec = LatestVersionSpec.INSTANCE;
            }

            if (!destroyFlags.contains(DestroyFlags.KEEP_HISTORY)) {
                /*
                 * Note from the Visual Studio code:
                 *
                 * When the user is previewing, or the user is and hasn't
                 * answered a prompt by choosing all, then lookup the affected
                 * shelved and pending changes by the destroy. The reason we
                 * look this data up for non-preview but interactive case is
                 * that the code uses a different resource further down in the
                 * code path if affected changes are discovered.
                 */
                final List<PendingSet> affectedPendingChanges = new ArrayList<PendingSet>();
                final List<PendingSet> affectedShelvedChanges = new ArrayList<PendingSet>();

                if (destroyFlags.contains(DestroyFlags.PREVIEW) || (everSaidAll == false && noPrompt == false)) {
                    /*
                     * Destroy can calculate affected shelvesets and pending
                     * changes for us.
                     */
                    client.destroy(
                        itemSpec,
                        versionSpec,
                        stopAt,
                        destroyFlags.combine(DestroyFlags.AFFECTED_CHANGES).combine(DestroyFlags.PREVIEW),
                        affectedPendingChanges,
                        affectedShelvedChanges);

                    affectedChangesThisItem = (affectedPendingChanges.size() > 0 || affectedShelvedChanges.size() > 0);
                }

                if (destroyFlags.contains(DestroyFlags.PREVIEW)) {
                    if (affectedPendingChanges.size() > 0) {
                        final String messageFormat =
                            Messages.getString("CommandDestroy.FollowingPendingChangesWillBeDeletedFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, itemSpec.getItem());

                        getDisplay().printLine(message);
                        getDisplay().printLine(""); //$NON-NLS-1$
                        CommandStatus.printBrief(affectedPendingChanges, client, getDisplay(), true);
                        getDisplay().printLine(""); //$NON-NLS-1$
                    }

                    if (affectedShelvedChanges.size() > 0) {
                        final String messageFormat =
                            Messages.getString("CommandDestroy.FollowingShelvedChangesWillBeDeletedFormat"); //$NON-NLS-1$
                        final String message = MessageFormat.format(messageFormat, itemSpec.getItem());

                        getDisplay().printLine(message);
                        getDisplay().printLine(""); //$NON-NLS-1$
                        CommandStatus.printBrief(affectedShelvedChanges, client, getDisplay(), true);
                        getDisplay().printLine(""); //$NON-NLS-1$
                    }
                }
            }

            /*
             * If the user hasn't selected All at a prompt, and we're
             * interactive, and we're not in preview mode, then ask the prompt
             * question.
             */
            if (everSaidAll == false && noPrompt == false && destroyFlags.contains(DestroyFlags.PREVIEW) == false) {
                QuestionResponse response;

                if (affectedChangesThisItem) {
                    response = Prompt.askQuestion(
                        getDisplay(),
                        getInput(),
                        questionType,
                        MessageFormat.format(
                            Messages.getString("CommandDestroy.DestroyAffectedQuestionYNAFormat"), //$NON-NLS-1$
                            OptionsMap.getPreferredOptionPrefix(),
                            qualifiedItem.getPath()));
                } else {
                    response = Prompt.askQuestion(
                        getDisplay(),
                        getInput(),
                        questionType,
                        MessageFormat.format(
                            Messages.getString("CommandDestroy.DestroyQuestionYNAFormat"), //$NON-NLS-1$
                            qualifiedItem.getPath()));
                }

                if (response == QuestionResponse.ALL) {
                    everSaidAll = true;
                } else if (response == QuestionResponse.NO) {
                    setExitCode(ExitCode.PARTIAL_SUCCESS);
                    continue;
                }
            }

            final Item[] destroyedItems = client.destroy(itemSpec, versionSpec, stopAt, destroyFlags);

            if (destroyedItems.length > 0) {
                itemsDestroyed = true;
            }
        }

        if (destroyFlags.contains(DestroyFlags.SILENT) == false
            && itemsDestroyed == false
            && getExitCode() != ExitCode.PARTIAL_SUCCESS) {
            getDisplay().printLine(Messages.getString("CommandDestroy.NoItemsToDestroy")); //$NON-NLS-1$
        }
    }
}
