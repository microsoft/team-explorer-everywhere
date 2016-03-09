// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.internal.ntlm;

import java.security.DigestException;
import java.security.MessageDigestSpi;
import java.security.Provider;

/*
 * THIS FILE IS THIRD PARTY OPEN SOURCE SOFTWARE. PLEASE ENSURE ATTRIBUTIONS
 * REMAIN.
 *
 * Origin: Cryptix V3 <http://www.cryptix.org/>
 *
 * Software License:
 *
 * Cryptix General License
 *
 * Copyright (c) 1995-2005 The Cryptix Foundation Limited. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the copyright notice, this list
 * of conditions and the following disclaimer. 2. Redistributions in binary form
 * must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE CRYPTIX FOUNDATION LIMITED AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE CRYPTIX FOUNDATION LIMITED OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * MD4 message digest algorithm.
 *
 * <ul>
 * <li>Ronald L. Rivest, "<a href="http://www.ietf.org/rfc/rfc1320.html"> The
 * MD4 Message-Digest Algorithm</a>", IETF RFC-1320 (informational).</li>
 * </ul>
 *
 * @version $Revision: 1.8 $
 * @author Raif S. Naffah
 * @author Jeroen C. van Gelderen (gelderen@cryptix.org)
 */
public class MD4 extends MessageDigestSpi {
    /* Constants */

    /** Size (in bytes) of the blocks. */
    private final int blockSize = 64;

    /** Size (in bytes) of this hash */
    private static final int hashSize = 16;

    // Instance variables
    // ...........................................................................

    /** 64 byte buffer */
    private final byte[] buf;

    /** Buffer offset */
    private int bufOff;

    /** Number of bytes hashed 'till now. */
    private long byteCount;

    /** 4 32-bit words (interim result) */
    private int[] context = new int[4];

    /** 512 bits work buffer = 16 x 32-bit words */
    private int[] X = new int[16];

    /* package */

    // Constructors
    // ...........................................................................
    public MD4() {
        buf = new byte[blockSize];
        bufOff = 0;
        byteCount = 0;

        coreReset();
    }

    private MD4(final MD4 src) {
        buf = src.buf.clone();
        bufOff = src.bufOff;
        byteCount = src.byteCount;
        context = src.context.clone();
        X = src.X.clone();
    }

    @Override
    public Object clone() {
        return new MD4(this);
    }

    @Override
    protected void engineUpdate(final byte input) {
        byteCount += 1;
        buf[bufOff++] = input;
        if (bufOff == blockSize) {
            coreUpdate(buf, 0);
            bufOff = 0;
        }
    }

    @Override
    protected void engineUpdate(final byte[] input, int offset, int length) {
        byteCount += length;

        int todo;
        while (length >= (todo = blockSize - bufOff)) {
            System.arraycopy(input, offset, buf, bufOff, todo);
            coreUpdate(buf, 0);
            length -= todo;
            offset += todo;
            bufOff = 0;
        }

        // #ASSERT(this.bufOff < blockSize);

        System.arraycopy(input, offset, buf, bufOff, length);
        bufOff += length;
    }

    @Override
    protected byte[] engineDigest() {
        final byte[] tmp = new byte[hashSize];
        privateDigest(tmp, 0, hashSize);
        return tmp;
    }

    @Override
    protected int engineDigest(final byte[] buf, final int offset, final int len) throws DigestException {
        if (len < hashSize) {
            throw new DigestException();
        }

        return privateDigest(buf, offset, len);
    }

    /**
     * Same as protected int engineDigest(byte[] buf, int offset, int len)
     * except that we don't validate arguments.
     */
    private int privateDigest(final byte[] buf, final int offset, final int len) {
        this.buf[bufOff++] = (byte) 0x80;

        final int lenOfBitLen = (blockSize == 128) ? 16 : 8;
        final int C = blockSize - lenOfBitLen;
        if (bufOff > C) {
            while (bufOff < blockSize) {
                this.buf[bufOff++] = (byte) 0x00;
            }

            coreUpdate(this.buf, 0);
            bufOff = 0;
        }

        while (bufOff < C) {
            this.buf[bufOff++] = (byte) 0x00;
        }

        final long bitCount = byteCount * 8;
        if (blockSize == 128) {
            for (int i = 0; i < 8; i++) {
                this.buf[bufOff++] = 0x00;
            }
        }

        // 64-bit length is appended in little endian order
        for (int i = 0; i < 64; i += 8) {
            this.buf[bufOff++] = (byte) (bitCount >>> (i));
        }

        coreUpdate(this.buf, 0);
        coreDigest(buf, offset);

        engineReset();
        return hashSize;
    }

    @Override
    protected void engineReset() {
        bufOff = 0;
        byteCount = 0;
        coreReset();
    }

    /**
     * Return the hash bytes in <code>buf</code>, starting at offset
     * <code>off</code>.
     *
     * The subclass is expected to write exactly <code>hashSize</code> bytes in
     * the given buffer. The buffer is guaranteed to be large enough.
     */
    protected void coreDigest(final byte[] buf, final int off) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                buf[off + (i * 4 + j)] = (byte) (context[i] >>> (8 * j));
            }
        }
    }

    /**
     * Reset the hash internal structures to initial state.
     */
    protected void coreReset() {
        // initial values of MD4 i.e. A, B, C, D
        // as per rfc-1320; they are low-order byte first
        context[0] = 0x67452301;
        context[1] = 0xEFCDAB89;
        context[2] = 0x98BADCFE;
        context[3] = 0x10325476;
    }

    /**
     * Update the internal state with a single block.
     *
     * <code>buf</code> contains a single block (64 bytes, 512 bits) of data,
     * starting at offset <code>off</code>.
     */
    protected void coreUpdate(final byte[] block, int offset) {
        // encodes 64 bytes from input block into an
        // array of 16 32-bit entities.
        for (int i = 0; i < 16; i++) {
            X[i] = (block[offset++] & 0xFF)
                | (block[offset++] & 0xFF) << 8
                | (block[offset++] & 0xFF) << 16
                | (block[offset++] & 0xFF) << 24;
        }

        int A = context[0];
        int B = context[1];
        int C = context[2];
        int D = context[3];

        A = FF(A, B, C, D, X[0], 3);
        D = FF(D, A, B, C, X[1], 7);
        C = FF(C, D, A, B, X[2], 11);
        B = FF(B, C, D, A, X[3], 19);
        A = FF(A, B, C, D, X[4], 3);
        D = FF(D, A, B, C, X[5], 7);
        C = FF(C, D, A, B, X[6], 11);
        B = FF(B, C, D, A, X[7], 19);
        A = FF(A, B, C, D, X[8], 3);
        D = FF(D, A, B, C, X[9], 7);
        C = FF(C, D, A, B, X[10], 11);
        B = FF(B, C, D, A, X[11], 19);
        A = FF(A, B, C, D, X[12], 3);
        D = FF(D, A, B, C, X[13], 7);
        C = FF(C, D, A, B, X[14], 11);
        B = FF(B, C, D, A, X[15], 19);

        A = GG(A, B, C, D, X[0], 3);
        D = GG(D, A, B, C, X[4], 5);
        C = GG(C, D, A, B, X[8], 9);
        B = GG(B, C, D, A, X[12], 13);
        A = GG(A, B, C, D, X[1], 3);
        D = GG(D, A, B, C, X[5], 5);
        C = GG(C, D, A, B, X[9], 9);
        B = GG(B, C, D, A, X[13], 13);
        A = GG(A, B, C, D, X[2], 3);
        D = GG(D, A, B, C, X[6], 5);
        C = GG(C, D, A, B, X[10], 9);
        B = GG(B, C, D, A, X[14], 13);
        A = GG(A, B, C, D, X[3], 3);
        D = GG(D, A, B, C, X[7], 5);
        C = GG(C, D, A, B, X[11], 9);
        B = GG(B, C, D, A, X[15], 13);

        A = HH(A, B, C, D, X[0], 3);
        D = HH(D, A, B, C, X[8], 9);
        C = HH(C, D, A, B, X[4], 11);
        B = HH(B, C, D, A, X[12], 15);
        A = HH(A, B, C, D, X[2], 3);
        D = HH(D, A, B, C, X[10], 9);
        C = HH(C, D, A, B, X[6], 11);
        B = HH(B, C, D, A, X[14], 15);
        A = HH(A, B, C, D, X[1], 3);
        D = HH(D, A, B, C, X[9], 9);
        C = HH(C, D, A, B, X[5], 11);
        B = HH(B, C, D, A, X[13], 15);
        A = HH(A, B, C, D, X[3], 3);
        D = HH(D, A, B, C, X[11], 9);
        C = HH(C, D, A, B, X[7], 11);
        B = HH(B, C, D, A, X[15], 15);

        context[0] += A;
        context[1] += B;
        context[2] += C;
        context[3] += D;
    }

    // The basic MD4 atomic functions.
    // ..........................................................................

    private int FF(final int a, final int b, final int c, final int d, final int x, final int s) {
        final int t = a + ((b & c) | (~b & d)) + x;
        return t << s | t >>> (32 - s);
    }

    private int GG(final int a, final int b, final int c, final int d, final int x, final int s) {
        final int t = a + ((b & (c | d)) | (c & d)) + x + 0x5A827999;
        return t << s | t >>> (32 - s);
    }

    private int HH(final int a, final int b, final int c, final int d, final int x, final int s) {
        final int t = a + (b ^ c ^ d) + x + 0x6ED9EBA1;
        return t << s | t >>> (32 - s);
    }
}

class MD4Provider extends Provider {
    private static final long serialVersionUID = -8130712390950777255L;

    public MD4Provider() {
        super("MD4Provider", 1.0, "Java NTLM MD4 Security Provider"); //$NON-NLS-1$ //$NON-NLS-2$

        put("MessageDigest.MD4", "com.microsoft.tfs.jni.internal.ntlm.MD4"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
