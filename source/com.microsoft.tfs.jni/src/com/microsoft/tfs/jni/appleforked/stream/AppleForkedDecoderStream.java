// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked.stream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedConstants;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedEntryDescriptor;
import com.microsoft.tfs.jni.appleforked.fileformat.AppleForkedHeader;
import com.microsoft.tfs.jni.appleforked.stream.decoder.AppleForkedEntryDescriptorArrayDecoder;
import com.microsoft.tfs.jni.appleforked.stream.decoder.AppleForkedHeaderDecoder;
import com.microsoft.tfs.jni.appleforked.stream.decoder.entry.AppleForkedEntryDecoder;
import com.microsoft.tfs.jni.appleforked.stream.decoder.entry.AppleForkedEntryDecoderFactory;
import com.microsoft.tfs.jni.appleforked.stream.decoder.entry.AppleForkedNullEntryDecoder;

/**
 * Creates an on-disk representation of the data contained in an AppleSingle or
 * AppleDouble ("AppleForked") file. When a client calls write(), the
 * AppleForked data is automatically decoded and written to the appropriate
 * place on the Mac file system. That is, data fork contents of the AppleSingle
 * file go directly to the data fork (as initialized by the File or String
 * parameter to the ctor.) Resource fork data is sent to the resource fork, etc.
 *
 * Note that since this is a streaming interface, some fields are ignored
 * (notable the filename field.)
 */

public abstract class AppleForkedDecoderStream extends OutputStream {
    /* The expected magic, provided by subclasses */
    private final int magic;

    /* List of entries which we should ignore while decoding */
    private final List ignoredList = new ArrayList();

    /* The current index into the AppleSingle file */
    private long index = 0;

    /* The header, and a chunking encoder */
    private final AppleForkedHeaderDecoder headerDecoder = new AppleForkedHeaderDecoder();
    private AppleForkedHeader header;

    /* The entry descriptors and a chunking encoder */
    private AppleForkedEntryDescriptorArrayDecoder entryDescriptorDecoder;
    private AppleForkedEntryDescriptor[] entryDescriptor;

    /* The entries for this file */
    private AppleForkedEntryDecoder[] entryDecoder;

    private final File outputFile;

    public AppleForkedDecoderStream(final File file, final int magic) {
        this.magic = magic;
        outputFile = file;
    }

    public AppleForkedDecoderStream(final String string, final int magic) {
        this(new File(string), magic);
    }

    /**
     * Determines whether a particular entry will be ignored. (For example,
     * callers may choose to ignore the dates in an AppleSingle file.)
     *
     * Subclasses may override, but should call this method.
     *
     * @param entryType
     *        The entry type (see AppleSingleConstants) that should not be
     *        written to disk
     * @return
     */
    protected boolean isIgnored(final long entryType) {
        return ignoredList.contains(new Long(entryType));
    }

    /**
     * Toggles the decoder's ignorance of a particular field. If a field is
     * ignored, it will not be written to disk -- for example, pass
     * {@link AppleForkedConstants#ID_RESOURCEFORK} to ignore the resource fork
     * described in the AppleSingle document.
     *
     * @param entryType
     *        The entry ID to ignore
     * @param ignored
     *        true to ignore the entry, false to stream it to disk
     */
    public final void ignoreEntry(final long entryType) {
        ignoredList.add(new Long(entryType));
    }

    @Override
    public final void write(final int b) throws IOException {
        write(new byte[] {
            (byte) b
        }, 0, 1);
    }

    /*
     * Given the bytes specified by buf, beginning at offset off and spanning
     * length len, we stream these to the appropriate entry described by the
     * AppleSingle file.
     *
     * We first attempt to create the header, then each entry definition, then
     * we write to each entry. Some of these are stored in-memory, while some
     * (data fork) go directly to disk. Certain entries may be ignored, and
     * holes in the file (ie, undefined space outside the header) are ignored.
     *
     * (non-Javadoc)
     *
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public final void write(final byte[] buf, final int off, final int len) throws IOException {
        int writelen = 0;

        /* Our header is not complete */
        while (writelen < len) {
            int passlen;

            if (!headerDecoder.isComplete()) {
                passlen = headerDecoder.decode(buf, (off + writelen), (len - writelen));

                if (headerDecoder.isComplete()) {
                    setupHeader();
                }
            }

            /* Our entry descriptors aren't complete */
            else if (!entryDescriptorDecoder.isComplete()) {
                passlen = entryDescriptorDecoder.decode(buf, (off + writelen), (len - writelen));

                if (entryDescriptorDecoder.isComplete()) {
                    setupEntries();
                }
            }

            /* This is data for an entry */
            else {
                /* Determine which entry this data is for */
                AppleForkedEntryDescriptor currentDescriptor = null;
                AppleForkedEntryDecoder currentDecoder = null;

                /* Keep track of the next entry's offset if we're in some hole */
                long nextOffset = -1;

                for (int i = 0; i < entryDescriptor.length; i++) {
                    /* See if we're being asked to write inside an entry */
                    if (index >= entryDescriptor[i].getOffset()
                        && index < entryDescriptor[i].getOffset() + entryDescriptor[i].getLength()) {
                        currentDescriptor = entryDescriptor[i];
                        currentDecoder = entryDecoder[i];
                        break;
                    }
                    /* We need to keep track of the next entry */
                    else if (index < entryDescriptor[i].getOffset() && entryDescriptor[i].getOffset() < nextOffset) {
                        nextOffset = entryDescriptor[i].getOffset();
                    }
                }

                if (currentDescriptor != null) {
                    if (currentDecoder.isComplete()) {
                        throw new IOException(
                            MessageFormat.format("Entry {0} is full", Long.toString(currentDescriptor.getType()))); //$NON-NLS-1$
                    }

                    passlen = currentDecoder.decode(buf, (off + writelen), (len - writelen));
                }

                /*
                 * Note that data outside an entry is specifically legal in the
                 * specification. This is so that entries can grow as they see
                 * fit, thus we need to consume available bytes up to the next
                 * offset.
                 */
                else {
                    // Data at the end of the file (ignore index, we could be
                    // getting streamed more than Long.MAX_VALUE!)
                    if (nextOffset < 0) {
                        passlen = (len - writelen);
                    } else {
                        passlen = (int) Math.min(nextOffset - index, len - writelen);
                    }
                }
            }

            writelen += passlen;
            index += passlen;
        }
    }

    /**
     * Called to setup the header data once it has been successfully read from
     * the stream.
     *
     * @throws IOException
     */
    private final void setupHeader() throws IOException {
        header = headerDecoder.getHeader();

        if (header.getMagic() != magic) {
            throw new IOException(MessageFormat.format(
                "File is not an {0} file", //$NON-NLS-1$
                AppleForkedConstants.getNameFromMagic(magic)));
        }

        if (header.getVersion() != AppleForkedConstants.VERSION_1
            && header.getVersion() != AppleForkedConstants.VERSION_2) {
            throw new IOException(MessageFormat.format(
                "File is unknown AppleSingle version: 0x{0}", //$NON-NLS-1$
                Integer.toHexString(header.getVersion())));
        }

        entryDescriptorDecoder = new AppleForkedEntryDescriptorArrayDecoder(header.getEntryCount());
    }

    /**
     * Called to setup the entry data and the decoders once the entry list has
     * been successfully read from the stream.
     *
     * @throws IOException
     */
    private final void setupEntries() throws IOException {
        entryDescriptor = entryDescriptorDecoder.getEntryDescriptors();

        /* Create the decoders for our entry data */
        entryDecoder = new AppleForkedEntryDecoder[entryDescriptor.length];

        for (int i = 0; i < entryDescriptor.length; i++) {
            /*
             * Certain types of entries may be ignored -- for example, gets from
             * TFS should NOT set the date
             */
            if (isIgnored(entryDescriptor[i].getType())) {
                entryDecoder[i] = new AppleForkedNullEntryDecoder(entryDescriptor[i]);
            } else {
                entryDecoder[i] = AppleForkedEntryDecoderFactory.getDecoder(entryDescriptor[i], outputFile);
            }
        }
    }

    /*
     * Closes all resources associated with this decoder (ie, all subdecoders.)
     *
     * (non-Javadoc)
     *
     * @see java.io.OutputStream#close()
     */
    @Override
    public final void close() throws IOException {
        if (entryDecoder != null) {
            boolean complete = true;

            for (int i = 0; i < entryDecoder.length; i++) {
                if (entryDecoder[i] == null) {
                    complete = false;
                } else {
                    if (!entryDecoder[i].isComplete()) {
                        complete = false;
                    }

                    entryDecoder[i].close();
                }
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
