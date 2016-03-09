// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadOutput;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.IOUtils;

/**
 * Implements {@link DownloadOutput} to write to a {@link FileOutputStream} at a
 * specified path. Supports reset and reopen of the stream after a failure.
 *
 * @threadsafety thread-safe
 */
public class FileDownloadOutput extends BaseDownloadOutput {
    private final static Log log = LogFactory.getLog(FileDownloadOutput.class);

    private final File outputFile;

    /**
     * The output stream in use. <code>null</code> when uninitialized or after a
     * reset.
     */
    private FileOutputStream outputStream;

    /**
     * Constructs a {@link FileDownloadOutput} that writes to a file.
     *
     * @param outputFile
     *        the output file where downloaded bytes are written (must not be
     *        <code>null</code>)
     * @param autoGunzip
     *        if <code>true</code> the bytes are gunzipped before being written
     *        to outputstream; if <code>false</code> unprocessed bytes (could be
     *        gzip, could be raw) are written to output stream
     */
    public FileDownloadOutput(final File outputFile, final boolean autoGunzip) {
        super(autoGunzip);

        Check.notNull(outputFile, "outputFile"); //$NON-NLS-1$

        this.outputFile = outputFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized OutputStream getOutputStream() throws FileNotFoundException {
        if (outputStream == null) {
            if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) {
                /* Double-check before logging to avoid mkdirs race condition */
                if (!outputFile.getParentFile().isDirectory()) {
                    log.warn(MessageFormat.format(
                        "mkdirs() failed on {0}, output stream will probably fail", //$NON-NLS-1$
                        outputFile.getParentFile()));
                }
            }

            outputStream = new FileOutputStream(outputFile);
        }

        return outputStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void resetOutputStream() throws IOException {
        // Simply close and we're ready for another call to getOutputStream()
        closeOutputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeOutputStream() throws IOException {
        if (outputStream != null) {
            IOUtils.closeSafely(outputStream);
            outputStream = null;
        }
    }
}
