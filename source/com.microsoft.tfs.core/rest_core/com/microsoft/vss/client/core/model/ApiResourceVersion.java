// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.vss.client.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.tfs.util.StringHelpers;
import com.microsoft.vss.client.core.utils.StringUtil;

public class ApiResourceVersion {
    private final static String PREVIEW_STAGE_NAME = "preview"; //$NON-NLS-1$

    private Version apiVersion;
    private int resourceVersion;
    private boolean isPreview;

    public ApiResourceVersion() {
        this(new Version(1, 0));
    }

    public ApiResourceVersion(final Version apiVersion) {
        this(apiVersion, 0);
    }

    public ApiResourceVersion(final Version apiVersion, final int resourceVersion) {
        this.apiVersion = apiVersion;
        this.resourceVersion = resourceVersion;
        this.isPreview = resourceVersion > 0;
    }

    public ApiResourceVersion(final String apiResourceVersionString) {
        toVersion(apiResourceVersionString);
    }

    @JsonProperty("ApiVersion")
    public String getApiVersionString() {
        return apiVersion.toString();
    }

    @JsonProperty("ApiVersion")
    public void setApiVersionString(final String apiVersionString) {
        if (StringHelpers.isNullOrEmpty(apiVersionString)) {
            this.apiVersion = new Version(1, 0);
        } else {
            this.apiVersion = new Version(apiVersionString);
        }
    }

    public Version getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(final Version apiVersion) {
        this.apiVersion = apiVersion;
    }

    /**
     * Internal resource version. This is defined per-resource and is used to
     * support build-to-build compatibility of API changes within a given
     * (in-preview) public api version. For example, within the TFS 1.0 API
     * release cycle, while it is still in preview, a resource's data structure
     * may be changed. This resource can be versioned such that older clients
     * will still work (requests will be sent to the older version) and
     * new/upgraded clients will talk to the new version of the resource.
     */
    @JsonProperty
    public int getResourceVersion() {
        return resourceVersion;
    }

    @JsonProperty
    public void setResourceVersion(final int resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    /**
     * Is the public API version in preview
     */
    @JsonProperty("IsPreview")
    public boolean isPreview() {
        return this.isPreview;
    }

    @JsonProperty("IsPreview")
    public void setPreview(final boolean isPreview) {
        this.isPreview = isPreview;
    }

    /**
     * Returns the version string in the form:
     * {ApiMajor}.{ApiMinor}[-{stage}[.{resourceVersion}]]
     */
    @Override
    public String toString() {
        final StringBuilder sbVersion = new StringBuilder(apiVersion.toString());

        if (isPreview) {
            sbVersion.append('-');
            sbVersion.append(PREVIEW_STAGE_NAME);

            if (resourceVersion > 0) {
                sbVersion.append('.');
                sbVersion.append(resourceVersion);
            }
        }

        return sbVersion.toString();
    }

    private void toVersion(final String apiResourceVersionString) {
        if (StringUtil.isNullOrEmpty(apiResourceVersionString)) {
            throw new IllegalArgumentException("ApiVersion: is null or empty"); //$NON-NLS-1$
        }

        // Check for a stage/resourceVersion string
        final String[] apiResourceVersionParts = apiResourceVersionString.split("-"); //$NON-NLS-1$

        if (apiResourceVersionParts.length == 2) {
            if (!tryParsePreview(apiResourceVersionParts[1])) {
                throw new IllegalArgumentException("ApiVersion: " + apiResourceVersionString); //$NON-NLS-1$
            }
        }

        if (!tryParseVersion(apiResourceVersionParts[0])) {
            throw new IllegalArgumentException("ApiVersion: " + apiResourceVersionString); //$NON-NLS-1$
        }
    }

    private boolean tryParseVersion(final String apiVersion) {
        try {
            this.apiVersion = new Version(apiVersion);
        } catch (final NumberFormatException e) {
            return false;
        }

        return true;
    }

    private boolean tryParsePreview(final String previewVersion) {
        final String[] previewParts = previewVersion.split("\\."); //$NON-NLS-1$

        if (previewParts.length == 2) {
            try {
                this.resourceVersion = Integer.parseInt(previewParts[1]);
            } catch (final NumberFormatException e) {
                return false;
            }
        }

        if (previewParts.length > 0 && PREVIEW_STAGE_NAME.equalsIgnoreCase(previewParts[0])) {
            this.isPreview = true;
            return true;
        }

        return false;
    }

    public static class Version implements Comparable<Version> {
        final int major;
        final int minor;

        public Version(final int major) {
            this(major, 0);
        }

        public Version(final int major, final int minor) {
            this.major = major;
            this.minor = minor;
        }

        public Version(final String version) {
            String sMajor;
            String sMinor;

            final int n = version.indexOf('.');

            if (n < 0) {
                sMajor = version;
                sMinor = null;
            } else {
                sMajor = version.substring(0, n);
                sMinor = version.substring(n + 1);
            }

            if (StringHelpers.isNullOrEmpty(sMajor)) {
                major = 0;
            } else {
                major = Integer.parseInt(sMajor);
            }

            if (StringHelpers.isNullOrEmpty(sMinor)) {
                minor = 0;
            } else {
                minor = Integer.parseInt(sMinor);
            }
        }

        public int getMajor() {
            return major;
        }

        public int getMinor() {
            return minor;
        }

        @Override
        public int compareTo(final Version v) {
            if (this == null) {
                return -1;
            }

            if (v == null) {
                return 1;
            }

            if (major < v.getMajor()) {
                return -1;
            }

            if (major > v.getMajor()) {
                return 1;
            }

            if (minor < v.getMinor()) {
                return -1;
            }

            if (minor > v.getMinor()) {
                return 1;
            }

            return 0;
        }

        @Override
        public String toString() {
            return String.valueOf(major) + "." + String.valueOf(minor); //$NON-NLS-1$
        }
    }
}
