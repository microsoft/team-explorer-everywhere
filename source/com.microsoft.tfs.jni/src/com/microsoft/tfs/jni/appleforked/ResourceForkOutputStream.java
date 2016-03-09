// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.appleforked;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

import com.microsoft.tfs.jni.FileSystemUtils;
import com.microsoft.tfs.jni.Messages;
import com.microsoft.tfs.util.Check;

public class ResourceForkOutputStream extends OutputStream {
    /* The xattr name of the resource fork */
    public final static String RESOURCEFORK_NAME = "com.apple.ResourceFork"; //$NON-NLS-1$

    /* The filename (data fork) we're reading */
    private final String filename;

    /* Our position in the Resource Fork */
    private long position = 0;

    public ResourceForkOutputStream(final String filename) throws FileNotFoundException {
        Check.notNull(filename, "filename"); //$NON-NLS-1$

        if (!new File(filename).exists()) {
            throw new FileNotFoundException(
                MessageFormat.format(
                    Messages.getString("ResourceForkOutputStream.DataForkForFileDoesNotExistFormat"), //$NON-NLS-1$
                    filename));
        }

        this.filename = filename;
    }

    @Override
    public void write(final int b) throws IOException {
        write(new byte[] {
            (byte) b
        }, 0, 1);
    }

    @Override
    public void write(final byte[] buffer, final int off, final int len) throws IOException {
        byte[] writebuf;

        if (off == 0) {
            writebuf = buffer;
        } else {
            writebuf = new byte[len];
            System.arraycopy(buffer, len, writebuf, 0, len);
        }

        if (!FileSystemUtils.getInstance().writeMacExtendedAttribute(
            filename,
            RESOURCEFORK_NAME,
            buffer,
            len,
            position)) {
            throw new IOException("Could not write resource fork due to an internal error"); //$NON-NLS-1$
        }

        position += len;
    }
}
