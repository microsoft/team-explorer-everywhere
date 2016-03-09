// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.versioncontrol.clientservices._03._Resolution;

/**
 * Enumerates the types of conflict resolutions that can happen.
 *
 * @since TEE-SDK-10.1
 */
public class Resolution extends EnumerationWrapper {
    public static final Resolution NONE = new Resolution(_Resolution.None);
    public static final Resolution ACCEPT_MERGE = new Resolution(_Resolution.AcceptMerge);
    public static final Resolution ACCEPT_YOURS = new Resolution(_Resolution.AcceptYours);
    public static final Resolution ACCEPT_THEIRS = new Resolution(_Resolution.AcceptTheirs);
    public static final Resolution DELETE_CONFLICT = new Resolution(_Resolution.DeleteConflict);
    public static final Resolution ACCEPT_YOURS_RENAME_THEIRS = new Resolution(_Resolution.AcceptYoursRenameTheirs);
    public static final Resolution OVERWRITE_LOCAL = new Resolution(_Resolution.OverwriteLocal);

    private Resolution(final _Resolution resolution) {
        super(resolution);
    }

    /**
     * Gets the correct wrapper type for the given web service object.
     *
     * @param webServiceObject
     *        the web service object (must not be <code>null</code>)
     * @return the correct wrapper type for the given web service object
     * @throws RuntimeException
     *         if no wrapper type is known for the given web service object
     */
    public static Resolution fromWebServiceObject(final _Resolution webServiceObject) {
        return (Resolution) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Resolution getWebServiceObject() {
        return (_Resolution) webServiceObject;
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
