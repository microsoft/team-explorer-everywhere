// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._04._InformationEditOptions;

public class InformationEditOptions extends EnumerationWrapper {
    public static final InformationEditOptions MERGE_FIELDS =
        new InformationEditOptions(_InformationEditOptions.MergeFields);
    public static final InformationEditOptions REPLACE_FIELDS =
        new InformationEditOptions(_InformationEditOptions.ReplaceFields);

    private InformationEditOptions(final _InformationEditOptions type) {
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
    public static InformationEditOptions fromWebServiceObject(final _InformationEditOptions webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (InformationEditOptions) EnumerationWrapper.fromWebServiceObject(webServiceObject);
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
