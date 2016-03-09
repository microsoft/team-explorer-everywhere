// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.process;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@link ProcessRunner} creates these if it needs to read output from a child
 * process and write it to another place. Because this task is best done in
 * another thread for interruptability, the logic resides here.
 * <p>
 * There is usually one {@link ProcessOutputReader} per stream (standard output
 * or standard error).
 * <p>
 * run() always closes the input stream when it returns. The output stream is
 * not closed. If an exception happens reading from the input stream or writing
 * to the output stream, the thread simply terminates. No error status is
 * reported.
 * <p>
 * Simply interrupt the thread running this reader to cause it to close the
 * input stream and terminate run() early. Because reading from the input stream
 * is a blocking operation, run() may not finish until the read returns (the
 * input stream has new data or is at its end).
 * <p>
 * This class is immutable (and therefore thread-safe).
 */
class ProcessOutputReader implements Runnable {
    private static final Log log = LogFactory.getLog(ProcessOutputReader.class);

    private final InputStream input;
    private final OutputStream output;

    /**
     * The size in bytes of the buffer used to transfer data from the stream to
     * the buffer.
     */
    private final static int BUFFER_SIZE = 4096;

    /**
     * Creates a reader for the given input stream and output stream. The output
     * stream may be null, and in that case the input stream is read and the
     * data discarded.
     *
     * @param input
     *        the stream to read input from (not null).
     * @param output
     *        the stream to write data to (may be null).
     */
    protected ProcessOutputReader(final InputStream input, final OutputStream output) {
        this.input = input;
        this.output = output;
    }

    @Override
    public void run() {
        final BufferedInputStream bis = new BufferedInputStream(input);
        long bytesRead = 0;
        long bytesWritten = 0;

        try {
            final byte[] buffer = new byte[BUFFER_SIZE];

            int bytesReadThisTime;
            try {
                if (Thread.currentThread().isInterrupted()) {
                    log.debug("Normal interruption"); //$NON-NLS-1$
                    return;
                }

                while ((bytesReadThisTime = bis.read(buffer)) != -1) {
                    bytesRead += bytesReadThisTime;

                    if (Thread.currentThread().isInterrupted()) {
                        log.debug("Normal interruption"); //$NON-NLS-1$
                        return;
                    }

                    if (output != null) {
                        output.write(buffer, 0, bytesReadThisTime);
                        bytesWritten += bytesReadThisTime;
                    }
                }
            } catch (final IOException e) {
                log.warn("Error writing to output stream", e); //$NON-NLS-1$
            }
        } finally {
            try {
                bis.close();
            } catch (final IOException e) {
                log.warn("Error closing buffered input stream", e); //$NON-NLS-1$
            }

            final String messageFormat = "Read {0} bytes from input, wrote {1} bytes to output"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, bytesRead, bytesWritten);
            log.debug(message);
        }
    }
}
