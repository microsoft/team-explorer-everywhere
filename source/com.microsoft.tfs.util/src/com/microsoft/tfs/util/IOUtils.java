// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@link IOUtils} contains static IO-related utility methods.
 */
public class IOUtils {
    private static final Log log = LogFactory.getLog(IOUtils.class);

    /**
     * Unconditionally closes the specified {@link InputStream}, safely handling
     * any {@link IOException} thrown by the {@link InputStream#close()} method.
     * If the log for this class is properly configured, any such exception will
     * be logged, but it will never cause an exception to be thrown to the
     * caller. This method is typically called from a finally block in caller
     * code.
     *
     * @param stream
     *        the {@link InputStream} to close (must not be <code>null</code>)
     */
    public static void closeSafely(final InputStream stream) {
        Check.notNull(stream, "stream"); //$NON-NLS-1$

        try {
            stream.close();
        } catch (final IOException e) {
            if (log.isTraceEnabled()) {
                log.trace("error closing InputStream", e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Unconditionally closes the specified {@link OutputStream}, safely
     * handling any {@link IOException} thrown by the
     * {@link OutputStream#close()} method. If the log for this class is
     * properly configured, any such exception will be logged, but it will never
     * cause an exception to be thrown to the caller. This method is typically
     * called from a finally block in caller code.
     *
     * @param stream
     *        the {@link OutputStream} to close (must not be <code>null</code>)
     */
    public static void closeSafely(final OutputStream stream) {
        Check.notNull(stream, "stream"); //$NON-NLS-1$

        try {
            stream.close();
        } catch (final IOException e) {
            if (log.isTraceEnabled()) {
                log.trace("error closing OutputStream", e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Unconditionally closes the specified {@link Reader}, safely handling any
     * {@link IOException} thrown by the {@link Reader#close()} method. If the
     * log for this class is properly configured, any such exception will be
     * logged, but it will never cause an exception to be thrown to the caller.
     * This method is typically called from a finally block in caller code.
     *
     * @param reader
     *        the {@link Reader} to close (must not be <code>null</code>)
     */
    public static void closeSafely(final Reader reader) {
        Check.notNull(reader, "reader"); //$NON-NLS-1$

        try {
            reader.close();
        } catch (final IOException e) {
            if (log.isTraceEnabled()) {
                log.trace("error closing Reader", e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Unconditionally closes the specified {@link Writer}, safely handling any
     * {@link IOException} thrown by the {@link Writer#close()} method. If the
     * log for this class is properly configured, any such exception will be
     * logged, but it will never cause an exception to be thrown to the caller.
     * This method is typically called from a finally block in caller code.
     *
     * @param writer
     *        the {@link Writer} to close (must not be <code>null</code>)
     */
    public static void closeSafely(final Writer writer) {
        Check.notNull(writer, "writer"); //$NON-NLS-1$

        try {
            writer.close();
        } catch (final IOException e) {
            if (log.isTraceEnabled()) {
                log.trace("error closing Writer", e); //$NON-NLS-1$
            }
        }
    }

    /**
     * Reads bytes from the specified {@link InputStream} until end of stream
     * and returns all bytes read as a byte array. This method performs internal
     * buffering, so there is no need to wrap the argument in a
     * {@link BufferedInputStream}. The argument is closed by this method.
     *
     * @param inputStream
     *        the stream to read bytes from (must not be <code>null</code>)
     * @return an array containing all of the bytes read (never
     *         <code>null</code>)
     * @throws IOException
     *         if {@link InputStream#read(byte[])} throws an {@link IOException}
     */
    public static byte[] toByteArray(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(inputStream, baos);

        return baos.toByteArray();
    }

    /**
     * Reads bytes from the specified {@link InputStream} until the end of
     * stream and writes each byte to the specified {@link OutputStream}. This
     * method performs internal buffering, so there is no need to wrap the
     * arguments in a {@link BufferedInputStream} or a
     * {@link BufferedOutputStream}. The inputStream is closed by this method.
     *
     * @param inputStream
     *        the stream to read bytes from (must not be <code>null</code>)
     * @param outputStream
     *        the stream to write bytes to (must not be <code>null</code>)
     * @return the number of bytes read/written
     * @throws IOException
     *         if {@link InputStream#read(byte[])} or
     *         {@link OutputStream#write(byte[])} throws an {@link IOException}
     */
    public static long copy(final InputStream inputStream, final OutputStream outputStream) throws IOException {
        Check.notNull(inputStream, "inputStream"); //$NON-NLS-1$
        Check.notNull(outputStream, "outputStream"); //$NON-NLS-1$

        long count = 0;
        try {
            final byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
                count += length;
            }
        } finally {
            closeSafely(inputStream);
        }

        return count;
    }

    public static boolean copy(final File inputFile, final File outputFile) {
        Check.notNull(inputFile, "inputFile"); //$NON-NLS-1$
        Check.notNull(outputFile, "outputFile"); //$NON-NLS-1$

        try {
            final InputStream inputStream = new FileInputStream(inputFile);
            final OutputStream outputStream = new FileOutputStream(outputFile);

            copy(inputStream, outputStream);
            return true;
        } catch (final Exception e) {
            log.error(MessageFormat.format(
                "Error copying {0} to {1}: ", //$NON-NLS-1$
                inputFile.getAbsolutePath(),
                outputFile.getAbsolutePath()), e);
            return false;
        }
    }

    /**
     * <p>
     * Reads bytes from the specified {@link InputStream} until end of stream
     * and returns all bytes read as a {@link String} with the specified
     * encoding. The behaviour of this method when the given bytes are not valid
     * in the given charset is unspecified. The
     * {@link java.nio.charset.CharsetDecoder} class should be used when more
     * control over the decoding process is required.
     * </p>
     *
     * <p>
     * The inputStream is closed by this method.
     * </p>
     *
     * @param inputStream
     *        the stream to read bytes from (must not be <code>null</code>)
     * @param charsetName
     *        the name of a supported <code>{@link Charset}</code>
     * @throws IOException
     *         if {@link InputStream#read(byte[])} throws an {@link IOException}
     */
    public static String toString(final InputStream inputStream, final String charsetName) throws IOException {
        Check.notNull(inputStream, "inputStream"); //$NON-NLS-1$
        Check.notNullOrEmpty(charsetName, "charsetName"); //$NON-NLS-1$

        return new String(toByteArray(inputStream), charsetName);
    }
}
