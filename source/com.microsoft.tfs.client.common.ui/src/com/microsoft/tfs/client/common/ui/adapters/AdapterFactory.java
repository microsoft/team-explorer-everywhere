// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IActionFilter;
import org.eclipse.ui.PlatformUI;

import com.microsoft.alm.teamfoundation.build.webapi.DefinitionReference;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.productplugin.TFSProductPlugin;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;

public class AdapterFactory implements IAdapterFactory {
    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(final Object adaptableObject, final Class adapterType) {
        if (adaptableObject instanceof QueryDefinition) {
            return new IActionFilter() {
                @Override
                public boolean testAttribute(final Object target, final String name, final String value) {
                    final QueryDefinition queryDefinition = (QueryDefinition) target;
                    final WorkItemClient client = queryDefinition.getProject().getWorkItemClient();

                    if (name.equals("SupportsMyFavorites")) //$NON-NLS-1$
                    {
                        return TeamExplorerHelpers.supportsMyFavorites(client.getConnection());
                    } else if (name.equals("SupportsTeamFavorites")) //$NON-NLS-1$
                    {
                        return !queryDefinition.isPersonal()
                            && TeamExplorerHelpers.supportsTeamFavorites(client.getConnection());
                    }
                    return false;
                }
            };
        }

        if (adaptableObject instanceof IBuildDefinition) {
            return new IActionFilter() {
                @Override
                public boolean testAttribute(final Object target, final String name, final String value) {
                    final IBuildDefinition buildDefinition = (IBuildDefinition) target;
                    final IBuildServer server = buildDefinition.getBuildServer();

                    if (name.equals("SupportsMyFavorites")) //$NON-NLS-1$
                    {
                        return TeamExplorerHelpers.supportsMyFavorites(server.getConnection());
                    } else if (name.equals("SupportsTeamFavorites")) //$NON-NLS-1$
                    {
                        return TeamExplorerHelpers.supportsTeamFavorites(server.getConnection());
                    }
                    return false;
                }
            };
        }

        if (adaptableObject instanceof DefinitionReference) {
            return new IActionFilter() {
                @Override
                public boolean testAttribute(final Object target, final String name, final String value) {
                    final TFSProductPlugin plugin = TFSCommonUIClientPlugin.getDefault().getProductPlugin();
                    final TFSRepository repository = plugin.getRepositoryManager().getDefaultRepository();
                    final TFSTeamProjectCollection connection = (TFSTeamProjectCollection) repository.getConnection();

                    if (name.equals("SupportsMyFavorites")) //$NON-NLS-1$
                    {
                        return TeamExplorerHelpers.supportsMyFavorites(connection);
                    } else if (name.equals("SupportsTeamFavorites")) //$NON-NLS-1$
                    {
                        return TeamExplorerHelpers.supportsTeamFavorites(connection);
                    }
                    return false;
                }
            };
        }

        if (adaptableObject instanceof IBuildDetail) {
            return new IActionFilter() {
                @Override
                public boolean testAttribute(final Object target, final String name, final String value) {
                    if (name.equals("ActiveWorkbenchPart")) //$NON-NLS-1$
                    {
                        final String id =
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite().getId();
                        return id.equals(value);
                    }
                    return false;
                }
            };
        }

        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class[] getAdapterList() {
        return new Class[] {
            IActionFilter.class
        };
    }
}
