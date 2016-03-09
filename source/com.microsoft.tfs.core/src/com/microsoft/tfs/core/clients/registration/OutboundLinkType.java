// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.services.registration._03._OutboundLinkType;

/**
 * An {@link OutboundLinkType} binds a name to an artifact and the tool that can
 * consume that artifact.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public class OutboundLinkType extends WebServiceObjectWrapper {
    public OutboundLinkType(final String name, final String targetArtifactTool, final String targetArtifactTypeName) {
        super(new _OutboundLinkType(name, targetArtifactTool, targetArtifactTypeName));
    }

    public OutboundLinkType(final _OutboundLinkType linkType) {
        super(linkType);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _OutboundLinkType getWebServiceObject() {
        return (_OutboundLinkType) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getTargetArtifactTypeTool() {
        return getWebServiceObject().getTargetArtifactTypeTool();
    }

    public String getTargetArtifactTypeName() {
        return getWebServiceObject().getTargetArtifactTypeName();
    }

}
