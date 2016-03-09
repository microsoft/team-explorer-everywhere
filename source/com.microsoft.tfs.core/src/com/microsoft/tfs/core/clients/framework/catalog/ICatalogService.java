// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import com.microsoft.tfs.core.clients.framework.location.ILocationService;
import com.microsoft.tfs.util.GUID;

/**
 * The service responsible for providing a access to information about available
 * Team Foundation Server resources.
 *
 * @since TEE-SDK-10.1
 */
public interface ICatalogService {
    /**
     * Returns the resource types for all of the specified identifiers. If null
     * or an empty list is passed in, all resource types will be returned.
     *
     * @param resourceTypeIdentifiers
     *        Identifiers for the resource types that should be returned. If
     *        this has a value of null or empty, all resource types will be
     *        returned.
     *
     * @return Resource types for the requested identifiers.
     */
    public CatalogResourceType[] queryResourceTypes(GUID[] resourceTypeIdentifiers);

    /**
     * Returns the resource that is associated with the identifier.
     *
     * @param resourceIdentifiers
     *        The identifiers for the resources that are being search for.
     *
     * @param queryOptions
     *        If ExpandDependencies is specified, the Dependencies property on
     *        nodes will contain the nodes they are dependent on. If
     *        IncludeParents is specified, the ParentNode property on the
     *        CatalogNode will contain the parent node. Leaving a given option
     *        will result in the returned catalog nodes to have null for that
     *        value. Extra data should only be retrieved if it is needed since
     *        computing and sending information can be expensive.
     *
     * @return The resources with the specified identifiers.
     */
    public CatalogResource[] queryResources(GUID[] resourceIdentifiers, CatalogQueryOptions queryOptions);

    /**
     * Returns all of the catalog resources of the provided type. If
     * {@link GUID#EMPTY} is passed in, all resources are returned.
     *
     * @param resourceTypeIdentifiers
     *        The identifier for the type of resource to filter on.
     *
     * @param queryOptions
     *        If ExpandDependencies is specified, the Dependencies property on
     *        nodes will contain the nodes they are dependent on. If
     *        IncludeParents is specified, the ParentNode property on the
     *        CatalogNode will contain the parent node. Leaving a given option
     *        will result in the returned catalog nodes to have null for that
     *        value. Extra data should only be retrieved if it is needed since
     *        computing and sending information can be expensive.
     *
     * @return the catalog resources of the specified type.
     */
    public CatalogResource[] queryResourcesByType(GUID[] resourceTypeIdentifiers, CatalogQueryOptions queryOptions);

    /**
     * Returns all of the catalog resources of the provided type. If
     * {@link GUID#EMPTY} is passed in, all resources are returned.
     *
     * @param resourceTypeIdentifiers
     *        The identifier for the type of resource to filter on.
     *
     * @param propertyFilters
     *        The set of property filters to apply to the resource found.
     *        Matches will be based on both the key and the value of the
     *        property matching. If the value of a certain filter is null or
     *        empty then it will be assumed that all resource with the supplied
     *        property should be returned. A match consists of a resource that
     *        matches all of the propertyFilters.
     *
     * @param queryOptions
     *        If ExpandDependencies is specified, the Dependencies property on
     *        nodes will contain the nodes they are dependent on. If
     *        IncludeParents is specified, the ParentNode property on the
     *        CatalogNode will contain the parent node. Leaving a given option
     *        will result in the returned catalog nodes to have null for that
     *        value. Extra data should only be retrieved if it is needed since
     *        computing and sending information can be expensive.
     *
     * @return the catalog resources which matched the provided type
     */
    public CatalogResource[] queryResources(
        GUID[] resourceTypeIdentifiers,
        CatalogResourceProperty[] propertyFilters,
        CatalogQueryOptions queryOptions);

    /**
     * Queries "up" the tree from the provided path looking for the provided
     * types in its parent nodes' children. If the path is
     * "TFSInstance1/PG1/TPC1/TP1" this query will be translated into a series
     * of QueryNodes calls that have the following pathSpecs:
     *
     * "TFSInstance1/*" "TFSInstance1/PG1/*" "TFSInstance1/PG1/TPC1/*"
     *
     * An example of when this could be used is when a ReportingSite is being
     * added to a node and it must find the ReportServer that is a child of one
     * of its parent nodes.
     *
     * @param path
     *        The path of the item from where the "up" query should originate.
     *        Wildcards cannot be used in this path.
     *
     * @param resourceTypeFilters
     *        The list of types that this query should include. If this is null
     *        or empty, all types will be included.
     *
     * @param queryOptions
     *        If ExpandDependencies is specified, the Dependencies property on
     *        nodes will contain the nodes they are dependent on. If
     *        IncludeParents is specified, the ParentNode property on the
     *        CatalogNode will contain the parent node. Leaving a given option
     *        will result in the returned catalog nodes to have null for that
     *        value. Extra data should only be retrieved if it is needed since
     *        computing and sending information can be expensive.
     *
     * @return The catalog nodes that match the specified query.
     */
    public CatalogNode[] queryUpTree(String path, GUID[] resourceTypeFilters, CatalogQueryOptions queryOptions);

    /**
     * Returns the nodes for the resource provided as well as the parents. The
     * direct nodes and the parent nodes will not be returned if they are
     * filtered out. For the following tree:
     *
     * PG1 / \ PG2 PG3 / \ \ TPC1 TPC2 TPC3 / \ TP1 TP2
     *
     * Query for TP1's identifier with no filters and recursing to the root
     * would yield TP1, TPC1, PG2 and PG1.
     *
     * @param resourceIdentifier
     *        The identifier for the resource who's parents are being queried.
     *        The resource and its nodes will only be returned if they are not
     *        filtered out.
     *
     * @param pathFilters
     *        Nodes will only be returned if they live under one of the paths
     *        provided here. If this value is null or empty it will be assumed
     *        that parents from all places within the tree are valid.
     *
     * @param resourceTypeFilters
     *        The list of types that this query should include. If this is null
     *        or empty, all types will be included.
     *
     * @param recurseToRoot
     *        If this is true then parent nodes will be enumerated all the way
     *        to the root. If this is false then only the first level of parents
     *        will be returned.
     *
     * @param queryOptions
     *        If ExpandDependencies is specified, the Dependencies property on
     *        nodes will contain the nodes they are dependent on. If
     *        IncludeParents is specified, the ParentNode property on the
     *        CatalogNode will contain the parent node. Leaving a given option
     *        will result in the returned catalog nodes to have null for that
     *        value. Extra data should only be retrieved if it is needed since
     *        computing and sending information can be expensive.
     *
     * @return The nodes for the resource provided as well as the parents of
     *         those nodes that apply to the provided filters.
     */
    public CatalogNode[] queryParents(
        GUID resourceIdentifier,
        String[] pathFilters,
        GUID[] resourceTypeFilters,
        boolean recurseToRoot,
        CatalogQueryOptions queryOptions);

    /**
     * Returns the catalog nodes that exist below the parentPath and have a type
     * that is listed in resourceTypeFilters.
     *
     * @param pathSpecs
     *        The paths of the element or elements that are being searched for.
     *        This path can contain the wildcards "*", "**" and "..." where "*"
     *        means one-level and "**" and "..." means any number of levels.
     *
     * @param resourceTypeFilters
     *        The list of types that this query should include. If this is null
     *        or empty, all types will be included.
     *
     * @param queryOptions
     *        If ExpandDependencies is specified, the Dependencies property on
     *        nodes will contain the nodes they are dependent on. If
     *        IncludeParents is specified, the ParentNode property on the
     *        CatalogNode will contain the parent node. Leaving a given option
     *        will result in the returned catalog nodes to have null for that
     *        value. Extra data should only be retrieved if it is needed since
     *        computing and sending information can be expensive.
     *
     * @return The catalog nodes that exist below the parentPath and have a type
     *         that is listed in resourceTypeFilters.
     */
    public CatalogNode[] queryNodes(String[] pathSpecs, GUID[] resourceTypeFilters, CatalogQueryOptions queryOptions);

    /**
     * Returns the catalog nodes that exist below the parentPath and have a type
     * that is listed in resourceTypeFilters.
     *
     * @param pathSpecs
     *        The paths of the element or elements that are being searched for.
     *        This path can contain the wildcards "*", "**" and "..." where "*"
     *        means one-level and "**" and "..." means any number of levels.
     *
     * @param resourceTypeFilters
     *        The list of types that this query should include. If this is null
     *        or empty, all types will be included.
     *
     * @param propertyFilters
     *        The set of property filters to apply to the nodes found. Matches
     *        will be based on both the key and the value of the property
     *        matching. If the value of a certain filter is null or empty then
     *        it will be assumed that all nodes with the supplied property
     *        should be returned. A match consists of a node/resource that
     *        matches all of the propertyFilters.
     *
     * @param queryOptions
     *        If ExpandDependencies is specified, the Dependencies property on
     *        nodes will contain the nodes they are dependent on. If
     *        IncludeParents is specified, the ParentNode property on the
     *        CatalogNode will contain the parent node. Leaving a given option
     *        will result in the returned catalog nodes to have null for that
     *        value. Extra data should only be retrieved if it is needed since
     *        computing and sending information can be expensive.
     *
     * @return The catalog nodes that exist below the parentPath and have a type
     *         that is listed in resourceTypeFilters.
     */
    public CatalogNode[] queryNodes(
        String[] pathSpecs,
        GUID[] resourceTypeFilters,
        CatalogResourceProperty[] propertyFilters,
        CatalogQueryOptions queryOptions);

    /**
     * @return the root nodes in the tree.
     */
    public CatalogNode[] getRootNodes();

    /**
     * Returns the specified root node. Well-known root paths can be found in
     * Microsoft.TeamFoundation.Framework.Common.Catalog.CatalogTree.
     *
     * @param tree
     *        The tree of the desired root.
     *
     * @return The CatalogNode for the tree.
     */
    public CatalogNode queryRootNode(CatalogTree tree);

    /**
     * Saves the updated resource in the catalog. Note that service definitions
     * that exist as service references will be created if they are new and
     * updated if they are not.
     *
     * @param resource
     *        The resource to update.
     */
    public void saveResource(CatalogResource resource);

    /**
     * Saves the updated node and its resource in the catalog.
     *
     * @param node
     *        The node that has been created or updated.
     */
    public void saveNode(CatalogNode node);

    /**
     * Deletes this node from the catalog. If this node is the only node that
     * points to the resource it points to then this resource will also be
     * deleted. If this node exists in the infrastructure tree then the resource
     * that is associated with this node will also be deleted and it will be
     * inherently recursive.
     *
     * @param node
     *        The node to delete.
     *
     * @param recurse
     *        True if the children nodes of this node should be deleted.
     */
    public void saveDelete(CatalogNode node, Boolean recurse);

    /**
     * Adds this move to the change context. It will be sent to the server when
     * Save() is called. Note that if nodeToMove or newParent also have updated
     * properties then those will be committed as well. Any node that is
     * explicitly moved will have IsDefault set to 0.
     *
     * @param nodeToMove
     *        The node to move under the newParent.
     *
     * @param newParent
     *        The newParent to place nodeToMove under.
     */
    public void saveMove(CatalogNode nodeToMove, CatalogNode newParent);

    /**
     * Creates a change context in which many changes can be batched
     *
     * @return A change context in which many changes can be batched together.
     */
    public CatalogChangeContext createChangeContext();

    /**
     * @return the location service that this catalog uses for its service
     *         definition references.
     */
    public ILocationService getLocationService();
}
