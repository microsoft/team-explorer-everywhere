// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.connect;

import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.core.httpclient.Credentials;

public abstract class CredentialsCompleteDialog extends BaseDialog {
    public CredentialsCompleteDialog(final Shell parentShell) {
        super(parentShell);
    }

    public abstract void addCredentialsCompleteListener(CredentialsCompleteListener listener);

    public abstract Credentials getCredentials();
}
