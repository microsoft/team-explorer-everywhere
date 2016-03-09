// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util.temp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import junit.framework.TestCase;

public class FastTempOutputStreamTest extends TestCase {
    private static final byte[] BYTES = new byte[] {
        0x1,
        0x2,
        0x3,
        0x4
    };

    public void testConstructionLimits() throws IOException {
        // Shouldn't throw because default limits are sane.
        FastTempOutputStream ftos = new FastTempOutputStream();
        ftos.dispose();

        /*
         * Positive numbers are acceptable.
         */
        ftos = new FastTempOutputStream(2, 1);
        ftos.dispose();
        ftos = new FastTempOutputStream(1, 2);
        ftos.dispose();
        ftos = new FastTempOutputStream(1000, 100);
        ftos.dispose();

        /*
         * 0 is acceptable.
         */
        ftos = new FastTempOutputStream(1000, 0);
        ftos.dispose();
        ftos = new FastTempOutputStream(0, 1000);
        ftos.dispose();
        ftos = new FastTempOutputStream(0, 0);
        ftos.dispose();

        /*
         * Negative numbers trigger defaults.
         */
        ftos = new FastTempOutputStream(100, -1);
        ftos.dispose();
        ftos = new FastTempOutputStream(-1, 100);
        ftos.dispose();
        ftos = new FastTempOutputStream(-1, -999);
        ftos.dispose();
        ftos = new FastTempOutputStream(-999, -1);
        ftos.dispose();
    }

    public void testWriteReadNothing() throws IOException {
        final FastTempOutputStream ftos = new FastTempOutputStream();
        ftos.close();
        assertEquals(-1, ftos.getInputStream().read());
        ftos.dispose();
    }

    public void testWriteReadByte() throws IOException {
        final FastTempOutputStream ftos = new FastTempOutputStream();
        ftos.write(55);
        ftos.close();
        assertEquals(55, ftos.getInputStream().read());
        ftos.dispose();
    }

    public void testWriteReadByteArray() throws IOException {
        final FastTempOutputStream ftos = new FastTempOutputStream();
        ftos.write(BYTES);
        ftos.close();

        final byte[] result = new byte[BYTES.length];
        ftos.getInputStream().read(result);

        assertTrue("array contents should be equal", Arrays.equals(BYTES, result)); //$NON-NLS-1$
        ftos.dispose();
    }

    public void testWriteReadPartialByteArray() throws IOException {
        final FastTempOutputStream ftos = new FastTempOutputStream();
        ftos.write(BYTES, 1, 2);
        ftos.close();

        final byte[] result = new byte[2];

        final InputStream is = ftos.getInputStream();
        is.read(result);
        is.close();

        assertTrue("array contents should be equal", Arrays.equals(new byte[] //$NON-NLS-1$
        {
            BYTES[1],
            BYTES[2]
        }, result));
        ftos.dispose();
    }

    public void testReader() throws IOException {
        final FastTempOutputStream ftos = new FastTempOutputStream();
        ftos.write(BYTES, 1, 2);

        // didn't close the stream!

        try {
            final byte[] result = new byte[2];
            ftos.getInputStream().read(result);

            assertTrue("should throw because didn't close", false); //$NON-NLS-1$
        } catch (final IOException e) {
            ftos.dispose();
        }
    }

    public void testWriteReadBig() throws IOException {
        final byte[] BIG_BYTES = new byte[1048576];

        final FastTempOutputStream ftos = new FastTempOutputStream(1000, 100);
        ftos.write(BIG_BYTES);
        ftos.close();

        final byte[] result = new byte[BIG_BYTES.length];

        final InputStream is = ftos.getInputStream();
        is.read(result);
        is.close();

        assertTrue("array contents should be equal", Arrays.equals(result, BIG_BYTES)); //$NON-NLS-1$
        ftos.dispose();
    }
}
