// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.workspaces;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkspaceData;
import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkspaceEditControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.ExtendedButtonDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.IValidity;

public class WorkspaceEditDialog extends ExtendedButtonDialog {
    private static final int REMOVE_BUTTON_ID = IDialogConstants.CLIENT_ID;
    private static final int ADVANCED_BUTTON_ID = IDialogConstants.CLIENT_ID + 1;

    private final boolean forEdit;
    private final WorkspaceData workspaceData;
    private final TFSTeamProjectCollection connection;

    /* Immutable workspaces can only have their workfolds altered */
    private boolean immutable = false;

    private WorkspaceEditControl workspaceEditControl;

    /* Whether the advanced workspace data is shown by default */
    private boolean advanced = TFSCommonUIClientPlugin.getDefault().getPreferenceStore().getBoolean(
        UIPreferenceConstants.WORKSPACE_DIALOG_EXPANDED);

    public WorkspaceEditDialog(
        final Shell parentShell,
        final boolean forEdit,
        final WorkspaceData workspaceData,
        final TFSTeamProjectCollection connection) {
        super(parentShell);

        Check.notNull(workspaceData, "workspaceData"); //$NON-NLS-1$
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        this.forEdit = forEdit;
        this.workspaceData = workspaceData;
        this.connection = connection;

        addExtendedButtonDescription(
            REMOVE_BUTTON_ID,
            Messages.getString("WorkspaceEditDialog.RemoveButtonText"), //$NON-NLS-1$
            false);
        addExtendedButtonDescription(
            ADVANCED_BUTTON_ID,
            advanced ? Messages.getString("WorkspaceEditDialog.AdvancedCollapse") //$NON-NLS-1$
                : Messages.getString("WorkspaceEditDialog.AdvancedExpand"), //$NON-NLS-1$
            false);

        /* Persist geometry based on whether we start expanded or collapsed. */
        setOptionDialogSettingsKey(getClass().getName() + ((advanced) ? ":expanded" : "collapsed")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void setImmutable(final boolean immutable) {
        this.immutable = immutable;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final FillLayout layout = new FillLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.spacing = getSpacing();
        dialogArea.setLayout(layout);

        workspaceEditControl = new WorkspaceEditControl(dialogArea, SWT.NONE, connection);
        workspaceEditControl.setWorkspaceData(workspaceData);
        workspaceEditControl.setImmutable(immutable);
        workspaceEditControl.setAdvanced(advanced);
        workspaceEditControl.getWorkingFolderDataTable().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                getButton(REMOVE_BUTTON_ID).setEnabled(
                    workspaceEditControl.getWorkingFolderDataTable().getSelectedWorkingFolders().length > 0
                        && workspaceData.getWorkspaceDetails().isAdministerAllowed());
            }
        });
    }

    @Override
    protected String provideDialogTitle() {
        if (forEdit) {
            if (workspaceData.getWorkspaceDetails().getName() != null) {
                final String messageFormat = Messages.getString("WorkspaceEditDialog.EditWorkspaceDialogTitleFormat"); //$NON-NLS-1$
                return MessageFormat.format(messageFormat, workspaceData.getWorkspaceDetails().getName());
            }
            return Messages.getString("WorkspaceEditDialog.EditWorkspaceDialogTitle"); //$NON-NLS-1$
        } else {
            return Messages.getString("WorkspaceEditDialog.AddWorkspaceDialogTitle"); //$NON-NLS-1$
        }
    }

    @Override
    protected void hookDialogIsOpen() {
        getButton(REMOVE_BUTTON_ID).setEnabled(false);
    }

    @Override
    protected void hookCustomButtonPressed(final int buttonId) {
        if (buttonId == REMOVE_BUTTON_ID) {
            workspaceEditControl.getWorkingFolderDataTable().removeSelectedWorkingFolders();
        } else if (buttonId == ADVANCED_BUTTON_ID) {
            final Point windowSize = getShell().getSize();
            final Point oldSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);

            final Point oldMinimumSize = getShell().getMinimumSize();

            advanced = !advanced;
            workspaceEditControl.setAdvanced(advanced);
            getButton(ADVANCED_BUTTON_ID).setText(advanced ? Messages.getString("WorkspaceEditDialog.AdvancedCollapse") //$NON-NLS-1$
                : Messages.getString("WorkspaceEditDialog.AdvancedExpand")); //$NON-NLS-1$

            getShell().layout();

            final Point newSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
            getShell().setMinimumSize(new Point(oldMinimumSize.x, oldMinimumSize.y + (newSize.y - oldSize.y)));
            getShell().setSize(new Point(windowSize.x, windowSize.y + (newSize.y - oldSize.y)));

            /*
             * Update the persistence key so that we persist geometry based on
             * expansion state.
             */
            setOptionDialogSettingsKey(getClass().getName() + ((advanced) ? ":expanded" : "collapsed")); //$NON-NLS-1$ //$NON-NLS-2$

            TFSCommonUIClientPlugin.getDefault().getPreferenceStore().setValue(
                UIPreferenceConstants.WORKSPACE_DIALOG_EXPANDED,
                advanced);
        }
    }

    @Override
    protected void okPressed() {
        final IValidity validity = workspaceEditControl.getWorkingFolderDataTable().validate();

        if (!validity.isValid()) {
            final String messageFormat = Messages.getString("WorkspaceEditDialog.ErrorDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, validity.getFirstMessage().getMessage());
            MessageBoxHelpers.errorMessageBox(getShell(), null, message);
            return;
        }

        super.okPressed();
    }

    @Override
    protected void createButtonsForButtonBar(final Composite parent) {
        super.createButtonsForButtonBar(parent);
        final Button okButton = getButton(IDialogConstants.OK_ID);
        new ButtonValidatorBinding(okButton).bind(workspaceEditControl.getWorkspaceDetailsControl());
    }
}
