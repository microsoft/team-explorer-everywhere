// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.adapters;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.views.properties.IPropertySource;

import com.microsoft.tfs.client.eclipse.ui.propertysources.ChangesetPropertySource;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;

public class ChangesetAdapterFactory implements IAdapterFactory {
    @Override
    public Object getAdapter(final Object adaptableObject, final Class adapterType) {
        if (adaptableObject instanceof Changeset) {
            final Changeset changeset = (Changeset) adaptableObject;
            if (adapterType == IPropertySource.class) {
                return new ChangesetPropertySource(changeset);
            }
        }

        return null;
    }

    @Override
    public Class[] getAdapterList() {
        return new Class[] {
            IPropertySource.class
        };
    }

}
