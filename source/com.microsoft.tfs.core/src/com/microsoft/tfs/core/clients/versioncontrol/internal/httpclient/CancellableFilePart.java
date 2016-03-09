// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.httpclient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.core.httpclient.HttpException;
import com.microsoft.tfs.core.httpclient.methods.multipart.FilePart;
import com.microsoft.tfs.core.httpclient.methods.multipart.PartSource;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * Extends Apache's FilePart (which represents file data in a MIME multi-part
 * message) to be cancellable during file upload.
 * <p>
 * To use this part, make sure to wrap your method execution invocation with a
 * catch for SendDataCancellationException, which is thrown on cancel.
 * SendDataCancellationException extends HttpException, so even if you don't
 * catch it explicitly, cancellation will be unavoidably reported to you.
 */
public class CancellableFilePart extends FilePart {
    /** Log object for this class. */
    private static final Log logger = LogFactory.getLog(CancellableFilePart.class);

    /**
     * The default size of the I/O buffer used to read file from disk and
     * populate the HTTP output stream.
     *
     * This value could be overridden by setting the TF_DEFAULT_BUFFER_SIZE
     * environment variable.
     */
    protected static final int DEFAULT_BUFFER_SIZE = 64 * 1024;
    protected static final int BUFFER_SIZE = getBufferSize();

    /**
     * This is kind of a hack. We would like to throw
     * {@link CoreCancelException}, but Commons HTTP client can't deal with it.
     * Instead, we throw this exception, which extends HttpException, and allows
     * for the correct bits of the socket connection to be cleaned up via
     * Commons HTTP.
     * <p>
     * We can test for this type of exception in the code that uses
     * {@link CancellableFilePart} and rethrow as CancelException.
     */
    @SuppressWarnings("serial")
    public static final class SendDataCancellationException extends HttpException {
        public SendDataCancellationException() {
        }
    }

    public CancellableFilePart(final String name, final PartSource partSource) {
        super(name, partSource);
    }

    public CancellableFilePart(final String name, final File file) throws FileNotFoundException {
        super(name, file);
    }

    public CancellableFilePart(final String name, final String fileName, final File file) throws FileNotFoundException {
        super(name, fileName, file);
    }

    public CancellableFilePart(
        final String name,
        final PartSource partSource,
        final String contentType,
        final String charset) {
        super(name, partSource, contentType, charset);
    }

    public CancellableFilePart(final String name, final File file, final String contentType, final String charset)
        throws FileNotFoundException {
        super(name, file, contentType, charset);
    }

    public CancellableFilePart(
        final String name,
        final String fileName,
        final File file,
        final String contentType,
        final String charset) throws FileNotFoundException {
        super(name, fileName, file, contentType, charset);
    }

    /**
     * Write the data in "source" to the specified stream.
     *
     * This implementation is based on FilePart's implementation with the
     * addition of a check for cancellation.
     *
     * @param out
     *        The output stream.
     * @throws IOException
     *         if an IO problem occurs.
     * @see com.microsoft.tfs.core.httpclient.methods.multipart.FilePart#sendData(OutputStream)
     */
    @Override
    protected void sendData(final OutputStream out) throws IOException {
        if (lengthOfData() == 0) {
            // this file contains no data, so there is nothing to send.
            // we don't want to create a zero length buffer as this will
            // cause an infinite loop when reading.
            return;
        }

        final TaskMonitor monitor = TaskMonitorService.getTaskMonitor();

        final byte[] tmp = new byte[BUFFER_SIZE];
        final InputStream instream = getSource().createInputStream();

        try {
            /*
             * Reset the monitor because this could be low-level upload retry
             * caused by redirection.
             */
            monitor.begin(StringUtil.EMPTY, 0);

            int len;
            while ((len = instream.read(tmp)) >= 0) {
                if (monitor.isCanceled()) {
                    throw new SendDataCancellationException();
                }

                out.write(tmp, 0, len);
                monitor.worked(len);
            }

            monitor.done();
        } finally {
            // we're done with the stream, close it
            instream.close();
        }
    }

    /**
     * Tests if this part can be sent more than once.
     *
     * @return <code>true</code> if {@link #sendData(OutputStream)} can be
     *         successfully called more than once.
     * @since HttpClient 3.0
     */
    @Override
    public boolean isRepeatable() {
        return true;
    }

    protected static int getBufferSize() {
        final int bufferSize =
            EnvironmentVariables.getInt(EnvironmentVariables.UPLOAD_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);

        if (bufferSize > 0) {
            return bufferSize;
        } else {
            logger.warn("Wrong buffer size defined. Continue using the default value " + DEFAULT_BUFFER_SIZE); //$NON-NLS-1$
            return DEFAULT_BUFFER_SIZE;
        }
    }
}
