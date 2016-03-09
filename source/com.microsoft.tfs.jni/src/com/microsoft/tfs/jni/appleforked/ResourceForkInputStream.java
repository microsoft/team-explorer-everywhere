// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.Messages;
import com.microsoft.tfs.util.Check;

public class ResourceForkInputStream extends InputStream {
    /* The xattr name of the resource fork */
    public final static String RESOURCEFORK_NAME = "com.apple.ResourceFork"; //$NON-NLS-1$

    /* The filename (data fork) we're reading */
    private final String filename;

    /* Our position in the Resource Fork */
    private long position = 0;

    public ResourceForkInputStream(final String filename) throws FileNotFoundException {
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        if (!new File(filename).exists()) {
            throw new FileNotFoundException(
                MessageFormat.format(
                    Messages.getString("ResourceForkInputStream.DataForkForFileDoesNotExistFormat"), //$NON-NLS-1$
                    filename));
        }

        this.filename = filename;
    }

    @Override
    public int read() throws IOException {
        final byte[] readBytes = new byte[1];

        if (read(readBytes, 0, 1) < 0) {
            return -1;
        }

        return (readBytes[0] & 0xFF);
    }

    @Override
    public int read(final byte[] buffer, final int off, final int len) throws IOException {
        final byte[] readbuf = (off == 0) ? buffer : new byte[len];

        final int readlen =
            FileSystemUtils.getInstance().readMacExtendedAttribute(filename, RESOURCEFORK_NAME, readbuf, len, position);

        if (readlen == -1) {
            return -1;
        } else if (readlen == -2) {
            throw new IOException("Could not read resource fork due to internal error"); //$NON-NLS-1$
        }

        /* If offset was non-zero, we wrote to a temporary buffer. Fix that. */
        if (off != 0) {
            System.arraycopy(readbuf, 0, buffer, off, len);
        }

        position += readlen;

        return readlen;
    }
}
