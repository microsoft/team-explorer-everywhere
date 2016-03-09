// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.browser;

import java.net.URI;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;

public class URISchemeHelper {
    private static final String[] URI_SCHEME_WHITELIST = new String[] {
        "http", //$NON-NLS-1$
        "https", //$NON-NLS-1$
        "ftp", //$NON-NLS-1$
        "gopher", //$NON-NLS-1$
        "mailto", //$NON-NLS-1$
        "news", //$NON-NLS-1$
        "telnet", //$NON-NLS-1$
        "wais", //$NON-NLS-1$
        "vstfs", //$NON-NLS-1$
        "tfs", //$NON-NLS-1$
        "alm", //$NON-NLS-1$
        "mtm", //$NON-NLS-1$
        "mtms", //$NON-NLS-1$
        "mfbclient", //$NON-NLS-1$
        "mfbclients", //$NON-NLS-1$
        "x-mvwit" //$NON-NLS-1$
    };

    /**
     * Evaluates supplied address against a list of supported internet protocols
     * allowed for use on hyperlinks.
     *
     *
     * @param uri
     *        The URL to test.
     * @return Returns true for allowed protocols.
     */
    public static boolean isOnTrustedUriWhiteList(final URI uri) {
        if (uri == null || uri.getScheme() == null) {
            return false;
        }

        final String protocol = uri.getScheme().toLowerCase();
        for (final String allowedProtocol : URI_SCHEME_WHITELIST) {
            if (protocol.equalsIgnoreCase(allowedProtocol)) {
                return true;
            }
        }

        // Urls operating with an unrecognized protocol are rejected by default.
        return false;
    }

    public static void showUnsafeSchemeError(final URI uri) {
        final String format = Messages.getString("BrowserFacade.UnsafeUriSchemeErrorFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(format, uri);

        final Shell shell = ShellUtils.getBestParent(ShellUtils.getWorkbenchShell());
        MessageDialog.openError(shell, "", message); //$NON-NLS-1$
    }
}
