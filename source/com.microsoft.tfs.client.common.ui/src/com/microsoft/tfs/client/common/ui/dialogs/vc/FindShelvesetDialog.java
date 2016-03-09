// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.vc.ShelvesetSearchControl;
import com.microsoft.tfs.client.common.ui.framework.dialog.ExtendedButtonDialog;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.util.Check;

public class FindShelvesetDialog extends ExtendedButtonDialog {
    private static final int DETAILS_ID = IDialogConstants.CLIENT_ID + 1;

    private final TFSRepository repository;
    private ShelvesetSearchControl shelvesetSearchControl;
    private Shelveset selectedShelveset;

    public FindShelvesetDialog(final Shell parentShell, final TFSRepository repository) {
        super(parentShell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$
        this.repository = repository;

        // Add extended buttons on the left.
        addExtendedButtonDescription(DETAILS_ID, Messages.getString("FindShelvesetDialog.DetailsButtonText"), false); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout dialogLayout = new GridLayout();
        dialogLayout.marginWidth = getHorizontalMargin();
        dialogLayout.marginHeight = getVerticalMargin();
        dialogLayout.horizontalSpacing = getHorizontalSpacing();
        dialogLayout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(dialogLayout);

        shelvesetSearchControl = new ShelvesetSearchControl(dialogArea, SWT.NONE, false, repository);

        final GridData shelvesetSearchControlData = new GridData(GridData.FILL, GridData.FILL, true, true);
        shelvesetSearchControlData.widthHint = getMinimumMessageAreaWidth();

        shelvesetSearchControl.setLayoutData(shelvesetSearchControlData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void hookAfterButtonsCreated() {
        /*
         * Disable OK and Details initially.
         */
        getButton(IDialogConstants.OK_ID).setEnabled(false);
        getButton(DETAILS_ID).setEnabled(false);

        shelvesetSearchControl.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final Shelveset[] selection = shelvesetSearchControl.getSelectedShelvesets();

                final int selectionCount = (selection != null) ? selection.length : 0;

                getButton(IDialogConstants.OK_ID).setEnabled(selectionCount == 1);
                getButton(DETAILS_ID).setEnabled(selectionCount == 1);
            }
        });

        shelvesetSearchControl.addOwnerTextFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                getShell().setDefaultButton(shelvesetSearchControl.getFindButton());
            }

            @Override
            public void focusLost(final FocusEvent e) {
                getShell().setDefaultButton(getButton(DETAILS_ID));
            }
        });
    }

    @Override
    protected void hookDialogIsOpen() {
        shelvesetSearchControl.setFocus();
        shelvesetSearchControl.query();
    }

    @Override
    protected void hookCustomButtonPressed(final int buttonId) {
        if (buttonId == DETAILS_ID) {
            final Shelveset[] selection = shelvesetSearchControl.getSelectedShelvesets();

            if (selection.length != 1) {
                return;
            }

            shelvesetSearchControl.detailsForSelectedShelveset();
        }
    }

    @Override
    protected void hookDialogAboutToClose() {
        final Shelveset[] selection = shelvesetSearchControl.getSelectedShelvesets();

        selectedShelveset = selection.length == 0 ? null : selection[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String provideDialogTitle() {
        return Messages.getString("FindShelvesetDialog.DialogTitle"); //$NON-NLS-1$
    }

    /**
     * @return the shelveset the user selected
     */
    public Shelveset getSelectedShelveset() {
        return selectedShelveset;
    }
}
