// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes;

/**
 * A file attribute represents a single property of a filesystem item (file or
 * directory, but usually file) that can be tracked and/or updated by TEE and
 * stored as a string somewhere in TFS.
 */
public interface FileAttribute {
    /**
     * @return the file attribute as a string value.
     */
    @Override
    public abstract String toString();

    /**
     * Sets the name of this attribute.
     *
     * @param name
     *        the name of this attribute (must not be <code>null</code> or
     *        empty). Must not contain the pipe character ("|").
     */
    public void setName(String name);

    /**
     * @return the name of the attribute.
     */
    public String getName();
}
