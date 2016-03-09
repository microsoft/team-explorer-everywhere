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
import com.microsoft.tfs.client.common.ui.controls.vc.ShelvesetSearchControl.ShelvesetSearchUnshelveEvent;
import com.microsoft.tfs.client.common.ui.controls.vc.ShelvesetSearchControl.ShelvesetSearchUnshelveListener;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.framework.dialog.ExtendedButtonDialog;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

/**
 * Lets the user query for shelvesets on the server and choose one to unshelve.
 * Because the user is given a chance to select only some changes in a shelveset
 * to unshelve (via the Details button), the caller must retrieve the selected
 * shelveset via {@link #getSelectedShelveset()} and check for custom checked
 * items {@link #getCheckedItemSpecs()}.
 *
 */
public class UnshelveDialog extends ExtendedButtonDialog {
    private static final int DETAILS_ID = IDialogConstants.CLIENT_ID + 1;
    private static final int DELETE_ID = IDialogConstants.CLIENT_ID + 2;

    private final TFSRepository repository;

    private ShelvesetSearchControl shelvesetSearchControl;

    private Shelveset selectedShelveset;

    private ChangeItem[] checkedChangeItems;
    private boolean preserveShelveset = true;
    private boolean restoreData = true;

    /**
     * @param parentShell
     */
    public UnshelveDialog(final Shell parentShell, final TFSRepository repository) {
        super(parentShell);

        Check.notNull(repository, "repository"); //$NON-NLS-1$

        this.repository = repository;

        // Disable standard OK/Cancel buttons.
        setOptionIncludeDefaultButtons(false);

        /*
         * Add back Unshelve/Close in their place. Order is important here, we
         * want Close (Cancel) on the right so add it last.
         */
        addButtonDescription(IDialogConstants.OK_ID, Messages.getString("UnshelveDialog.UnshelveButtonText"), true); //$NON-NLS-1$
        addButtonDescription(IDialogConstants.CANCEL_ID, Messages.getString("UnshelveDialog.CloseButtonText"), false); //$NON-NLS-1$

        // Add extended buttons on the left.
        addExtendedButtonDescription(DETAILS_ID, Messages.getString("UnshelveDialog.DetailsButtonText"), false); //$NON-NLS-1$
        addExtendedButtonDescription(DELETE_ID, Messages.getString("UnshelveDialog.DeleteButtonText"), false); //$NON-NLS-1$
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

        shelvesetSearchControl = new ShelvesetSearchControl(dialogArea, SWT.NONE, true, repository);
        shelvesetSearchControl.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        shelvesetSearchControl.addUnshelveListener(new ShelvesetSearchUnshelveListener() {
            @Override
            public void onShelvesetSearchUnshelve(final ShelvesetSearchUnshelveEvent event) {
                final Shelveset[] shelvesets = shelvesetSearchControl.getSelectedShelvesets();

                Check.notNull(shelvesets, "shelvesets"); //$NON-NLS-1$
                Check.isTrue(shelvesets.length == 1, "shelvesets.length == 1"); //$NON-NLS-1$

                setReturnCode(IDialogConstants.OK_ID);
                close();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void hookAfterButtonsCreated() {
        /*
         * Disable OK (Unshelve), Details, and Delete initially.
         */
        getButton(IDialogConstants.OK_ID).setEnabled(false);
        getButton(DETAILS_ID).setEnabled(false);
        getButton(DELETE_ID).setEnabled(false);

        shelvesetSearchControl.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final Shelveset[] selection = shelvesetSearchControl.getSelectedShelvesets();

                final int selectionCount = (selection != null) ? selection.length : 0;

                getButton(IDialogConstants.OK_ID).setEnabled(selectionCount == 1);
                getButton(DETAILS_ID).setEnabled(selectionCount == 1);
                getButton(DELETE_ID).setEnabled(selectionCount >= 1);
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
        } else if (buttonId == DELETE_ID) {
            shelvesetSearchControl.deleteSelectedShelvesets();
        }
    }

    @Override
    protected void hookDialogAboutToClose() {
        final Shelveset[] selection = shelvesetSearchControl.getSelectedShelvesets();

        selectedShelveset = selection.length == 0 ? null : selection[0];

        /*
         * The checked change items in the search control may be null, because
         * the user hasn't launched the details dialog to check any items, and
         * this is OK. That means the entire shelveset will be unshelved.
         */
        checkedChangeItems = shelvesetSearchControl.getCheckedChangeItems();

        preserveShelveset = shelvesetSearchControl.isPreserveShelveset();
        restoreData = shelvesetSearchControl.isRestoreData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String provideDialogTitle() {
        return Messages.getString("UnshelveDialog.DialogTitle"); //$NON-NLS-1$
    }

    /**
     * @return the shelveset the user selected
     */
    public Shelveset getSelectedShelveset() {
        return selectedShelveset;
    }

    /**
     * Gets the items in the shelveset the user wants to unshelve, or
     * <code>null</code> if the entire shelveset should be unshelved.
     *
     * @return the items in the shelveset the user selected which should be
     *         included in the unshelve operation, or <code>null</code> if the
     *         entire shelveset should be unshelved. An empty array indicates
     *         the shelveset should unshelve but no pending changes should be
     *         included (comment, associated work items, etc. will be
     *         unshelved).
     */
    public ItemSpec[] getCheckedItemSpecs() {
        if (checkedChangeItems == null) {
            return null;
        }

        final ItemSpec[] specs = new ItemSpec[checkedChangeItems.length];
        for (int i = 0; i < checkedChangeItems.length; i++) {
            specs[i] = new ItemSpec(checkedChangeItems[i].getServerItem(), RecursionType.NONE);
        }

        return specs;
    }

    /**
     * @return the preserveShelveset
     */
    public boolean isPreserveShelveset() {
        return preserveShelveset;
    }

    /**
     * @param preserveShelveset
     *        the preserveShelveset to set
     */
    public void setPreserveShelveset(final boolean preserveShelveset) {
        this.preserveShelveset = preserveShelveset;
    }

    /**
     * @return the restoreData
     */
    public boolean isRestoreData() {
        return restoreData;
    }

    /**
     * @param restoreData
     *        the restoreData to set
     */
    public void setRestoreData(final boolean restoreData) {
        this.restoreData = restoreData;
    }
}