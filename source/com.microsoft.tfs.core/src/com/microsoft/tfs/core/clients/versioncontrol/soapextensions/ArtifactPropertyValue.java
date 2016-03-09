// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.versioncontrol.clientservices._03._ArtifactPropertyValue;
import ms.tfs.versioncontrol.clientservices._03._PropertyValue;

/**
 * Represents a property-value pair that has a user-defined name (moniker). The
 * moniker can be optionally versioned by using a version number.
 *
 * @since TEE-SDK-10.1
 */
public class ArtifactPropertyValue extends WebServiceObjectWrapper {
    public ArtifactPropertyValue(final ArtifactSpec spec, final PropertyValue[] propertyValues) {
        this(
            new _ArtifactPropertyValue(
                spec.getWebServiceObject(),
                (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, propertyValues)));
    }

    public ArtifactPropertyValue(final _ArtifactPropertyValue webServiceObject) {
        super(webServiceObject);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ArtifactPropertyValue getWebServiceObject() {
        return (_ArtifactPropertyValue) webServiceObject;
    }

    public ArtifactSpec getSpec() {
        return new ArtifactSpec(getWebServiceObject().getSpec());
    }

    public PropertyValue[] getPropertyValues() {
        return (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObject().getPropertyValues());
    }
}
