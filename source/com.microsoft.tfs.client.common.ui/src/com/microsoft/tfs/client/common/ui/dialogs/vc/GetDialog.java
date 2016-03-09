// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.ServerItemSelectionTable;
import com.microsoft.tfs.client.common.ui.controls.vc.VersionPickerControl;
import com.microsoft.tfs.client.common.ui.controls.vc.VersionPickerControl.VersionSpecChangedEvent;
import com.microsoft.tfs.client.common.ui.controls.vc.VersionPickerControl.VersionSpecChangedListener;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.util.Check;

public class GetDialog extends BaseDialog implements VersionSpecChangedListener {
    private final TFSRepository repository;
    private final TypedServerItem[] initialServerItems;
    private final TypedServerItem[] displayServerItems;

    private ServerItemSelectionTable serverItemTable;
    private VersionPickerControl versionControl;
    private Button forceButton;
    private Button overwriteButton;

    private TypedServerItem[] checkedServerItems;
    private VersionSpec versionSpec;
    private boolean force;
    private boolean overwrite;

    /**
     * Constructs a {@link GetDialog} to choose versions of the initial server
     * items to get.
     *
     * @param parent
     *        the parent shell (must not be <code>null</code>)
     * @param repository
     *        the repository to get from (must not be <code>null</code>)
     * @param initialServerItems
     *        the server items to show in the dialog; <code>null</code> or empty
     *        shows the root folder and triggers a whole workspace get
     */
    public GetDialog(final Shell parent, final TFSRepository repository, final TypedServerItem[] initialServerItems) {
        super(parent);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;
        this.initialServerItems = initialServerItems;

        if (initialServerItems != null && initialServerItems.length > 0) {
            this.displayServerItems = initialServerItems;
        } else {
            this.displayServerItems = new TypedServerItem[] {
                TypedServerItem.ROOT
            };
        }
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing() / 2;
        dialogArea.setLayout(layout);

        serverItemTable = new ServerItemSelectionTable(dialogArea, SWT.CHECK | SWT.FULL_SELECTION);
        serverItemTable.setText(Messages.getString("GetDialog.FilesLabelText")); //$NON-NLS-1$
        serverItemTable.setServerItems(displayServerItems);
        serverItemTable.setCheckedServerItems(displayServerItems);
        GridDataBuilder.newInstance().grab().fill().applyTo(serverItemTable);

        ControlSize.setCharWidthHint(serverItemTable, 80);
        ControlSize.setCharHeightHint(serverItemTable, 10);

        versionControl = new VersionPickerControl(dialogArea, SWT.NONE);
        versionControl.setRepository(repository);
        versionControl.setText(Messages.getString("GetDialog.VersionPickerLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(versionControl);

        if (displayServerItems.length == 1) {
            if (displayServerItems[0].getLocalPath() != null) {
                versionControl.setPath(displayServerItems[0].getLocalPath());
            }

            else if (displayServerItems[0].getServerPath() != null) {
                versionControl.setPath(displayServerItems[0].getServerPath());
            }
        }

        versionControl.addVersionSpecChangedListener(this);

        forceButton = new Button(dialogArea, SWT.CHECK);
        forceButton.setText(Messages.getString("GetDialog.ForceButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().vIndent(getVerticalSpacing() * 2).applyTo(forceButton);

        overwriteButton = new Button(dialogArea, SWT.CHECK);
        overwriteButton.setText(Messages.getString("GetDialog.OverwriteButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().vIndent(getVerticalSpacing() / 2).applyTo(overwriteButton);

        forceButton.setFocus();
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        final Button getButton =
            createButton(parent, IDialogConstants.OK_ID, Messages.getString("GetDialog.GetButtonText"), true); //$NON-NLS-1$

        new ButtonValidatorBinding(getButton).bind(serverItemTable.getCheckboxValidator());
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void hookDialogAboutToClose() {
        checkedServerItems = serverItemTable.getCheckedServerItems();
        versionSpec = versionControl.getVersionSpec();

        force = forceButton.getSelection();
        overwrite = overwriteButton.getSelection();
    }

    public boolean isForce() {
        return force;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public GetOptions getGetOptions() {
        GetOptions getOptions = GetOptions.NONE;

        if (force) {
            getOptions = getOptions.combine(GetOptions.GET_ALL);
        }

        if (overwrite) {
            getOptions = getOptions.combine(GetOptions.OVERWRITE);
        }

        return getOptions;
    }

    public VersionSpec getVersionSpec() {
        return versionSpec;
    }

    public GetRequest[] getGetRequests() {
        GetRequest[] getRequests;

        final VersionSpec versionSpec = getVersionSpec();

        if (initialServerItems == null || initialServerItems.length == 0) {
            // Dialog was initialized for a whole-workspace get (root was
            // displayed).
            getRequests = new GetRequest[] {
                new GetRequest(null, versionSpec)
            };
        } else {
            getRequests = new GetRequest[checkedServerItems.length];

            for (int i = 0; i < checkedServerItems.length; i++) {
                final RecursionType recursionType = (checkedServerItems[i].getType().equals(ServerItemType.FILE)
                    ? RecursionType.NONE : RecursionType.FULL);

                getRequests[i] =
                    new GetRequest(new ItemSpec(checkedServerItems[i].getServerPath(), recursionType), versionSpec);
            }
        }

        return getRequests;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("GetDialog.DialogTitle"); //$NON-NLS-1$
    }

    private void setEnabled(boolean enable) {
        final Button ok = getButton(IDialogConstants.OK_ID);
        if (ok != null) {
            enable = enable && serverItemTable.getCheckedProjectsCount() > 0;
            ok.setEnabled(enable);
        }
    }

    @Override
    public void onVersionSpecChanged(final VersionSpecChangedEvent e) {
        setEnabled(e.isValid());
    }
}
