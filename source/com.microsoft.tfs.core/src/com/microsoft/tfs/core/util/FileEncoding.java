// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util;

import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.util.CodePageMapping.UnknownEncodingException;
import com.microsoft.tfs.jni.PlatformMiscUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;

/**
 * <p>
 * Represents a text file encoding as expected to be used by TFS, which can be
 * represented solely by a "code page" integer (normal code page numbers with a
 * few TFS-specific numbers reserved for binary files, etc.). These instances
 * are light-weight.
 * </p>
 * <p>
 * A {@link FileEncoding} is equal to ({@link #equals(Object)}) another
 * {@link FileEncoding} if and only if its code page number matches.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public final class FileEncoding {
    private static final Log log = LogFactory.getLog(FileEncoding.class);

    protected static final int AUTO_DETECT_SPECIAL_VALUE = -9999;
    protected static final int TEXT_SPECIAL_VALUE = -9998;

    public static final String ENCODING_ERROR_MESSAGE_FORMAT =
        Messages.getString("FileEncoding.EncodingErrorMessageFormat"); //$NON-NLS-1$

    /**
     * The Windows code page number for this encoding. These are sent to the
     * TFS.
     */
    private final int tfsCodePage;

    /**
     * Construct a file encoding that represents the given code page.
     *
     * @param codePage
     *        the code page this encoding represents.
     */
    public FileEncoding(final int codePage) {
        tfsCodePage = codePage;
    }

    /*
     * Below are the built-in encodings. Some are actual encodings listed here
     * for convenience. Some do not actually represent code pages that can be
     * sent to the server, but are handy to be able to specify to
     * com.microsoft.tfs.core so that a code page can be detected later.
     */

    /**
     * This will be treated as a byte stream, and not interpreted as text.
     */
    public final static FileEncoding BINARY = new FileEncoding(VersionControlConstants.ENCODING_BINARY);

    /**
     * The file described by this encoding object should have its encoding
     * detected some time in the future and thereafter this value be ignored.
     *
     * getCodePage() is not valid for this type.
     */
    public final static FileEncoding AUTOMATICALLY_DETECT = new FileEncoding(AUTO_DETECT_SPECIAL_VALUE);

    /**
     * The file described by this encoding object should have its encoding
     * detected to be the default text encoding for this platform some time in
     * the future and thereafter this value be ignored.
     *
     * getCodePage() is not valid for this type.
     */
    public final static FileEncoding DEFAULT_TEXT = new FileEncoding(TEXT_SPECIAL_VALUE);

    /**
     * UTF-8.
     */
    public final static FileEncoding UTF_8;

    /**
     * UTF-16, little-endian (the Windows convention for non-endian-explicit
     * names, which is the opposite of Java; see the note in
     * {@link CodePageMapping}).
     */
    public final static FileEncoding UTF_16;

    /**
     * UTF-16, big-endian.
     */
    public final static FileEncoding UTF_16BE;

    /**
     * UTF-32, little-endian (the Windows convention for non-endian-explicit
     * names, which is the opposite of Java; see the note in
     * {@link CodePageMapping}).
     */
    public final static FileEncoding UTF_32;

    /**
     * UTF-32, big-endian.
     */
    public final static FileEncoding UTF_32BE;

    static {
        /*
         * If you need to define new types, make sure that CodePageMapping's
         * static initializer defines the string name you use to guarantee
         * success when this block executes.
         *
         * Must be explicit about endianness with CodePageMapping (see the class
         * Javadoc).
         */
        UTF_8 = new FileEncoding(CodePageMapping.getCodePage("UTF-8")); //$NON-NLS-1$
        UTF_16 = new FileEncoding(CodePageMapping.getCodePage("UTF-16LE")); //$NON-NLS-1$
        UTF_16BE = new FileEncoding(CodePageMapping.getCodePage("UTF-16BE")); //$NON-NLS-1$
        UTF_32 = new FileEncoding(CodePageMapping.getCodePage("UTF-32LE")); //$NON-NLS-1$
        UTF_32BE = new FileEncoding(CodePageMapping.getCodePage("UTF-32BE")); //$NON-NLS-1$
    }

    /**
     * Gets the default text encoding for this platform, usually so we can
     * assume newly found files are this encoding if they're not marked by BOM.
     * <p>
     * <ul>
     * <li>First we see if Java declares a default {@link Charset} for this
     * platform (only available on Java 5). If it's set, we try to convert that
     * to a defined {@link FileEncoding}, and if that succeeds we return that
     * value.
     * <li>Next we check if the system property "file.encoding" is set, and try
     * to convert that to a defined {@link FileEncoding}. If that succeeds, we
     * return that value. This value will almost always be set on all platforms
     * and able to be converted to a code page, so most searches end here.
     * <li>Next, if we're on Windows we query for Windows's default code page
     * via JNI, then return a {@link FileEncoding} that wraps that exact value.
     * <li>Next, if we're on z/OS we return a {@link FileEncoding} for IBM1047,
     * the default EBCDIC code page for recent versions of z/OS.
     * <li>Lastly, we return a {@link FileEncoding} for UTF-8, which covers
     * ASCII and ISO-8859-1 character sets, which covers generic Unix.
     * </ul>
     *
     * @return the default text encoding for this platform.
     */
    public static FileEncoding getDefaultTextEncoding() {
        try {
            final Charset defaultCharset = Charset.defaultCharset();

            final int cp = CodePageMapping.getCodePage(defaultCharset);
            log.trace(MessageFormat.format(
                "Detected default encoding from Charset.defaultCharset(): {0} -> {1}", //$NON-NLS-1$
                defaultCharset,
                Integer.toString(cp)));
            return new FileEncoding(cp);
        } catch (final UnknownEncodingException e) {
            log.warn("Could not parse Charset.defaultCharset() value", e); //$NON-NLS-1$
        } catch (final Throwable t) {
            // Charset.defaultCharset() wasn't found in this Java.
        }

        // "file.encoding" method.
        final String fileEncodingValue = System.getProperty("file.encoding"); //$NON-NLS-1$
        if (!StringUtil.isNullOrEmpty(fileEncodingValue)) {
            try {
                final int cp = CodePageMapping.getCodePage(fileEncodingValue);
                log.trace(
                    MessageFormat.format(
                        "Detected default encoding from system property file.encoding: {0} -> {1}", //$NON-NLS-1$
                        fileEncodingValue,
                        Integer.toString(cp)));
                return new FileEncoding(cp);
            } catch (final UnknownEncodingException e) {
                log.warn("Could not parse file.encoding value", e); //$NON-NLS-1$
            }
        }

        if (Platform.isCurrentPlatform(Platform.WINDOWS)) {
            int cp = PlatformMiscUtils.getInstance().getDefaultCodePage();

            /*
             * WindowsFallbackFileSystem always returns -1. Assume Windows-1252
             * in this case.
             */
            if (cp == -1) {
                log.trace("FileSystem.getDefaultCodePage() returned -1, assuming Windows-1252"); //$NON-NLS-1$
                cp = CodePageMapping.getCodePage("Windows-1252"); //$NON-NLS-1$
            }

            log.trace("Detected default encoding for Windows: " + cp); //$NON-NLS-1$
            return new FileEncoding(cp);
        }

        if (Platform.isCurrentPlatform(Platform.Z_OS)) {
            final int cp = 1047;
            log.trace("Detected default encoding for z/OS: " + cp); //$NON-NLS-1$
            return new FileEncoding(cp);
        }

        // Catch all other platforms.
        log.trace("Could not determine a default encoding for this platform, assuming UTF-8"); //$NON-NLS-1$
        return FileEncoding.UTF_8;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Integer.toString(tfsCodePage);
    }

    /**
     * Gets the code page number that corresponds to this FileEncoding instance.
     * The code page returned will be a valid code page OR one of the special
     * numbers reserved for the TFS protocol for binary, unchanged, auto, etc.
     * files.
     *
     * @return the code page number that matches this class's encoding, or one
     *         of the TFS special code page numbers.
     */
    public int getCodePage() {
        return tfsCodePage;
    }

    /**
     * Gets a pretty name to display to the user for this encoding. If no name
     * for the encoding can be found, the encoding's code page will simply be
     * returned as a string.
     *
     * @return the fancy string name of the given encoding (e.g. "utf-8"), or
     *         simply the code page number formatted as string if the name was
     *         not found (e.g. "1253").
     */
    public String getName() {
        return getName(Locale.getDefault());
    }

    /**
     * Gets a pretty name to display to the user for this encoding. If no name
     * for the encoding can be found, the encoding's code page will simply be
     * returned as a string.
     *
     * @param locale
     *        the locale to use when finding localized encoding names (not
     *        null).
     * @return the fancy string name of the given encoding (e.g. "utf-8"), or
     *         simply the code page number formatted as string if the name was
     *         not found (e.g. "1253").
     */
    public String getName(final Locale locale) {
        Check.notNull(locale, "locale"); //$NON-NLS-1$

        if (equals(BINARY)) {
            return "binary"; //$NON-NLS-1$
        } else if (equals(AUTOMATICALLY_DETECT)) {
            return "auto"; //$NON-NLS-1$
        } else if (equals(DEFAULT_TEXT)) {
            return "text"; //$NON-NLS-1$
        } else {
            final Charset c = CodePageMapping.getCharset(tfsCodePage, false);

            if (c != null) {
                return c.displayName(locale);
            }
        }

        return toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof FileEncoding == false) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        final FileEncoding other = (FileEncoding) obj;
        return (other.tfsCodePage == tfsCodePage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + tfsCodePage;

        return result;
    }
}
