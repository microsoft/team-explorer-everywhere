// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.location;

import ms.ws._AccessMapping;

/**
 * Peer object to the {@link _AccessMapping} proxy data object.
 *
 * @since TEE-SDK-10.1
 */
public class AccessMapping {
    private final String moniker;
    private String displayName;
    private String accessPoint;

    /**
     * Construct from specified parameters.
     */
    public AccessMapping(final String theMoniker, final String theDisplayName, final String theAccessPoint) {
        moniker = theMoniker;
        displayName = theDisplayName;
        accessPoint = theAccessPoint;
    }

    /**
     * Construct from a proxy object.
     */
    public AccessMapping(final _AccessMapping accessMapping) {
        moniker = accessMapping.getMoniker();
        displayName = accessMapping.getDisplayName();
        accessPoint = accessMapping.getAccessPoint();
    }

    /**
     * Returns the prefix for this access mapping's access point.
     */
    public String getAccessPoint() {
        return accessPoint;
    }

    /**
     * Sets the prefix for this access mapping's access point.
     */
    public void setAccessPoint(final String value) {
        accessPoint = value;
    }

    /**
     * Returns the display name for the for the access mapping.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name for the for the access mapping.
     */
    public void setDisplayName(final String value) {
        displayName = value;
    }

    /**
     * This is the unique moniker that represents the access mapping.
     */
    public String getMoniker() {
        return moniker;
    }
}
