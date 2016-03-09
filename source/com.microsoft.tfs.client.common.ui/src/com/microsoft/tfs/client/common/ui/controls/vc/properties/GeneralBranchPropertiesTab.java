// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.commands.vc.UpdateBranchPropertiesCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.vc.tfsitem.TFSItem;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchObject;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.BranchProperties;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemIdentifier;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;

public class GeneralBranchPropertiesTab extends GeneralPropertiesTab {
    protected Text ownerValue;
    protected Text descriptionValue;

    private BranchProperties branchProperties;
    private boolean valueChanged = false;

    private TFSRepository repository;
    private Control branchPropertiesControl;

    public class BranchPropertiesControl extends GeneralPropertiesControl {
        public BranchPropertiesControl(final Composite parent, final int style) {
            super(parent, style);

            final Label spacerLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().hSpan(2).applyTo(spacerLabel);

            /* Owner */
            final Label ownerLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().vAlignTop().hAlignPrompt().applyTo(ownerLabel);
            ownerLabel.setText(Messages.getString("GeneralBranchPropertiesTab.OwnerLabelText")); //$NON-NLS-1$

            ownerValue = new Text(this, SWT.BORDER);
            GridDataBuilder.newInstance().hGrab().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(ownerValue);
            ownerValue.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    valueChanged = true;
                    branchProperties.setOwner(ownerValue.getText());
                }
            });

            /* Description */
            final Label descriptionLabel = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().vAlignTop().hAlignPrompt().applyTo(descriptionLabel);
            descriptionLabel.setText(Messages.getString("GeneralBranchPropertiesTab.DescriptionLabelText")); //$NON-NLS-1$

            descriptionValue = new Text(this, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
            GridDataBuilder.newInstance().hGrab().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(descriptionValue);
            descriptionValue.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    valueChanged = true;
                    branchProperties.setDescription(descriptionValue.getText());
                }
            });
            ControlSize.setCharHeightHint(descriptionValue, 4);
        }
    }

    @Override
    public void populate(final TFSRepository repository, final TFSItem item) {
        super.populate(repository, item);
        this.repository = repository;
        serverNameLabel.setText(Messages.getString("GeneralBranchPropertiesTab.BranchNameLabelText")); //$NON-NLS-1$
        final BranchObject[] b = repository.getVersionControlClient().queryBranchObjects(
            new ItemIdentifier(item.getFullPath(), LatestVersionSpec.INSTANCE, 0),
            RecursionType.NONE);
        for (int i = 0; i < b.length; i++) {
            branchProperties = b[i].getProperties();
            if (item.getFullPath().equals(branchProperties.getRootItem().getItem())) {
                final String owner = branchProperties.getOwnerDisplayName();
                final String description = branchProperties.getDescription();
                if (owner != null) {
                    ownerValue.setText(owner);
                }
                if (description != null) {
                    descriptionValue.setText(description);
                }
            }
        }
    }

    @Override
    public Control setupTabItemControl(final Composite parent) {
        if (branchPropertiesControl == null) {
            branchPropertiesControl = new BranchPropertiesControl(parent, SWT.NONE);
        }

        return branchPropertiesControl;
    }

    @Override
    public boolean okPressed() {
        if (!super.okPressed()) {
            return false;
        }

        if (valueChanged) {
            final UpdateBranchPropertiesCommand command =
                new UpdateBranchPropertiesCommand(repository, branchProperties);
            final IStatus status =
                UICommandExecutorFactory.newUICommandExecutor(branchPropertiesControl.getShell()).execute(command);

            if (!status.isOK()) {
                return false;
            }
        }

        return true;
    }
}
