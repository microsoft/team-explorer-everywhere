// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadOutput;
import com.microsoft.tfs.util.Check;

/**
 * Implements {@link DownloadOutput} to write to an existing
 * {@link OutputStream}. Does not support reset.
 *
 * @threadsafety thread-safe
 */
public class OutputStreamDownloadOutput extends BaseDownloadOutput {
    private final static Log log = LogFactory.getLog(OutputStreamDownloadOutput.class);

    private final OutputStream outputStream;

    /**
     * Constructs a {@link OutputStreamDownloadOutput} that wraps an
     * already-open {@link OutputStream}.
     *
     * @param outputStream
     *        the stream to wrap (must not be <code>null</code>)
     * @param autoGunzip
     *        if <code>true</code> the bytes are gunzipped before being written
     *        to outputstream; if <code>false</code> unprocessed bytes (could be
     *        gzip, could be raw) are written to output stream
     */
    public OutputStreamDownloadOutput(final OutputStream outputStream, final boolean autoGunzip) {
        super(autoGunzip);

        Check.notNull(outputStream, "outputStream"); //$NON-NLS-1$

        this.outputStream = outputStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized OutputStream getOutputStream() throws FileNotFoundException {
        return outputStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void resetOutputStream() throws IOException {
        throw new IOException(
            MessageFormat.format("{0} does not support reset", OutputStreamDownloadOutput.class.getName())); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeOutputStream() throws IOException {
        outputStream.close();
    }
}
