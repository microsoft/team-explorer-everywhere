// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.commonstructure;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.tfs.services.classification._03._NodeInfo;

/**
 * <p>
 * A node in the graph of configured areas or iterations in a team project.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class NodeInfo extends WebServiceObjectWrapper {
    /**
     * Creates a {@link NodeInfo} from the given web service object.
     *
     * @param nodeInfo
     *        the web service object
     */
    public NodeInfo(final _NodeInfo nodeInfo) {
        super(nodeInfo);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _NodeInfo getWebServiceObject() {
        return (_NodeInfo) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getParentURI() {
        return getWebServiceObject().getParentUri();
    }

    public String getPath() {
        return getWebServiceObject().getPath();
    }

    public String getProjectURI() {
        return getWebServiceObject().getProjectUri();
    }

    public Property[] getProperties() {
        return (Property[]) WrapperUtils.wrap(Property.class, getWebServiceObject().getProperties());
    }

    public String getStructureType() {
        return getWebServiceObject().getStructureType();
    }

    public String getURI() {
        return getWebServiceObject().getUri();
    }
}
