// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes;

/**
 * Contains defined file attribute names for any kind of FileAttribute.
 */
public abstract class FileAttributeNames {
    /**
     * A boolean file attribute that, when present, means a file should be
     * marked executable on Unix platforms.
     */
    public final static String EXECUTABLE = "x"; //$NON-NLS-1$

    /**
     * A symbolic link whose value is a relative or absolute server path where
     * the link should point when created on disk.
     */
    public final static String LINK = "link"; //$NON-NLS-1$

    /**
     * A symbolic link whose value is the local target file or directory, a
     * literal local path string (possibly non-portable). The string is not
     * tested against working folder mappings. It is simply given to the
     * operating system to create the link for a file.
     */
    public final static String LOCAL_LINK = "local-link"; //$NON-NLS-1$

    /**
     * A string pair attribute that denotes the kind of end-of-line convention
     * to convert to when writing files into working folders during a "get"
     * operation.
     */
    public final static String CLIENT_EOL = "client-eol"; //$NON-NLS-1$

    /**
     * A string pair attribute that denotes the kind of end-of-line convention
     * to convert to when uploading files to TFS during a check-in operation.
     */
    public final static String SERVER_EOL = "server-eol"; //$NON-NLS-1$

    /**
     * A string pair attribute that allows transformations between a native
     * format on the client and a suitable TFS representation (for example,
     * AppleSingle encoding of Mac data/resource forks.)
     */
    public final static String TRANSFORM = "transform"; //$NON-NLS-1$
}
