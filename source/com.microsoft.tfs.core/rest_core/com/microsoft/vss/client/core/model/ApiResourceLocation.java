// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.tfs.util.StringHelpers;
import com.microsoft.vss.client.core.model.ApiResourceVersion.Version;

public class ApiResourceLocation {
    private UUID id;
    private String area;
    private String resourceName;
    private String routeTemplate;
    private String routeName;
    private int resourceVersion;
    private Version minVersion = new Version(1, 0);
    private Version maxVersion = new Version(1, 0);
    private Version releasedVersion = new Version(0, 0);

    @JsonProperty
    public UUID getId() {
        return id;
    }

    @JsonProperty
    public void setId(final UUID id) {
        this.id = id;
    }

    @JsonProperty
    public String getArea() {
        return area;
    }

    @JsonProperty
    public void setArea(final String area) {
        this.area = area;
    }

    @JsonProperty
    public String getResourceName() {
        return resourceName;
    }

    @JsonProperty
    public void setResourceName(final String resourceName) {
        this.resourceName = resourceName;
    }

    @JsonProperty
    public String getRouteTemplate() {
        return routeTemplate;
    }

    @JsonProperty
    public void setRouteTemplate(final String routeTemplate) {
        this.routeTemplate = routeTemplate;
    }

    @JsonProperty
    public String getRouteName() {
        return routeName;
    }

    @JsonProperty
    public void setRouteName(final String routeName) {
        this.routeName = routeName;
    }

    @JsonProperty
    public int getResourceVersion() {
        return resourceVersion;
    }

    @JsonProperty
    public void setResourceVersion(final int resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    @JsonProperty("minVersion")
    public String getMinVersionString() {
        return minVersion.toString();
    }

    @JsonProperty("minVersion")
    public void setMinVersionString(final String minVersion) {
        if (StringHelpers.isNullOrEmpty(minVersion)) {
            this.minVersion = new Version(1, 0);
        } else {
            this.minVersion = new Version(minVersion);
        }
    }

    public Version getMinVersion() {
        return minVersion;
    }

    @JsonProperty("maxVersion")
    public String getMaxVersionString() {
        return maxVersion.toString();
    }

    @JsonProperty("maxVersion")
    public void setMaxVersionString(final String maxVersion) {
        if (StringHelpers.isNullOrEmpty(maxVersion)) {
            this.maxVersion = new Version(1, 0);
        } else {
            this.maxVersion = new Version(maxVersion);
        }
    }

    public Version getMaxVersion() {
        return maxVersion;
    }

    @JsonProperty("releasedVersion")
    public String getReleasedVersionString() {
        return releasedVersion.toString();
    }

    @JsonProperty("releasedVersion")
    public void setReleasedVersionString(final String releasedVersion) {
        if (StringHelpers.isNullOrEmpty(releasedVersion)) {
            this.releasedVersion = new Version(1, 0);
        } else {
            this.releasedVersion = new Version(releasedVersion);
        }
    }

    public Version getReleasedVersion() {
        return releasedVersion;
    }
}
