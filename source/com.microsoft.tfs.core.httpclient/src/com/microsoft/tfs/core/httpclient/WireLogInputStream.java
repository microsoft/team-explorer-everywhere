/*
 * $Header:
 * /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons
 * //httpclient/src/java/org/apache/commons/httpclient/WireLogInputStream.java,v
 * 1.15 2004/06/24 21:39:52 mbecke Exp $ $Revision: 480424 $ $Date: 2006-11-29
 * 06:56:49 +0100 (Wed, 29 Nov 2006) $
 *
 * ====================================================================
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many individuals on
 * behalf of the Apache Software Foundation. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */

package com.microsoft.tfs.core.httpclient;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Logs all data read to the wire LOG.
 *
 * @author Ortwin Gl�ck
 * @author <a href="mailto:mbowler@GargoyleSoftware.com">Mike Bowler</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 * @since 2.0
 */
class WireLogInputStream extends FilterInputStream {

    /** Original input stream. */
    private final InputStream in;

    /** The wire log to use for writing. */
    private final Wire wire;

    /**
     * Create an instance that wraps the specified input stream.
     *
     * @param in
     *        The input stream.
     * @param wire
     *        The wire log to use.
     */
    public WireLogInputStream(final InputStream in, final Wire wire) {
        super(in);
        this.in = in;
        this.wire = wire;
    }

    /**
     *
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        final int l = in.read(b, off, len);
        if (l > 0) {
            wire.input(b, off, l);
        }
        return l;
    }

    /**
     *
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        final int l = in.read();
        if (l > 0) {
            wire.input(l);
        }
        return l;
    }

    /**
     *
     * @see java.io.InputStream#read(byte[])
     */
    @Override
    public int read(final byte[] b) throws IOException {
        final int l = in.read(b);
        if (l > 0) {
            wire.input(b, 0, l);
        }
        return l;
    }
}
