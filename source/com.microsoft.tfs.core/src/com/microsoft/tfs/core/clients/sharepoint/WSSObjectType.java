// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.sharepoint;

/**
 * Types of Sharepoint Objects
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public interface WSSObjectType {
    /**
     * A folder object, which can contain other folder objects and file objects.
     */
    public static final String FOLDER = "1"; //$NON-NLS-1$

    /**
     * A file object, which contains document data.
     */
    public static final String FILE = "0"; //$NON-NLS-1$
}
