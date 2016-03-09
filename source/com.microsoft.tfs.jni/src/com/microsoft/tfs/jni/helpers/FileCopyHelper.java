// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.jni.FileSystemAttributes;
import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.appleforked.ResourceForkInputStream;
import com.microsoft.tfs.jni.appleforked.ResourceForkOutputStream;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;

/**
 * FileCopyHelper will copy a file, including any platform-specific metadata.
 * (For example, Apple extended attributes)
 */
public class FileCopyHelper {
    private final static Log log = LogFactory.getLog(FileCopyHelper.class);

    /**
     * Copies a text file and any extended attributes, converting charsets along
     * the way. If any provided charsets are <code>null</code>, the default
     * charset is assumed.
     *
     * This method should not be used for binary files, instead
     * {@link FileCopyHelper#copy(String, String)} should be used.
     *
     * @param source
     *        The path to the source file
     * @param sourceCharset
     *        The charset of the source file (may be <code>null</code>)
     * @param destination
     *        The path to the target
     * @param destinationCharset
     *        The charset of the target (may be <code>null</code>)
     * @throws FileNotFoundException
     *         If the source does not exist, or the target's parent folder does
     *         not exist
     * @throws IOException
     *         If there was an IOException reading the source or writing the
     *         target
     */
    public static void copyText(
        final String source,
        Charset sourceCharset,
        final String destination,
        Charset destinationCharset)
            throws FileNotFoundException,
                IOException,
                MalformedInputException,
                UnmappableCharacterException {
        Check.notNull(source, "source"); //$NON-NLS-1$
        Check.notNull(destination, "destination"); //$NON-NLS-1$

        if (sourceCharset == null) {
            sourceCharset = Charset.defaultCharset();
        }

        if (destinationCharset == null) {
            destinationCharset = Charset.defaultCharset();
        }

        Reader in = null;
        Writer out = null;

        try {
            final CharsetDecoder decoder = sourceCharset.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

            in = new BufferedReader(new InputStreamReader(new FileInputStream(source), decoder));
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destination), destinationCharset));

            copy(in, out);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                    log.warn(MessageFormat.format("Could not close {0} for reading: {1}", source, e.getMessage())); //$NON-NLS-1$
                }
            }

            if (out != null) {
                try {
                    out.close();
                } catch (final IOException e) {
                    log.warn(MessageFormat.format("Could not close {0} for writing: {1}", destination, e.getMessage())); //$NON-NLS-1$
                }
            }
        }

        copyAttributes(source, destination);
    }

    /**
     * Copies a file and any extended attributes.
     *
     * @param source
     *        The path to the source file (not <code>null</code>)
     * @param destination
     *        The path to the target (not <code>null</code>)
     * @throws FileNotFoundException
     *         If the source does not exist, or the target's parent folder does
     *         not exist
     * @throws IOException
     *         If there was an IOException reading the source or writing the
     *         target
     */
    public static void copy(final String source, final String destination) throws FileNotFoundException, IOException {
        Check.notNull(source, "source"); //$NON-NLS-1$
        Check.notNull(destination, "destination"); //$NON-NLS-1$

        final File sourceFile = new File(source);
        final FileSystemUtils util = FileSystemUtils.getInstance();
        final FileSystemAttributes attr = util.getAttributes(sourceFile);
        if (attr.isSymbolicLink()) {
            final String targetPath = util.getSymbolicLink(source);
            util.createSymbolicLink(targetPath, destination);
        } else {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new BufferedInputStream(new FileInputStream(source));
                out = new BufferedOutputStream(new FileOutputStream(destination));

                copy(in, out);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (final IOException e) {
                        log.warn(MessageFormat.format("Could not close {0} for reading: {1}", source, e.getMessage())); //$NON-NLS-1$
                    }
                }

                if (out != null) {
                    try {
                        out.close();
                    } catch (final IOException e) {
                        log.warn(
                            MessageFormat.format("Could not close {0} for writing: {1}", destination, e.getMessage())); //$NON-NLS-1$
                    }
                }
            }
            // apply file attributes (e.g. +x)
            util.setAttributes(destination, attr);
            copyAttributes(source, destination);
        }
    }

    private static void copyAttributes(final String source, final String destination)
        throws FileNotFoundException,
            IOException {
        Check.notNull(source, "source"); //$NON-NLS-1$
        Check.notNull(destination, "destination"); //$NON-NLS-1$

        /* Copy Mac OS X extended attributes */
        if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            final String[] xattrs = FileSystemUtils.getInstance().listMacExtendedAttributes(source);

            if (xattrs == null) {
                log.warn(MessageFormat.format("Could not query extended attributes for {0}", source)); //$NON-NLS-1$
            } else {
                for (int i = 0; i < xattrs.length; i++) {
                    final String xattrName = xattrs[i];

                    if (xattrName == null || xattrName.equals("")) //$NON-NLS-1$
                    {
                        log.warn(MessageFormat.format("Got empty extended attribute for {0}, ignored", source)); //$NON-NLS-1$
                    }

                    /*
                     * Resource Forks should be handled differently than other
                     * extended attributes, as they can be arbitrary length. We
                     * use an input stream / output stream to copy them in
                     * chunks.
                     */
                    else if (xattrName.equals("com.apple.ResourceFork")) //$NON-NLS-1$
                    {
                        InputStream in = null;
                        OutputStream out = null;

                        try {
                            in = new BufferedInputStream(new ResourceForkInputStream(source));
                            out = new BufferedOutputStream(new ResourceForkOutputStream(destination));

                            copy(in, out);
                        } finally {
                            if (in != null) {
                                try {
                                    in.close();
                                } catch (final IOException e) {
                                    log.warn(MessageFormat.format(
                                        "Could not close resource fork {0} for reading: {1}", //$NON-NLS-1$
                                        source,
                                        e.getMessage()));
                                }
                            }

                            if (out != null) {
                                try {
                                    out.close();
                                } catch (final IOException e) {
                                    log.warn(MessageFormat.format(
                                        "Could not close resource fork {0} for reading: {1}", //$NON-NLS-1$
                                        destination,
                                        e.getMessage()));
                                }
                            }
                        }
                    }

                    /*
                     * All other extended attributes, we just treat as opaque
                     * byte buckets.
                     */
                    else {
                        final byte[] xattrValue =
                            FileSystemUtils.getInstance().getMacExtendedAttribute(source, xattrName);

                        if (!FileSystemUtils.getInstance().setMacExtendedAttribute(
                            destination,
                            xattrName,
                            xattrValue)) {
                            throw new IOException(MessageFormat.format(
                                "Could not write extended attribute {0} for {1}", //$NON-NLS-1$
                                xattrName,
                                destination));
                        }
                    }
                }
            }
        }
    }

    private static void copy(final InputStream source, final OutputStream destination) throws IOException {
        final byte[] buffer = new byte[4096];
        int len;

        while ((len = source.read(buffer)) != -1) {
            destination.write(buffer, 0, len);
        }
    }

    private static void copy(final Reader source, final Writer destination) throws IOException {
        final char[] buffer = new char[4096];
        int len;

        while ((len = source.read(buffer)) != -1) {
            destination.write(buffer, 0, len);
        }
    }
}
