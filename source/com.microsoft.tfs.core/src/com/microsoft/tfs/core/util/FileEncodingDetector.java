// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.Wildcard;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.IOUtils;
import com.microsoft.tfs.util.Platform;

/**
 * Static methods which assist in detecting text encodings in disk files. Always
 * returns encodings represented by known {@link FileEncoding} instances (which
 * can be queried for their text names and code page numbers).
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class FileEncodingDetector {
    private static final Log tracer = LogFactory.getLog(FileEncodingDetector.class);

    /**
     * This class is static, no one constructs us.
     */
    private FileEncodingDetector() {
    }

    /**
     * Detects the encoding used for a server or local path with hints. For
     * server paths, the contents are never read and only the hint is used.
     * <p>
     * The {@link FileEncoding#AUTOMATICALLY_DETECT} encoding hint is only valid
     * for local (not server) paths that do not contain wildcard characters,
     * exist on disk, and are files (not directories). If it is specified for
     * other kinds of paths, an exception is thrown.
     * <p>
     * Encoding hints are evaluated in the following way:
     * <ul>
     * <li>If the hint is {@link FileEncoding#BINARY} that encoding is returned
     * immediately for all item types.</li>
     * <li>If the hint is {@link FileEncoding#DEFAULT_TEXT} the default encoding
     * for this platform is returned immediately for all item types.</li>
     * <li>If the hint is something other than
     * {@link FileEncoding#AUTOMATICALLY_DETECT}, that encoding is returned
     * immediately for all item types.</li>
     * <li>The hint is {@link FileEncoding#AUTOMATICALLY_DETECT}:
     * <ul>
     * <li>If the path is a server path, is a local directory, or contains
     * wildcards, an exception is thrown.</li>
     * <li>The file's contents are read to determine the encoding, which is
     * returned.</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param path
     *        the path to detect encoding for (must not be <code>null</code>)
     * @param encodingHint
     *        the encoding hint (must not be <code>null</code>)
     * @return the {@link FileEncoding} that matches the given file's encoding.
     * @throws TECoreException
     *         if the specified encoding hint is not valid for the type of path
     *         given
     */
    public static FileEncoding detectEncoding(final String path, final FileEncoding encodingHint) {
        tracer.trace(MessageFormat.format("path={0}, encodingHint={1}", path, encodingHint)); //$NON-NLS-1$

        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(encodingHint, "encodingHint"); //$NON-NLS-1$

        // These hints apply immediately to all types of paths.

        if (encodingHint == FileEncoding.BINARY) {
            return encodingHint;
        } else if (encodingHint == FileEncoding.DEFAULT_TEXT) {
            return FileEncoding.getDefaultTextEncoding();
        } else if (encodingHint != FileEncoding.AUTOMATICALLY_DETECT) {
            return encodingHint;
        }

        // The encoding is FileEncoding.AUTOMATICALLY_DETECT

        if (ServerPath.isServerPath(path)) {
            throw new TECoreException(
                MessageFormat.format(
                    Messages.getString("FileEncodingDetector.ServerPathsNotDetectedFormat"), //$NON-NLS-1$
                    path));
        } else if (Wildcard.isWildcard(path)) {
            throw new TECoreException(
                MessageFormat.format(
                    Messages.getString("FileEncodingDetector.LocalItemContainsWildcardsFormat"), //$NON-NLS-1$
                    path));
        }

        // The path is a local path without wildcards

        final File file = new File(LocalPath.canonicalize(path));

        if (file.exists() == false) {
            throw new TECoreException(
                MessageFormat.format(
                    Messages.getString("FileEncodingDetector.LocalItemDoesNotExistFormat"), //$NON-NLS-1$
                    path));
        } else if (file.isDirectory()) {
            throw new TECoreException(
                MessageFormat.format(
                    Messages.getString("FileEncodingDetector.LocalItemIsDirectoryFormat"), //$NON-NLS-1$
                    path));
        }

        FileInputStream stream = null;

        try {
            stream = new FileInputStream(path);

            final byte[] buffer = new byte[1024];

            // Look for the Unicode Byte Order Mark (BOM).
            final int read = stream.read(buffer, 0, buffer.length);

            if (read < 0) {
                return FileEncoding.getDefaultTextEncoding();
            }

            /*
             * Examine the BOM for details. Java uses signed bytes, so we use
             * (signed) integers for easier comparison.
             */
            if (read >= 2 && buffer[0] == -2 && buffer[1] == -1) {
                return FileEncoding.UTF_16BE;
            } else if (read >= 2 && buffer[0] == -1 && buffer[1] == -2) {
                if (read >= 4 && buffer[2] == 0 && buffer[3] == 0) {
                    return FileEncoding.UTF_32;
                } else {
                    return FileEncoding.UTF_16;
                }
            } else if (read >= 3 && buffer[0] == -17 && buffer[1] == -69 && buffer[2] == -65) {
                return FileEncoding.UTF_8;
            } else if (read >= 4 && buffer[0] == 0 && buffer[1] == 0 && buffer[2] == -2 && buffer[3] == -1) {
                return FileEncoding.UTF_32BE;
            } else if (startsWithPDFHeader(buffer, buffer.length)) {
                /*
                 * We go out of our way to detect PDF files so we can claim
                 * they're all binary. This is because a PDF file can be created
                 * with no non-text bytes in the first 1024 bytes (or in the
                 * whole file itself). However, these files shouldn't ever be
                 * automatically merged, and in the case where the first 1024
                 * bytes are clean, there may lie non-text bytes near the end.
                 */
                return FileEncoding.BINARY;
            }

            /*
             * No encoding determined yet. Search the chunk we read for
             * "non-text" characters that would indicate this is not a text
             * file. The values we search for on z/OS are for EBCDIC, the others
             * use ASCII.
             */
            if (Platform.isCurrentPlatform(Platform.Z_OS)) {
                if (looksLikeEBCDIC(buffer, read) == false) {
                    return FileEncoding.BINARY;
                }
            } else {
                if (looksLikeANSI(buffer, read) == false) {
                    return FileEncoding.BINARY;
                }
            }

            /*
             * If we got down here, we could not identify the file as certainly
             * binary by searching the first block of the file we read, so its
             * probably simple text. We should use the code default encoding for
             * this platform / user.
             *
             * This covers the case of an empty file, which VSS treats as a text
             * file in the default encoding, and so does TFS, so we should too.
             */
            return FileEncoding.getDefaultTextEncoding();
        } catch (final IOException e) {
            throw new TECoreException(e);
        } finally {
            if (stream != null) {
                IOUtils.closeSafely(stream);
            }
        }
    }

    /**
     * Tests whether the given bytes declare an Adobe PDF file.
     *
     * See the PDF file spec at
     * http://partners.adobe.com/public/developer/pdf/index_reference.html for
     * details on file structure.
     *
     * @param bytes
     *        the bytes to test, where the byte at index 0 is the first byte of
     *        the file.
     * @param size
     *        the maximum number of bytes to evaluate in the given byte array.
     * @return true if the bytes denote a PDF file of any version, false if they
     *         do not.
     */
    private static boolean startsWithPDFHeader(final byte[] bytes, final int size) {
        /*
         * PDF files start with the header "%PDF-X.Y" where X is the major
         * version number and Y is the minor. See the PDF specification for
         * notes on this header and the version scheme.
         */

        // The header is 8 bytes, so smaller files can't be PDF.
        if (size < 8) {
            return false;
        }

        // Make sure the first five bytes are an exact match.
        byte[] firstFive;
        try {
            firstFive = "%PDF-".getBytes("US-ASCII"); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (final UnsupportedEncodingException e) {
            /*
             * Should never happen as all JVMs are required to support US-ASCII.
             */
            throw new RuntimeException(e);
        }
        for (int i = 0; i < firstFive.length; i++) {
            if (firstFive[i] != bytes[i]) {
                return false;
            }
        }

        // Finally, it should end with "X.Y".
        if (Character.isDigit((char) bytes[5]) && bytes[6] == '.' && Character.isDigit((char) bytes[7])) {
            return true;
        }

        return false;
    }

    /**
     * Tests whether the given byte array looks like an ANSI text file with the
     * default text encoding, i.e. can be decoded with the current ANSI
     * character set. In multi-byte character sets (like Japanese, for example)
     * the entire byte array might not be converted entirely, because at the end
     * of array it might contain a broken multi-byte character. We still accept
     * this kind of files as ANSI ones if the not converted reminder of the
     * array is short enough.
     *
     * @param bytes
     *        the bytes to check for ANSI-ness (must not be <code>null</code>)
     * @param limit
     *        the maximum number of bytes to read.
     * @return true if the given bytes look like part of an ANSI text file,
     *         false if they do not (because they contain control characters or
     *         other patterns).
     */
    protected static boolean looksLikeANSI(final byte[] bytes, final int limit) {
        final Charset charSet = CodePageMapping.getCharset(FileEncoding.getDefaultTextEncoding().getCodePage());

        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, 0, limit);
        final CharBuffer charBuffer = CharBuffer.allocate(limit);

        final CharsetDecoder decoder = charSet.newDecoder();
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        decoder.onMalformedInput(CodingErrorAction.REPORT);

        final CoderResult rc = decoder.decode(byteBuffer, charBuffer, true);

        if (!rc.isError()) {
            return true;
        } else {
            return byteBuffer.position() > limit - 5;
        }
    }

    /**
     * Tests whether the given byte array looks like an EBCDIC text file
     * (contains character values that would be present in an EBCDIC text file
     * without the control characters that would not).
     *
     * @param bytes
     *        the bytes to check for EBCDIC-ness (must not be <code>null</code>)
     * @param limit
     *        the maximum number of bytes to read.
     * @return true if the given bytes look like part of an EBCDIC text file,
     *         false if they do not (because they contain control characters or
     *         other patterns).
     */
    protected static boolean looksLikeEBCDIC(final byte[] bytes, final int limit) {
        /*
         * EBDIC is like ASCII in that it uses the lower values for control
         * characters, but it uses up to value 62. We test exceptions for a few
         * of those. Because Java uses signed bytes, and EBCDIC uses values >
         * 127, we can ignore the high half of the EBCDIC space (valid
         * characters, mostly) by just accepting all negative values.
         *
         * 5: horizontal tab
         *
         * 6: required new line
         *
         * 9: superscript
         *
         * 11: vertical tab
         *
         * 12: form feed
         *
         * 13: carriage return
         *
         * 21: new line
         *
         * 37: line feed
         *
         * 56: subscript
         *
         * 57: indent tab
         */
        for (int i = 0; i < limit; i++) {
            if ((bytes[i] >= 0 && bytes[i] <= 62)
                && (bytes[i] != 5
                    && bytes[i] != 6
                    && bytes[i] != 9
                    && bytes[i] != 11
                    && bytes[i] != 12
                    && bytes[i] != 13
                    && bytes[i] != 21
                    && bytes[i] != 37
                    && bytes[i] != 56
                    && bytes[i] != 57)) {
                return false;
            }
        }

        return true;
    }
}
