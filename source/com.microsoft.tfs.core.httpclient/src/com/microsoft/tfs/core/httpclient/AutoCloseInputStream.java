/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient
 * /src/java/org/apache/commons/httpclient/AutoCloseInputStream.java,v 1.9
 * 2004/04/18 23:51:34 jsdever Exp $ $Revision: 505890 $ $Date: 2007-02-11
 * 12:25:25 +0100 (Sun, 11 Feb 2007) $
 *
 * ====================================================================
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */

package com.microsoft.tfs.core.httpclient;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Closes an underlying stream as soon as the end of the stream is reached, and
 * notifies a client when it has done so.
 *
 * @author Ortwin Glueck
 * @author Eric Johnson
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 *
 * @since 2.0
 */
class AutoCloseInputStream extends FilterInputStream {

    /**
     * True if this stream is open. Assume that the underlying stream is open
     * until we get an EOF indication.
     */
    private boolean streamOpen = true;

    /** True if the stream closed itself. */
    private boolean selfClosed = false;

    /**
     * The watcher is notified when the contents of the stream have been
     * exhausted
     */
    private ResponseConsumedWatcher watcher = null;

    /**
     * Create a new auto closing stream for the provided connection
     *
     * @param in
     *        the input stream to read from
     * @param watcher
     *        To be notified when the contents of the stream have been consumed.
     */
    public AutoCloseInputStream(final InputStream in, final ResponseConsumedWatcher watcher) {
        super(in);
        this.watcher = watcher;
    }

    /**
     * Reads the next byte of data from the input stream.
     *
     * @throws IOException
     *         when there is an error reading
     * @return the character read, or -1 for EOF
     */
    @Override
    public int read() throws IOException {
        int l = -1;

        if (isReadAllowed()) {
            // underlying stream not closed, go ahead and read.
            l = super.read();
            checkClose(l);
        }

        return l;
    }

    /**
     * Reads up to <code>len</code> bytes of data from the stream.
     *
     * @param b
     *        a <code>byte</code> array to read data into
     * @param off
     *        an offset within the array to store data
     * @param len
     *        the maximum number of bytes to read
     * @return the number of bytes read or -1 for EOF
     * @throws IOException
     *         if there are errors reading
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        int l = -1;

        if (isReadAllowed()) {
            l = super.read(b, off, len);
            checkClose(l);
        }

        return l;
    }

    /**
     * Reads some number of bytes from the input stream and stores them into the
     * buffer array b.
     *
     * @param b
     *        a <code>byte</code> array to read data into
     * @return the number of bytes read or -1 for EOF
     * @throws IOException
     *         if there are errors reading
     */
    @Override
    public int read(final byte[] b) throws IOException {
        int l = -1;

        if (isReadAllowed()) {
            l = super.read(b);
            checkClose(l);
        }
        return l;
    }

    /**
     * Obtains the number of bytes that can be read without blocking.
     *
     * @return the number of bytes available without blocking
     * @throws IOException
     *         in case of a problem
     */
    @Override
    public int available() throws IOException {
        int a = 0; // not -1

        if (isReadAllowed()) {
            a = super.available();
            // no checkClose() here, available() can't trigger EOF
        }

        return a;
    }

    /**
     * Close the stream, and also close the underlying stream if it is not
     * already closed.
     *
     * @throws IOException
     *         If an IO problem occurs.
     */
    @Override
    public void close() throws IOException {
        if (!selfClosed) {
            selfClosed = true;
            notifyWatcher();
        }
    }

    /**
     * Close the underlying stream should the end of the stream arrive.
     *
     * @param readResult
     *        The result of the read operation to check.
     * @throws IOException
     *         If an IO problem occurs.
     */
    private void checkClose(final int readResult) throws IOException {
        if (readResult == -1) {
            notifyWatcher();
        }
    }

    /**
     * See whether a read of the underlying stream should be allowed, and if
     * not, check to see whether our stream has already been closed!
     *
     * @return <code>true</code> if it is still OK to read from the stream.
     * @throws IOException
     *         If an IO problem occurs.
     */
    private boolean isReadAllowed() throws IOException {
        if (!streamOpen && selfClosed) {
            throw new IOException("Attempted read on closed stream.");
        }
        return streamOpen;
    }

    /**
     * Notify the watcher that the contents have been consumed.
     *
     * @throws IOException
     *         If an IO problem occurs.
     */
    private void notifyWatcher() throws IOException {
        if (streamOpen) {
            super.close();
            streamOpen = false;

            if (watcher != null) {
                watcher.responseConsumed();
            }
        }
    }
}
