// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.jni.appleforked.stream.encoder.entry.AppleForkedCommentEncoder;
import com.microsoft.tfs.jni.appleforked.stream.encoder.entry.AppleForkedDataForkEncoder;
import com.microsoft.tfs.jni.appleforked.stream.encoder.entry.AppleForkedDateEncoder;
import com.microsoft.tfs.jni.appleforked.stream.encoder.entry.AppleForkedEntryEncoder;
import com.microsoft.tfs.jni.appleforked.stream.encoder.entry.AppleForkedFilenameEncoder;
import com.microsoft.tfs.jni.appleforked.stream.encoder.entry.AppleForkedFinderInfoEncoder;
import com.microsoft.tfs.jni.appleforked.stream.encoder.entry.AppleForkedMacFileInfoEncoder;
import com.microsoft.tfs.jni.appleforked.stream.encoder.entry.AppleForkedResourceForkEncoder;

/**
 * Streams an AppleSingle file from the representative portions on the
 * filesystem (eg, the data fork, resource fork, finder info, etc.)
 *
 * Uses the {@link AppleForkedEncoderStream} framework to handle the encoding.
 */
public class AppleSingleEncoderStream extends AppleForkedEncoderStream {
    /* Callers can override the filename */
    private String filename = null;

    /* Callers can override the timestamp */
    private Date date = null;

    /**
     * Creates an AppleSingle stream from the resources in the given file
     *
     * @param file
     *        The File to read data and resources from (must exist)
     */
    public AppleSingleEncoderStream(final File file) {
        super(file, AppleForkedConstants.MAGIC_APPLESINGLE);
    }

    /**
     * Creates an AppleSingle stream from the resources in the given file
     *
     * @param filename
     *        The name of the file to read data and resources from (must exist)
     */
    public AppleSingleEncoderStream(final String filename) {
        super(filename, AppleForkedConstants.MAGIC_APPLESINGLE);
    }

    /**
     * Callers can override the (output) filename
     *
     * @param filename
     *        The filename to use for the AppleSingle decoded output
     */
    public void setFilename(final String filename) {
        this.filename = filename;
    }

    /**
     * Callers can override the (output) date
     *
     * @param date
     *        The date to use for the AppleSingle decoded output
     */
    public void setDate(final Date date) {
        this.date = date;
    }

    @Override
    protected AppleForkedEntryEncoder[] configureEncoders(final File file) {
        final List encoders = new ArrayList();

        /*
         * Order is optimized for streaming, placing the data fork last. (This
         * is most convenient for converting into an AppleDouble file in a
         * streaming manner.)
         */

        /*
         * Set the filename -- callers may have overridden, otherwise just use
         * the filename we were passed
         */
        if (filename != null) {
            encoders.add(new AppleForkedFilenameEncoder(filename));
        } else {
            encoders.add(new AppleForkedFilenameEncoder(file));
        }

        /*
         * Set the date -- callers may have overridden, otherwise just use the
         * file's date
         */
        if (date != null) {
            encoders.add(new AppleForkedDateEncoder(date));
        } else {
            encoders.add(new AppleForkedDateEncoder(file));
        }

        /* Mac File Info */
        encoders.add(new AppleForkedMacFileInfoEncoder(file));

        /* Finder Info */
        final AppleForkedFinderInfoEncoder finderInfoEncoder = new AppleForkedFinderInfoEncoder(file);
        if (finderInfoEncoder.getLength() > 0) {
            encoders.add(new AppleForkedFinderInfoEncoder(file));
        }

        /* Finder Comments (Spotlight Comments in 10.4+) */
        final AppleForkedCommentEncoder commentEncoder = new AppleForkedCommentEncoder(file);
        if (commentEncoder.getLength() > 0) {
            encoders.add(commentEncoder);
        }

        /* Resource Fork */
        final AppleForkedResourceForkEncoder resourceForkEncoder = new AppleForkedResourceForkEncoder(file);
        if (resourceForkEncoder.getLength() > 0) {
            encoders.add(resourceForkEncoder);
        }

        /* Data Fork */
        encoders.add(new AppleForkedDataForkEncoder(file));

        return (AppleForkedEntryEncoder[]) encoders.toArray(new AppleForkedEntryEncoder[encoders.size()]);
    }
}
