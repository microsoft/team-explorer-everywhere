// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.util.Check;

/**
 * Encapsulates a set of properties associated with a path.
 *
 * @threadsafety thread-compatible
 * @since TFS-SDK-11.0
 */
public class ItemProperties {
    private String path;
    private PropertyValue[] properties;

    public ItemProperties(final String path, final PropertyValue[] properties) {
        super();

        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(properties, "properties"); //$NON-NLS-1$

        this.path = path;
        this.properties = properties;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public PropertyValue[] getProperties() {
        return properties;
    }

    public void setProperties(final PropertyValue[] properties) {
        this.properties = properties;
    }

    /**
     * Returns an array of new {@link ItemProperties} objects, one for each
     * given path, each containing all the specified properties.
     *
     * @param paths
     *        the paths to create an {@link ItemProperties} object for (must not
     *        be <code>null</code>)
     * @param properties
     *        the properties to set on each path's {@link ItemProperties} object
     *        (must not be <code>null</code>)
     * @return an array of {@link ItemProperties} (never <code>null</code>)
     */
    public static ItemProperties[] fromStrings(final String[] paths, final PropertyValue[] properties) {
        Check.notNull(paths, "paths"); //$NON-NLS-1$
        Check.notNull(properties, "properties"); //$NON-NLS-1$

        final ItemProperties[] ret = new ItemProperties[paths.length];

        for (int i = 0; i < paths.length; i++) {
            ret[i] = new ItemProperties(paths[i], properties);
        }

        return ret;
    }
}
