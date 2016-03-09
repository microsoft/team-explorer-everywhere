// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;

import ms.ws._CatalogResourceType;

/**
 * Wrapper class for the {@link _CatalogResourceType} which is returned as part
 * of a catalog web service request result.
 *
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class CatalogResourceType extends WebServiceObjectWrapper {
    /**
     * Wrapper constructor.
     */
    public CatalogResourceType(final _CatalogResourceType catalogResourceType) {
        super(catalogResourceType);
    }

    /**
     * Returns the wrapped proxy object.
     */
    public _CatalogResourceType getWebServiceObject() {
        return (_CatalogResourceType) webServiceObject;
    }

    /**
     * Returns the identifier for this resource type.
     */
    public String getIdentifier() {
        return getWebServiceObject().getIdentifier();
    }

    /**
     * Returns the display name for this resource type.
     */
    public String getDisplayName() {
        return getWebServiceObject().getDisplayName();
    }

    /**
     * Returns the description for this resource type.
     */
    public String getDescription() {
        return getWebServiceObject().getDescription();
    }
}
