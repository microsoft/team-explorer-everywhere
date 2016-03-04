// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiResourceLocationCollection {

    private List<ApiResourceLocation> locations;
    private Map<UUID, ApiResourceLocation> locationsById;

    // @SuppressWarnings("unused")
    private int count;

    @JsonProperty("value")
    public List<ApiResourceLocation> getLocations() {
        return locations;
    }

    @JsonProperty("value")
    public void setLocations(final List<ApiResourceLocation> locations) {
        this.locations = locations;

        locationsById = new HashMap<UUID, ApiResourceLocation>();

        for (final ApiResourceLocation location : locations) {
            locationsById.put(location.getId(), location);
        }
    }

    public int getCount() {
        return count;
    }

    @JsonProperty("count")
    public void setCount(final int count) {
        this.count = count;
    }

    public int size() {
        return locations.size();
    }

    public ApiResourceLocation getLocationById(final UUID locationId) {
        return locationsById.get(locationId);
    }
}
