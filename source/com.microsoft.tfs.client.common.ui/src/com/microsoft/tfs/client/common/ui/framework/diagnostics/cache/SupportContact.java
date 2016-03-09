// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.diagnostics.cache;

public class SupportContact implements Comparable {
    private final String id;
    private final String label;
    private final String value;
    private final String url;
    private final String description;
    private final boolean launchable;
    private final SupportContactCategory category;

    public SupportContact(
        final String id,
        final String label,
        final String value,
        final String url,
        final String description,
        final boolean launchable,
        final SupportContactCategory category) {
        this.id = id;
        this.label = label;
        this.value = value;
        this.url = url;
        this.description = description;
        this.launchable = launchable;
        this.category = category;
    }

    @Override
    public int compareTo(final Object o) {
        final SupportContact other = (SupportContact) o;
        int c = category.compareTo(other.category);
        if (c == 0) {
            c = label.compareToIgnoreCase(other.label);
        }
        return c;
    }

    public SupportContactCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getID() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public boolean isLaunchable() {
        return launchable;
    }

    public String getURL() {
        return url;
    }

    public String getValue() {
        return value;
    }
}
