// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.specs;

import java.io.IOException;
import java.io.OutputStream;

import com.microsoft.tfs.core.clients.versioncontrol.DownloadContentTypes;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;

/**
 * Used by {@link VersionControlClient}'s download methods to support writing to
 * multiple output streams simultaneously. Each output can have its own content
 * type preference (auto gunzip or not).
 * <p>
 * Callers can see which content type was actually written to the output stream
 * by calling {@link #getActualContentType()}.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-11.0
 */
public interface DownloadOutput {
    /**
     * Gets the {@link OutputStream} to write bytes to. See the notes on
     * {@link #resetOutputStream()} on recreating a stream after a transient
     * download failure.
     *
     * @return the {@link OutputStream} this class was constructed with (never
     *         <code>null</code>)
     */
    public OutputStream getOutputStream() throws IOException;

    /**
     * Called by the download process if a transient download error happened and
     * the download will be retried with a new output stream.
     * {@link #getOutputStream()} will be called after this method to obtain the
     * output stream to retry the download with. If an implementation returns
     * the same stream after {@link #resetOutputStream()} is called, it should
     * rewind or reset the stream to accept the first bytes of the download
     * again.
     * <p>
     * Implementations are not required to implement this method and may throw
     * an {@link IOException} if they choose not to.
     */
    public void resetOutputStream() throws IOException;

    /**
     * Called to close the {@link OutputStream} if it was open. Safe to call
     * again on an already-closed output.
     */
    public void closeOutputStream() throws IOException;

    /**
     * @return <code>true</code> if the download process should gunzip the bytes
     *         before writing them to the output stream; <code>false</code> if
     *         the download process should write unprocessed bytes
     */
    public boolean isAutoGunzip();

    /**
     * Sets the content type of the data that was actually written to the output
     * stream during the download.
     *
     * @param type
     *        the type that was written (must not be <code>null</code>, must be
     *        one of the types defined by {@link DownloadContentTypes})
     */
    public void setActualContentType(final String type);

    /**
     * @return the content type of the data that was actually written to the
     *         output stream during the download; <code>null</code> if the
     *         download did not complete
     *
     * @see DownloadContentTypes
     */
    public String getActualContentType();
}
