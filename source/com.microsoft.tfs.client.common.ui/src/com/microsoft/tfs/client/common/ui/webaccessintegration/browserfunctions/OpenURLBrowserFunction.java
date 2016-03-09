// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.webaccessintegration.browserfunctions;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.commands.wit.DownloadFileAttachmentCommand;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.wit.DownloadAttachmentOpenType;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.ShellUtils;
import com.microsoft.tfs.client.common.ui.framework.launcher.Launcher;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ArtifactLinkHelpers;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.temp.TempStorageService;

public class OpenURLBrowserFunction extends BrowserFunction {
    private static final Pattern OPEN_ATTCHMENT_URL =
        Pattern.compile(".*_wit/downloadattachment?.*filename=([^&$]*)", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private final TFSServer server;
    private String lastDownloadDirectory;

    public OpenURLBrowserFunction(final Browser browser, final String name, final TFSServer server) {
        super(browser, name);
        this.server = server;
    }

    @Override
    public Object function(final Object[] arguments) {
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$
        Check.isTrue(arguments.length == 1, "arguments.length == 1"); //$NON-NLS-1$
        Check.isTrue(arguments[0] instanceof String, "arguments[0] instanceof String"); //$NON-NLS-1$

        final String uri = ((String) arguments[0]);
        final String uriLower = uri.toLowerCase();

        if (uriLower.startsWith("http://") || uriLower.startsWith("https://")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            final Shell shell = ShellUtils.getParentShell(getBrowser());
            ArtifactLinkHelpers.openHyperlinkLink(shell, (String) arguments[0]);
            return true;
        } else if (uriLower.startsWith("file://///")) //$NON-NLS-1$
        {
            final String uncPath = "\\\\" + uri.substring(10).replace("/", "\\"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            Launcher.launch(uncPath);
            return true;
        } else {
            final String decoded = URIUtils.decodeForDisplay(uri);
            final Matcher matcher = OPEN_ATTCHMENT_URL.matcher(decoded);

            if (matcher.find()) {
                if (matcher.groupCount() > 0) {
                    final String attachmentFileName = matcher.group(1);
                    final Shell shell = getBrowser().getShell();

                    if (uriLower.indexOf("contentonly=true") != -1) //$NON-NLS-1$
                    {
                        performOpen(shell, getURL(uri), attachmentFileName);
                        return true;
                    } else {
                        performDownloadTo(getBrowser().getShell(), getURL(uri), attachmentFileName);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void performOpen(final Shell shell, final URL attachmentURL, final String attachmentFileName) {
        final DownloadAttachmentOpenType openType = DownloadAttachmentOpenType.getPreferredOpenType();

        String toLaunch = null;

        if (openType == DownloadAttachmentOpenType.BROWSER) {
            toLaunch = attachmentURL.toExternalForm();
        } else {
            String extension = null;
            final int ix = attachmentFileName.lastIndexOf("."); //$NON-NLS-1$
            if (ix != -1) {
                extension = attachmentFileName.substring(ix);
            }

            try {
                toLaunch = TempStorageService.getInstance().createTempFile(extension).getAbsolutePath();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            final DownloadFileAttachmentCommand downloadCommand =
                new DownloadFileAttachmentCommand(attachmentURL, new File(toLaunch), server.getConnection());

            final IStatus downloadStatus =
                UICommandExecutorFactory.newUICommandExecutor(shell).execute(downloadCommand);

            if (!downloadStatus.isOK() && downloadStatus.getSeverity() != IStatus.CANCEL) {
                return;
            }
        }

        Launcher.launch(toLaunch);
    }

    private void performDownloadTo(final Shell shell, final URL attachmentURL, final String attachmentFileName) {
        final DirectoryDialog dialog = new DirectoryDialog(shell);

        if (lastDownloadDirectory != null) {
            dialog.setFilterPath(lastDownloadDirectory);
        }

        final String directoryPath = dialog.open();
        if (directoryPath != null) {
            lastDownloadDirectory = directoryPath;
            final File targetFile = new File(directoryPath, attachmentFileName);

            if (targetFile.exists()) {
                final String title = Messages.getString("FileAttachmentsControl.ConfirmOverwriteDialogTitle"); //$NON-NLS-1$
                final String messageFormat =
                    Messages.getString("FileAttachmentsControl.ConfirmOverwriteDialogTextFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, targetFile.getAbsolutePath());

                if (!MessageBoxHelpers.dialogConfirmPrompt(shell, title, message)) {
                    return;
                }
            }

            final DownloadFileAttachmentCommand downloadCommand =
                new DownloadFileAttachmentCommand(attachmentURL, targetFile, server.getConnection());
            UICommandExecutorFactory.newUICommandExecutor(shell).execute(downloadCommand);
        }
    }

    private URL getURL(final String relativeURI) {
        try {
            final URI baseURI = server.getConnection().getBaseURI();

            final StringBuffer sb = new StringBuffer();
            sb.append(baseURI.getScheme());
            sb.append("://"); //$NON-NLS-1$
            sb.append(baseURI.getHost());

            if (baseURI.getPort() != -1) {
                sb.append(":"); //$NON-NLS-1$
                sb.append(baseURI.getPort());
            }

            sb.append(relativeURI);
            return new URL(sb.toString());
        } catch (final MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }
}
