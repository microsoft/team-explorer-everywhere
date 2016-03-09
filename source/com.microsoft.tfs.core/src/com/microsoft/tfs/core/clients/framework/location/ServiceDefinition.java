// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location;

import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

import ms.ws._LocationMapping;
import ms.ws._ServiceDefinition;

/**
 * Wrapper class for the {@link _ServiceDefinition} proxy object.
 *
 * @since TEE-SDK-10.1
 */
public class ServiceDefinition extends WebServiceObjectWrapper {
    GUID serviceIdentifier;
    RelativeToSetting relativeToSetting;
    LocationMapping[] locationMappings;

    public ServiceDefinition(final _ServiceDefinition serviceDefinition) {
        super(serviceDefinition);

        serviceIdentifier = new GUID(serviceDefinition.getIdentifier());
        relativeToSetting = RelativeToSetting.intToRelativeToSetting(serviceDefinition.getRelativeToSetting());
        locationMappings =
            (LocationMapping[]) WrapperUtils.wrap(LocationMapping.class, serviceDefinition.getLocationMappings());
    }

    public _ServiceDefinition getWebServiceObject() {
        return (_ServiceDefinition) webServiceObject;
    }

    @Override
    public Object clone() {
        final _ServiceDefinition me = getWebServiceObject();
        final _ServiceDefinition copy = new _ServiceDefinition();
        copy.setIdentifier(me.getIdentifier());
        copy.setDisplayName(me.getDisplayName());
        copy.setDescription(me.getDescription());
        copy.setRelativePath(me.getRelativePath());
        copy.setRelativeToSetting(me.getRelativeToSetting());
        copy.setServiceType(me.getServiceType());
        copy.setToolId(me.getToolId());

        final _LocationMapping[] myLocationMappings = me.getLocationMappings();
        copy.setLocationMappings(new _LocationMapping[myLocationMappings.length]);
        for (int i = 0; i < myLocationMappings.length; i++) {
            final _LocationMapping myLocationMapping = myLocationMappings[i];
            final _LocationMapping newLocationMapping = new _LocationMapping();
            newLocationMapping.setAccessMappingMoniker(myLocationMapping.getAccessMappingMoniker());
            newLocationMapping.setLocation(myLocationMapping.getLocation());
            copy.getLocationMappings()[i] = newLocationMapping;
        }

        return new ServiceDefinition(copy);
    }

    public LocationMapping getLocationMapping(final AccessMapping accessMapping) {
        Check.notNull(accessMapping, "accessMapping"); //$NON-NLS-1$

        // If this is FullyQualified then look through our location mappings
        if (getRelativeToSetting().toInt() == RelativeToSetting.FULLY_QUALIFIED.toInt()) {
            final LocationMapping[] mappings = getLocationMappings();
            for (int i = 0; i < mappings.length; i++) {
                final LocationMapping mapping = mappings[i];
                if (mapping.getAccessMappingMoniker().equalsIgnoreCase(accessMapping.getMoniker())) {
                    return mapping;
                }
            }
        }

        // We weren't able to find the location for the access mapping. Return
        // null.
        return null;
    }

    public String getServiceType() {
        return getWebServiceObject().getServiceType();
    }

    public String getDisplayName() {
        return getWebServiceObject().getDisplayName();
    }

    public String getDescription() {
        return getWebServiceObject().getDescription();
    }

    public GUID getIdentifier() {
        return serviceIdentifier;
    }

    public String getRelativePath() {
        return getWebServiceObject().getRelativePath();
    }

    public RelativeToSetting getRelativeToSetting() {
        return relativeToSetting;
    }

    public String getToolID() {
        return getWebServiceObject().getToolId();
    }

    public LocationMapping[] getLocationMappings() {
        return locationMappings;
    }

    /**
     * This method is for internal use only.
     */
    public void internalRemoveLocationMappingAt(final int index) {
        Check.isTrue(index >= 0, "index >= 0"); //$NON-NLS-1$
        Check.isTrue(index < locationMappings.length, "index < locationMappings.length"); //$NON-NLS-1$

        int count = 0;
        final LocationMapping[] newMappings = new LocationMapping[locationMappings.length - 1];
        for (int i = 0; i < locationMappings.length; i++) {
            if (i != index) {
                newMappings[count++] = locationMappings[i];
            }
        }

        locationMappings = newMappings;
    }
}
