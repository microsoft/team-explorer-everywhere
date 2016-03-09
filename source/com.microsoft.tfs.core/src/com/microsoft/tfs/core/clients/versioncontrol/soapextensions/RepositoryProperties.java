// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import com.microsoft.tfs.core.clients.versioncontrol.SupportedFeatures;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.GUID;

import ms.tfs.versioncontrol.clientservices._03._RepositoryProperties;

/**
 * Describes the global properties of this repository.
 *
 * @since TEE-SDK-11.0
 */
public class RepositoryProperties extends WebServiceObjectWrapper {
    public RepositoryProperties(final _RepositoryProperties properties) {
        super(properties);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _RepositoryProperties getWebServiceObject() {
        return (_RepositoryProperties) webServiceObject;
    }

    /**
     * @return the download key for this repository
     */
    public byte[] getDownloadKey() {
        return getWebServiceObject().getDkey().clone();
    }

    /**
     * @return the unique identifer for this repository.
     */
    public GUID getID() {
        return new GUID(getWebServiceObject().getId());
    }

    public int getLatestChangesetID() {
        return getWebServiceObject().getLcset();
    }

    /**
     * @return the name describing this repository
     */
    public String getName() {
        return getWebServiceObject().getName();
    }

    /**
     * @return the features supported by the server
     */
    public SupportedFeatures getSupportedFeatures() {
        return new SupportedFeatures(getWebServiceObject().getFeatures());
    }

    /**
     * @return the version of the mid/data tier system. For the system to start
     *         and run normally both the mid and data tiers must have the same
     *         version information.
     */
    public String getVersion() {
        return getWebServiceObject().getVer();
    }
}
