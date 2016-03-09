// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.versioncontrol.clientservices._03._LocalItemExclusionSet;
import ms.tfs.versioncontrol.clientservices._03._ServerSettings;

/**
 * Wrapper for the ServerSettings proxy object.
 *
 * @since TEE-SDK-11.0
 */
public final class ServerSettings extends WebServiceObjectWrapper {
    public ServerSettings(final _ServerSettings settings) {
        super(settings);
    }

    public ServerSettings(final WorkspaceLocation location) {
        super(new _ServerSettings((byte) location.getValue(), new _LocalItemExclusionSet(), false, 0, null));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _ServerSettings getWebServiceObject() {
        return (_ServerSettings) webServiceObject;
    }

    public WorkspaceLocation getDefaultWorkspaceLocation() {
        return WorkspaceLocation.fromInteger(getWebServiceObject().getDefaultWorkspaceLocation());
    }

    /**
     * @return the default local item exclusion set
     */
    public LocalItemExclusionSet getDefaultLocalItemExclusionSet() {
        return new LocalItemExclusionSet(getWebServiceObject().getDefaultLocalItemExclusionSet());
    }

    public boolean isAllowAsynchronousCheckoutInServerWorkspaces() {
        return getWebServiceObject().isAllowAsynchronousCheckoutInServerWorkspaces();
    }

    public int getMaxAllowedServerPathLength() {
        final int pathLength = getWebServiceObject().getMaxAllowedServerPathLength();
        if (pathLength > 0) {
            return pathLength;
        } else {
            // On old TFS servers the maximum path length was not specified and
            // hard coded as 259
            return VersionControlConstants.MAX_SERVER_PATH_SIZE_OLD;
        }
    }

    public String getStableHashString() {
        return getWebServiceObject().getStableHashString();
    }

}
