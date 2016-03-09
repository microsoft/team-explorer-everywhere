// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._VersionControlLink;

/**
 * Represents a link between a changeset or shelveset and a work item to be
 * updated.
 *
 * @since TEE-SDK-10.1
 */
public final class VersionControlLink extends WebServiceObjectWrapper {

    public VersionControlLink() {
        super(new _VersionControlLink());
    }

    public VersionControlLink(final _VersionControlLink link) {
        super(link);
    }

    public VersionControlLink(final VersionControlLinkType linkType, final String URL) {
        super(new _VersionControlLink(linkType.getValue(), URL));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _VersionControlLink getWebServiceObject() {
        return (_VersionControlLink) webServiceObject;
    }

    /**
     * Gets the link type.
     *
     * @return the link type, never null.
     */
    public VersionControlLinkType getLinkType() {
        if (getWebServiceObject().getType() == VersionControlLinkType.RESOLVE.getValue()) {
            return VersionControlLinkType.RESOLVE;
        }

        if (getWebServiceObject().getType() == VersionControlLinkType.ASSOCIATE.getValue()) {
            return VersionControlLinkType.ASSOCIATE;
        }

        return VersionControlLinkType.INVALID;
    }

    /**
     * Gets the URL.
     *
     * @return the URL (may be null).
     */
    public String getURL() {
        return getWebServiceObject().getUrl();
    }
}
