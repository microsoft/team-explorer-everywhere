// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.temp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.util.StringUtil;

/**
 * <p>
 * An {@link OutputStream} that writes bytes to temporary storage and can be
 * read from multiple times before being disposed. Internally the class first
 * writes to heap memory, then switches to a temporary file once a threshold is
 * exceeded. The temporary file is obtained through {@link TempStorageService}
 * so temp files can be removed automatically during JVM shutdown (if they were
 * not already disposed of manually before).
 * </p>
 * <p>
 * Common usage steps for this class:
 * <ol>
 * <li>Intantiate {@link FastTempOutputStream}</li>
 * <li>Write data to it via {@link #write(byte[])}, {@link #write(int)}, and
 * {@link #write(byte[], int, int)}</li>
 * <li>Call {@link #close()} when done writing all data</li>
 * <li>Call {@link #getInputStream()} to get an {@link InputStream} (this can be
 * done multiple times)</li>
 * <li>Read data via the {@link InputStream}</li>
 * <li>Close each {@link InputStream} via {@link InputStream#close()}</li>
 * <li>Call {@link #dispose()} on {@link FastTempOutputStream} to free resources
 * </li>
 * </ol>
 * Data cannot be read (via {@link #getInputStream()}) before
 * {@link InputStream#close()} is called (the behavior for concurrent open
 * writer and reader is undefined). You can open multiple {@link InputStream}s,
 * but they all must be closed before calling {@link #dispose()}.
 * </p>
 * <p>
 * Call {@link #dispose()} to free temp storage when finished with an instance
 * and all of its {@link InputStream}s have been closed.
 * </p>
 * <p>
 * This class is useful because creating temp files on some operating systems
 * (Windows) is very slow compared to others (most Unixes), especially when they
 * only store a small amount of data (a few hundred bytes) and are used for a
 * short time (tens or hundreds of milliseconds). This class is designed for
 * these scenarios, when the eventual size of the temp file is unknown
 * (streaming from the network, for instance) but are often small enough to fit
 * in main memory.
 * </p>
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class FastTempOutputStream extends OutputStream {
    private final static Log log = LogFactory.getLog(FastTempOutputStream.class);

    /**
     * Extends {@link ByteArrayOutputStream} so {@link FastTempOutputStream} can
     * get at the underlying byte array for efficiency.
     */
    private static class DirectAccessByteArrayOutputStream extends ByteArrayOutputStream {
        public DirectAccessByteArrayOutputStream(final int size) {
            super(size);
        }

        /**
         * @return the byte array this {@link DirectAccessByteArrayOutputStream}
         *         stores its data in. The array may be larger than the contents
         *         ({@link #size()}) of the stream
         */
        protected byte[] getByteArray() {
            return super.buf;
        }
    }

    /**
     * The default initial size for heap storage when the user didn't specify a
     * hint during construction. This is just the default for efficiency, it
     * will grow automatically until the configured heap storage limit is
     * reached.
     */
    public static final int DEFAULT_HEAP_STORAGE_INITIAL_SIZE_BYTES = 4096;

    /**
     * The default limit on heap storage, after this size file storage is used.
     */
    public static final int DEFAULT_HEAP_STORAGE_LIMIT_BYTES = getDefaultHeapStorageLimit();

    /**
     * Holds the limit on heap storage we can't exceed. We track this
     * independent of the heap storage object because we don't wish to
     * pre-allocate all of it (this is just the configured limit for this
     * instance).
     */
    private final int heapStorageLimitBytes;

    /**
     * The actual heap storage. Allocated during construction, nulled out when
     * {@link #adjustStorage(int)} decides to switch to file storage.
     */
    private DirectAccessByteArrayOutputStream heapStream;

    /**
     * Actual file storage. Only allocated when {@link #adjustStorage(int)}
     * decides to switch to file storage (which can be during construction).
     */
    private FileOutputStream fileStream;

    /**
     * The file storage location (non-null whenever {@link #fileStream} is being
     * used).
     */
    private File file;

    /**
     * Updated by {@link #adjustStorage(int)} to always point to the currently
     * used stream (heap or file).
     */
    private OutputStream currentStream;

    /**
     * When true, the write methods can be called. Once {@link #close()} is
     * called, {@link #writable} is set to false and only then can
     * {@link #getInputStream()} be called (possibly multiple times).
     */
    private boolean writable = true;

    /**
     * Creates a {@link FastTempOutputStream} with default values for the size
     * hint and heap limit.
     */
    public FastTempOutputStream() {
        this(DEFAULT_HEAP_STORAGE_LIMIT_BYTES, DEFAULT_HEAP_STORAGE_INITIAL_SIZE_BYTES);
    }

    /**
     * Creates a {@link FastTempOutputStream} that will not use more than the
     * given limit for heap storage (before transparently switch to file
     * storage), and starts with the given heap storage pre-allocated (for more
     * efficient writing of smaller files).
     *
     * @param heapStorageLimitBytes
     *        the (approximate) limit on heap storage before file storage is
     *        used instead. If negative, default values are used.
     * @param initialHeapStorageSizeBytes
     *        the amount of heap storage to initially allocate. Using numbers
     *        near the ultimate size of the content written helps reduce heap
     *        allocations and fragmentation (at the expense of more initial
     *        memory used). If negative, default values are used.
     */
    public FastTempOutputStream(int heapStorageLimitBytes, int initialHeapStorageSizeBytes) {
        super();

        if (heapStorageLimitBytes < 0) {
            heapStorageLimitBytes = DEFAULT_HEAP_STORAGE_LIMIT_BYTES;
        }

        if (initialHeapStorageSizeBytes < 0) {
            initialHeapStorageSizeBytes = DEFAULT_HEAP_STORAGE_INITIAL_SIZE_BYTES;
        }

        final String messageFormat = "New instance with heap limit of {0} bytes, initial heap size {1}"; //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, heapStorageLimitBytes, initialHeapStorageSizeBytes);
        log.trace(message);

        synchronized (this) {
            this.heapStorageLimitBytes = heapStorageLimitBytes;

            heapStream = new DirectAccessByteArrayOutputStream(initialHeapStorageSizeBytes);
            currentStream = heapStream;
        }
    }

    private static int getDefaultHeapStorageLimit() {
        final String propertyName = "com.microsoft.tfs.fasttempstream.heaplimit"; //$NON-NLS-1$
        final int defaultValue = 8 * 1024 * 1024;
        final String value = System.getProperty(propertyName);
        if (StringUtil.isNullOrEmpty(value)) {
            return defaultValue;
        } else {
            try {
                final int number = StringUtil.toInt(value);
                return number;
            } catch (final NumberFormatException e) {
                final String message =
                    MessageFormat.format("Incorrect value of the system property {0} = {1}", propertyName, value); //$NON-NLS-1$
                log.error(message, e);

                return defaultValue;
            }
        }
    }

    /**
     * Examines the current configuration of the buffer relative to the new size
     * and adjusts the storage method if required.
     * <p>
     * This method should be called <b>before</b> writing to the underlying
     * streams, so big writes can be aimed at the right stream (file).
     * <p>
     * This method always updates {@link #currentStream} before returning.
     *
     * @param increase
     *        the increase in size this buffer needs (usually the length of a
     *        write operation)
     */
    private synchronized void adjustStorage(final int increase) {
        /*
         * If we're already using a file, there's nothing more to adjust (and
         * the current stream is already correct).
         */
        if (increase <= 0 || fileStream != null) {
            return;
        }

        /*
         * If we're currently using a heap stream, and the new total size is
         * big, start using a file.
         */
        if (heapStream != null && heapStream.size() + increase > heapStorageLimitBytes) {
            String messageFormat =
                "adjustment for {0} total bytes (increase of {1}) exceeds threshold of {2}, switching to file storage"; //$NON-NLS-1$
            String message =
                MessageFormat.format(messageFormat, (heapStream.size() + increase), increase, heapStorageLimitBytes);

            log.trace(message);

            /*
             * Big size, time to allocate a file.
             */
            try {
                file = TempStorageService.getInstance().createTempFile();

                messageFormat = "Created temporary file {0} (exceeded heap limit of {1} bytes)"; //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, file.getAbsolutePath(), heapStorageLimitBytes);
                log.debug(message);

                fileStream = new FileOutputStream(file);
            } catch (final IOException e) {
                log.error("Error creating temp file", e); //$NON-NLS-1$

                if (file != null) {
                    file.delete();
                }

                throw new RuntimeException(e);
            }

            /*
             * Write the previous heap data to the file and free it.
             */
            try {
                messageFormat = "Copying {0} initial bytes from heap to file"; //$NON-NLS-1$
                message = MessageFormat.format(messageFormat, heapStream.size());
                log.trace(message);

                heapStream.writeTo(fileStream);
            } catch (final IOException e) {
                log.error("Error copying initial bytes", e); //$NON-NLS-1$

                if (file != null) {
                    file.delete();
                }

                throw new RuntimeException(e);
            }

            heapStream = null;
            currentStream = fileStream;

            log.trace("Now using file storage"); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() throws IOException {
        currentStream.close();
        writable = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void flush() throws IOException {
        checkWritable();

        currentStream.flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
        checkWritable();

        adjustStorage(len);
        currentStream.write(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void write(final byte[] b) throws IOException {
        checkWritable();

        adjustStorage(b.length);
        currentStream.write(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void write(final int b) throws IOException {
        checkWritable();

        adjustStorage(1);
        currentStream.write(b);
    }

    /**
     * Tests whether this stream is still writable, and throws if it is not.
     *
     * @throws IOException
     *         an exception with a message about the stream being closed
     */
    private synchronized void checkWritable() throws IOException {
        if (writable == false) {
            throw new IOException("The stream has been closed"); //$NON-NLS-1$
        }
    }

    /**
     * Gets an {@link InputStream} that reads the buffer contents that were
     * previously written before {@link #close()} was called. This method may be
     * invoked multiple times.
     *
     * @return an {@link InputStream} that reads the data that was written to
     *         this buffer
     * @throws IOException
     *         if the stream is still writable, or if the temporary file (if a
     *         file is being used for storage) could not be opened
     */
    public synchronized InputStream getInputStream() throws IOException {
        if (writable) {
            throw new IOException(
                "Cannot get an InputStream because the stream is still open for writing (call close())"); //$NON-NLS-1$
        }

        if (heapStream != null) {
            /*
             * The byte array in the heapStream won't change from here out.
             */
            log.trace("Creating new ByteArrayInputStream"); //$NON-NLS-1$
            return new ByteArrayInputStream(heapStream.getByteArray(), 0, heapStream.size());
        } else {
            final String messageFormat = "Creating new FileInputStream for {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, file.getAbsolutePath());
            log.trace(message);
            return new FileInputStream(file);
        }
    }

    /**
     * Disposes of temporary resources used by this {@link FastTempOutputStream}
     * . Users must call this when finished writing and reading from
     * {@link FastTempOutputStream} (all {@link InputStream}s retrieved via
     * {@link #getInputStream()} must be closed), otherwise temporary files may
     * remain on disk.
     *
     * @throws IOException
     *         if an error occurred closing the temporary file stream
     */
    public synchronized void dispose() throws IOException {
        log.trace("Disposing"); //$NON-NLS-1$

        close();

        if (heapStream != null) {
            heapStream = null;
            log.trace("Cleared heap storage"); //$NON-NLS-1$
        }

        if (fileStream != null) {
            fileStream = null;
        }

        if (file != null) {
            TempStorageService.getInstance().cleanUpItem(file);

            final String messageFormat = "Deleted file storage {0}"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, file.getAbsolutePath());
            log.debug(message);
            file = null;
        }
    }
}