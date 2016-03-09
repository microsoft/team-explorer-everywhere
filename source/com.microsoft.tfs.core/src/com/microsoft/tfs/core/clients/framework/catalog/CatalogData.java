// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.ws._CatalogData;

/**
 * Wrappper class for the {@link _CatalogData} proxy object. The
 * {@link CatalogData} class is returned by most catalog web service calls and
 * contains raw data returned by the web service. The contents of
 * {@link CatalogData} may need additional processing to complete a
 * {@link CatalogService} request.
 *
 * @since TEE-SDK-10.1
 */
public class CatalogData extends WebServiceObjectWrapper {
    private final CatalogNode[] catalogNodes;
    private final CatalogResource[] catalogResources;
    private final CatalogResourceType[] catalogResourceTypes;

    private final CatalogNode[] deletedNodes;
    private final CatalogResource[] deletedResources;
    private final CatalogResource[] deletedNodeResources;

    /**
     * Wrapper constructor.
     *
     * @param catalogData
     *        The proxy object to wrap.
     */
    public CatalogData(final _CatalogData catalogData) {
        super(catalogData);

        catalogNodes = (CatalogNode[]) WrapperUtils.wrap(CatalogNode.class, catalogData.getCatalogNodes());
        catalogResources =
            (CatalogResource[]) WrapperUtils.wrap(CatalogResource.class, catalogData.getCatalogResources());
        catalogResourceTypes =
            (CatalogResourceType[]) WrapperUtils.wrap(CatalogResourceType.class, catalogData.getCatalogResourceTypes());

        deletedNodes = (CatalogNode[]) WrapperUtils.wrap(CatalogNode.class, catalogData.getDeletedNodes());
        deletedResources =
            (CatalogResource[]) WrapperUtils.wrap(CatalogResource.class, catalogData.getDeletedResources());
        deletedNodeResources =
            (CatalogResource[]) WrapperUtils.wrap(CatalogResource.class, catalogData.getDeletedNodeResources());
    }

    /**
     * Returns the underlying proxy object.
     */
    public _CatalogData getWebServiceObject() {
        return (_CatalogData) webServiceObject;
    }

    /**
     * Returns the catalog nodes.
     */
    public CatalogNode[] getCatalogNodes() {
        return catalogNodes;
    }

    /**
     * Returns the catalog resources.
     */
    public CatalogResource[] getCatalogResources() {
        return catalogResources;
    }

    /**
     * Returns the catalog resource types.
     */
    public CatalogResourceType[] getCatalogResourceTypes() {
        return catalogResourceTypes;
    }

    /**
     * Returns the deleted nodes.
     */
    public CatalogNode[] getDeletedNodes() {
        return deletedNodes;
    }

    /**
     * Returns the deleted resources.
     */
    public CatalogResource[] getDeletedResources() {
        return deletedResources;
    }

    /**
     * Returns the deleted node resources.
     */
    public CatalogResource[] getDeletedNodeResources() {
        return deletedNodeResources;
    }

    public int getLocationServiceLastChangeID() {
        return getWebServiceObject().getLocationServiceLastChangeId();
    }

}
