// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.sdk.samples.snippets;

import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogQueryOptions;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogResource;
import com.microsoft.tfs.core.clients.framework.catalog.CatalogResourceTypes;
import com.microsoft.tfs.util.GUID;

public class EnumerateTeamProjectCollections {
    public static void main(final String[] args) {
        final TFSConfigurationServer configurationServer = SnippetSettings.connectToTFS().getConfigurationServer();

        final GUID[] resourceTypes = new GUID[] {
            CatalogResourceTypes.PROJECT_COLLECTION
        };

        final CatalogResource[] resources =
            configurationServer.getCatalogService().queryResourcesByType(resourceTypes, CatalogQueryOptions.NONE);

        if (resources != null) {
            for (final CatalogResource resource : resources) {
                final String instanceId = resource.getProperties().get("InstanceId"); //$NON-NLS-1$
                final TFSTeamProjectCollection tpc = configurationServer.getTeamProjectCollection(new GUID(instanceId));

                System.out.println("TFSTeamProjectCollection"); //$NON-NLS-1$
                System.out.println("\tName: " + tpc.getName().toString()); //$NON-NLS-1$
                System.out.println("\tURI: " + tpc.getBaseURI()); //$NON-NLS-1$
            }
        }
    }
}
