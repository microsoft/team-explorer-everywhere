// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.chunkingcodec;

import java.io.IOException;

/**
 * This is an abstract class which will linearly process a series of
 * ChunkedEncoders. This is useful, for example, if you have a byte array with
 * some number of types concatenated together.
 *
 * Subclasses should override start() to provide the next ChunkedEncoder.
 */
public abstract class ChunkedEncoderArray implements ChunkedEncoder {
    private ChunkedEncoder currentEncoder;
    private int encoderIdx = 0;

    private boolean complete = false;

    protected ChunkedEncoderArray() {
    }

    /*
     * (non-Javadoc)
     *
     * @see bytearrayutils.ChunkedEncoder#isComplete()
     */
    @Override
    public boolean isComplete() {
        return complete;
    }

    /**
     * This method will be called to get the next Encoder in the array, or null
     * when they have finished decoding. Subclasses must override.
     *
     * @param idx
     *        The zero-based index of the encoder requested
     * @return The next encoder, or null if encoder has finished
     */
    protected abstract ChunkedEncoder start(int idx);

    private void getNextEncoder() {
        currentEncoder = start(encoderIdx);
        encoderIdx++;

        if (currentEncoder == null) {
            complete = true;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see bytearrayutils.ChunkedEncoder#encode(byte[], int, int)
     */
    @Override
    public int encode(final byte[] buf, final int off, final int len) throws IOException {
        int doneLen = 0;

        if (currentEncoder == null) {
            getNextEncoder();
        }

        while (doneLen < len && complete == false) {
            doneLen += currentEncoder.encode(buf, (off + doneLen), (len - doneLen));

            if (currentEncoder.isComplete()) {
                getNextEncoder();
            }
        }

        return doneLen;
    }
}
