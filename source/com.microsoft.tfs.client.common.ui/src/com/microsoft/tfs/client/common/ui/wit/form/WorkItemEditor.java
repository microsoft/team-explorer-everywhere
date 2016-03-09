// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.commands.wit.SaveWorkItemCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.server.ServerManager;
import com.microsoft.tfs.client.common.server.ServerManagerAdapter;
import com.microsoft.tfs.client.common.server.ServerManagerEvent;
import com.microsoft.tfs.client.common.server.ServerManagerListener;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateAdapter;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateListener;

public class WorkItemEditor extends EditorPart {
    public static final CodeMarker CODEMARKER_SAVE_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.common.ui.wit.form.WorkItemEditor#saveComplete"); //$NON-NLS-1$

    // The logger.
    private static final Log log = LogFactory.getLog(WorkItemEditor.class);

    private FieldTracker fieldTracker;
    private WorkItemStateListener stateListener;

    private WorkItemFormHeader workItemFormHeader;
    private WorkItemForm workItemForm;

    private ServerManagerListener serverManagerListener;

    @Override
    public void doSave(final IProgressMonitor monitor) {
        final WorkItemEditorInput editorInput = (WorkItemEditorInput) getEditorInput();
        final WorkItem workItem = editorInput.getWorkItem();
        final TFSServer server = editorInput.getServer();

        final ServerManager serverManager = TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager();
        final TFSServer defaultServer = serverManager.getDefaultServer();

        /* Make sure we're still connected */
        if (defaultServer != server) {
            final String message = Messages.getString("WorkItemEditor.ErrorDialogText"); //$NON-NLS-1$
            MessageBoxHelpers.errorMessageBox(
                getSite().getShell(),
                Messages.getString("WorkItemEditor.ErrorDialogTitle"), //$NON-NLS-1$
                message);
            monitor.setCanceled(true);
            return;
        }

        if (!workItem.isValid()) {
            final String messageFormat = Messages.getString("WorkItemEditor.SaveFailedFormat"); //$NON-NLS-1$
            final String message =
                MessageFormat.format(messageFormat, fieldTracker.getMessageFromFirstInvalidField(workItem));
            MessageBoxHelpers.errorMessageBox(
                getSite().getShell(),
                Messages.getString("WorkItemEditor.ErrorDialogTitle"), //$NON-NLS-1$
                message);
            monitor.setCanceled(true);
            return;
        }

        final ICommandExecutor executor = UICommandExecutorFactory.newUICommandExecutor(getSite().getShell());
        final IStatus status = executor.execute(new SaveWorkItemCommand(workItem));

        if (!status.isOK()) {
            log.warn("unable to save work item", status.getException()); //$NON-NLS-1$
            monitor.setCanceled(true);
            return;
        }
    }

    @Override
    public void doSaveAs() {

    }

    @Override
    public void init(final IEditorSite site, final IEditorInput input) throws PartInitException {
        if (!(input instanceof WorkItemEditorInput)) {
            throw new PartInitException("Invalid Input: Must be WITQueryResultsEditorInput"); //$NON-NLS-1$
        }
        setSite(site);
        setInput(input);

        final WorkItemEditorInput editorInput = (WorkItemEditorInput) getEditorInput();
        final WorkItem workItem = editorInput.getWorkItem();

        stateListener = new StateListener();
        workItem.addWorkItemStateListener(stateListener);

        fieldTracker = new FieldTracker();
    }

    private class StateListener extends WorkItemStateAdapter {
        @Override
        public void dirtyStateChanged(final boolean isDirty, final WorkItem workItem) {
            if (log.isDebugEnabled()) {
                final String messageFormat = "dirtyStateChanged({0}, {1})"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, isDirty, workItem);
                log.debug(message);
            }

            UIHelpers.runOnUIThread(getSite().getWorkbenchWindow().getWorkbench().getDisplay(), true, new Runnable() {
                @Override
                public void run() {
                    firePropertyChange(IEditorPart.PROP_DIRTY);
                }
            });
        }

        @Override
        public void saved(final WorkItem workItem) {
            if (log.isDebugEnabled()) {
                final String messageFormat = "saved({0})"; //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, workItem);
                log.debug(message);
            }

            UIHelpers.runOnUIThread(getSite().getWorkbenchWindow().getWorkbench().getDisplay(), true, new Runnable() {
                @Override
                public void run() {
                    /*
                     * update the part name (delegating to getName on
                     * WITEditorInput) this handles the case where a newly
                     * created work item is saved, and the part names changes
                     * from something like "New Bug" to "Bug 51"
                     */
                    setPartName(getEditorInput().getName());

                    CodeMarkerDispatch.dispatch(CODEMARKER_SAVE_COMPLETE);
                }
            });
        }
    }

    @Override
    public boolean isDirty() {
        final WorkItemEditorInput editorInput = (WorkItemEditorInput) getEditorInput();
        final WorkItem workItem = editorInput.getWorkItem();
        return workItem.isDirty();
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void createPartControl(final Composite parent) {
        final WorkItemEditorInput editorInput = (WorkItemEditorInput) getEditorInput();
        final WorkItem workItem = editorInput.getWorkItem();
        final TFSServer server = editorInput.getServer();

        setPartName(editorInput.getName());

        workItem.open();

        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        workItemFormHeader = new WorkItemFormHeader(composite, SWT.NONE, workItem, fieldTracker);
        workItemFormHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        workItemForm = new WorkItemForm(composite, SWT.NONE, server, workItem, fieldTracker);
        workItemForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        workItemFormHeader.refresh();

        serverManagerListener = new ServerManagerAdapter() {
            @Override
            public void onServerAdded(final ServerManagerEvent event) {
                UIHelpers.runOnUIThread(true, new Runnable() {
                    @Override
                    public void run() {
                        final WorkItemEditorInput editorInput = (WorkItemEditorInput) getEditorInput();
                        final TFSServer server = editorInput.getServer();

                        if (event.getServer() == server) {
                            setDisconnected(false);
                        } else if (event.getServer().connectionsEquivalent(server)) {
                            editorInput.setServer(event.getServer());
                            setDisconnected(false);
                        }
                    }
                });
            }

            @Override
            public void onServerRemoved(final ServerManagerEvent event) {
                UIHelpers.runOnUIThread(true, new Runnable() {
                    @Override
                    public void run() {
                        if (event.getServer() == ((WorkItemEditorInput) getEditorInput()).getServer()) {
                            setDisconnected(true);
                        }
                    }
                });
            }
        };
        TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().addListener(serverManagerListener);
    }

    public void setDisconnected(final boolean disconnected) {
        workItemFormHeader.setDisconnected(disconnected);
        setEnabled(!disconnected);
    }

    public void setEnabled(final boolean enabled) {
        workItemFormHeader.setEnabled(enabled);
        workItemForm.setEnabled(enabled);
    }

    @Override
    public void setFocus() {
        fieldTracker.setFocusToFirstInvalidField();
    }

    @Override
    public void dispose() {
        super.dispose();

        final WorkItemEditorInput editorInput = (WorkItemEditorInput) getEditorInput();
        final WorkItem workItem = editorInput.getWorkItem();

        if (stateListener != null) {
            workItem.removeWorkItemStateListener(stateListener);
        }

        if (serverManagerListener != null) {
            TFSCommonUIClientPlugin.getDefault().getProductPlugin().getServerManager().removeListener(
                serverManagerListener);
            serverManagerListener = null;
        }

        workItem.reset();
    }
}
