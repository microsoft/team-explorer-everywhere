// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import java.net.URI;
import java.text.MessageFormat;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.dialogs.generic.TextDisplayDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.client.common.ui.wit.dialogs.CopyWorkItemsDialog;
import com.microsoft.tfs.client.common.ui.wit.form.link.NewLinkedWorkItemAction;
import com.microsoft.tfs.core.artifact.LinkingFacade;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.pguidance.ProcessGuidanceURLInfo;
import com.microsoft.tfs.core.util.URIUtils;

public class WorkItemEditorContextMenu {
    private final TFSServer server;
    private final WorkItem workItem;
    private final Shell shell;
    private final Control widgetTreeRootControl;

    private Menu contextMenu;

    private IAction processGuidanceAction;
    private IAction newLinkedWorkItemAction;
    private IAction createCopyAction;
    private IAction serviceViewUrlClipboardAction;
    private IAction serviceViewIdClipboardAction;
    private IAction updateXmlAction;
    private IAction revertAction;
    private IAction associateWithPendingChangeAction;

    private boolean shiftKeyPressed = false;

    public WorkItemEditorContextMenu(
        final TFSServer server,
        final WorkItem workItem,
        final Shell shell,
        final Control widgetTreeRootControl) {
        this.server = server;
        this.workItem = workItem;
        this.shell = shell;
        this.widgetTreeRootControl = widgetTreeRootControl;
        initialize();
    }

    public void setMenuOnControl(final Control control) {
        control.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(final MouseEvent e) {
                shiftKeyPressed = ((e.stateMask & SWT.SHIFT) > 0);
            }
        });

        control.setMenu(contextMenu);
    }

    private void initialize() {
        newLinkedWorkItemAction = new NewLinkedWorkItemAction(shell, server, workItem, null);
        newLinkedWorkItemAction.setText(Messages.getString("WorkItemEditorContextMenu.NewLinkedWorkItemActionText")); //$NON-NLS-1$

        createCopyAction = new CreateCopyAction();
        createCopyAction.setText(Messages.getString("WorkItemEditorContextMenu.CreateCopyActionText")); //$NON-NLS-1$

        processGuidanceAction = new ProcessGuidanceAction();
        processGuidanceAction.setText(Messages.getString("WorkItemEditorContextMenu.ProcessGuidanceActionText")); //$NON-NLS-1$

        serviceViewUrlClipboardAction = new ServiceViewUrlClipboardAction();
        serviceViewUrlClipboardAction.setText(
            Messages.getString("WorkItemEditorContextMenu.CopyUrlToClipboardActionText")); //$NON-NLS-1$

        serviceViewIdClipboardAction = new ServiceViewIdClipboardAction();
        serviceViewIdClipboardAction.setText(
            Messages.getString("WorkItemEditorContextMenu.CopyIdToClipboardActionText")); //$NON-NLS-1$

        associateWithPendingChangeAction = new AssociateWithPendingChangeAction();
        associateWithPendingChangeAction.setText(
            Messages.getString("QueryResultsControl.AssocWithPendingChangeActionText")); //$NON-NLS-1$

        updateXmlAction = new UpdateXMLAction();
        updateXmlAction.setText(Messages.getString("WorkItemEditorContextMenu.ShowUpdateXMLActionText")); //$NON-NLS-1$

        revertAction = new RevertAction();

        /*
         * create the menu manager
         */
        final MenuManager menuManager = new MenuManager("#PopUp"); //$NON-NLS-1$
        menuManager.addMenuListener(new AboutToShowMenuListener());
        menuManager.setRemoveAllWhenShown(true);

        /*
         * create the SWT Menu object
         */
        contextMenu = menuManager.createContextMenu(widgetTreeRootControl);

        /*
         * add the menu to the root of the widget tree
         */
        setMenuOnControl(widgetTreeRootControl);
    }

    private class AboutToShowMenuListener implements IMenuListener {
        @Override
        public void menuAboutToShow(final IMenuManager manager) {
            /*
             * compute enablement state and set enablement on actions
             */

            final boolean unsaved = (workItem.getFields().getID() == 0);

            newLinkedWorkItemAction.setEnabled(!unsaved);
            createCopyAction.setEnabled(!unsaved);
            revertAction.setEnabled(!unsaved);
            serviceViewUrlClipboardAction.setEnabled(!unsaved);
            serviceViewIdClipboardAction.setEnabled(!unsaved);
            associateWithPendingChangeAction.setEnabled(!unsaved);

            if (workItem.isDirty()) {
                revertAction.setText(Messages.getString("WorkItemEditorContextMenu.UndoWorkItemActionText")); //$NON-NLS-1$
            } else {
                revertAction.setText(Messages.getString("WorkItemEditorContextMenu.RefreshWorkItemActionText")); //$NON-NLS-1$
            }

            /*
             * add actions / submenus to menu manager
             */

            manager.add(revertAction);
            manager.add(new Separator());

            manager.add(associateWithPendingChangeAction);
            manager.add(new Separator());

            manager.add(newLinkedWorkItemAction);
            manager.add(createCopyAction);
            manager.add(new Separator());
            manager.add(processGuidanceAction);
            manager.add(new Separator());
            manager.add(serviceViewUrlClipboardAction);
            manager.add(serviceViewIdClipboardAction);

            if (shiftKeyPressed) {
                manager.add(new Separator());
                manager.add(updateXmlAction);
            }
        }
    }

    private class UpdateXMLAction extends Action {
        @Override
        public void run() {
            String message;
            final int id = workItem.getFields().getID();
            if (id == 0) {
                final String messageFormat = Messages.getString("WorkItemEditorContextMenu.NewItemDialogTitleFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, workItem.getType().getName());
            } else {
                final String messageFormat =
                    Messages.getString("WorkItemEditorContextMenu.ExistingItemDialogTitleFormat"); //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, workItem.getType().getName(), Integer.toString(id));
            }

            final String xml = workItem.getClient().getUpdateXMLForDebugging(workItem);
            final TextDisplayDialog dlg = new TextDisplayDialog(shell, message, xml, getClass().getName());

            dlg.open();
        }
    }

    private class ProcessGuidanceAction extends Action {
        @Override
        public void run() {
            final ProcessGuidanceURLInfo urlInfo = workItem.getType().getProcessGuidanceURL();

            if (urlInfo.isValid()) {
                final URI uri = URIUtils.newURI(urlInfo.getURL());
                BrowserFacade.launchURL(uri, Messages.getString("WorkItemEditorContextMenu.BrowserTitle")); //$NON-NLS-1$
            } else {
                MessageBoxHelpers.errorMessageBox(shell, "", urlInfo.getInvalidMessage()); //$NON-NLS-1$
            }
        }
    }

    private class CreateCopyAction extends Action {
        @Override
        public void run() {
            final CopyWorkItemsDialog dialog = new CopyWorkItemsDialog(
                shell,
                workItem.getClient(),
                workItem.getType().getProject(),
                workItem.getType());

            if (dialog.open() == IDialogConstants.OK_ID) {
                final WorkItem newWorkItem = workItem.copy(dialog.getWorkItemType());
                WorkItemEditorHelper.openEditor(server, newWorkItem);
            }
        }
    }

    private class ServiceViewUrlClipboardAction extends Action {
        @Override
        public void run() {
            final String url = LinkingFacade.getExternalURL(workItem, workItem.getClient().getConnection());
            UIHelpers.copyToClipboard(url);
        }

    }

    private class ServiceViewIdClipboardAction extends Action {
        @Override
        public void run() {
            final WorkItem[] workItems = new WorkItem[] {
                workItem
            };
            final String gitCommitWorkItemsLink = WorkItemEditorHelper.createGitCommitWorkItemsLink(workItems);
            UIHelpers.copyToClipboard(gitCommitWorkItemsLink);
        }

    }

    private class RevertAction extends Action {
        @Override
        public void run() {
            if (workItem.isDirty()) {
                if (!MessageBoxHelpers.dialogConfirmPrompt(
                    shell,
                    Messages.getString("WorkItemEditorContextMenu.ConfirmDialogTitle"), //$NON-NLS-1$
                    Messages.getString("WorkItemEditorContextMenu.ConfirmDialogText"))) //$NON-NLS-1$
                {
                    return;
                }
            }
            workItem.syncToLatest();
        }
    }

    private class AssociateWithPendingChangeAction extends Action {
        @Override
        public void run() {
            final TFSCommonUIClientPlugin plugin = TFSCommonUIClientPlugin.getDefault();
            plugin.getPendingChangesViewModel().associateWorkItem(workItem);
        }
    }
}
