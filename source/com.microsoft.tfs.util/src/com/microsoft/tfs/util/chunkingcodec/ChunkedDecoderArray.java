// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.chunkingcodec;

import java.io.IOException;

/**
 * This is an abstract class which will linearly process a series of
 * ChunkedDecoders. This is useful, for example, if you have a byte array with
 * some number of types concatenated together.
 *
 * Subclasses should override start() to provide the next ChunkedDecoder, and
 * may override completed() which will be called whenever the current
 * ChunkedDecoder has finished decoding.
 */
public abstract class ChunkedDecoderArray implements ChunkedDecoder {
    private ChunkedDecoder currentDecoder;
    private int decoderIdx = 0;

    private boolean complete = false;

    protected ChunkedDecoderArray() {
    }

    private void getNextDecoder() {
        currentDecoder = start(decoderIdx);
        decoderIdx++;

        if (currentDecoder == null) {
            complete = true;
        }
    }

    /**
     * This method will be called to get the next Decoder in the array, or null
     * when they have finished decoding. Subclasses must override.
     *
     * @param idx
     *        The zero-based index of the decoder requested
     * @return The next decoder, or null if decoding has finished
     */
    protected abstract ChunkedDecoder start(int idx);

    /*
     * (non-Javadoc)
     *
     * @see bytearrayutils.ChunkedDecoder#decode(byte[], int, int)
     */
    @Override
    public final int decode(final byte[] buf, final int off, final int len) throws IOException {
        int doneLen = 0;

        if (currentDecoder == null) {
            getNextDecoder();
        }

        while (doneLen < len && complete == false) {
            doneLen += currentDecoder.decode(buf, (off + doneLen), (len - doneLen));

            if (currentDecoder.isComplete()) {
                getNextDecoder();
            }
        }

        return doneLen;
    }

    /**
     * This will be called when a single entry in the array has finished
     * deserializing. Subclasses should override.
     *
     * @param idx
     *        The entry that is contained
     * @param decoder
     *        The decoder which completed
     */
    protected void completed(final int idx, final ChunkedDecoder decoder) {
    }

    /*
     * (non-Javadoc)
     *
     * @see bytearrayutils.ChunkedDecoder#isComplete()
     */
    @Override
    public final boolean isComplete() {
        return complete;
    }

    /*
     * (non-Javadoc)
     *
     * @see bytearrayutils.ChunkedDecoder#close()
     */
    public void close() throws IOException {
    }
}
