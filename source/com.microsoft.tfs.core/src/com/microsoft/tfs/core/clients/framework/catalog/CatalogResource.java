// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.catalog;

import java.util.HashMap;

import com.microsoft.tfs.core.clients.framework.location.ILocationService;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.util.GUID;

import ms.ws._CatalogResource;
import ms.ws._CatalogServiceReference;
import ms.ws._KeyValueOfStringString;

/**
 * Wrapper class for the {@link _CatalogResource} proxy object which is
 * contained within the result of a TFS catalog web service.
 *
 * @threadsafety thread-safe
 * @since TEE-SDK-10.1
 */
public class CatalogResource extends WebServiceObjectWrapper {
    private CatalogResourceType resourceType;
    private HashMap<String, ServiceDefinition> serviceReferences;
    private HashMap<String, String> properties;
    private CatalogNode[] nodeReferences;

    /**
     * Wrapper constructor.
     */
    public CatalogResource(final _CatalogResource catalogResource) {
        super(catalogResource);
    }

    /**
     * Returns the wrapped proxy object.
     */
    public _CatalogResource getWebServiceObject() {
        return (_CatalogResource) webServiceObject;
    }

    /**
     * Returns the identifier for this resource.
     */
    public String getIdentifier() {
        return getWebServiceObject().getIdentifier();
    }

    /**
     * Returns the resource type identifier for this resource.
     */
    public String getResourceTypeIdentifier() {
        return getWebServiceObject().getResourceTypeIdentifier();
    }

    /**
     * Returns the display name for this resource.
     */
    public String getDisplayName() {
        return getWebServiceObject().getDisplayName();
    }

    /**
     * Returns the description for this resource.
     */
    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    /**
     * Returns true if this resource matched the query criteria.
     */
    public boolean isMatchedQuery() {
        return getWebServiceObject().isMatchedQuery();
    }

    /**
     * Return the type for this resource.
     */
    public CatalogResourceType getResourceType() {
        return resourceType;
    }

    /**
     * Returns the serviceReferences associated with this resource. The map
     * which is returned contains a key which is the
     * _ServiceReference.AssociationKey and a value which is a
     * ServiceDefinition.
     */
    public HashMap<String, ServiceDefinition> getServiceReferences() {
        return serviceReferences;
    }

    /**
     * Returns the properties associated with this resource. The map which is
     * returned contains name/value pairs which are String/String.
     */
    public HashMap<String, String> getProperties() {
        return properties;
    }

    /**
     * Returns the node references for this resource.
     */
    public CatalogNode[] getNodeReferences() {
        return nodeReferences;
    }

    /**
     * Used internally during post-processing of a catalog web service request
     * result. Initializes the members of this class which are not part of the
     * underlying proxy object.
     *
     * @param typeMap
     *        A map of resource types keyed by resource type identifiers.
     *
     * @param nodeMap
     *        A map of catalog nodes keyed by node paths.
     *
     * @param locationService
     *        The catalog service that was used to retrieve these results.
     */
    public void initializeFromWebService(
        final HashMap<String, CatalogResourceType> typeMap,
        final HashMap<String, CatalogNode> nodeMap,
        final ILocationService locationService) {
        // Initialize the resource type.
        resourceType = typeMap.get(getWebServiceObject().getResourceTypeIdentifier());

        // Initialize the node references.
        final String[] nodeReferencePaths = getWebServiceObject().getNodeReferencePaths();
        nodeReferences = new CatalogNode[nodeReferencePaths.length];
        for (int i = 0; i < nodeReferencePaths.length; i++) {
            final CatalogNode node = nodeMap.get(nodeReferencePaths[i]);
            node.setCatalogResource(this);
            nodeReferences[i] = node;
        }

        // Initialize the service definition references.
        final _CatalogServiceReference[] references = getWebServiceObject().getCatalogServiceReferences();
        serviceReferences = new HashMap<String, ServiceDefinition>();

        for (int i = 0; i < references.length; i++) {
            final _CatalogServiceReference serviceReference = references[i];
            ServiceDefinition serviceDefinition = null;

            if (serviceReference.getServiceDefinition() != null) {
                final String serviceType = serviceReference.getServiceDefinition().getServiceType();
                final GUID serviceIdentifier = new GUID(serviceReference.getServiceDefinition().getIdentifier());
                serviceDefinition = locationService.findServiceDefinition(serviceType, serviceIdentifier);
            }

            serviceReferences.put(serviceReference.getAssociationKey(), serviceDefinition);
        }

        // Initialize the properties.
        properties = new HashMap<String, String>();
        final _KeyValueOfStringString[] propertyPairs = getWebServiceObject().getProperties();
        for (int i = 0; i < propertyPairs.length; i++) {
            properties.put(propertyPairs[i].getKey(), propertyPairs[i].getValue());
        }
    }
}
