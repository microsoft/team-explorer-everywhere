// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.config;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsCompleteDialog;
import com.microsoft.tfs.client.common.ui.dialogs.connect.CredentialsCompleteListener;
import com.microsoft.tfs.core.httpclient.Credentials;

public abstract class UITransportAuthRunnable implements Runnable {
    private final Object lock = new Object();

    private Credentials credentials;
    private boolean complete = false;

    public Credentials getCredentials() {
        synchronized (lock) {
            return credentials;
        }
    }

    @Override
    public final void run() {
        synchronized (lock) {
            if (complete) {
                return;
            }
        }

        final CredentialsCompleteDialog credentialsDialog = getCredentialsDialog();

        /*
         * Subclasses may return null, indicating that they cannot handle the
         * given credentials (eg, federated credentials but we can't open a web
         * browser.)
         */
        if (credentialsDialog == null) {
            setComplete(null);
            return;
        }

        credentialsDialog.setBlockOnOpen(false);
        credentialsDialog.addCredentialsCompleteListener(new CredentialsCompleteListener() {
            @Override
            public void credentialsComplete() {
                final Credentials credentials = (credentialsDialog.getReturnCode() == IDialogConstants.OK_ID)
                    ? credentialsDialog.getCredentials() : null;

                setComplete(credentials);
            }
        });

        if (credentialsDialog.open() == IDialogConstants.CANCEL_ID) {
            setComplete(null);
        };

        /*
         * Store a copy of the display, because the shell may be closed /
         * disposed (below) while processing the UI event loop. (The display
         * itself will never be disposed unless the program has exited.)
         */
        final Display display = credentialsDialog.getShell().getDisplay();

        /* Process the UI thread until the user closes the auth dialog. */
        while (!isComplete()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    protected abstract CredentialsCompleteDialog getCredentialsDialog();

    private void setComplete(final Credentials credentials) {
        synchronized (lock) {
            this.credentials = credentials;
            this.complete = true;

            lock.notifyAll();
        }
    }

    public final boolean isComplete() {
        synchronized (lock) {
            return complete;
        }
    }

    public final void join() throws InterruptedException {
        while (true) {
            synchronized (lock) {
                if (complete) {
                    return;
                }

                lock.wait();
            }
        }
    }
}
