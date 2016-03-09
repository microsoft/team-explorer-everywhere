// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.commands.helpers;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

public final class ItemSpecHelper {
    public static final ItemSpec[] getItemSpecs(final GetRequest[] getRequests) {
        Check.notNull(getRequests, "getRequests"); //$NON-NLS-1$

        final ItemSpec[] itemSpecs = new ItemSpec[getRequests.length];

        for (int i = 0; i < getRequests.length; i++) {
            itemSpecs[i] = getRequests[i].getItemSpec();
        }

        return itemSpecs;
    }
}
