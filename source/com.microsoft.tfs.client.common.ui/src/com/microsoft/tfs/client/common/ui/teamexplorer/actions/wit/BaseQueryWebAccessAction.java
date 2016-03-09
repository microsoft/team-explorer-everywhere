// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.actions.wit;

import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.exceptions.NotSupportedException;
import com.microsoft.tfs.core.util.TSWAHyperlinkBuilder;
import com.microsoft.tfs.util.LocaleUtil;

public abstract class BaseQueryWebAccessAction extends TeamExplorerWITBaseAction {
    protected abstract URI getWebAccessURI(TSWAHyperlinkBuilder tswaBuilder, QueryItem queryItem);

    @Override
    protected void doRun(final IAction action) {
        final String actionName = action.getText();

        final TFSCommand command = new TFSCommand() {
            @Override
            public String getName() {
                return Messages.getString("BaseQueryWebAccessAction.CommandName"); //$NON-NLS-1$
            }

            @Override
            public String getErrorDescription() {
                return Messages.getString("BaseQueryWebAccessAction.CommandErrorText"); //$NON-NLS-1$
            }

            @Override
            public String getLoggingDescription() {
                return Messages.getString("BaseQueryWebAccessAction.CommandName", LocaleUtil.ROOT); //$NON-NLS-1$
            }

            @Override
            protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
                try {
                    final TSWAHyperlinkBuilder tswaBuilder =
                        new TSWAHyperlinkBuilder(getContext().getServer().getConnection());
                    final URI uri = getWebAccessURI(tswaBuilder, selectedQueryItem);

                    getShell().getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            BrowserFacade.launchURL(
                                uri,
                                selectedQueryItem.getName(),
                                selectedQueryItem.getName(),
                                actionName + selectedQueryItem.getID(),
                                LaunchMode.USER_PREFERENCE);
                        }
                    });
                } catch (final NotSupportedException e) {
                    getShell().getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageBoxHelpers.errorMessageBox(
                                getShell(),
                                Messages.getString("BaseQueryWebAccessAction.ErrorDialogTitle"), //$NON-NLS-1$
                                Messages.getString("BaseQueryWebAccessAction.ErrorDialogText")); //$NON-NLS-1$
                        }
                    });
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }

                return Status.OK_STATUS;
            }
        };

        UICommandExecutorFactory.newUIJobCommandExecutor(getShell()).execute(command);
    }
}
