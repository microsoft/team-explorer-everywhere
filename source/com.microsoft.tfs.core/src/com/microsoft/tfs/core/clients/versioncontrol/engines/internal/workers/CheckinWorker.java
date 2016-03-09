// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.text.MessageFormat;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WebServiceLevel;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.AsyncCheckinOperation;
import com.microsoft.tfs.core.clients.versioncontrol.engines.internal.workers.WorkerStatus.FinalState;
import com.microsoft.tfs.core.clients.versioncontrol.exceptions.VersionControlException;
import com.microsoft.tfs.core.clients.versioncontrol.internal.httpclient.CancellableChunkPart;
import com.microsoft.tfs.core.clients.versioncontrol.internal.httpclient.CancellableFilePart;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.config.EnvironmentVariables;
import com.microsoft.tfs.core.exceptions.internal.CoreCancelException;
import com.microsoft.tfs.core.httpclient.methods.PostMethod;
import com.microsoft.tfs.core.httpclient.methods.multipart.MultipartRequestEntity;
import com.microsoft.tfs.core.httpclient.methods.multipart.Part;
import com.microsoft.tfs.core.httpclient.methods.multipart.StringPart;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.base64.Base64;
import com.microsoft.tfs.util.tasks.FileProcessingProgressMonitorAdapter;
import com.microsoft.tfs.util.tasks.TaskMonitor;
import com.microsoft.tfs.util.tasks.TaskMonitorService;
import com.microsoft.tfs.util.temp.TempStorageService;

/**
 * A worker that can be run in its own thread to perform file uploads.
 * <p>
 * <b>Cancellation Policy</b>
 * <p>
 * A worker can be interrupted through the {@link TaskMonitor} given to it
 * during construction. If the {@link TaskMonitor} becomes canceled before
 * {@link #call()} is invoked, or while it is running, the worker will return a
 * {@link WorkerStatus} indicating the cancellation. If the {@link TaskMonitor}
 * becomes canceled after {@link #call()} completes, the {@link WorkerStatus}
 * will indicate a normal completion.
 * <p>
 * This class is immutable (and therefore thread-safe).
 */
public class CheckinWorker implements Worker {
    private static final Log log = LogFactory.getLog(CheckinWorker.class);

    /**
     * This is the object we use to detect user cancellation from the UI or
     * other layer. We must poll on this object for its state; it can't
     * interrupt worker threads.
     */
    private final TaskMonitor userCancellationMonitor;

    private final VersionControlClient client;
    private final Workspace workspace;
    private final PendingChange change;
    private final byte[] localMD5Hash;
    private final AsyncCheckinOperation state;

    /**
     * The size of the buffer used to read bytes from an uncompressed file and
     * send them to a GZIP output stream.
     */
    private static final int GZIP_COMPRESS_READ_BUFFER = 4096;

    /**
     * The default size of the upload chunk.
     *
     * This value could be overridden by setting the TF_UPLOAD_CHUNK_SIZE
     * environment variable.
     */
    private static final int DEFAULT_UPLOAD_CHUNK_SIZE = 4 * 1024 * 1024;

    /**
     * The actual size of the upload chunk.
     */
    private static final int MAX_CHUNK_SIZE = getMaxChunkSize(DEFAULT_UPLOAD_CHUNK_SIZE);

    /**
     * The default maximum number of attempts we try to upload the entire file.
     *
     * This value could be overridden by setting the TF_MAX_FILE_RETRY_ATTEMPTS
     * environment variable.
     */
    private static final int DEFAULT_FILE_RETRY_ATTEMPTS = 2;

    /**
     * The actual maximum number of attempts we try to upload the entire file.
     */
    private static final int MAX_FILE_RETRY_ATTEMPTS =
        getRetryAttempts(EnvironmentVariables.MAX_FILE_RETRY_ATTEMPTS, DEFAULT_FILE_RETRY_ATTEMPTS);;

    /**
     * The default maximum number of attempts we try to upload each chunk of the
     * file.
     *
     * This value could be overridden by setting the TF_CHUNK_RETRY_ATTEMPTS
     * environment variable.
     */
    private static final int DEFAULT_CHUNK_RETRY_ATTEMPTS = 1;

    /**
     * The actual maximum number of attempts we try to upload each chunk of the
     * file.
     */
    private static final int MAX_CHUNK_RETRY_ATTEMPTS =
        getRetryAttempts(EnvironmentVariables.MAX_CHUNK_RETRY_ATTEMPTS, DEFAULT_CHUNK_RETRY_ATTEMPTS);

    /**
     * Compressed file content type
     */
    private static final String COMPRESSED = "application/gzip"; //$NON-NLS-1$

    /**
     * Uncompressed file content type
     */
    private static final String UNCOMPRESSED = "application/octet-stream"; //$NON-NLS-1$

    /*
     * We always use UTF-8 as the part character set so Unicode filenames can be
     * uploaded.
     */
    private static final String partCharSet = "utf-8"; //$NON-NLS-1$

    public CheckinWorker(
        final TaskMonitor userCancellationMonitor,
        final VersionControlClient client,
        final Workspace workspace,
        final PendingChange change,
        final byte[] localMD5Hash,
        final AsyncCheckinOperation state) {
        this.userCancellationMonitor = userCancellationMonitor;
        this.client = client;
        this.workspace = workspace;
        this.change = change;
        this.localMD5Hash = localMD5Hash;
        this.state = state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkerStatus call() throws Exception {
        /*
         * Try not to run any code outside this try block, so our Throwable
         * catch below can report every error (even RuntimeExceptions).
         */
        try {
            /*
             * We may have been queued to run but the user wants to cancel the
             * operation.
             */
            if (userCancellationMonitor.isCanceled()) {
                return new WorkerStatus(this, WorkerStatus.FinalState.CANCELED);
            }

            if (change.getLocalItem() == null) {
                log.warn(MessageFormat.format(
                    "Skipping upload for change {0} because local item is null", //$NON-NLS-1$
                    change.toString()));
            } else {
                upload(change.getLocalItem(), localMD5Hash);
            }
        } catch (final CoreCancelException e) {
            /*
             * CancelException is our exception that code in this class throws
             * only if the TaskMonitor was canceled during out work. There's no
             * cleanup because all temporary resources were cleaned up as the
             * exception travelled through the stack.
             */
            return new WorkerStatus(this, WorkerStatus.FinalState.CANCELED);
        } catch (final Throwable t) {
            /*
             * An actual error happened. We have to communicate this problem to
             * the thread submitting tasks so it can take the correct action
             * (shut down other workers).
             */
            state.setFatalError(t);
            return new WorkerStatus(this, FinalState.ERROR);
        } finally {
            /*
             * The local item for upload is usually a working folder file, for
             * which cleaning up via TempStorageService makes no sense, but it
             * is harmless (the file is not deleted). However, the local item
             * may be a file in a temporary directory, the result of an EOL
             * conversion, and this file does need cleaned up.
             */
            try {
                TempStorageService.getInstance().cleanUpItem(new File(change.getLocalItem()));
            } catch (final Throwable t) {
                log.error("Error cleaning up temp file after upload", t); //$NON-NLS-1$
            }
        }

        return new WorkerStatus(this, WorkerStatus.FinalState.NORMAL);
    }

    /**
     * Upload the given source file to the Team Foundation Server. If a socket
     * exception is encountered uploading the file, the upload is retried one
     * time.
     * <p>
     * If another exception is thrown, no clean-up of the source file is
     * performed (the caller must handle this condition).
     *
     * @param uncompressedSourceFile
     *        the uncompressed local file that will be sent to the server, which
     *        must exist (must not be <code>null</code> or empty)
     * @param md5Hash
     *        the pre-computed MD5 hash of the uncompressed source file. If
     *        null, the hash is computed automatically. This parameter exists
     *        for performance reasons (so callers can compute the hash only
     *        once).
     * @throws CoreCancelException
     *         if the upload was cancelled by the user via the
     *         {@link TaskMonitor}.
     */
    private void upload(final String uncompressedSourceFile, final byte[] md5Hash) throws CoreCancelException {
        Check.notNullOrEmpty(uncompressedSourceFile, "uncompressedSourceFile"); //$NON-NLS-1$
        Check.notNull(md5Hash, "md5Hash"); //$NON-NLS-1$

        File compressedFile = null;

        try {
            /*
             * Define the multi part message permanent parts.
             */
            final Part[] parts = new Part[7];
            parts[0] = new StringPart(VersionControlConstants.SERVER_ITEM_FIELD, change.getServerItem(), partCharSet);
            parts[1] = new StringPart(VersionControlConstants.WORKSPACE_NAME_FIELD, workspace.getName(), partCharSet);
            parts[2] =
                new StringPart(VersionControlConstants.WORKSPACE_OWNER_FIELD, workspace.getOwnerName(), partCharSet);

            final long uncompressedFileLength = new File(uncompressedSourceFile).length();
            parts[3] = new StringPart(
                VersionControlConstants.LENGTH_FIELD,
                Long.toString(uncompressedFileLength),
                partCharSet);

            /*
             * Force ASCII encoding for the hash string (because we declare
             * UTF-8 and ASCII is a strict subset).
             */
            final String hashString = new String(Base64.encodeBase64(md5Hash), "US-ASCII"); //$NON-NLS-1$

            parts[4] = new StringPart(VersionControlConstants.HASH_FIELD, hashString, partCharSet);
            // part 5 is the chunk byte range.
            // part 6 is the chunk content.

            /*
             * Compress the file to a temporary file if possible.
             */
            if (0 < uncompressedFileLength && uncompressedFileLength < MAX_GZIP_INPUT_SIZE) {
                compressedFile = compressToTempFile(uncompressedSourceFile);
            }

            /*
             * Use the uncompressed file if compression increases the file size.
             */
            final File uploadFile;
            final String contentType;
            if (compressedFile == null || compressedFile.length() > uncompressedFileLength) {
                uploadFile = new File(uncompressedSourceFile);
                contentType = UNCOMPRESSED;
            } else {
                uploadFile = compressedFile;
                contentType = COMPRESSED;
            }

            int attempt = 0;
            do {
                attempt++;

                try {
                    if (userCancellationMonitor.isCanceled()) {
                        throw new CoreCancelException();
                    }

                    retryableUpload(uploadFile, parts, contentType);

                    /*
                     * Upload succeeded.
                     */
                    return;
                } catch (final SocketException e) {
                    log.warn(MessageFormat.format(
                        "SocketException during {0} attempt to upload the file {1}", //$NON-NLS-1$
                        attempt,
                        uncompressedSourceFile), e);

                    if (attempt < MAX_FILE_RETRY_ATTEMPTS) {
                        log.info("Retrying"); //$NON-NLS-1$
                    } else {
                        final String message = MessageFormat.format(
                            Messages.getString("CheckinEngineUploadWorker.SocketExceptionDuringUploadRetryFormat"), //$NON-NLS-1$
                            e.getLocalizedMessage());

                        throw new VersionControlException(message, e);
                    }
                }
            } while (true);
        } catch (final IOException e) {
            throw new VersionControlException(e);
        } finally {
            if (compressedFile != null && compressedFile.exists()) {
                try {
                    compressedFile.delete();
                } catch (final Exception e) {
                    // do nothing
                }
            }
        }
    }

    /**
     * Upload the given source file to the Team Foundation Server. If an
     * exception is thrown, no clean-up of the source file is performed (the
     * caller must handle this condition).
     *
     * @param uploadFile
     *        the compressed or uncompressed local file that will be sent to the
     *        server, which must exist (must not be <code>null</code> or empty)
     * @param parts
     *        partially populated content parts. The Range part and file part
     *        are filled in for each chunk here.
     * @param contentType
     *        the type of the file content (compressed or uncompressed).
     * @throws CoreCancelException
     *         if the upload was cancelled by the user via the
     *         {@link TaskMonitor}.
     */
    private void retryableUpload(final File uploadFile, final Part[] parts, final String contentType)
        throws SocketException,
            CoreCancelException {
        Check.notNull(uploadFile, "uploadFile"); //$NON-NLS-1$
        Check.notNullOrEmpty(parts, "parts"); //$NON-NLS-1$
        Check.notNullOrEmpty(contentType, "contentType"); //$NON-NLS-1$

        PostMethod method = null;
        InputStream fileStream = null;
        BufferedInputStream bufferedStream = null;

        try {
            final long uploadFileLength = uploadFile.length();
            long uploadFilePos = 0;
            boolean aChunkHasBeenRetried = false;

            fileStream = new FileInputStream(uploadFile);
            bufferedStream = new BufferedInputStream(fileStream);

            do {
                final long bytesLeft = uploadFileLength - uploadFilePos;
                final long chunkSize = 0 < MAX_CHUNK_SIZE && MAX_CHUNK_SIZE < bytesLeft ? MAX_CHUNK_SIZE : bytesLeft;

                final CancellableFilePart filePart;
                if (chunkSize == uploadFileLength) {
                    /*
                     * The calculated chunk size can be equal to the size of the
                     * file only if:
                     *
                     * (1) this is the first iteration of the loop, and
                     *
                     * (2) the chunk size is zero, i.e. chunked upload is not
                     * allowed, or the entire file is small and fits in a single
                     * chunk.
                     *
                     * In this case we use the full file upload not bothering
                     * with chunks at all.
                     */

                    filePart = new CancellableFilePart("content", "item", uploadFile, contentType, null); //$NON-NLS-1$ //$NON-NLS-2$
                    /*
                     * NOTE We construct the file part in a special way so the
                     * character set is never sent to the server (TFS can't
                     * handle that header, and it causes an internal server
                     * error). If we construct the CancellableFilePart object
                     * with a null charset, the header is still included, and
                     * its value is the default charset. If we invoke
                     * setCharSet() with null, the header is never supplied
                     * (which is what we desire).
                     *
                     * Also, we use the file name "item" to match Visual
                     * Studio's implementation. Sending the actual file name
                     * doesn't seem to hurt, but appears to be ignored by the
                     * server.
                     */
                    filePart.setCharSet(null);
                } else {
                    /*
                     * Chunked upload. We mark the current position in the
                     * buffered stream to allow re-sending of the chunk in case
                     * redirection or authentication is required on the lower
                     * HTTP Client level.
                     */

                    bufferedStream.mark(MAX_CHUNK_SIZE);
                    filePart = new CancellableChunkPart(uploadFile, bufferedStream, contentType, chunkSize);
                }

                /*
                 * The Range part. NOTE There must be the extra newline after
                 * the range line, or it will cause an internal server error.
                 */
                parts[5] = new StringPart(
                    VersionControlConstants.RANGE_FIELD,
                    "bytes=" //$NON-NLS-1$
                        + uploadFilePos
                        + "-" //$NON-NLS-1$
                        + (uploadFilePos + chunkSize - 1)
                        + "/" //$NON-NLS-1$
                        + uploadFile.length()
                        + "\r\n", //$NON-NLS-1$
                    partCharSet);

                /*
                 * The File part. It could be the entire file or its chunk.
                 */
                parts[6] = filePart;

                int attempt = 0;

                do {
                    attempt++;

                    if (TaskMonitorService.getTaskMonitor().isCanceled()) {
                        throw new CoreCancelException();
                    }

                    try {
                        final String messageFormat = MessageFormat.format(
                            Messages.getString("CheckinWorker.UploadFileProgressFormat_SKIPVALIDATE"), //$NON-NLS-1$
                            change.getServerItem());
                        final FileProcessingProgressMonitorAdapter monitor = new FileProcessingProgressMonitorAdapter(
                            userCancellationMonitor,
                            uploadFilePos,
                            uploadFileLength,
                            messageFormat);

                        TaskMonitorService.pushTaskMonitor(monitor);

                        /*
                         * Connect to the server.
                         */
                        method = client.beginUploadRequest();

                        /*
                         * Create the multi-part request entity that wraps our
                         * parts.
                         */
                        method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));

                        client.executeUploadRequest(method);

                        /*
                         * Uploaded successfully
                         */
                        uploadFilePos += chunkSize;
                        break;

                    } catch (final SocketException e) {
                        if (attempt == MAX_CHUNK_RETRY_ATTEMPTS
                            || chunkSize == uploadFileLength
                            || client.getServiceLevel().getValue() < WebServiceLevel.TFS_2012_QU1_1.getValue()) {
                            /*
                             * We're here because:
                             *
                             * i. We already have retried the chunk allowed
                             * number of times or
                             *
                             * ii. It does not make sense to retry because the
                             * server does not support chunk retrying or
                             *
                             * iii. We're uploading the entire file as one chunk
                             *
                             * The caller might wish to retry the entire file
                             */
                            throw e;
                        } else {
                            /*
                             * Let's retry this chunk. On the pre-Dev12.M46
                             * servers that could cause VersionControlException
                             * on the same or the following chunks and we'll
                             * need to retry the entire file.
                             */
                            aChunkHasBeenRetried = true;
                        }
                    } catch (final VersionControlException e) {
                        if (aChunkHasBeenRetried) {
                            /*
                             * Most likely this is a pre-Dev12.M46 server that
                             * does not support chunk retrying.
                             *
                             * TODO. We might need to perform some deeper
                             * analysis of the exception as VS does.
                             */
                            throw new SocketException(
                                "This version of the TFS Server does not support chunk retrying."); //$NON-NLS-1$
                        } else {
                            throw e;
                        }
                    } finally {
                        if (method != null) {
                            client.finishUploadRequest(method);
                        }

                        TaskMonitorService.popTaskMonitor();
                    }
                } while (true);

            } while (uploadFilePos < uploadFileLength);
        } catch (final CancellableFilePart.SendDataCancellationException e) {
            /*
             * This is thrown by the CancellableFilePart because it must throw
             * an IOException to work inside Commons HTTP. Treat like a normal
             * cancel exception.
             */
            throw new CoreCancelException();
        } catch (final SocketException e) {
            /*
             * Throw this so it can be caught and the upload retried.
             */
            throw e;
        } catch (final IOException e) {
            throw new VersionControlException(e);
        } finally {
            if (bufferedStream != null) {
                try {
                    bufferedStream.close();
                } catch (final IOException e) {
                    // Do nothing.
                }
            }

            /*
             * Surprisingly the BufferedStream class does not close the
             * underlying file stream. So, we have to close it explicitly.
             */
            if (fileStream != null) {
                try {
                    fileStream.close();
                } catch (final IOException e) {
                    // Do nothing.
                }
            }
        }
    }

    /**
     * Compress the source file to a new temporary file, and return the absolute
     * path to the new temporary file. The algorithm used is gzip.
     *
     * @param sourceFile
     *        the source file to compress (must not be <code>null</code> or
     *        empty).
     * @return the full path to the new compressed temp file that was created.
     * @throws CoreCancelException
     *         if the compression was cancelled by the user via Core's
     *         TaskMonitor. The output file is removed before this exception is
     *         thrown.
     */
    private File compressToTempFile(final String sourceFile) throws CoreCancelException {
        Check.notNullOrEmpty(sourceFile, "sourceFile"); //$NON-NLS-1$

        FileInputStream is = null;
        FileOutputStream os = null;
        GZIPOutputStream gzos = null;

        final String messageFormat =
            MessageFormat.format(
                Messages.getString("CheckinWorker.CompressFIleProgressFormat_SKIPVALIDATE"), //$NON-NLS-1$
                change.getServerItem());
        final FileProcessingProgressMonitorAdapter monitor = new FileProcessingProgressMonitorAdapter(
            userCancellationMonitor,
            new File(sourceFile).length(),
            messageFormat);

        TaskMonitorService.pushTaskMonitor(monitor);

        try {
            final File temp = File.createTempFile("teamexplorer", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$

            final String tempFileName = temp.getAbsolutePath();

            is = new FileInputStream(sourceFile);
            os = new FileOutputStream(tempFileName);
            gzos = new GZIPOutputStream(os);

            final byte[] buffer = new byte[GZIP_COMPRESS_READ_BUFFER];
            int read = 0;
            while ((read = is.read(buffer)) != -1) {
                if (TaskMonitorService.getTaskMonitor().isCanceled()) {
                    temp.delete();
                    throw new CoreCancelException();
                }

                gzos.write(buffer, 0, read);
                TaskMonitorService.getTaskMonitor().worked(read);
            }

            return temp;
        } catch (final IOException e) {
            throw new VersionControlException(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (final IOException e) {
            }

            try {
                if (gzos != null) {
                    gzos.close();
                }
            } catch (final IOException e) {
            }

            TaskMonitorService.popTaskMonitor();
        }
    }

    private static int getMaxChunkSize(final int defaultValue) {
        final int chunkSize = EnvironmentVariables.getInt(EnvironmentVariables.UPLOAD_CHUNK_SIZE, defaultValue);

        // need > 2 for server's magic number check
        if (chunkSize > 2) {
            return chunkSize;
        } else {
            log.warn("Chunked upload is disabled"); //$NON-NLS-1$
            return 0;
        }
    }

    private static int getRetryAttempts(final String varName, final int defaultValue) {
        try {
            return EnvironmentVariables.getInt(varName, defaultValue);
        } catch (final NumberFormatException e) {
            log.warn("Wrong numeric value of the environment variable" + varName); //$NON-NLS-1$
        }

        return defaultValue;
    }
}
