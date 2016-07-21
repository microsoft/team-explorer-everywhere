// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IActionFilter;

import com.microsoft.alm.teamfoundation.build.webapi.DefinitionType;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.favorites.BuildFavoriteItem;

public class BuildFavoriteAdapterFactory implements IAdapterFactory {
    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(final Object adaptableObject, final Class adapterType) {
        if (adaptableObject instanceof BuildFavoriteItem) {
            return new IActionFilter() {
                @Override
                public boolean testAttribute(final Object target, final String name, final String value) {
                    final BuildFavoriteItem favorite = (BuildFavoriteItem) target;

                    if (name.equals("XAML")) //$NON-NLS-1$
                    {
                        return favorite.getBuildDefinitionType() == DefinitionType.XAML;
                    } else if (name.equals("BUILD")) //$NON-NLS-1$
                    {
                        return favorite.getBuildDefinitionType() == DefinitionType.BUILD;
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
