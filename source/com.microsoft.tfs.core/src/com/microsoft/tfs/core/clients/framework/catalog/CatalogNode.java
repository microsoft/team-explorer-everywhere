// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;

import ms.ws._CatalogNode;

/**
 * Wrapper class for the {@link _CatalogNode} proxy object of the TFS catalog
 * web service. A {@link CatalogNode} represents a node in the catalogs resource
 * hierarchy.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class CatalogNode extends WebServiceObjectWrapper {
    private CatalogResource resource;
    private CatalogNode parentNode;
    private CatalogService catalogService;
    private final CatalogNodeDependency[] nodeDependencies;
    private CatalogDependencyGroup dependencyGroup;

    /**
     * Wrapper constructor.
     */
    public CatalogNode(final _CatalogNode catalogNode) {
        super(catalogNode);

        nodeDependencies =
            (CatalogNodeDependency[]) WrapperUtils.wrap(CatalogNodeDependency.class, catalogNode.getNodeDependencies());
    }

    /**
     * Returns the underlying web service proxy object.
     */
    public _CatalogNode getWebServiceObject() {
        return (_CatalogNode) webServiceObject;
    }

    /**
     * Returns the node's full path in the hierarchy.
     */
    public String getFullPath() {
        return getWebServiceObject().getFullPath();
    }

    /**
     * Returns the node's parent path in the hierarchy.
     */
    public String getParentPath() {
        return getWebServiceObject().getParentPath();
    }

    /**
     * Returns the node's resource identifier.
     */
    public String getResourceIdentifier() {
        return getWebServiceObject().getResourceIdentifier();
    }

    /**
     * Returns true if this node matched the query criteria.
     */
    public boolean isMatchedQuery() {
        return getWebServiceObject().isMatchedQuery();
    }

    /**
     * Returns true if dependents for this node were include in the query
     * result.
     */
    public boolean isNodeDependenciesIncluded() {
        return getWebServiceObject().isNodeDependenciesIncluded();
    }

    /**
     * Returns the resource associated with this node.
     */
    public CatalogResource getResource() {
        return resource;
    }

    /**
     * Associates the specified resource with this node. This is used internally
     * during post-processing of the web service result.
     */
    void setCatalogResource(final CatalogResource value) {
        resource = value;
    }

    /**
     * Return the parent node of this node.
     */
    public CatalogNode getParentNode() {
        return parentNode;
    }

    /**
     * Returns the leaf segment of the child's full hierarchy path.
     */
    public String getChildItem() {
        return getWebServiceObject().getChildItem();
    }

    /**
     * Sets the specified node as the parent node of this node. This is used
     * internally during post-processing of the web service result.
     */
    void setParentNode(final CatalogNode value) {
        parentNode = value;
    }

    /**
     * Returns the catalog service which was used to retrieve this node.
     */
    public CatalogService getCatalogService() {
        return catalogService;
    }

    /**
     * Returns an array of nodes which are dependent on this node.
     */
    public CatalogNodeDependency[] getNodeDependencies() {
        return nodeDependencies;
    }

    /**
     * Returns the dependency groups for this node.
     */
    public CatalogDependencyGroup getDependencyGroup() {
        return dependencyGroup;
    }

    /**
     * Called during post-processing of the web service result. This method
     * initializes members that are not part of the underlying proxy object.
     */
    public void initializeFromWebService(final CatalogService service) {
        catalogService = service;

        final String fullPath = getWebServiceObject().getFullPath();
        getWebServiceObject().setParentPath(
            fullPath.substring(0, fullPath.length() - CatalogConstants.MANDATORY_NODE_PATH_LENGTH));
        getWebServiceObject().setChildItem(
            fullPath.substring(fullPath.length() - CatalogConstants.MANDATORY_NODE_PATH_LENGTH));

        if (getWebServiceObject().isNodeDependenciesIncluded()) {
            dependencyGroup = new CatalogDependencyGroup();
        }
    }
}
