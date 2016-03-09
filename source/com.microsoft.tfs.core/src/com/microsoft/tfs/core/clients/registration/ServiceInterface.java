// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.tfs.services.registration._03._RegistrationServiceInterface;

/**
 * Describes the name and location of a registered Team Foundation Server
 * service.
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class ServiceInterface extends WebServiceObjectWrapper {
    public ServiceInterface(final String name, final String url) {
        super(new _RegistrationServiceInterface(name, url));
    }

    public ServiceInterface(final _RegistrationServiceInterface serviceInterface) {
        super(serviceInterface);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _RegistrationServiceInterface getWebServiceObject() {
        return (_RegistrationServiceInterface) webServiceObject;
    }

    public String getName() {
        return getWebServiceObject().getName();
    }

    public String getURL() {
        return getWebServiceObject().getUrl();
    }

    /**
     * Return the URL without and leading slash. This is often required in
     * TFS2010 where the services are located inside the server url (i.e.
     * http://tfsserver:8080/tfs/defaultCollection)
     */
    public String getRelativeURL() {
        String url = getURL();
        if (url == null || url.length() == 0) {
            return url;
        }

        if (url.startsWith("/")) //$NON-NLS-1$
        {
            url = url.substring(1);
        }
        return url;
    }

    public void setName(final String value) {
        getWebServiceObject().setName(value);
    }

    public void setURL(final String value) {
        getWebServiceObject().setUrl(value);
    }

}
