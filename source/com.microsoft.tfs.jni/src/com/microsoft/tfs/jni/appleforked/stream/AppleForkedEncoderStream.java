// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedEntryDescriptor;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedHeader;
import com.microsoft.tfs.jni.appleforked.stream.encoder.AppleForkedEntryDescriptorArrayEncoder;
import com.microsoft.tfs.jni.appleforked.stream.encoder.AppleForkedHeaderEncoder;
import com.microsoft.tfs.jni.appleforked.stream.encoder.entry.AppleForkedEntryEncoder;
import com.microsoft.tfs.util.Check;

public abstract class AppleForkedEncoderStream extends InputStream {
    private final int magic;
    private int version = AppleForkedConstants.VERSION_2;
    private String filesystem = null;

    private final File inputFile;

    private boolean configured = false;
    private boolean complete = false;

    private int index = 0;

    /* The header encoder */
    private AppleForkedHeaderEncoder headerEncoder;

    /* The entry descriptors (and encoder) */
    private AppleForkedEntryDescriptor[] entryDescriptor;
    private AppleForkedEntryDescriptorArrayEncoder descriptorArrayEncoder;

    /* The entry encoders */
    private AppleForkedEntryEncoder[] entryEncoder;

    protected AppleForkedEncoderStream(final File file, final int magic) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        this.magic = magic;
        inputFile = file;
    }

    protected AppleForkedEncoderStream(final String filename, final int magic) {
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        this.magic = magic;
        inputFile = new File(filename);
    }

    public final void setVersion(final int version) {
        this.version = version;
    }

    public final void setFilesystem(final String filesystem) {
        this.filesystem = filesystem;
    }

    protected abstract AppleForkedEntryEncoder[] configureEncoders(File file);

    @Override
    public final int read() throws IOException {
        final byte[] readBytes = new byte[1];

        if (read(readBytes, 0, 1) < 0) {
            return -1;
        }

        return (readBytes[0] & 0xFF);
    }

    @Override
    public final int read(final byte[] buf, final int off, final int len) throws IOException {
        int readlen = 0;

        if (!configured) {
            configure();
        }

        if (complete == true) {
            return -1;
        }

        while (readlen < len && complete == false) {
            int passlen;

            /* Encode the header until it's been fully delivered */
            if (!headerEncoder.isComplete()) {
                passlen = headerEncoder.encode(buf, (off + readlen), (len - readlen));
            }

            /* Encode the entry descriptor array until it's been delivered */
            else if (!descriptorArrayEncoder.isComplete()) {
                passlen = descriptorArrayEncoder.encode(buf, (off + readlen), (len - readlen));
            }

            /* We're encoding an entry */
            else {
                /* Determine which entry this data is for */
                AppleForkedEntryDescriptor currentDescriptor = null;
                AppleForkedEntryEncoder currentEncoder = null;

                /* Keep track of the next entry's offset if we're in some hole */
                long nextOffset = -1;

                for (int i = 0; i < entryDescriptor.length; i++) {
                    /* See if we're being asked to encode inside an entry */
                    if (index >= entryDescriptor[i].getOffset()
                        && index < entryDescriptor[i].getOffset() + entryDescriptor[i].getLength()) {
                        currentDescriptor = entryDescriptor[i];
                        currentEncoder = entryEncoder[i];
                        break;
                    }

                    /*
                     * We're in a "hole" between entries - keep track of the
                     * next entry offset
                     */
                    else if (index < entryDescriptor[i].getOffset() && entryDescriptor[i].getOffset() < nextOffset) {
                        nextOffset = entryDescriptor[i].getOffset();
                    }
                }

                if (currentDescriptor != null) {
                    if (currentEncoder.isComplete()) {
                        throw new IOException(MessageFormat.format(
                            "Buffer underrun on entry {0}", //$NON-NLS-1$
                            Long.toString(currentDescriptor.getType())));
                    }

                    passlen = currentEncoder.encode(buf, (off + readlen), (len - readlen));
                }

                /*
                 * We're being asked to encode a "hole" in the AppleSingle file.
                 * This is specifically allowed by the format (to allow entries
                 * to grow and shrink without rearranging the file contents)
                 * although we do not create them at this time, we support them
                 * here in case we wish to in the future.
                 */
                else if (nextOffset > 0) {
                    passlen = (int) Math.min(nextOffset - index, len - readlen);
                }

                /* End of file */
                else {
                    complete = true;
                    passlen = 0;
                }
            }

            readlen += passlen;
            index += passlen;
        }

        if (readlen == 0 && complete == true) {
            return -1;
        }

        return readlen;
    }

    private void configure() throws IOException {
        /*
         * Subclasses configure the various encoders they're interested in
         * delivering
         */
        entryEncoder = configureEncoders(inputFile);

        if (entryEncoder == null) {
            throw new IOException(MessageFormat.format(
                "No entry encoders found for {0} file", //$NON-NLS-1$
                AppleForkedConstants.getNameFromMagic(magic)));
        }

        /*
         * Header (and encoder)
         */
        final AppleForkedHeader header = new AppleForkedHeader(magic, version, filesystem, entryEncoder.length);
        headerEncoder = new AppleForkedHeaderEncoder(header);

        /*
         * Entry descriptors (and encoder)
         */
        entryDescriptor = new AppleForkedEntryDescriptor[entryEncoder.length];

        long nextOffset =
            AppleForkedHeader.HEADER_SIZE + (AppleForkedEntryDescriptor.ENTRY_DESCRIPTOR_SIZE * entryEncoder.length);

        for (int i = 0; i < entryEncoder.length; i++) {
            final long entryLength = entryEncoder[i].getLength();

            entryDescriptor[i] = new AppleForkedEntryDescriptor(entryEncoder[i].getType(), nextOffset, entryLength);

            nextOffset += entryLength;
        }

        descriptorArrayEncoder = new AppleForkedEntryDescriptorArrayEncoder(entryDescriptor);

        configured = true;
    }

    /*
     * Closes all resources associated with this encoder (ie, all subdecoders.)
     *
     * (non-Javadoc)
     *
     * @see java.io.InputStream#close()
     */
    @Override
    public final void close() throws IOException {
        if (entryEncoder != null) {
            boolean complete = true;

            for (int i = 0; i < entryEncoder.length; i++) {
                if (!entryEncoder[i].isComplete()) {
                    complete = false;
                }

                entryEncoder[i].close();
            }

            if (!complete) {
                throw new IOException(MessageFormat.format(
                    "Incomplete {0} file", //$NON-NLS-1$
                    AppleForkedConstants.getNameFromMagic(magic)));
            }
        }
    }

    /*
     * Finalizer to ensure that we've been closed.
     *
     * (non-Javadoc)
     *
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }
}
