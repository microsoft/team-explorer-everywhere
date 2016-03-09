// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import java.util.ArrayList;
import java.util.HashMap;

import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.clients.framework.catalog.exceptions.CatalogMethodNotImplementedException;
import com.microsoft.tfs.core.clients.framework.catalog.exceptions.CatalogNodeDoesNotExistException;
import com.microsoft.tfs.core.clients.framework.catalog.exceptions.CatalogResourceTypeDoesNotExistException;
import com.microsoft.tfs.core.clients.framework.location.ILocationService;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

/**
 * The TFS catalog service. Implements the {@link ICatalogService} interface.
 *
 * @since TEE-SDK-10.1
 */
public class CatalogService implements ICatalogService {
    private final TFSConfigurationServer connection;
    private final ILocationService locationService;
    private final CatalogWebServiceProxy catalogProxy;

    /**
     * True when the resource type cache has been populated.
     */
    private boolean resourceTypesLoaded = false;

    /**
     * A synchronization object for the resource types cache.
     */
    private final Object mapResourceTypesLock = new Object();

    /**
     * Resource types are cached on first access. The map key is the resource
     * GUID.
     */
    private final HashMap<GUID, CatalogResourceType> mapResourceTypes = new HashMap<GUID, CatalogResourceType>();

    /**
     * Construct the TFS Catalog Service.
     *
     * @param connection
     *        a {@link TFSConfigurationServer}
     */
    public CatalogService(final TFSConfigurationServer connection) {
        this.connection = connection;
        locationService = (ILocationService) connection.getClient(ILocationService.class);
        catalogProxy = new CatalogWebServiceProxy(connection);
    }

    /**
     * Returns the location service that this catalog uses for its service
     * definition references.
     *
     * @return The TFS location service.
     */
    @Override
    public ILocationService getLocationService() {
        return locationService;
    }

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
    @Override
    public CatalogResourceType[] queryResourceTypes(final GUID[] resourceTypeIdentifiers) {
        ensureResourceTypesLoaded();

        // Return all resource types if no filters are specified.
        if (resourceTypeIdentifiers == null
            || resourceTypeIdentifiers.length == 0
            || resourceTypeIdentifiers[0].equals(GUID.EMPTY)) {
            return mapResourceTypes.values().toArray(new CatalogResourceType[mapResourceTypes.size()]);
        }

        // Return only the resource types whose GUIDs appear in the filter list.
        final ArrayList<CatalogResourceType> list = new ArrayList<CatalogResourceType>();
        for (int i = 0; i < resourceTypeIdentifiers.length; i++) {
            final GUID id = resourceTypeIdentifiers[i];
            if (mapResourceTypes.containsKey(id)) {
                list.add(mapResourceTypes.get(id));
            } else {
                throw new CatalogResourceTypeDoesNotExistException(id);
            }
        }

        return list.toArray(new CatalogResourceType[list.size()]);
    }

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
    @Override
    public CatalogResource[] queryResources(final GUID[] resourceIdentifiers, final CatalogQueryOptions queryOptions) {
        final CatalogData data = catalogProxy.queryResources(resourceIdentifiers, queryOptions.toIntFlags());
        final CatalogDataProcessedResult result = processCatalogData(data, queryOptions);
        return result.getMatchingResources();
    }

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
    @Override
    public CatalogResource[] queryResourcesByType(
        final GUID[] resourceTypeIdentifiers,
        final CatalogQueryOptions queryOptions) {
        return queryResources(resourceTypeIdentifiers, null, queryOptions);
    }

    /**
     * Returns the resource that is associated with the identifier.
     *
     * @param resourceTypeIdentifiers
     *        The identifiers for the resources that are being search for.
     * @param propertyFilters
     *        The set of property filters to apply to the nodes found. Matches
     *        will be based on both the key and the value of the property
     *        matching. If the value of a certain filter is null or empty then
     *        it will be assumed that all nodes with the supplied property
     *        should be returned. A match consists of a node/resource that
     *        matches all of the propertyFilters.
     * @param queryOptions
     *        If ExpandDependencies is specified, the Dependencies property on
     *        nodes will contain the nodes they are dependent on. If
     *        IncludeParents is specified, the ParentNode property on the
     *        CatalogNode will contain the parent node. Leaving a given option
     *        will result in the returned catalog nodes to have null for that
     *        value. Extra data should only be retrieved if it is needed since
     *        computing and sending information can be expensive.
     *
     * @return the resources with the specified identifiers.
     */
    @Override
    public CatalogResource[] queryResources(
        final GUID[] resourceTypeIdentifiers,
        final CatalogResourceProperty[] propertyFilters,
        final CatalogQueryOptions queryOptions) {
        final CatalogData data =
            catalogProxy.queryResourcesByType(resourceTypeIdentifiers, propertyFilters, queryOptions.toIntFlags());
        final CatalogDataProcessedResult result = processCatalogData(data, queryOptions);
        return result.getMatchingResources();
    }

    /**
     * @return the root nodes in the tree.
     */
    @Override
    public CatalogNode[] getRootNodes() {
        final String[] pathSpecs = new String[] {
            CatalogConstants.SINGLE_RECURSE_STAR
        };
        final CatalogData data = catalogProxy.queryNodes(pathSpecs, null, null, CatalogQueryOptions.NONE.toIntFlags());
        final CatalogDataProcessedResult result = processCatalogData(data, CatalogQueryOptions.NONE);
        return result.getMatchingNodes();
    }

    /**
     * Returns the specified root node. Well-known root paths can be found in
     * Microsoft.TeamFoundation.Framework.Common.Catalog.CatalogTree.
     *
     * @param tree
     *        The tree of the desired root.
     *
     * @return The CatalogNode for the tree.
     */
    @Override
    public CatalogNode queryRootNode(final CatalogTree tree) {
        final String[] pathSpecs = new String[] {
            CatalogRoots.determinePath(tree)
        };
        final CatalogData data = catalogProxy.queryNodes(pathSpecs, null, null, CatalogQueryOptions.NONE.toIntFlags());
        final CatalogDataProcessedResult result = processCatalogData(data, CatalogQueryOptions.NONE);

        final CatalogNode[] matchingNodes = result.getMatchingNodes();
        if (matchingNodes.length != 1) {
            throw new CatalogNodeDoesNotExistException();
        }
        return matchingNodes[0];
    }

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
    @Override
    public CatalogNode[] queryNodes(
        final String[] pathSpecs,
        final GUID[] resourceTypeFilters,
        final CatalogQueryOptions queryOptions) {
        return queryNodes(pathSpecs, resourceTypeFilters, null, queryOptions);
    }

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
    @Override
    public CatalogNode[] queryNodes(
        final String[] pathSpecs,
        final GUID[] resourceTypeFilters,
        final CatalogResourceProperty[] propertyFilters,
        final CatalogQueryOptions queryOptions) {
        final CatalogData data =
            catalogProxy.queryNodes(pathSpecs, resourceTypeFilters, propertyFilters, queryOptions.toIntFlags());
        final CatalogDataProcessedResult result = processCatalogData(data, queryOptions);
        return result.getMatchingNodes();
    }

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
    @Override
    public CatalogNode[] queryParents(
        final GUID resourceIdentifier,
        final String[] pathFilters,
        final GUID[] resourceTypeFilters,
        final boolean recurseToRoot,
        final CatalogQueryOptions queryOptions) {
        final CatalogData data = catalogProxy.queryParents(
            resourceIdentifier,
            pathFilters,
            resourceTypeFilters,
            recurseToRoot,
            queryOptions.toIntFlags());
        final CatalogDataProcessedResult result = processCatalogData(data, queryOptions);
        return result.getMatchingNodes();
    }

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
    @Override
    public CatalogNode[] queryUpTree(
        final String path,
        final GUID[] resourceTypeFilters,
        final CatalogQueryOptions queryOptions) {
        throw new CatalogMethodNotImplementedException("QueryUpTree"); //$NON-NLS-1$
    }

    /**
     * Creates a change context in which many changes can be batched
     *
     * @return A change context in which many changes can be batched together.
     */
    @Override
    public CatalogChangeContext createChangeContext() {
        throw new CatalogMethodNotImplementedException("CreateChangeContext"); //$NON-NLS-1$
    }

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
    @Override
    public void saveDelete(final CatalogNode node, final Boolean recurse) {
        throw new CatalogMethodNotImplementedException("SaveDelete"); //$NON-NLS-1$
    }

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
    @Override
    public void saveMove(final CatalogNode nodeToMove, final CatalogNode newParent) {
        throw new CatalogMethodNotImplementedException("SaveMove"); //$NON-NLS-1$
    }

    /**
     * Saves the updated node and its resource in the catalog.
     *
     * @param node
     *        The node that has been created or updated.
     */
    @Override
    public void saveNode(final CatalogNode node) {
        throw new CatalogMethodNotImplementedException("SaveNode"); //$NON-NLS-1$
    }

    /**
     * Saves the updated resource in the catalog. Note that service definitions
     * that exist as service references will be created if they are new and
     * updated if they are not.
     *
     * @param resource
     *        The resource to update.
     */
    @Override
    public void saveResource(final CatalogResource resource) {
        throw new CatalogMethodNotImplementedException("SaveResource"); //$NON-NLS-1$
    }

    /**
     * Ensure resource types have been loaded from the server and cached locally
     * in resource map. The first caller to this method will load the resources
     * from the server while holding a write lock which provides access to the
     * resource types map.
     */
    private void ensureResourceTypesLoaded() {
        if (resourceTypesLoaded) {
            return;
        }

        synchronized (mapResourceTypesLock) {
            if (resourceTypesLoaded) {
                return;
            }

            mapResourceTypes.clear();
            final CatalogResourceType[] resourceTypes = catalogProxy.queryResourceTypes(null);
            for (int i = 0; i < resourceTypes.length; i++) {
                final CatalogResourceType resourceType = resourceTypes[i];
                mapResourceTypes.put(new GUID(resourceType.getIdentifier()), resourceType);
            }

            resourceTypesLoaded = true;
        }
    }

    private CatalogDataProcessedResult processCatalogData(
        final CatalogData data,
        final CatalogQueryOptions queryOptions) {
        // Make sure we tell the location service what the latest change id is
        // from the
        // server so that it knows whether or not it has to refresh cache.

        connection.reactToPossibleServerUpdate(data.getLocationServiceLastChangeID());

        final ArrayList<CatalogNode> matchingNodes = new ArrayList<CatalogNode>();
        final ArrayList<CatalogResource> matchingResources = new ArrayList<CatalogResource>();

        final CatalogResourceType[] resourceTypes = data.getCatalogResourceTypes();
        final CatalogNode[] catalogNodes = data.getCatalogNodes();
        final CatalogResource[] catalogResources = data.getCatalogResources();

        // Build up a map of types to make the resource join easier.
        final HashMap<String, CatalogResourceType> typeTable = new HashMap<String, CatalogResourceType>();

        for (int i = 0; i < resourceTypes.length; i++) {
            final CatalogResourceType resourceType = resourceTypes[i];
            typeTable.put(resourceType.getIdentifier(), resourceType);
        }

        // Build up a dictionary of nodes to make the resource join easier.
        final HashMap<String, CatalogNode> nodeTable = new HashMap<String, CatalogNode>();

        for (int i = 0; i < catalogNodes.length; i++) {
            final CatalogNode catalogNode = catalogNodes[i];
            catalogNode.initializeFromWebService(this);
            nodeTable.put(catalogNode.getFullPath(), catalogNode);

            if (catalogNode.isMatchedQuery()) {
                matchingNodes.add(catalogNode);
            }

        }

        // Now hook up the node dependencies and the parents
        if (!CatalogQueryOptions.NONE.equals(queryOptions)) {
            for (int i = 0; i < catalogNodes.length; i++) {
                final CatalogNode node = catalogNodes[i];

                if (queryOptions.contains(CatalogQueryOptions.EXPAND_DEPENDENCIES)) {
                    Check.isTrue(node.isNodeDependenciesIncluded(), "node.isNodeDependenciesIncluded()"); //$NON-NLS-1$
                    final CatalogNodeDependency[] dependencies = node.getNodeDependencies();
                    for (int j = 0; j < dependencies.length; j++) {
                        final CatalogNodeDependency dependency = dependencies[j];
                        final String requiredNodeFullPath = dependency.getRequiredNodeFullPath();
                        final String associationKey = dependency.getAssociationKey();

                        if (dependency.isSingleton()) {
                            node.getDependencyGroup().setSingletonDependency(
                                associationKey,
                                nodeTable.get(requiredNodeFullPath));
                        } else {
                            node.getDependencyGroup().addSetDependency(
                                associationKey,
                                nodeTable.get(requiredNodeFullPath));
                        }
                    }
                }

                if (queryOptions.contains(CatalogQueryOptions.INCLUDE_PARENTS)
                    && node.getParentPath() != null
                    && node.getParentPath().length() != 0) {
                    node.setParentNode(nodeTable.get(node.getParentPath()));
                }
            }
        }

        for (int i = 0; i < catalogResources.length; i++) {
            final CatalogResource resource = catalogResources[i];
            resource.initializeFromWebService(typeTable, nodeTable, locationService);

            if (resource.isMatchedQuery()) {
                matchingResources.add(resource);
            }
        }

        final CatalogResource[] matchingResourcesArray =
            matchingResources.toArray(new CatalogResource[matchingResources.size()]);
        final CatalogNode[] matchingNodesArray = matchingNodes.toArray(new CatalogNode[matchingNodes.size()]);
        return new CatalogDataProcessedResult(matchingResourcesArray, matchingNodesArray);
    }
}
