// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.telemetry;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.PopupDialog;

import com.microsoft.tfs.client.common.framework.command.ICommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.action.ObjectActionDelegate;
import com.microsoft.tfs.client.common.ui.framework.action.SelectionProviderAction;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizard;
import com.microsoft.tfs.client.common.ui.productplugin.TFSProductPlugin;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.microsoft.tfs.core.telemetry.TfsTelemetryConstants;
import com.microsoft.tfs.core.telemetry.TfsTelemetryHelper;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class ClientTelemetryHelper extends TfsTelemetryHelper {
    public static void sendWizardOpened(final ExtendedWizard wizard) {
        Check.notNull(wizard, "wizard"); //$NON-NLS-1$

        final String wizardName = getName(wizard);
        final String pageName = MessageFormat.format(TfsTelemetryConstants.WIZARD_PAGE_VIEW_NAME_FORMAT, wizardName);

        final Map<String, String> properties = new HashMap<String, String>();
        addContextProperties(properties);

        sendPageView(pageName, properties);
    }

    public static void sendDialogOpened(final BaseDialog dialog) {
        Check.notNull(dialog, "dialog"); //$NON-NLS-1$

        final String dialogName = getName(dialog);
        sendDialogOpened(dialogName);
    }

    public static void sendDialogOpened(final PopupDialog dialog) {
        Check.notNull(dialog, "dialog"); //$NON-NLS-1$

        final String dialogName = getName(dialog);
        sendDialogOpened(dialogName);
    }

    private static void sendDialogOpened(final String dialogName) {
        final String pageName = MessageFormat.format(TfsTelemetryConstants.DIALOG_PAGE_VIEW_NAME_FORMAT, dialogName);

        final Map<String, String> properties = new HashMap<String, String>();
        addContextProperties(properties);

        sendPageView(pageName, properties);
    }

    public static void sendTeamExplorerPageView(final String viewID) {
        sendTeamExplorerPageView(viewID, true);
    }

    public static void sendPageView(final TeamExplorerNavigationItemConfig item) {
        if (item != null) {
            sendTeamExplorerPageView(item.getTargetPageID(), false);
        }
    }

    public static void sendTeamExplorerPageView(final String viewID, final Boolean undocked) {
        final String[] idParts = viewID.split("\\."); //$NON-NLS-1$
        final String viewName = idParts[idParts.length - 1];
        final String pageName = MessageFormat.format(TfsTelemetryConstants.EXPLORER_PAGE_VIEW_NAME_FORMAT, viewName);

        final Map<String, String> properties = new HashMap<String, String>();
        addContextProperties(properties);

        if (undocked != null) {
            properties.put(TfsTelemetryConstants.PLUGIN_PAGE_VIEW_PROPERTY_UNDOCKED, undocked.toString());
        }

        sendPageView(pageName, properties);
    }

    public static void sendRunActionEvent(final SelectionProviderAction action) {
        Check.notNull(action, "action"); //$NON-NLS-1$

        final String actionName = getName(action);
        sendRunActionEvent(actionName);
    }

    public static void sendRunActionEvent(final ObjectActionDelegate action) {
        Check.notNull(action, "action"); //$NON-NLS-1$

        final String actionName = getName(action);
        sendRunActionEvent(actionName);
    }

    public static void sendRunActionEvent(final String actionName) {
        Check.notNull(actionName, "actionName"); //$NON-NLS-1$

        sendRunActionEvent(actionName, new HashMap<String, String>());
    }

    public static void sendRunActionEvent(final String actionName, final Map<String, String> properties) {
        final String eventName =
            MessageFormat.format(TfsTelemetryConstants.PLUGIN_ACTION_EVENT_NAME_FORMAT, actionName);

        addCommandNameProperty(properties, actionName);
        addContextProperties(properties);

        sendEvent(eventName, properties);
    }

    public static void sendCommandFinishedEvent(final ICommand command, final IStatus status) {
        sendCommandFinishedEvent(command, status, new HashMap<String, String>());
    }

    public static void sendCommandFinishedEvent(
        final ICommand command,
        final IStatus status,
        final Map<String, String> properties) {
        Check.notNull(command, "command"); //$NON-NLS-1$

        final String commandName = getName(command);
        final String eventName =
            MessageFormat.format(TfsTelemetryConstants.PLUGIN_COMMAND_EVENT_NAME_FORMAT, commandName);

        addSuccessProperty(properties, status.getCode() != IStatus.ERROR);
        addCommandNameProperty(properties, commandName);
        addContextProperties(properties);

        sendEvent(eventName, properties);
    }

    private static void addSuccessProperty(final Map<String, String> properties, final Boolean success) {
        if (success != null) {
            properties.put(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_IS_SUCCESS, success.toString());
        }
    }

    private static void addCommandNameProperty(final Map<String, String> properties, final String commandName) {
        if (!StringUtil.isNullOrEmpty(commandName)) {
            properties.put(TfsTelemetryConstants.PLUGIN_EVENT_PROPERTY_COMMAND_NAME, commandName);
        }
    }

    private static void addContextProperties(final Map<String, String> properties) {
        TFSProductPlugin plugin;
        TFSRepository repository;

        if ((plugin = TFSCommonUIClientPlugin.getDefault().getProductPlugin()) != null
            && (repository = plugin.getRepositoryManager().getDefaultRepository()) != null) {
            addContextProperties(properties, repository.getConnection());
        }
    }
}
