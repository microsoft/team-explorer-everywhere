// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._04._ProcessTemplateType;

/**
 * Describes the type of a process template.
 *
 * @since TEE-SDK-10.1
 */
public class ProcessTemplateType extends EnumerationWrapper {
    public static final ProcessTemplateType CUSTOM = new ProcessTemplateType(_ProcessTemplateType.Custom);
    public static final ProcessTemplateType DEFAULT = new ProcessTemplateType(_ProcessTemplateType.Default);
    public static final ProcessTemplateType UPGRADE = new ProcessTemplateType(_ProcessTemplateType.Upgrade);

    private ProcessTemplateType(final _ProcessTemplateType type) {
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
    public static ProcessTemplateType fromWebServiceObject(final _ProcessTemplateType webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (ProcessTemplateType) EnumerationWrapper.fromWebServiceObject(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ProcessTemplateType getWebServiceObject() {
        return (_ProcessTemplateType) webServiceObject;
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
