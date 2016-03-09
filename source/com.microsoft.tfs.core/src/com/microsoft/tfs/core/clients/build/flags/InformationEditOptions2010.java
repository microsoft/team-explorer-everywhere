// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._03._InformationEditOptions;

public class InformationEditOptions2010 extends EnumerationWrapper {
    public static final InformationEditOptions2010 MERGE_FIELDS =
        new InformationEditOptions2010(_InformationEditOptions.MergeFields);
    public static final InformationEditOptions2010 REPLACE_FIELDS =
        new InformationEditOptions2010(_InformationEditOptions.ReplaceFields);

    private InformationEditOptions2010(final _InformationEditOptions type) {
        super(type);
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
    public static InformationEditOptions2010 fromWebServiceObject(final _InformationEditOptions webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (InformationEditOptions2010) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _InformationEditOptions getWebServiceObject() {
        return (_InformationEditOptions) webServiceObject;
    }
}
