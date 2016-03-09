// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.fileattributes;

import com.microsoft.tfs.util.Check;

/**
 * Base class implementation of a simple file attribute with only a short name
 * and no value. Not to be used directly; see {@link BooleanFileAttribute} and
 * {@link StringPairFileAttribute}.
 *
 * This class thread-safe.
 */
public abstract class FileAttributeImpl implements FileAttribute {
    private String name;

    /**
     * Creates a file attribute of the given name.
     *
     * @param name
     *        the name of this attribute (must not be <code>null</code> or
     *        empty). Must not contain the pipe character ("|").
     */
    public FileAttributeImpl(final String name) {
        Check.notNullOrEmpty(name, "name"); //$NON-NLS-1$
        this.name = name;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.core.vc.fileattributes.FileAttribute#setName(java.lang.
     * String)
     */
    @Override
    public void setName(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$
        this.name = name;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.core.vc.fileattributes.FileAttribute#getName()
     */
    @Override
    public String getName() {
        return name;
    }
}
