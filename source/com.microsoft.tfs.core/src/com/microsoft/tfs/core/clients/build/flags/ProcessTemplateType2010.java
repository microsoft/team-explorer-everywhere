// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.build.flags;

import com.microsoft.tfs.core.internal.wrappers.EnumerationWrapper;

import ms.tfs.build.buildservice._03._ProcessTemplateType;

/**
 * Describes the type of a process template.
 *
 * @since TEE-SDK-10.1
 */
public class ProcessTemplateType2010 extends EnumerationWrapper {
    public static final ProcessTemplateType2010 CUSTOM = new ProcessTemplateType2010(_ProcessTemplateType.Custom);
    public static final ProcessTemplateType2010 DEFAULT = new ProcessTemplateType2010(_ProcessTemplateType.Default);
    public static final ProcessTemplateType2010 UPGRADE = new ProcessTemplateType2010(_ProcessTemplateType.Upgrade);

    private ProcessTemplateType2010(final _ProcessTemplateType type) {
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
    public static ProcessTemplateType2010 fromWebServiceObject(final _ProcessTemplateType webServiceObject) {
        if (webServiceObject == null) {
            return null;
        }
        return (ProcessTemplateType2010) EnumerationWrapper.fromWebServiceObject(webServiceObject);
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
