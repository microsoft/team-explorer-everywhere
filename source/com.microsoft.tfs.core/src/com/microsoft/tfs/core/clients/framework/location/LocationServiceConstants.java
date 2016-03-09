// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location;

import com.microsoft.tfs.core.clients.framework.internal.ServiceInterfaceIdentifiers;
import com.microsoft.tfs.util.GUID;

/**
 * Constants for use of the Location Service by the core framework.
 *
 * @threadsafety Thread safe, all values are static final.
 */
public class LocationServiceConstants {
    /**
     * This identifier should be used to request all instances of a given
     * service type when using ServiceTypeFilters.
     */
    public static final GUID ALL_INSTANCES_IDENTIFIER = new GUID("567713db-d56d-4bb0-8f35-604e0e116174"); //$NON-NLS-1$

    /**
     * This is the ServiceType value that should be used in order to request all
     * services.
     */
    public static final String ALL_SERVICES_TYPE_FILTER = "*"; //$NON-NLS-1$

    /**
     * If a Location Service has an entry for an application location service,
     * that location service definition will have an identifier of this value.
     */
    public static final GUID APPLICATION_LOCATION_SERVICE_IDENTIFIER = new GUID("8d299418-9467-402b-a171-9165e2f703e2"); //$NON-NLS-1$

    /**
     * The path of the Location Service relative to the TFS application.
     */
    public static final String APPLICATION_LOCATION_SERVICE_RELATIVE_PATH =
        "/TeamFoundation/Administration/v3.0/LocationService.asmx"; //$NON-NLS-1$

    /**
     * The path of the Location Service relative to the TFS Project collection.
     */
    public static final String COLLECTION_LOCATION_SERVICE_RELATIVE_PATH = "/Services/v3.0/LocationService.asmx"; //$NON-NLS-1$

    // Not Implemented - This is the environment variable that can be used to
    // override where the location services client cache is stored.
    // public static final String DATA_DIR_ENV_VAR = "LOCATION_CACHE_DIRECTORY";

    /**
     * Represents a service definition that hasn't been registered or an invalid
     * one.
     */
    public static final GUID INVALID_SERVICE_DEFINITION_IDENTIFIER = new GUID("39b10086-1c48-4f34-a73b-73043d5170df"); //$NON-NLS-1$

    /**
     * All Location Services have a reference to their own service definition.
     * That service definition has an identifier of this value.
     */
    public static final GUID SELF_REFERENCE_LOCATION_SERVICE_IDENTIFIER = ServiceInterfaceIdentifiers.LOCATION;

    /**
     * This identifier should be used to request a Singleton instance of a
     * service definition if you must pass an identifier value. Note that this
     * does not mean that all singleton service definitions will have this
     * identifer, it is just a way to refer to all of them.
     */
    public static final GUID SINGLETON_INSTANCE_IDENTIFIER = new GUID("562703e8-d8fb-476c-b599-ed49d80eb92b"); //$NON-NLS-1$

    // This class should not be constructed.
    private LocationServiceConstants() {
    };
}
