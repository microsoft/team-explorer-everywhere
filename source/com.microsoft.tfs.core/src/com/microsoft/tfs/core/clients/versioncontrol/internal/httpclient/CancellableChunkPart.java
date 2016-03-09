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

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;

/**
 * Extends CancellableFilePart (which represents file data in a MIME multi-part
 * message), but uses a buffered input stream instead of a File.
 * <p>
 * To use this part, make sure to wrap your method execution invocation with a
 * catch for SendDataCancellationException, which is thrown on cancel.
 * SendDataCancellationException extends HttpException, so even if you don't
 * catch it explicitly, cancellation will be unavoidably reported to you.
 */
public class CancellableChunkPart extends CancellableFilePart {
    /** Log object for this class. */
    private static final Log logger = LogFactory.getLog(CancellableChunkPart.class);

    final InputStream stream;
    final long chunkSize;

    public CancellableChunkPart(
        final File file,
        final InputStream stream,
        final String contentType,
        final long chunkSize) throws FileNotFoundException {
        /*
         * NOTE We construct the file part in a special way so the character set
         * is never sent to the server (TFS can't handle that header, and it
         * causes an internal server error). If we construct the
         * CancellableFilePart object with a null charset, the header is still
         * included, and its value is the default charset. If we invoke
         * setCharSet() with null, the header is never supplied (which is what
         * we desire).
         *
         * Also, we use the file name "item" to match Visual Studio's
         * implementation. Sending the actual file name doesn't seem to hurt,
         * but appears to be ignored by the server.
         */
        super("content", "item", file, contentType, null); //$NON-NLS-1$ //$NON-NLS-2$
        setCharSet(null);

        Check.isTrue(stream.markSupported(), "The stream does not support retry."); //$NON-NLS-1$
        this.stream = stream;

        this.chunkSize = chunkSize;
    }

    /**
     * Write the data in "source" to the specified stream.
     *
     * @param out
     *        The output stream.
     * @throws IOException
     *         if an IO problem occurs.
     * @see com.microsoft.tfs.core.httpclient.methods.multipart.FilePart#sendData(OutputStream)
     */
    @Override
    protected void sendData(final OutputStream out) throws IOException {
        final TaskMonitor monitor = TaskMonitorService.getTaskMonitor();
        final byte[] tmp = new byte[BUFFER_SIZE];

        /*
         * We might be retrying upload, so first jump to the beginning of the
         * chunk. And also reset the monitor because this could be low-level
         * upload retry caused by redirection.
         */
        stream.reset();
        monitor.begin(StringUtil.EMPTY, 0);

        long dataLeft = chunkSize;
        int len;

        while ((len = stream.read(tmp, 0, (int) Math.min(BUFFER_SIZE, Math.max(dataLeft, 0)))) > 0) {
            if (monitor.isCanceled()) {
                throw new SendDataCancellationException();
            }

            out.write(tmp, 0, len);
            dataLeft -= len;
            monitor.worked(len);
        }

        monitor.done();
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

    /**
     * Return the length of the data.
     *
     * @return The length.
     * @throws IOException
     *         if an IO problem occurs
     * @see com.microsoft.tfs.core.httpclient.methods.multipart.Part#lengthOfData()
     */
    @Override
    protected long lengthOfData() throws IOException {
        logger.trace("enter lengthOfData()"); //$NON-NLS-1$
        return chunkSize;
    }
}
