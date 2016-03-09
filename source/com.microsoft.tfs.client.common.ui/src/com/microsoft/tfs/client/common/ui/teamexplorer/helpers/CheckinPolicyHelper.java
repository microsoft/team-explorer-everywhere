// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.helpers;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.checkinpolicies.ExtensionPointPolicyLoader;
import com.microsoft.tfs.client.common.commands.vc.GetCheckinPoliciesCommand;
import com.microsoft.tfs.client.common.commands.vc.QueryItemsCommand;
import com.microsoft.tfs.client.common.commands.vc.SetCheckinPoliciesCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies.PolicyConfiguration;
import com.microsoft.tfs.client.common.ui.dialogs.vc.checkinpolicies.DefinePoliciesDialog;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.core.checkinpolicies.PolicyDefinition;
import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoader;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoaderException;
import com.microsoft.tfs.core.checkinpolicies.PolicySerializationException;
import com.microsoft.tfs.core.clients.versioncontrol.GetItemsOptions;
import com.microsoft.tfs.core.clients.versioncontrol.TeamProject;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.DeletedState;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Item;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemSet;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;

public class CheckinPolicyHelper {
    public static void showCheckinPolicyDialog(final Shell shell, final TeamExplorerContext context) {
        final String teamProjectName = context.getCurrentProjectInfo().getName();
        final Item teamProjectItem = getTeamProjectItem(shell, teamProjectName);

        if (teamProjectItem == null) {
            return;
        }

        final VersionControlClient vcClient = context.getServer().getConnection().getVersionControlClient();

        final TeamProject teamProject = new TeamProject(teamProjectItem, vcClient);

        final PolicyLoader policyLoader = new ExtensionPointPolicyLoader();
        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(shell);

        /*
         * Get the current policies.
         */

        final GetCheckinPoliciesCommand getCommand =
            new GetCheckinPoliciesCommand(vcClient, teamProject.getServerItem());

        final IStatus result = executor.execute(getCommand);

        PolicyDefinition[] policyDefinitions;
        if (result.getException() != null && result.getException() instanceof PolicySerializationException) {
            /*
             * The annotation on the server is probably corrupt. Start off with
             * an empty set.
             */
            String messageFormat = Messages.getString("CheckinPoliciesAction.SettingsCorruptFormat"); //$NON-NLS-1$
            String message = MessageFormat.format(messageFormat, teamProject.getName());

            final IStatus status =
                new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, result.getException());

            TFSCommonUIClientPlugin.getDefault().getLog().log(status);

            messageFormat = Messages.getString("CheckinPoliciesAction.SettingsCorruptDialogTextFormat"); //$NON-NLS-1$
            message = MessageFormat.format(messageFormat, teamProjectName, teamProjectName);

            if (MessageBoxHelpers.dialogConfirmPrompt(shell, null, message) == false) {
                return;
            }

            policyDefinitions = new PolicyDefinition[0];
        } else {
            policyDefinitions = getCommand.getPolicyDefinitions();
        }

        PolicyConfiguration[] policyConfigurations = createConfigurations(policyDefinitions, policyLoader);

        final DefinePoliciesDialog dialog =
            new DefinePoliciesDialog(shell, teamProject, policyConfigurations, policyLoader);

        if (IDialogConstants.OK_ID != dialog.open()) {
            return;
        }

        policyConfigurations = dialog.getPolicyConfigurations();
        policyDefinitions = new PolicyDefinition[policyConfigurations.length];
        for (int i = 0; i < policyConfigurations.length; i++) {
            policyDefinitions[i] = policyConfigurations[i].toDefinition();
        }

        final SetCheckinPoliciesCommand setCommand =
            new SetCheckinPoliciesCommand(vcClient, teamProject.getServerItem(), policyDefinitions);

        executor.execute(setCommand);

        /*
         * This is heavier than is required, but gets the job done. Ideally we
         * should just get the PolicyEvaluator we use and call
         * reloadAndEvaluate(). refreshPendingChanges() refreshes the pending
         * changes, which forces some check state changed events, which the
         * evaluator listens to, which causes a re-evaluation.
         */

        /* TODO: hack */
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getRepositoryManager().getDefaultRepository().getPendingChangeCache().refresh();
    }

    private static Item getTeamProjectItem(final Shell shell, final String teamProjectName) {
        final TFSServer defaultServer =
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().getDefaultServer();
        final VersionControlClient client = defaultServer.getConnection().getVersionControlClient();

        final QueryItemsCommand queryCommand = new QueryItemsCommand(client, new ItemSpec[] {
            new ItemSpec(ServerPath.combine(ServerPath.ROOT, teamProjectName), RecursionType.NONE)
        }, LatestVersionSpec.INSTANCE, DeletedState.NON_DELETED, ItemType.FOLDER, GetItemsOptions.UNSORTED);

        final IStatus queryStatus = UICommandExecutorFactory.newUICommandExecutor(shell).execute(queryCommand);

        if (!queryStatus.isOK()) {
            return null;
        }

        final ItemSet[] results = queryCommand.getItemSets();

        if (results == null || results.length != 1 || results[0] == null) {
            errorForUnableToFindServerPath(shell, teamProjectName);
            return null;
        }

        final Item[] items = results[0].getItems();

        if (items == null || items.length != 1 || items[0] == null) {
            errorForUnableToFindServerPath(shell, teamProjectName);
            return null;
        }

        return items[0];
    }

    private static void errorForUnableToFindServerPath(final Shell shell, final String teamProjectName) {
        final String messageFormat = Messages.getString("CheckinPoliciesAction.BadServerPathDialogTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, teamProjectName);
        MessageBoxHelpers.errorMessageBox(shell, null, message);
    }

    private static PolicyConfiguration[] createConfigurations(
        final PolicyDefinition[] policyDefinitions,
        final PolicyLoader policyLoader) {
        final PolicyConfiguration[] policyConfigurations = new PolicyConfiguration[policyDefinitions.length];
        for (int i = 0; i < policyDefinitions.length; i++) {
            policyConfigurations[i] = configurationFor(policyDefinitions[i], policyLoader);
        }
        return policyConfigurations;
    }

    private static PolicyConfiguration configurationFor(
        final PolicyDefinition policyDefinition,
        final PolicyLoader policyLoader) {
        try {
            final PolicyInstance instance = policyLoader.load(policyDefinition.getType().getID());
            if (instance != null) {
                try {
                    instance.loadConfiguration(policyDefinition.getConfigurationMemento());
                } catch (final Exception e) {
                    final String messageFormat = Messages.getString("CheckinPoliciesAction.PolicyFailedToLoadFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, instance.getPolicyType().getID());
                    final IStatus status = new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, e);
                    TFSCommonUIClientPlugin.getDefault().getLog().log(status);
                }

                return PolicyConfiguration.configurationFor(
                    instance,
                    policyDefinition.isEnabled(),
                    policyDefinition.getPriority(),
                    policyDefinition.getScopeExpressions());
            }

            final String messageFormat = Messages.getString("CheckinPoliciesAction.PolicyNotInstalledFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(
                messageFormat,
                policyDefinition.getType().getID(),
                policyDefinition.getType().getInstallationInstructions());

            final IStatus status = new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, null);
            TFSCommonUIClientPlugin.getDefault().getLog().log(status);
        } catch (final PolicyLoaderException e) {
            final String messageFormat = Messages.getString("CheckinPoliciesAction.UnableToLoadPolicyFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, policyDefinition.getType().getID());
            final IStatus status = new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, e);
            TFSCommonUIClientPlugin.getDefault().getLog().log(status);
        }

        return PolicyConfiguration.configurationFor(policyDefinition);
    }
}
