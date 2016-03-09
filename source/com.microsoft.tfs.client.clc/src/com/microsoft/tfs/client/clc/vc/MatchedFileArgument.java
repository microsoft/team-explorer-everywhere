// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc;

/**
 *         Represents a source code control item that a command line free
 *         argument refers to. Is a simple struct-like class, because it's only
 *         used within the CLC for command-line parsing.
 */
public final class MatchedFileArgument {
    /**
     * True if the file argument describes a server path, false if it describes
     * a local path.
     */
    public boolean isServerItem;

    /**
     * The exact text of the free argument that was used to construct this spec.
     */
    public String exactString;

    /**
     * The full path (server or local) to the item, after expanding any relative
     * path components.
     */
    public String fullPath;

    /**
     * The full path of the item if the item is a directory, otherwise the
     * parent directory of the item if it is a file.
     */
    public String folderPart;

    /**
     * The file part of the argument if the argument describes a file, otherwise
     * null.
     */
    public String filePart;

    /**
     * True if the item matches a pending change by path, false if it does not
     * match any.
     */
    public boolean isMatched;

    public MatchedFileArgument() {
    }
}
