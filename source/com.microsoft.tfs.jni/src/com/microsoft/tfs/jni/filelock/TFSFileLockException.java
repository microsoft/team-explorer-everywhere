// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni.filelock;

/**
 * An exception representing a fatal error encountered while locking filesystem
 * resources.
 *
 * @threadsafety unknown
 */
public class TFSFileLockException extends RuntimeException {
    private static final long serialVersionUID = 3386908303262104262L;

    public TFSFileLockException(final String message) {
        super(message);
    }

    public TFSFileLockException(final String message, final Exception e) {
        super(message, e);
    }
}
