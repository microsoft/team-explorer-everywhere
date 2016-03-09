// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.files;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.exceptions.UnableToSaveException;
import com.microsoft.tfs.core.clients.workitem.files.DownloadException;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.methods.GetMethod;
import com.microsoft.tfs.core.httpclient.methods.PostMethod;
import com.microsoft.tfs.core.httpclient.methods.multipart.FilePart;
import com.microsoft.tfs.core.httpclient.methods.multipart.MultipartRequestEntity;
import com.microsoft.tfs.core.httpclient.methods.multipart.Part;
import com.microsoft.tfs.core.httpclient.methods.multipart.StringPart;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

public class AttachmentUpDownHelper {
    private static final int DOWNLOAD_BUFFER_SIZE = 2048;
    private static final Log log = LogFactory.getLog(AttachmentUpDownHelper.class);

    public static void download(
        final URL attachmentUrl,
        final File localTarget,
        final TFSTeamProjectCollection connection) throws DownloadException {
        final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();
        final HttpClient httpClient = connection.getHTTPClient();
        final GetMethod method = new GetMethod(attachmentUrl.toExternalForm());
        boolean cancelled = false;
        OutputStream outputStream = null;

        try {
            final int statusCode = httpClient.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) {
                throw new DownloadException(
                    MessageFormat.format(
                        Messages.getString("AttachmentUpDownHelper.ServerReturnedHTTPStatusFormat"), //$NON-NLS-1$
                        Integer.toString(statusCode)));
            }

            taskMonitor.begin(
                Messages.getString("AttachmentUpDownHelper.Downloading"), //$NON-NLS-1$
                computeTaskSize(method.getResponseContentLength()));

            final InputStream input = method.getResponseBodyAsStream();
            outputStream = new FileOutputStream(localTarget);
            outputStream = new BufferedOutputStream(outputStream);

            final byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            int len;
            long totalBytesDownloaded = 0;

            while ((len = input.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
                totalBytesDownloaded += len;
                taskMonitor.worked(1);
                taskMonitor.setCurrentWorkDescription(
                    MessageFormat.format(
                        Messages.getString("AttachmentUpDownHelper.DownloadedCountBytesFormat"), //$NON-NLS-1$
                        totalBytesDownloaded));
                if (taskMonitor.isCanceled()) {
                    cancelled = true;
                    break;
                }
                Thread.sleep(10);
            }

        } catch (final Exception ex) {
            throw new DownloadException(ex);
        } finally {
            method.releaseConnection();
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (final IOException e) {
                }
            }
        }

        if (cancelled) {
            localTarget.delete();
        }
    }

    private static int computeTaskSize(final long responseContentLength) {
        return (int) Math.ceil(((double) responseContentLength) / DOWNLOAD_BUFFER_SIZE);
    }

    public static String upload(
        final String areaNodeUri,
        final String projectUri,
        final File fileToUpload,
        final String uploadUrl,
        final TFSTeamProjectCollection connection) throws UnableToSaveException {
        final String guid = GUID.newGUIDString();

        final HttpClient httpClient = connection.getHTTPClient();

        final Part[] parts = new Part[4];

        parts[0] = new StringPart("AreaNodeUri", areaNodeUri); //$NON-NLS-1$
        parts[1] = new StringPart("ProjectUri", projectUri); //$NON-NLS-1$
        parts[2] = new StringPart("FileNameGUID", guid); //$NON-NLS-1$
        try {
            parts[3] = new FilePart("Content", "FileNameGUID", fileToUpload); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (final FileNotFoundException ex) {
            throw new UnableToSaveException(
                MessageFormat.format(
                    Messages.getString("AttachmentUpDownHelper.FileNotFoundFormat"), //$NON-NLS-1$
                    fileToUpload.getAbsolutePath()),
                ex);
        }

        final PostMethod method = new PostMethod(uploadUrl);
        method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));

        int status;
        try {
            status = httpClient.executeMethod(method);
        } catch (final Exception ex) {
            throw new UnableToSaveException(Messages.getString("AttachmentUpDownHelper.ErrorUploadingFile"), ex); //$NON-NLS-1$
        } finally {
            method.releaseConnection();
        }

        if (status != HttpStatus.SC_OK) {
            throw new UnableToSaveException(
                MessageFormat.format(
                    Messages.getString("AttachmentUpDownHelper.ServerReturnedHTTPStatusFormat"), //$NON-NLS-1$
                    Integer.toString(status)));
        }

        return guid;
    }
}
