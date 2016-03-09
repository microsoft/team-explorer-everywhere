// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.services.registration._03._OutboundLinkType;
import ms.tfs.services.registration._03._RegistrationArtifactType;

/**
 * Associates a name with one or more {@link OutboundLinkType}s to describe a
 * registered artifact.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public class ArtifactType extends WebServiceObjectWrapper {
    public ArtifactType(final String name, final OutboundLinkType[] linkTypes) {
        super(
            new _RegistrationArtifactType(
                name,
                (_OutboundLinkType[]) WrapperUtils.unwrap(_OutboundLinkType.class, linkTypes)));
    }

    public ArtifactType(final _RegistrationArtifactType artifactType) {
        super(artifactType);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _RegistrationArtifactType getWebServiceObject() {
        return (_RegistrationArtifactType) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public OutboundLinkType[] getOutboundLinkTypes() {
        return (OutboundLinkType[]) WrapperUtils.wrap(
            OutboundLinkType.class,
            getWebServiceObject().getOutboundLinkTypes());
    }

}
