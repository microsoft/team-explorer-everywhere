// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.FindInSCEQueryOptionsPersistence;
import com.microsoft.tfs.client.common.ui.FindInSourceControlQuery;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.WorkspaceItemSource;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.util.MementoRepository;
import com.microsoft.tfs.util.Check;

public class FindInSourceControlDialog extends BaseDialog {
    private final TFSRepository repository;

    private Text pathText;
    private Text wildcardText;
    private Button recursiveButton;
    private Button allItemsButton;
    private Button checkedOutAllButton;
    private Button checkedOutUserButton;
    private Button showCheckoutStatusButton;
    private Text checkedOutUserText;
    private boolean valid;

    private FindInSourceControlQuery query = new FindInSourceControlQuery();

    public FindInSourceControlDialog(final Shell parentShell, final TFSRepository repository) {
        super(parentShell);

        this.repository = repository;

        setOptionResizableDirections(SWT.HORIZONTAL);

        valid = false;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("FindInSourceControlDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final Button button = getButton(IDialogConstants.OK_ID);
        button.setText(Messages.getString("FindInSourceControlDialog.FindButtonText")); //$NON-NLS-1$
        setButtonLayoutData(button);
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        FindInSCEQueryOptionsPersistence.restore(
            new MementoRepository(DefaultPersistenceStoreProvider.INSTANCE.getCachePersistenceStore()),
            query);

        final GridLayout dialogLayout = new GridLayout(3, false);
        dialogLayout.marginWidth = getHorizontalMargin();
        dialogLayout.marginHeight = getVerticalMargin();
        dialogLayout.horizontalSpacing = getHorizontalSpacing();
        dialogLayout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(dialogLayout);

        final Label pathPromptLabel = new Label(dialogArea, SWT.NONE);
        pathPromptLabel.setText(Messages.getString("FindInSourceControlDialog.PathPrompt")); //$NON-NLS-1$

        pathText = new Text(dialogArea, SWT.BORDER);
        pathText.setText(query.getServerPath());
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(pathText);
        pathText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                updateOKEnablement();
            }
        });

        final Button pathBrowseButton = new Button(dialogArea, SWT.PUSH);
        pathBrowseButton.setText(Messages.getString("FindInSourceControlDialog.PathBrowseButtonText")); //$NON-NLS-1$

        pathBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final ServerItemSource serverItemSource = new WorkspaceItemSource(repository.getWorkspace());

                String startingPath = pathText.getText().trim();

                if (pathText.getText().length() == 0 || ServerPath.equals(startingPath, ServerPath.ROOT)) {
                    startingPath = null;
                }

                final ServerItemTreeDialog dialog = new ServerItemTreeDialog(
                    getShell(),
                    Messages.getString("FindInSourceControlDialog.BrowseDialogTitle"), //$NON-NLS-1$
                    startingPath,
                    serverItemSource,
                    ServerItemType.ALL_FOLDERS);

                if (IDialogConstants.OK_ID != dialog.open()) {
                    return;
                }

                pathText.setText(dialog.getSelectedServerPath());
            }
        });

        final Label wildcardPromptLabel = new Label(dialogArea, SWT.NONE);
        wildcardPromptLabel.setText(Messages.getString("FindInSourceControlDialog.WildcardPrompt")); //$NON-NLS-1$

        wildcardText = new Text(dialogArea, SWT.BORDER);
        wildcardText.setText(query.getWildcard() != null ? query.getWildcard() : ""); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().applyTo(wildcardText);

        SWTUtil.createGridLayoutSpacer(dialogArea);
        final Label wildcardHelpLabel = new Label(dialogArea, SWT.NONE);
        wildcardHelpLabel.setText(Messages.getString("FindInSourceControlDialog.WildcardHelpLabel")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().applyTo(wildcardHelpLabel);

        recursiveButton = new Button(dialogArea, SWT.CHECK);
        recursiveButton.setText(Messages.getString("FindInSourceControlDialog.RecursiveButtonText")); //$NON-NLS-1$
        recursiveButton.setSelection(query.isRecursive());
        GridDataBuilder.newInstance().hSpan(3).hGrab().hFill().applyTo(recursiveButton);

        SWTUtil.createGridLayoutSpacer(dialogArea, 3, 1);

        final Group statusGroup = new Group(dialogArea, SWT.NONE);
        statusGroup.setText(Messages.getString("FindInSourceControlDialog.StatusButtonGroupText")); //$NON-NLS-1$

        GridDataBuilder.newInstance().hSpan(3).hGrab().hFill().wHint(getMinimumMessageAreaWidth()).applyTo(statusGroup);

        final GridLayout statusLayout = new GridLayout(2, false);
        statusLayout.marginWidth = getHorizontalMargin();
        statusLayout.marginHeight = getVerticalMargin();
        statusLayout.horizontalSpacing = getHorizontalSpacing();
        statusLayout.verticalSpacing = getVerticalSpacing();
        statusGroup.setLayout(statusLayout);

        allItemsButton = new Button(statusGroup, SWT.RADIO);
        allItemsButton.setText(Messages.getString("FindInSourceControlDialog.StatusAllItems")); //$NON-NLS-1$
        allItemsButton.setSelection(!query.isCheckedOut());
        allItemsButton.setEnabled(true);
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().applyTo(allItemsButton);
        allItemsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (allItemsButton.getSelection()) {
                    checkedOutUserText.setEnabled(false);
                }

                updateOKEnablement();
            }
        });
        checkedOutAllButton = new Button(statusGroup, SWT.RADIO);
        checkedOutAllButton.setText(Messages.getString("FindInSourceControlDialog.StatusCheckedOutAll")); //$NON-NLS-1$
        checkedOutAllButton.setSelection(!allItemsButton.getSelection() && query.getCheckedOutUser() == null);
        checkedOutAllButton.setEnabled(true);
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().applyTo(checkedOutAllButton);
        checkedOutAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (checkedOutAllButton.getSelection()) {
                    checkedOutUserText.setEnabled(false);
                }

                updateOKEnablement();
            }
        });

        checkedOutUserButton = new Button(statusGroup, SWT.RADIO);
        checkedOutUserButton.setSelection(query.getCheckedOutUser() != null);
        checkedOutUserButton.setEnabled(true);
        checkedOutUserButton.setText(Messages.getString("FindInSourceControlDialog.StatusCheckedOutUser")); //$NON-NLS-1$
        checkedOutUserButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (checkedOutUserButton.getSelection()) {
                    checkedOutUserText.setEnabled(true);
                    checkedOutUserText.setFocus();
                }

                updateOKEnablement();
            }
        });

        checkedOutUserText = new Text(statusGroup, SWT.BORDER);
        checkedOutUserText.setText(
            query.getCheckedOutUser() != null ? query.getCheckedOutUser()
                : repository.getVersionControlClient().getConnection().getAuthorizedTFSUser().getUsername());
        checkedOutUserText.setEnabled(checkedOutUserButton.getSelection());
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(checkedOutUserText);
        checkedOutUserText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                updateOKEnablement();
            }
        });

        SWTUtil.createGridLayoutSpacer(dialogArea, 3, 1);

        showCheckoutStatusButton = new Button(dialogArea, SWT.CHECK);
        showCheckoutStatusButton.setText(Messages.getString("FindInSourceControlDialog.ShowCheckoutStatusButtonText")); //$NON-NLS-1$
        showCheckoutStatusButton.setSelection(query.showStatus());
        GridDataBuilder.newInstance().hSpan(3).hGrab().hFill().applyTo(showCheckoutStatusButton);
    }

    private void updateOKEnablement() {
        if (pathText.getText().trim().length() == 0
            || (checkedOutUserButton.getSelection() && checkedOutUserText.getText().trim().length() == 0)) {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        } else {
            getButton(IDialogConstants.OK_ID).setEnabled(true);
        }
    }

    @Override
    protected void okPressed() {
        final String serverPath = pathText.getText().trim();

        if (!ServerPath.isServerPath(serverPath)) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("FindInSourceControlDialog.InvalidServerPathTitle"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("FindInSourceControlDialog.InvalidServerPathFormat"), //$NON-NLS-1$
                    serverPath));
            return;
        }

        if (!repository.getWorkspace().serverPathExists(serverPath)) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("FindInSourceControlDialog.InvalidServerPathTitle"), //$NON-NLS-1$
                MessageFormat.format(
                    Messages.getString("FindInSourceControlDialog.NonExistingServerPathFormat"), //$NON-NLS-1$
                    serverPath));
            return;
        }

        /*
         * Server path cannot contain wildcards (Visual Studio enforces this, I
         * suppose we should, too.)
         */
        if (serverPath.contains("*") || serverPath.contains("?")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            MessageDialog.openError(
                getShell(),
                Messages.getString("FindInSourceControlDialog.InvalidServerPathTitle"), //$NON-NLS-1$
                Messages.getString("FindInSourceControlDialog.WildcardsNotAllowedInServerPath")); //$NON-NLS-1$
            return;
        }
        valid = true;
        super.okPressed();
    }

    @Override
    protected void hookDialogAboutToClose() {
        final String serverPath = pathText.getText().trim();
        String wildcard = wildcardText.getText().trim();

        if (wildcard.length() == 0) {
            wildcard = null;
        }

        final boolean recursive = recursiveButton.getSelection();

        final boolean checkedOut;
        final boolean showStatus;
        final String checkedOutUser;

        if (checkedOutAllButton.getSelection()) {
            checkedOut = true;
            checkedOutUser = null;
        } else if (checkedOutUserButton.getSelection()) {
            checkedOut = true;
            checkedOutUser = checkedOutUserText.getText().trim();

            if (checkedOutUser.length() == 0) {
                MessageDialog.openError(
                    getShell(),
                    Messages.getString("FindInSourceControlDialog.EmptyUserNameTitle"), //$NON-NLS-1$
                    Messages.getString("FindInSourceControlDialog.EmptyUserNameMessage")); //$NON-NLS-1$
                return;
            }
        } else {
            checkedOut = false;
            checkedOutUser = null;
        }

        showStatus = showCheckoutStatusButton.getSelection();

        query = new FindInSourceControlQuery(serverPath, wildcard, recursive, checkedOut, showStatus, checkedOutUser);

        // Only save selection if the user clicks find and it was a valid query
        if (valid) {
            FindInSCEQueryOptionsPersistence.persist(
                new MementoRepository(DefaultPersistenceStoreProvider.INSTANCE.getCachePersistenceStore()),
                query);
        }
    }

    public void setQuery(final FindInSourceControlQuery query) {
        Check.notNull(query, "query"); //$NON-NLS-1$

        this.query = query;
    }

    public FindInSourceControlQuery getQuery() {
        return query;
    }
}
