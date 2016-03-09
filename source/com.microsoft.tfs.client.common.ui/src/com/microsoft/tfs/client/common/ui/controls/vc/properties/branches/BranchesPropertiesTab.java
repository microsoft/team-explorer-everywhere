// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.properties.branches;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.properties.PropertiesTab;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.BranchHistory;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;

public class BranchesPropertiesTab implements PropertiesTab {
    private BranchesPropertiesControl branchesPropertiesControl;

    private final static Log log = LogFactory.getLog(BranchesPropertiesTab.class);

    @Override
    public Control setupTabItemControl(final Composite parent) {
        branchesPropertiesControl = new BranchesPropertiesControl(parent, SWT.NONE);
        return branchesPropertiesControl;
    }

    @Override
    public String getTabItemText() {
        return Messages.getString("BranchesPropertiesTab.TablItemText"); //$NON-NLS-1$
    }

    @Override
    public void populate(final TFSRepository repository, final TFSItem item) {
        UIHelpers.asyncExec(new Runnable() {
            @Override
            public void run() {
                BranchHistory branchHistory = null;

                if (item != null) {
                    final Workspace workspace = repository.getWorkspace();
                    try {
                        // create a new itemSpec so that we don't recurse
                        final ItemSpec branchSpec = new ItemSpec(item.getFullPath(), RecursionType.NONE);
                        branchHistory = workspace.getBranchHistory(
                            branchSpec,
                            new ChangesetVersionSpec(item.getExtendedItem().getLatestVersion()));
                    } catch (final ProxyException e) {
                        log.error("ProxyException populating branch history tab", e); //$NON-NLS-1$
                    }

                }

                // Check the control still exists - user may have shut it down
                if (!branchesPropertiesControl.isDisposed()) {
                    branchesPropertiesControl.setInput(branchHistory);
                }
            }
        });
    }

    @Override
    public void populate(final TFSRepository repository, final ItemIdentifier item) {
        UIHelpers.asyncExec(new Runnable() {
            @Override
            public void run() {
                BranchHistory branchHistory = null;

                if (item != null) {
                    final Workspace workspace = repository.getWorkspace();
                    try {
                        // create a new itemSpec so that we don't recurse
                        final ItemSpec branchSpec = new ItemSpec(item.getItem(), RecursionType.NONE);
                        branchHistory = workspace.getBranchHistory(branchSpec, item.getVersion());
                    } catch (final ProxyException e) {
                        log.error("ProxyException populating branch history tab", e); //$NON-NLS-1$
                    }

                }

                // Check the control still exists - user may have shut it down
                if (!branchesPropertiesControl.isDisposed()) {
                    branchesPropertiesControl.setInput(branchHistory);
                }
            }
        });
    }

    @Override
    public boolean okPressed() {
        return true;
    }

}
