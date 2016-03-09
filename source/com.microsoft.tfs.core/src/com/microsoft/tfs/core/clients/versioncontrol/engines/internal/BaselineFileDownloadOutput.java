// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.DownloadContentTypes;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace.BaselineFolderCollection;
import com.microsoft.tfs.core.clients.versioncontrol.localworkspace.BaselineFolder;
import com.microsoft.tfs.core.clients.versioncontrol.specs.DownloadOutput;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.IOUtils;

/**
 * Implements {@link DownloadOutput} to write to a local workspace's baseline
 * file. Supports reset and reopen of the stream after a failure.
 *
 * @threadsafety thread-safe
 */
public class BaselineFileDownloadOutput extends BaseDownloadOutput {
    private final static Log log = LogFactory.getLog(BaselineFileDownloadOutput.class);

    private final File baselineFileNoSuffix;

    /**
     * The output stream in use. <code>null</code> when uninitialized or after a
     * reset.
     */
    private FileOutputStream outputStream;

    /**
     * Set to the actual file {@link #outputStream} is opened to.
     */
    private File outputStreamFile;

    /**
     * Set when the output stream is open; logs whether the
     * {@link BaselineFolderCollection} could not create a file in the baseline
     * folder and instead gave us a temp file.
     */
    private boolean tempFileCreatedInsteadOfBaseline;

    /**
     * Constructs a {@link BaselineFileDownloadOutput} that writes to a file in
     * the baseline folder.
     *
     * @param baselineFileNoSuffix
     *        the full path to the baseline file except file suffix where
     *        downloaded bytes are written (must not be <code>null</code>). A
     *        suffix will be appended based on content type when the stream is
     *        first opened.
     * @param autoGunzip
     *        if <code>true</code> the bytes are gunzipped before being written
     *        to outputstream; if <code>false</code> unprocessed bytes (could be
     *        gzip, could be raw) are written to output stream
     */
    public BaselineFileDownloadOutput(final File baselineFileNoSuffix, final boolean autoGunzip) {
        super(autoGunzip);

        Check.notNull(baselineFileNoSuffix, "baselineFileNoSuffix"); //$NON-NLS-1$

        this.baselineFileNoSuffix = baselineFileNoSuffix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized OutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            final String contentType = getActualContentType();
            Check.notNull(contentType, "Cannot open output stream until actual content type is set"); //$NON-NLS-1$

            String path = baselineFileNoSuffix.getAbsolutePath();
            if (contentType.equals(DownloadContentTypes.APPLICATION_GZIP)) {
                path = path + BaselineFolder.getGzipExtension();
            } else if (contentType.equals(DownloadContentTypes.APPLICATION_OCTET_STREAM)) {
                path = path + BaselineFolder.getRawExtension();
            } else {
                throw new VersionControlException(
                    MessageFormat.format(
                        Messages.getString("VersionControlClient.UnsupportedContentTypeFormat"), //$NON-NLS-1$
                        contentType));

            }

            final AtomicBoolean tempCreated = new AtomicBoolean();

            final AtomicReference<String> pathReference = new AtomicReference<String>(path);
            outputStream = BaselineFolderCollection.createFile(pathReference, true, null /* ignored */, tempCreated);
            path = pathReference.get();

            outputStreamFile = new File(path);
            tempFileCreatedInsteadOfBaseline = tempCreated.get();
        }

        return outputStream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void resetOutputStream() throws IOException {
        final File file = outputStreamFile;

        closeOutputStream();

        if (file != null && !file.delete()) {
            log.warn(MessageFormat.format(
                "Couldn''t delete baseline output file {0} during stream reset", //$NON-NLS-1$
                outputStreamFile));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeOutputStream() throws IOException {
        if (outputStream != null) {
            IOUtils.closeSafely(outputStream);

            outputStreamFile = null;
            outputStream = null;
            tempFileCreatedInsteadOfBaseline = false;
        }
    }

    /**
     * @return the actual file {@link #outputStream} is opened to, or
     *         <code>null</code> if the stream has not been opened or was closed
     */
    public File getOutputStreamFile() {
        return outputStreamFile;
    }

    /**
     * @return <code>true</code> if {@link BaselineFolderCollection} could not
     *         create a file in the baseline folder when
     *         {@link #getOutputStream()} was called and instead gave us a temp
     *         file, <code>false</code> if we got a real baseline file
     */
    public synchronized boolean isTempFileCreatedInsteadOfBaseline() {
        return tempFileCreatedInsteadOfBaseline;
    }
}
