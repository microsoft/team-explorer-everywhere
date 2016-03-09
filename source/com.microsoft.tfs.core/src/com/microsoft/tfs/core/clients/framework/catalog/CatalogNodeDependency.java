// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.ws._CatalogNodeDependency;

/**
 * Wrapper class for the {@link _CatalogNodeDependency} proxy object returned as
 * part of the result of a TFS catalog web service request.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class CatalogNodeDependency extends WebServiceObjectWrapper {
    /**
     * Wrapper constructor.
     */
    public CatalogNodeDependency(final _CatalogNodeDependency catalogNodeDependency) {
        super(catalogNodeDependency);
    }

    /**
     * Returns the wrapped proxy object.
     */
    public _CatalogNodeDependency getWebServiceObject() {
        return (_CatalogNodeDependency) webServiceObject;
    }

    public String getFullPath() {
        return getWebServiceObject().getFullPath();
    }

    public String getAssociationKey() {
        return getWebServiceObject().getAssociationKey();
    }

    /**
     * Returns the full hierarchy path to the dependent node.
     */
    public String getRequiredNodeFullPath() {
        return getWebServiceObject().getRequiredNodeFullPath();
    }

    /**
     * Returns true if this dependency is a singleton.
     */
    public boolean isSingleton() {
        return getWebServiceObject().isIsSingleton();
    }
}
