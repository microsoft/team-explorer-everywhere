// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.MessageFormat;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.methods.GetMethod;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;
import com.microsoft.tfs.util.temp.TempStorageService;

public class HTTPTempDownloader {
    private static final int DOWNLOAD_BUFFER_SIZE = 2048;

    public static File downloadToTempLocation(final TFSTeamProjectCollection connection, final URI uri) {
        final String file = uri.getPath();
        final String localFileName = file.substring(file.lastIndexOf('/'));

        /*
         * Create a temporary directory using the TempStorageService, since it
         * can clean up automatically when this application exits.
         */
        File tempDir;
        try {
            tempDir = TempStorageService.getInstance().createTempDirectory();
            Check.notNull(tempDir, "tempDir"); //$NON-NLS-1$
        } catch (final IOException ex) {
            throw new RuntimeException(ex);
        }

        final File tempFile = new File(tempDir, localFileName);

        // download the file.

        final TaskMonitor taskMonitor = TaskMonitorService.getTaskMonitor();
        final HttpClient httpClient = connection.getHTTPClient();
        final GetMethod method = new GetMethod(uri.toString());
        boolean cancelled = false;
        OutputStream outputStream = null;

        try {
            final int statusCode = httpClient.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) {
                final String messageFormat = Messages.getString("HttpTempDownloader.ServerErrorCodeFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, Integer.toString(statusCode));
                throw new RuntimeException(message);
            }

            taskMonitor.begin(
                Messages.getString("HttpTempDownloader.ProgressMonitorStatus"), //$NON-NLS-1$
                computeTaskSize(method.getResponseContentLength()));

            final InputStream input = method.getResponseBodyAsStream();
            outputStream = new FileOutputStream(tempFile);
            outputStream = new BufferedOutputStream(outputStream);

            final byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            int len;
            long totalBytesDownloaded = 0;

            while ((len = input.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
                totalBytesDownloaded += len;
                taskMonitor.worked(1);

                final String messageFormat = Messages.getString("HttpTempDownloader.ProgressMonitorTextFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, totalBytesDownloaded);
                taskMonitor.setCurrentWorkDescription(message);

                if (taskMonitor.isCanceled()) {
                    cancelled = true;
                    break;
                }
                Thread.sleep(10);
            }

        } catch (final Exception ex) {
            throw new RuntimeException(ex);
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
            tempFile.delete();
        }

        return tempFile;
    }

    private static int computeTaskSize(final long responseContentLength) {
        return (int) Math.ceil(((double) responseContentLength) / DOWNLOAD_BUFFER_SIZE);
    }
}