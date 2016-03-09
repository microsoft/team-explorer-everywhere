// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkType;

public class RegisteredLinkTypeImpl implements RegisteredLinkType {
    private final String name;

    public RegisteredLinkTypeImpl(final String nameInput) {
        if (nameInput == null || nameInput.trim().length() == 0) {
            throw new IllegalArgumentException("name must not be null or empty"); //$NON-NLS-1$
        }
        name = nameInput.trim();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RegisteredLinkTypeImpl) {
            final RegisteredLinkTypeImpl other = (RegisteredLinkTypeImpl) obj;
            return name.equals(other.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }
}
