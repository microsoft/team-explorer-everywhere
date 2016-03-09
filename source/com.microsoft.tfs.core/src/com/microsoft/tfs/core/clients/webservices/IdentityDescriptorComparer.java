// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.webservices;

import java.util.Comparator;

/**
 * Class used for comparing IdentityDescriptors
 */
public class IdentityDescriptorComparer implements Comparator<IdentityDescriptor> {
    public static final IdentityDescriptorComparer INSTANCE = new IdentityDescriptorComparer();

    private IdentityDescriptorComparer() {
    }

    @Override
    public int compare(final IdentityDescriptor x, final IdentityDescriptor y) {
        if (x == y) {
            return 0;
        }
        if (x == null && y != null) {
            return -1;
        }
        if (x != null && y == null) {
            return 1;
        }

        int retValue = 0;
        if ((retValue = x.getIdentifier().compareToIgnoreCase(y.getIdentifier())) != 0) {
            return retValue;
        }

        return x.getIdentityType().compareToIgnoreCase(y.getIdentityType());
    }
}