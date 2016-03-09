// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.connect;

import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.connect.ConnectionErrorControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManagerFactory;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class CredentialsDialog extends CredentialsCompleteDialog {
    public static final String USERNAME_TEXT_ID = "CredentialsDialog.userText"; //$NON-NLS-1$
    public static final String PASSWORD_TEXT_ID = "CredentialsDialog.passwordText"; //$NON-NLS-1$
    public static final String SAVE_PASSWORD_BUTTON_ID = "CredentialsDialog.savePasswordButton"; //$NON-NLS-1$

    private static final Log log = LogFactory.getLog(CredentialsDialog.class);

    private final URI serverURI;
    private Credentials credentials;
    private String errorMessage;

    private Text usernameText;
    private Text passwordText;
    private Button savePasswordButton;

    private Composite insecureComposite;
    private Label insecureSaveSpacer;
    private Label insecureSaveImageLabel;
    private Label insecureSaveTextLabel;

    private boolean allowSavePassword = false;
    private boolean alwaysSavePassword = false;

    private volatile boolean isSavePasswordChecked = false;

    private final SingleListenerFacade credentialsCompleteListeners =
        new SingleListenerFacade(CredentialsCompleteListener.class);

    public CredentialsDialog(final Shell parent, final URI serverURI) {
        super(parent);

        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        this.serverURI = serverURI;

        setOptionPersistGeometry(false);
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public Credentials getCredentials() {
        return credentials;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getError() {
        return errorMessage;
    }

    public void setAllowSavePassword(final boolean allowSavePassword) {
        this.allowSavePassword = allowSavePassword;
    }

    public void setAlwaysSavePassword(final boolean alwaysSavePassword) {
        this.alwaysSavePassword = alwaysSavePassword;
    }

    @Override
    protected String provideDialogTitle() {
        return Messages.getString("CredentialsDialog.DialogTitle"); //$NON-NLS-1$
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        if (errorMessage != null) {
            String profileMessage = ""; //$NON-NLS-1$

            if (serverURI != null) {
                final String subMessageFormat = Messages.getString("CredentialsDialog.EnterPasswordFormat"); //$NON-NLS-1$
                profileMessage = MessageFormat.format(subMessageFormat, serverURI.toString());
            }

            final String message = (serverURI == null) ? errorMessage : errorMessage + "\n\n" + profileMessage; //$NON-NLS-1$

            final ConnectionErrorControl errorControl = new ConnectionErrorControl(dialogArea, SWT.NONE);
            errorControl.setServerURI(serverURI);
            errorControl.setMessage(message);
            errorControl.setMessageWidthHint(
                convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH));
            GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().applyTo(errorControl);

            final Label spacerLabel = new Label(dialogArea, SWT.NONE);
            spacerLabel.setText(""); //$NON-NLS-1$
            GridDataBuilder.newInstance().hSpan(2).applyTo(spacerLabel);
        } else {
            final String messageFormat = Messages.getString("CredentialsDialog.EnterPasswordFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, serverURI.toString());

            final Label promptLabel = new Label(dialogArea, SWT.NONE);
            promptLabel.setText(message);
            GridDataBuilder.newInstance().hSpan(2).hGrab().hFill().wHint(
                convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH)).applyTo(promptLabel);

            final Label spacerLabel = new Label(dialogArea, SWT.NONE);
            spacerLabel.setText(""); //$NON-NLS-1$
            GridDataBuilder.newInstance().hSpan(2).applyTo(spacerLabel);
        }

        final Label usernameLabel = new Label(dialogArea, SWT.NONE);
        usernameLabel.setText(Messages.getString("CredentialsDialog.UserNameLabelText")); //$NON-NLS-1$

        usernameText = new Text(dialogArea, SWT.BORDER);
        AutomationIDHelper.setWidgetID(usernameText, USERNAME_TEXT_ID);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(usernameText);

        if (credentials != null && credentials instanceof UsernamePasswordCredentials) {
            final String username = ((UsernamePasswordCredentials) credentials).getUsername();

            usernameText.setText(username);
        }

        final Label passwordLabel = new Label(dialogArea, SWT.NONE);
        passwordLabel.setText(Messages.getString("CredentialsDialog.PasswordLabelText")); //$NON-NLS-1$

        passwordText = new Text(dialogArea, SWT.PASSWORD | SWT.BORDER);
        AutomationIDHelper.setWidgetID(passwordText, PASSWORD_TEXT_ID);
        GridDataBuilder.newInstance().hGrab().hFill().wCHint(passwordText, 16).applyTo(passwordText);

        if (allowSavePassword) {
            SWTUtil.createGridLayoutSpacer(dialogArea, 1, 1);

            savePasswordButton = new Button(dialogArea, SWT.CHECK);
            AutomationIDHelper.setWidgetID(savePasswordButton, SAVE_PASSWORD_BUTTON_ID);
            savePasswordButton.setText(Messages.getString("CredentialsDialog.SavePasswordButtonText")); //$NON-NLS-1$

            if (!CredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE).isSecure()) {
                savePasswordButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                        toggleInsecureWarning();
                    }
                });

                insecureComposite = dialogArea;
            }
        }

        if (alwaysSavePassword) {
            if (!CredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE).isSecure()) {
                insecureComposite = dialogArea;
                createInsecureWarning();
            }

            if (credentials != null && credentials instanceof UsernamePasswordCredentials) {
                final String password = ((UsernamePasswordCredentials) credentials).getPassword();

                if (password != null && password.length() > 0) {
                    passwordText.setText(password);
                }
            }
        }

        if (usernameText.getText().length() > 0) {
            passwordText.setFocus();
        }
    }

    private void toggleInsecureWarning() {
        final Point windowSize = getShell().getSize();
        final Point oldSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);

        if (savePasswordButton.getSelection()) {
            createInsecureWarning();
        } else {
            insecureSaveSpacer.dispose();
            insecureSaveSpacer = null;

            insecureSaveImageLabel.dispose();
            insecureSaveImageLabel = null;

            insecureSaveTextLabel.dispose();
            insecureSaveTextLabel = null;
        }

        getShell().layout();

        final Point newSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        getShell().setSize(new Point(windowSize.x, windowSize.y + (newSize.y - oldSize.y)));
    }

    private void createInsecureWarning() {
        insecureSaveSpacer = new Label(insecureComposite, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(2).applyTo(insecureSaveSpacer);

        insecureSaveImageLabel = new Label(insecureComposite, SWT.NONE);
        insecureSaveImageLabel.setImage(
            PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
        GridDataBuilder.newInstance().hAlign(SWT.RIGHT).vAlign(SWT.TOP).applyTo(insecureSaveImageLabel);

        insecureSaveTextLabel = new Label(insecureComposite, SWT.WRAP);
        insecureSaveTextLabel.setText(Messages.getString("CredentialsDialog.InsecureStorageWarning")); //$NON-NLS-1$
        GridDataBuilder.newInstance().fill().align(SWT.FILL, SWT.BEGINNING).hGrab().wHint(
            IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH).applyTo(insecureSaveTextLabel);
    }

    @Override
    protected void okPressed() {
        final String username = usernameText.getText();
        final String password = passwordText.getText();

        credentials = new UsernamePasswordCredentials(username, password);

        /* Save the username and password if the user requested it. */
        if (allowSavePassword && savePasswordButton.getSelection()) {
            if (!CredentialsManagerFactory.getCredentialsManager(
                DefaultPersistenceStoreProvider.INSTANCE).setCredentials(
                    new CachedCredentials(serverURI, usernameText.getText(), passwordText.getText()))) {
                Shell shell = ShellUtils.getBestParent(null);

                if (shell == null) {
                    shell = ShellUtils.getWorkbenchShell();
                }

                MessageDialog.openError(
                    shell,
                    Messages.getString("CredentialsDialog.SavePasswordFailedTitle"), //$NON-NLS-1$
                    Messages.getString("CredentialsDialog.SavePasswordFailedMessage")); //$NON-NLS-1$

                return;
            }
        }

        super.okPressed();
    }

    @Override
    protected void hookDialogAboutToClose() {
        ((CredentialsCompleteListener) credentialsCompleteListeners.getListener()).credentialsComplete();
    }

    @Override
    public void addCredentialsCompleteListener(final CredentialsCompleteListener listener) {
        credentialsCompleteListeners.addListener(listener);
    }

    public boolean isSavePasswordChecked() {
        return isSavePasswordChecked;
    }
}
