// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.connect;

import java.net.URI;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials.PatCredentials;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class ServerAuthDialog extends BaseDialog {

    private final URI serverURI;
    private Credentials credentials;
    private String savedUserName = StringUtil.EMPTY;

    private Text usernameText;
    private Text passwordText;
    private Button userpasswordButton;
    private Button patButton;

    public ServerAuthDialog(final Shell parent, final URI serverURI) {
        super(parent);

        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        this.serverURI = serverURI;

        setOptionPersistGeometry(false);
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ServerAuthDialog.DialogTitleText"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        final String messageFormat = Messages.getString("ServerAuthDialog.PromptMessageFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, serverURI.toString());

        final Label promptLabel = new Label(dialogArea, SWT.NONE);
        promptLabel.setText(message);
        GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().wHint(
            convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH)).applyTo(promptLabel);

        final Label credentialsTypeLabel = new Label(dialogArea, SWT.NONE);
        credentialsTypeLabel.setText(Messages.getString("ServerAuthDialog.CredentialsTypeLabel")); //$NON-NLS-1$

        final Composite buttonComposite = new Composite(dialogArea, SWT.NONE);
        final GridLayout buttonCompositeLayout = new GridLayout(2, false);
        buttonCompositeLayout.horizontalSpacing = getHorizontalSpacing();
        buttonCompositeLayout.verticalSpacing = getVerticalSpacing();
        buttonCompositeLayout.marginWidth = 0;
        buttonCompositeLayout.marginHeight = 0;
        buttonComposite.setLayout(buttonCompositeLayout);

        userpasswordButton = new Button(buttonComposite, SWT.RADIO);
        userpasswordButton.setText(Messages.getString("ServerAuthDialog.UserPasswordTypeLabel")); //$NON-NLS-1$
        userpasswordButton.setSelection(true);
        // userpasswordButton.addSelectionListener(new SelectionAdapter() {
        // @Override
        // public void widgetSelected(final SelectionEvent e) {
        // onCredentialTypeSelected();
        // }
        // });

        patButton = new Button(buttonComposite, SWT.RADIO);
        patButton.setText(Messages.getString("ServerAuthDialog.PatTypeLabel")); //$NON-NLS-1$
        patButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                onCredentialTypeSelected();
            }
        });

        final Label usernameLabel = new Label(dialogArea, SWT.NONE);
        usernameLabel.setText(Messages.getString("ServerAuthDialog.UsernameLabel")); //$NON-NLS-1$

        usernameText = new Text(dialogArea, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(usernameText);

        final Label passwordLabel = new Label(dialogArea, SWT.NONE);
        passwordLabel.setText(Messages.getString("ServerAuthDialog.PasswordTokenLabel")); //$NON-NLS-1$

        passwordText = new Text(dialogArea, SWT.PASSWORD | SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().wCHint(passwordText, 16).applyTo(passwordText);

        if (credentials == null || credentials instanceof PatCredentials) {
            patButton.setSelection(true);
            userpasswordButton.setSelection(false);
        } else if (credentials instanceof UsernamePasswordCredentials) {
            savedUserName = ((UsernamePasswordCredentials) credentials).getUsername();
            usernameText.setText(savedUserName);
            patButton.setSelection(false);
            userpasswordButton.setSelection(true);
        }

        if (usernameText.getText().length() > 0) {
            passwordText.setFocus();
        }

        onCredentialTypeSelected();
    }

    private void onCredentialTypeSelected() {
        if (patButton.getSelection()) {
            savedUserName = usernameText.getText();
            usernameText.setText(StringUtil.EMPTY);
        } else {
            usernameText.setText(savedUserName);
        }
        usernameText.setEnabled(userpasswordButton.getSelection());
    }

    @Override
    protected void okPressed() {
        final String username = usernameText.getText();
        final String password = passwordText.getText();

        if (userpasswordButton.getSelection()) {
            credentials = new UsernamePasswordCredentials(username, password);
        } else if (userpasswordButton.getSelection()) {
            credentials = new PatCredentials(password);
        }

        super.okPressed();
    }
}
