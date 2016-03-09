// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.framework.configuration.internal;

import com.microsoft.tfs.core.clients.framework.catalog.CatalogNode;
import com.microsoft.tfs.core.clients.framework.configuration.TFSEntityDataProvider;
import com.microsoft.tfs.core.clients.framework.location.ServiceDefinition;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public class TFSCatalogEntityDataProvider implements TFSEntityDataProvider {
    private final CatalogNode node;

    public TFSCatalogEntityDataProvider(final CatalogNode node) {
        Check.notNull(node, "node"); //$NON-NLS-1$

        this.node = node;
    }

    CatalogNode getCatalogNode() {
        return node;
    }

    @Override
    public GUID getResourceTypeID() {
        return new GUID(node.getResource().getResourceTypeIdentifier());
    }

    @Override
    public String getDisplayName() {
        return node.getResource().getDisplayName();
    }

    @Override
    public String getDescription() {
        return node.getResource().getDescription();
    }

    @Override
    public String getProperty(final String propertyName) {
        return node.getResource().getProperties().get(propertyName);
    }

    @Override
    public ServiceDefinition getServiceReference(final String serviceName) {
        return node.getResource().getServiceReferences().get(serviceName);
    }
}
