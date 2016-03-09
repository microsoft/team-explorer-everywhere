// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.versioncontrol.clientservices._03._LabelChildOption;

/**
 * Enumerates the ways labels are applied to child items during label creation
 * or update.
 *
 * @since TEE-SDK-10.1
 */
public class LabelChildOption extends EnumerationWrapper {
    public static final LabelChildOption FAIL = new LabelChildOption(_LabelChildOption.Fail);
    public static final LabelChildOption REPLACE = new LabelChildOption(_LabelChildOption.Replace);
    public static final LabelChildOption MERGE = new LabelChildOption(_LabelChildOption.Merge);

    private LabelChildOption(final _LabelChildOption option) {
        super(option);
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
    public static LabelChildOption fromWebServiceObject(final _LabelChildOption webServiceObject) {
        return (LabelChildOption) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _LabelChildOption getWebServiceObject() {
        return (_LabelChildOption) webServiceObject;
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
