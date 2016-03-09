// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertypages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import com.microsoft.tfs.client.common.commands.vc.GetBranchHistoryCommand;
import com.microsoft.tfs.client.common.framework.command.CommandExecutor;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.branches.BranchesPropertiesControl;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.branches.BranchesPropertiesTab;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistory;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;

public abstract class BaseBranchesPropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
    private final static Log log = LogFactory.getLog(BranchesPropertiesTab.class);

    protected abstract String getLocation();

    protected abstract TFSRepository getRepository();

    @Override
    protected final Control createContents(final Composite parent) {
        final BranchesPropertiesControl branchesPropertiesControl = new BranchesPropertiesControl(parent, SWT.NONE);

        final TFSRepository repository = getRepository();
        final String location = getLocation();

        if (repository == null || location == null) {
            branchesPropertiesControl.setInput(null);
            return branchesPropertiesControl;
        }

        new Job(Messages.getString("BaseBranchesPropertyPage.QueryBranchJobTitle")) //$NON-NLS-1$
        {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                final GetBranchHistoryCommand historyCommand =
                    new GetBranchHistoryCommand(repository, new ItemSpec(location, RecursionType.NONE));
                final IStatus historyStatus = new CommandExecutor().execute(historyCommand);

                final BranchHistory branchHistory =
                    (historyStatus.isOK() && historyCommand.getBranchHistory().length == 1)
                        ? historyCommand.getBranchHistory()[0] : null;

                UIHelpers.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (!branchesPropertiesControl.isDisposed()) {
                            if (historyStatus.isOK()) {
                                branchesPropertiesControl.setInput(branchHistory);
                            } else {
                                branchesPropertiesControl.setError(historyStatus);
                            }
                        }
                    }
                });

                return Status.OK_STATUS;
            }
        }.schedule();

        return branchesPropertiesControl;
    }
}