// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.jni.appleforked.AppleForkedUtils;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedHeader;
import com.microsoft.tfs.jni.appleforked.stream.AppleSingleDecoderStream;
import com.microsoft.tfs.jni.appleforked.stream.AppleSingleEncoderStream;
import com.microsoft.tfs.util.Platform;

/**
 * Helper methods for dealing with AppleSingle files. These create AppleSingle
 * files from an on-disk resource for Mac OS X platforms. For all other
 * platforms, these return without doing any work.
 */
public class AppleSingleUtil {
    private static final Log log = LogFactory.getLog(AppleSingleUtil.class);

    public static boolean isSupportedPlatform() {
        return Platform.isCurrentPlatform(Platform.MAC_OS_X);
    }

    /**
     * Encodes a file in-place, creating an AppleSingle file in place of the
     * given file.
     *
     * Technically creates an AppleSingle file in a temp file and renames it
     * over the given file. Note that this is not atomic.
     *
     * @param file
     *        The file to encode into an AppleSingle file
     */
    public static void encodeFile(final File file) throws IOException {
        encodeFile(file, file.getName());
    }

    /**
     * Encodes a file in-place, creating an AppleSingle file in place of the
     * given file.
     *
     * Technically creates an AppleSingle file in a temp file and renames it
     * over the given file. Note that this is not atomic.
     *
     * @param file
     *        The file to encode into an AppleSingle file
     * @param filename
     *        The output filename to encode in the AppleSingle file
     */
    public static void encodeFile(final File file, final String filename) throws IOException {
        /*
         * Do not alter files on non-Mac OS X platforms
         *
         * TODO: at some point we should split these into data fork +
         * AppleDouble files
         */
        if (!Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            throw new IOException(
                MessageFormat.format(
                    Messages.getString("AppleSingleUtil.NotSupportedOnPlatformFormat"), //$NON-NLS-1$
                    Platform.getCurrentPlatformString()));
        }

        final File directory = file.getParentFile();
        File temp = File.createTempFile("teApple", ".tmp", directory); //$NON-NLS-1$ //$NON-NLS-2$

        try {
            AppleSingleEncoderStream input = null;
            OutputStream output = null;

            /*
             * Create an AppleSingleEncoderStream. This is what does all the
             * magic, it reads a file and its resource forks, metadata, etc, off
             * disk and provides that information in an AppleSingle stream.
             *
             * Note that we allow callers to override filename (as they're
             * usually giving us a temp file) and we use an epoch date (to
             * ensure md5 sanity on unchanged files.)
             */
            try {
                input = new AppleSingleEncoderStream(file);
                input.setFilesystem("Teamprise"); //$NON-NLS-1$
                input.setFilename(filename);
                input.setDate(new Date(0));

                output = new FileOutputStream(temp);

                duplicateFile(input, output);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (final Exception e) {
                        log.warn(Messages.getString("AppleSingleUtil.CouldNotCloseAppleSingleInputStream"), e); //$NON-NLS-1$
                    }
                }

                if (output != null) {
                    try {
                        output.close();
                    } catch (final Exception e) {
                        log.warn(Messages.getString("AppleSingleUtil.CouldNotCloseAppleSingleOutputStream"), e); //$NON-NLS-1$
                    }
                }
            }

            renameFile(temp, file);
            temp = null;
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }

    /**
     * Decodes a file in-place, creating an on-disk resource that is
     * representative of the AppleSingle file provided.
     *
     * Technically creates the files in a temp file and renames it over the
     * given file. Note that this is not atomic.
     *
     * @param file
     *        The AppleSingle file to decode
     */
    public static void decodeFile(final File file) throws IOException {
        /*
         * Do nothing on non-Mac OS X platforms.
         *
         * TODO: at some point we should split this into data fork + AppleDouble
         */
        if (!Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
            throw new IOException(
                MessageFormat.format(
                    Messages.getString("AppleSingleUtil.NotSupportedOnPlatformFormat"), //$NON-NLS-1$
                    Platform.getCurrentPlatformString()));
        }

        /*
         * Check to make sure that this is a valid AppleSingle file that we
         * created (ie, has filesystem stamp of "Teamprise").
         */
        final AppleForkedHeader header = AppleForkedUtils.getHeader(file);

        if (header.getMagic() != AppleForkedConstants.MAGIC_APPLESINGLE || !header.getFilesystem().equals("Teamprise")) //$NON-NLS-1$
        {
            throw new IOException(Messages.getString("AppleSingleUtil.FileIsNotTFSApplieSingleFile")); //$NON-NLS-1$
        }

        final File directory = file.getParentFile();
        File temp = File.createTempFile("teApple", ".tmp", directory); //$NON-NLS-1$ //$NON-NLS-2$

        try {
            InputStream input = null;
            AppleSingleDecoderStream output = null;

            /*
             * Create an AppleSingleDecoderStream. This is what does all the
             * magic, it reads an AppleSingle file streamed to it and creates an
             * output file (data fork), its resource forks, metadata, etc.
             */
            try {
                input = new FileInputStream(file);
                output = new AppleSingleDecoderStream(temp);
                output.ignoreEntry(AppleForkedConstants.ID_DATEINFO);

                duplicateFile(input, output);
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (final Exception e) {
                        log.warn(Messages.getString("AppleSingleUtil.CouldNotCloseAppleSingleInputStream"), e); //$NON-NLS-1$
                    }
                }

                if (output != null) {
                    try {
                        output.close();
                    } catch (final Exception e) {
                        log.warn(Messages.getString("AppleSingleUtil.CouldNotCloseAppleSingleOutputStream"), e); //$NON-NLS-1$
                    }
                }
            }

            renameFile(temp, file);
            temp = null;
        } finally {
            if (temp != null) {
                temp.delete();
            }
        }
    }

    private static void duplicateFile(final InputStream input, final OutputStream output) throws IOException {
        final byte[] buffer = new byte[102400];
        int readlen;

        while ((readlen = input.read(buffer, 0, buffer.length)) > 0) {
            output.write(buffer, 0, readlen);
        }
    }

    private static void renameFile(final File source, final File target) throws IOException {
        if (target.delete() == false) {
            final String message =
                MessageFormat.format(
                    Messages.getString("AppleSingleUtil.ErrorDeletingFileForReplacementFormat"), //$NON-NLS-1$
                    target.getAbsolutePath());

            log.error(message);
            throw new IOException(message);
        }

        if (source.renameTo(target) == false) {
            final String message =
                MessageFormat.format(
                    Messages.getString("AppleSingleUtil.ErrorRenamingTempFileFormat"), //$NON-NLS-1$
                    source.getAbsolutePath(),
                    target.getAbsolutePath());

            log.error(message);
            throw new IOException(message);
        }
    }
}
