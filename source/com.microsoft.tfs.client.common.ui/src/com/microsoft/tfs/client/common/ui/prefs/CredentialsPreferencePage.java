// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.prefs;

import java.net.URI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.CredentialsTable;
import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.credentials.CredentialsManagerFactory;
import com.microsoft.tfs.util.Platform;

public class CredentialsPreferencePage extends BasePreferencePage {
    private final CredentialsManager credentialsManager =
        CredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);

    private CredentialsTable credentialsTable;

    private Button editButton;
    private Button removeButton;

    public CredentialsPreferencePage() {
        super();
    }

    public CredentialsPreferencePage(final String title) {
        super(title);
    }

    public CredentialsPreferencePage(final String title, final ImageDescriptor image) {
        super(title, image);
    }

    @Override
    public void init(final IWorkbench workbench) {
        noDefaultAndApplyButton();
    }

    @Override
    protected Control createContents(final Composite parent) {
        final Composite container = new Composite(parent, SWT.NONE);

        final GridLayout layout = new GridLayout(3, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        /* Windows and Mac platforms do not allow editing the credentials. */
        if (Platform.isCurrentPlatform(Platform.WINDOWS) || Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            String message;

            if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
                message = Messages.getString("CredentialsPreferencePage.EditingNotSupportedWindows"); //$NON-NLS-1$
            } else if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
                message = Messages.getString("CredentialsPreferencePage.EditingNotSupportedMac"); //$NON-NLS-1$
            } else {
                message = Messages.getString("CredentialsPreferencePage.EditingNotSupportedGeneric"); //$NON-NLS-1$
            }

            final Label label = new Label(container, SWT.NONE);
            label.setText(message);
            GridDataBuilder.newInstance().hSpan(3).hGrab().hFill().applyTo(label);

            return container;
        }

        final Label label = new Label(container, SWT.NONE);
        label.setText(Messages.getString("CredentialsPreferencePage.Description")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(3).hGrab().hFill().applyTo(label);

        credentialsTable = new CredentialsTable(container, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(3).grab().fill().applyTo(credentialsTable);
        credentialsTable.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final boolean enabled = (credentialsTable.getSelectedCredentials() != null);

                editButton.setEnabled(enabled);
                removeButton.setEnabled(enabled);
            }
        });

        removeButton = new Button(container, SWT.PUSH);
        removeButton.setText(Messages.getString("CredentialsPreferencePage.RemoveCredentialsButtonText")); //$NON-NLS-1$
        removeButton.setEnabled(false);
        GridDataBuilder.newInstance().hAlign(SWT.RIGHT).hGrab().applyTo(removeButton);
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                removeCredentials(credentialsTable.getSelectedCredentials());
            }
        });

        editButton = new Button(container, SWT.PUSH);
        editButton.setText(Messages.getString("CredentialsPreferencePage.EditCredentialsButtonText")); //$NON-NLS-1$
        editButton.setEnabled(false);
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                editCredentials(credentialsTable.getSelectedCredentials());
            }
        });

        /* Resize button sizes */
        ButtonHelper.setButtonsToButtonBarSize(new Button[] {
            editButton,
            removeButton,
        });

        credentialsTable.setCredentials(credentialsManager.getCredentials());

        return container;
    }

    private void removeCredentials(final CachedCredentials credentials) {
        if (!credentialsManager.removeCredentials(credentials)) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("CredentialsPreferencePage.UpdateFailedTitle"), //$NON-NLS-1$
                Messages.getString("CredentialsPreferencePage.UpdateFailedMessage")); //$NON-NLS-1$
            return;
        }

        credentialsTable.setCredentials(credentialsManager.getCredentials());
    }

    private void editCredentials(final CachedCredentials credentials) {
        final URI serverURI = credentials.getURI();

        final CredentialsDialog credentialsDialog = new CredentialsDialog(getShell(), serverURI);
        credentialsDialog.setCredentials(credentials.toCredentials());
        credentialsDialog.setAlwaysSavePassword(true);

        if (credentialsDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        final CachedCredentials newCredentials = new CachedCredentials(serverURI, credentialsDialog.getCredentials());

        if (!credentialsManager.setCredentials(newCredentials)) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("CredentialsPreferencePage.UpdateFailedTitle"), //$NON-NLS-1$
                Messages.getString("CredentialsPreferencePage.UpdateFailedMessage")); //$NON-NLS-1$
            return;
        }

        credentialsTable.setCredentials(credentialsManager.getCredentials());

        credentialsTable.setSelectedCredentials(newCredentials);
    }
}
