// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location;

import com.microsoft.tfs.core.clients.framework.location.exceptions.InvalidAccessPointException;
import com.microsoft.tfs.core.clients.framework.location.exceptions.InvalidServiceDefinitionException;
import com.microsoft.tfs.core.clients.framework.location.exceptions.ServiceDefinitionDoesNotExistException;
import com.microsoft.tfs.util.GUID;

/**
 * The service responsible for providing a connection to a Team Foundation
 * Server as well as the locations of other services that are available on it.
 *
 * @since TEE-SDK-10.1
 */
public interface ILocationService {
    /**
     * The AccessMapping for the current connection to the server. Note, it is
     * possible that the current ClientAccessMapping is not a member of the
     * ConfiguredAccessMappings if the access point this client used to connect
     * to the server has not been configured on it. This will never be null.
     *
     * @return The AccessMapping for the current connection to the server.
     */
    public AccessMapping getClientAccessMapping();

    /**
     * All of the AccessMappings that this location service knows about. Because
     * a given location service can inherit AccessMappings from its parent these
     * AccessMappings may exist on this location service or its parent.
     *
     * @return All of the AccessMappings that this location service knows about.
     */
    public AccessMapping getDefaultAccessMapping();

    /**
     * All of the AccessMappings that this location service knows about. Because
     * a given location service can inherit AccessMappings from its parent these
     * AccessMappings may exist on this location service or its parent.
     *
     * @return All of the AccessMappings that this location service knows about.
     */
    public AccessMapping[] getConfiguredAccessMappings();

    /**
     * Saves the provided ServiceDefinition within the location service. This
     * operation will assign the Identifier property on the ServiceDefinition
     * object if one is not already assigned. Any AccessMappings referenced in
     * the LocationMappings property must already be configured with the
     * location service.
     *
     * @param serviceDefinition
     *        The ServiceDefinition to save. This object will be updated with a
     *        new Identifier if one is not already assigned.
     *
     * @exception InvalidServiceDefinitionException
     *            The ServiceDefinition being saved is not valid.
     *
     * @exception AccessMappingNotRegisteredException
     *            The ServiceDefinition references an AccessMapping that has not
     *            been registered.
     *
     * @exception DuplicateLocationMappingException
     *            Thrown if a given AccessMapping has two or more
     *            LocationMappings on a ServiceDefinition.
     */
    public void saveServiceDefinition(ServiceDefinition serviceDefinition);

    /**
     * Saves the provided ServiceDefinitions within the location service. This
     * operation will assign the Identifier property on the ServiceDefinition
     * objects if one is not already assigned. Any AccessMappings referenced in
     * the LocationMappings property must already be configured with the
     * location service.
     *
     * @param serviceDefinitions
     *        The ServiceDefinitions to save. These objects will be updated with
     *        a new Identifier if one is not already assigned.
     *
     * @exception InvalidServiceDefinitionException
     *            The ServiceDefinition being saved is not valid.
     *
     * @exception AccessMappingNotRegisteredException
     *            The ServiceDefinition references an AccessMapping that has not
     *            been registered.
     *
     * @exception DuplicateLocationMappingException
     *            Thrown if a given AccessMapping has two or more
     *            LocationMappings on a ServiceDefinition
     */
    public void saveServiceDefinitions(ServiceDefinition[] serviceDefinitions);

    /**
     * Removes the ServiceDefinition with the specified service type and service
     * identifier from the location service.
     *
     * @param serviceType
     *        The service type of the ServiceDefinition to remove.
     *
     * @param serviceIdentifier
     *        The service identifier of the ServiceDefinition to remove.
     *
     * @exception IllegalDeleteSelfReferenceServiceDefinitionExceptio
     *            Thrown if the caller tries to delete the self-reference
     *            (location service) ServiceDefinition.
     */
    public void removeServiceDefinition(String serviceType, GUID serviceIdentifier);

    /**
     * Removes the specified ServiceDefinition from the location service.
     *
     * @param serviceDefinition
     *        The ServiceDefinition to remove. This must be a ServiceDefinition
     *        that is already registered in the location service. Equality is
     *        decided by matching the service type and the identifier.
     *
     * @exception IllegalDeleteSelfReferenceServiceDefinitionException
     *            Thrown if the caller tries to delete the self-reference
     *            (location service) ServiceDefinition.
     */
    public void removeServiceDefinition(ServiceDefinition serviceDefinition);

    /**
     * Removes the specified ServiceDefinitions from the location service.
     *
     * @param serviceDefinitions
     *        The ServiceDefinitions to remove. These must be ServiceDefinitions
     *        that are already registered in the location service. Equality is
     *        decided by matching the service type and the identifier.
     *
     * @exception IllegalDeleteSelfReferenceServiceDefinitionException
     *            Thrown if the caller tries to delete the self-reference
     *            (location service) ServiceDefinition.
     */
    public void removeServiceDefinitions(ServiceDefinition[] serviceDefinitions);

    /**
     * Finds the ServiceDefinition with the specified service type and service
     * identifier. If no matching ServiceDefinition exists, null is returned.
     *
     * @param serviceType
     *        The service type of the ServiceDefinition to find.
     *
     * @param serviceIdentifier
     *        The service identifier of the ServiceDefinition to find.
     *
     * @return The ServiceDefinition with the specified service type and service
     *         identifier. If no matching ServiceDefinition exists, null is
     *         returned.
     */
    public ServiceDefinition findServiceDefinition(String serviceType, GUID serviceIdentifier);

    /**
     * Finds the ServiceDefinitions for all of the services with the specified
     * service type. If no ServiceDefinitions of this type exist, an empty
     * enumeration will be returned.
     *
     * @param serviceType
     *        The case-insensitive string that identifies what type of service
     *        is being requested. If this value is null, ServiceDefinitions for
     *        all services registered with this location service will be
     *        returned.
     *
     * @return ServiceDefinitions for all of the services with the specified
     *         service type. If no ServiceDefinitions of this type exist, an
     *         empty enumeration will be returned.
     */
    public ServiceDefinition[] findServiceDefinitions(String serviceType);

    /**
     * Finds the ServiceDefinitions for all of the services with the specified
     * tool type. If no services exist for this tool type, an empty enumeration
     * will be returned.
     *
     * @param toolType
     *        The case-insensitive string that will match the tool type of a set
     *        of ServiceDefinitions. If null or empty is passed in for this
     *        value then all of the ServiceDefinitions will be returned.
     *
     * @return ServiceDefinitions for all of the services with the specified
     *         tool type. If no services exist for this tool type, an empty
     *         enumeration will be returned.
     */
    public ServiceDefinition[] findServiceDefinitionsByToolType(String toolType);

    /**
     * Returns the location for the ServiceDefintion associated with the
     * ServiceType and ServiceIdentifier that should be used based on the
     * current connection. If a ServiceDefinition with the ServiceType and
     * ServiceIdentifier does not exist then null will be returned. If a
     * ServiceDefinition with the ServiceType and ServiceIdentifier is found
     * then a location will be returned if the ServiceDefinition is well formed
     * (otherwise an exception will be thrown).
     *
     * When determining what location to return for the ServiceDefinition and
     * current connection the following rules will be applied:
     *
     * 1. Try to find a location for the ClientAccessMapping. 2. Try to find a
     * location for the DefaultAccessMapping. 3. Use the first location in the
     * LocationMappings list.
     *
     * @param serviceType
     *        The service type of the ServiceDefinition to find the location
     *        for.
     *
     * @param serviceIdentifier
     *        The service identifier of the ServiceDefinition to find the
     *        location for.
     *
     * @return The location for the ServiceDefinition with the provided service
     *         type and identifier that should be used based on the current
     *         connection.
     *
     * @exception InvalidServiceDefinitionException
     *            The associated ServiceDefinition is not valid and no location
     *            can be found.
     */
    public String locationForCurrentConnection(String serviceType, GUID serviceIdentifier);

    /**
     * Returns the location for the ServiceDefintion that should be used based
     * on the current connection. This method will never return null or empty.
     * If it succeeds it will return a target-able location for the provided
     * ServiceDefinition.
     *
     * When determining what location to return for the ServiceDefinition and
     * current connection the following rules will be applied:
     *
     * 1. Try to find a location for the ClientAccessMapping. 2. Try to find a
     * location for the DefaultAccessMapping. 3. Use the first location in the
     * LocationMappings list.
     *
     * @param serviceDefinition
     *        The ServiceDefinition to find the location for.
     *
     * @return The location for the given ServiceDefinition that should be used
     *         based on the current connection.
     *
     * @exception InvalidServiceDefinitionException
     *            The ServiceDefinition passed in is not valid and no location
     *            can be found.
     */
    public String locationForCurrentConnection(ServiceDefinition serviceDefinition);

    /**
     * Returns the location for the ServiceDefinition that has the specified
     * service type and service identifier for the provided AccessMapping. If
     * this ServiceDefinition is FullyQualified and no LocationMapping exists
     * for this AccessMapping then null will be returned.
     *
     * @param serviceType
     *        The service type of the ServiceDefinition to find the location
     *        for.
     *
     * @param serviceIdentifier
     *        The service identifier of the ServiceDefinition to find the
     *        location for.
     *
     * @param accessMapping
     *        The AccessMapping to find the location for.
     *
     * @return The location for the ServiceDefinition for the provided
     *         AccessMapping. If this ServiceDefinition is FullyQualified and no
     *         LocationMapping exists for this AccessMapping then null will be
     *         returned.
     *
     * @exception InvalidServiceDefinitionException
     *            The associated ServiceDefinition is not valid and no location
     *            can be found.
     *
     * @exception ServiceDefinitionDoesNotExistException
     *            A ServiceDefinition with the provided service type and
     *            identifier does not exist.
     *
     * @exception InvalidAccessPointException
     *            The AccessMapping passed in does not have a valid access
     *            point.
     */
    public String locationForAccessMapping(String serviceType, GUID serviceIdentifier, AccessMapping accessMapping);

    /**
     * Returns the location for the ServiceDefinition for the provided
     * AccessMapping. If this ServiceDefinition is FullyQualified and no
     * LocationMapping exists for this AccessMapping then null will be returned.
     *
     * @param serviceDefinition
     *        The ServiceDefinition to find the location for.
     *
     * @param accessMapping
     *        The AccessMapping to find the location for.
     *
     * @return The location for the ServiceDefinition for the provided
     *         AccessMapping. If this ServiceDefinition is FullyQualified and no
     *         LocationMapping exists for this AccessMapping then null will be
     *         returned.
     *
     * @exception InvalidServiceDefinitionException
     *            The ServiceDefinition passed in is not valid.
     *
     * @exception InvalidAccessPointException
     *            The AccessMapping passed in does not have a valid access
     *            point.
     */
    public String locationForAccessMapping(ServiceDefinition serviceDefinition, AccessMapping accessMapping);

    /**
     * Returns the location for the ServiceDefinition for the provided
     * AccessMapping. If this ServiceDefinition is FullyQualified and no
     * LocationMapping exists for this AccessMapping then null will be returned.
     *
     * @param serviceDefinition
     *        The ServiceDefinition to find the location for.
     *
     * @param accessMapping
     *        The AccessMapping to find the location for.
     *
     * @param encodeRelativeComponents
     *        If true, URI-encode any relative URI path components before
     *        appending to the root URI.
     *
     * @return The location for the ServiceDefinition for the provided
     *         AccessMapping. If this ServiceDefinition is FullyQualified and no
     *         LocationMapping exists for this AccessMapping then null will be
     *         returned.
     *
     * @exception InvalidServiceDefinitionException
     *            The ServiceDefinition passed in is not valid.
     *
     * @exception InvalidAccessPointException
     *            The AccessMapping passed in does not have a valid access
     *            point.
     */
    public String locationForAccessMapping(
        ServiceDefinition serviceDefinition,
        AccessMapping accessMapping,
        boolean encodeRelativeComponents);

    /**
     * Configures the AccessMapping with the provided moniker to have the
     * provided display name and access point. This function also allows for
     * this AccessMapping to be made the default AccessMapping.
     *
     * @param moniker
     *        A string that uniquely identifies this AccessMapping. This value
     *        cannot be null or empty.
     *
     * @param displayName
     *        Display name for this AccessMapping. This value cannot be null or
     *        empty.
     *
     * @param accessPoint
     *        This is the base URL for the server that will map to this
     *        AccessMapping. This value cannot be null or empty.
     *
     *        The access point should consist of the scheme, authority, port and
     *        web application virtual path of the target-able server address.
     *        For example, an access point will most commonly look like this:
     *        http://server:8080/tfs/
     *
     * @param makeDefault
     *        If true, this AccessMapping will be made the default
     *        AccessMapping. If false, the default AccessMapping will not
     *        change.
     *
     * @return The AccessMapping object that was just configured.
     *
     * @exception InvalidAccessPointException
     *            Thrown if the access point for this AccessMapping is invalid
     *            or if it conflicts with an already registered access point.
     */
    public AccessMapping configureAccessMapping(
        String moniker,
        String displayName,
        String accessPoint,
        boolean makeDefault);

    /**
     * Sets the default AccessMapping to the AccessMapping passed in.
     *
     * @param accessMapping
     *        The AccessMapping that should become the default AccessMapping.
     *        This AccessMapping must already be configured with this location
     *        service.
     *
     * @exception AccessMappingNotRegisteredException
     *            The AccessMapping being set to the default has not been
     *            registered.
     */
    public void setDefaultAccessMapping(AccessMapping accessMapping);

    /**
     * Gets the AccessMapping with the specified moniker. Returns null if an
     * AccessMapping with the supplied moniker does not exist.
     *
     * @param moniker
     *        The moniker for the desired AccessMapping. This value cannot be
     *        null or empty.
     *
     * @return The AccessMapping with the supplied moniker or null if one does
     *         not exist.
     */
    public AccessMapping getAccessMapping(String moniker);

    /**
     * Removes an AccessMapping and all of the locations that are mapped to it
     * within ServiceDefinitions.
     *
     * @param moniker
     *        The moniker for the AccessMapping to remove.
     *
     * @exception RemoveAccessMappingException
     *            Thrown if the caller tries to remove the default AccessMapping
     *            and this location service cannot inherit its default
     *            AccessMapping from a parent.
     */
    public void removeAccessMapping(String moniker);
}
