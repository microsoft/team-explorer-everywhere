// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;

import com.microsoft.tfs.client.eclipse.ui.propertysources.ShelvesetPropertySource;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;

public class ShelvesetAdapterFactory implements IAdapterFactory {
    @Override
    public Object getAdapter(final Object adaptableObject, final Class adapterType) {
        if (adaptableObject instanceof Shelveset) {
            final Shelveset shelveset = (Shelveset) adaptableObject;
            if (adapterType == IPropertySource.class) {
                return new ShelvesetPropertySource(shelveset);
            }
            if (adapterType == IWorkbenchAdapter.class) {
                return ShelvesetWorkbenchAdapter.INSTANCE;
            }
        }

        return null;
    }

    @Override
    public Class[] getAdapterList() {
        return new Class[] {
            IPropertySource.class,
            IWorkbenchAdapter.class
        };
    }

    private static class ShelvesetWorkbenchAdapter implements IWorkbenchAdapter {
        public static final ShelvesetWorkbenchAdapter INSTANCE = new ShelvesetWorkbenchAdapter();

        @Override
        public Object[] getChildren(final Object o) {
            return new Object[0];
        }

        @Override
        public ImageDescriptor getImageDescriptor(final Object object) {
            return null;
        }

        @Override
        public String getLabel(final Object o) {
            return ((Shelveset) o).getName();
        }

        @Override
        public Object getParent(final Object o) {
            return null;
        }
    }
}
