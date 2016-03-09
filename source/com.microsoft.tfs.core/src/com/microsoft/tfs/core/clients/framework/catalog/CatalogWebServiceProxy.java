// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.exceptions.mappers.CatalogExceptionMapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.core.ws.runtime.exceptions.ProxyException;
import com.microsoft.tfs.util.GUID;

import ms.ws._CatalogData;
import ms.ws._CatalogResourceType;
import ms.ws._CatalogWebServiceSoap;
import ms.ws._KeyValueOfStringString;

/**
 * A proxy class for the TFS catalog web service.
 *
 * @since TEE-SDK-10.1
 */
public class CatalogWebServiceProxy {
    _CatalogWebServiceSoap webService;

    /**
     * Constructor
     */
    public CatalogWebServiceProxy(final TFSConfigurationServer server) {
        webService = (_CatalogWebServiceSoap) server.getWebService(_CatalogWebServiceSoap.class);
    }

    /**
     * Retrieve resource type objects for the specified resource type
     * identifiers.
     *
     * @param resourceTypeIdentifiers
     *        Identifies the resource types that should be returned. Use null or
     *        an empty array to retrieve all resource types.
     *
     * @return The set of resource types matching the specified filter.
     */
    public CatalogResourceType[] queryResourceTypes(final GUID[] resourceTypeIdentifiers) {
        try {
            final _CatalogResourceType[] resourceTypes =
                webService.queryResourceTypes(GUID.toStringArray(resourceTypeIdentifiers));
            return (CatalogResourceType[]) WrapperUtils.wrap(CatalogResourceType.class, resourceTypes);
        } catch (final ProxyException e) {
            throw CatalogExceptionMapper.map(e);
        }
    }

    /**
     * Return the CatalogData which contains resources and nodes which match the
     * specified filters.
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
     *        The set of property filters to apply to the resource found.
     *        Matches will be based on both the key and the value of the
     *        property matching. If the value of a certain filter is null or
     *        empty then it will be assumed that all resource with the supplied
     *        property should be returned. A match consists of a resource that
     *        matches all of the propertyFilters
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
     * @return The resulting set of matching nodes and resources contained
     *         within a CatalogData object.
     */
    public CatalogData queryNodes(
        final String[] pathSpecs,
        final GUID[] resourceTypeFilters,
        final CatalogResourceProperty[] propertyFilters,
        final int queryOptions) {
        try {
            final String[] types = GUID.toStringArray(resourceTypeFilters);
            final _KeyValueOfStringString[] properties =
                CatalogResourceProperty.toKeyValueOfStringStringArray(propertyFilters);

            final _CatalogData data = webService.queryNodes(pathSpecs, types, properties, queryOptions);
            return new CatalogData(data);
        } catch (final ProxyException e) {
            throw CatalogExceptionMapper.map(e);
        }
    }

    /**
     * Return the CatalogData which contains resources and nodes which match the
     * specified filters.
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
     * @return The resulting set of matching nodes and resources contained
     *         within a CatalogData object.
     */
    public CatalogData queryResources(final GUID[] resourceTypeFilters, final int queryOptions) {
        try {
            final _CatalogData data = webService.queryResources(GUID.toStringArray(resourceTypeFilters), queryOptions);
            return new CatalogData(data);
        } catch (final ProxyException e) {
            throw CatalogExceptionMapper.map(e);
        }
    }

    /**
     * Returns all of the catalog resources of the provided type. If
     * {@link GUID#EMPTY} is passed in, all resources are returned.
     *
     * @param resourceTypeFilters
     *        The list of types that this query should include. If this is null
     *        or empty, all types will be included.
     *
     * @param propertyFilters
     *        The set of property filters to apply to the resource found.
     *        Matches will be based on both the key and the value of the
     *        property matching. If the value of a certain filter is null or
     *        empty then it will be assumed that all resource with the supplied
     *        property should be returned. A match consists of a resource that
     *        matches all of the propertyFilters
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
     * @return The resulting set of matching nodes and resources contained
     *         within a CatalogData object.
     */
    public CatalogData queryResourcesByType(
        final GUID[] resourceTypeFilters,
        final CatalogResourceProperty[] propertyFilters,
        final int queryOptions) {
        try {
            final String[] types = GUID.toStringArray(resourceTypeFilters);
            final _KeyValueOfStringString[] properties =
                CatalogResourceProperty.toKeyValueOfStringStringArray(propertyFilters);

            final _CatalogData data = webService.queryResourcesByType(types, properties, queryOptions);
            return new CatalogData(data);
        } catch (final ProxyException e) {
            throw CatalogExceptionMapper.map(e);
        }
    }

    /**
     * Returns all of the nodes that depend on the nodes existence.
     *
     * @param path
     *        The path whose dependents are being queried.
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
     * @return The resulting set of dependents contained within a CatalogData
     *         object.
     */
    public CatalogData queryDependents(final String path, final int queryOptions) {
        try {
            final _CatalogData data = webService.queryDependents(path, queryOptions);
            return new CatalogData(data);
        } catch (final ProxyException e) {
            throw CatalogExceptionMapper.map(e);
        }
    }

    /**
     * Returns the nodes for the resource provided as well as the parents. The
     * direct nodes and the parent nodes will not be returned if they are
     * filtered out.
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
     * @return The resulting set of matching nodes and resources contained
     *         within a CatalogData object.
     */
    public CatalogData queryParents(
        final GUID resourceIdentifier,
        final String[] pathFilters,
        final GUID[] resourceTypeFilters,
        final boolean recurseToRoot,
        final int queryOptions) {
        try {
            final String id = resourceIdentifier.getGUIDString();
            final String[] types = GUID.toStringArray(resourceTypeFilters);

            final _CatalogData data = webService.queryParents(id, pathFilters, types, recurseToRoot, queryOptions);
            return new CatalogData(data);
        } catch (final ProxyException e) {
            throw CatalogExceptionMapper.map(e);
        }
    }
}
