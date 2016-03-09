// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.views.properties.IPropertySource;

import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItem;
import com.microsoft.tfs.client.common.ui.controls.vc.changes.ChangeItemType;
import com.microsoft.tfs.client.eclipse.ui.propertysources.ChangePropertySource;
import com.microsoft.tfs.client.eclipse.ui.propertysources.PendingChangePropertySource;

public class ChangeItemAdapterFactory implements IAdapterFactory {
    @Override
    public Object getAdapter(final Object adaptableObject, final Class adapterType) {
        if (adaptableObject instanceof ChangeItem) {
            final ChangeItem changeItem = (ChangeItem) adaptableObject;
            if (adapterType == IPropertySource.class) {
                if (changeItem.getType() == ChangeItemType.CHANGESET) {
                    return new ChangePropertySource(changeItem.getChange());
                } else {
                    return new PendingChangePropertySource(
                        changeItem.getPendingChange(),
                        changeItem.getPropertyValues());
                }
            }
            if (adapterType == IWorkbenchAdapter.class) {
                return ChangeItemWorkbenchAdapter.INSTANCE;
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

    private static class ChangeItemWorkbenchAdapter implements IWorkbenchAdapter {
        public static final ChangeItemWorkbenchAdapter INSTANCE = new ChangeItemWorkbenchAdapter();

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
            return ((ChangeItem) o).getName();
        }

        @Override
        public Object getParent(final Object o) {
            return null;
        }
    }
}
