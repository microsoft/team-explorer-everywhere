// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.vc;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.util.Check;

/**
 * Extends {@link ItemSpec} to contain an {@link ItemType}.
 *
 * @threadsafety thread-compatible
 */
public class TypedItemSpec extends ItemSpec {
    private final ItemType type;

    public TypedItemSpec(final String item, final RecursionType recurse, final int did, final ItemType type) {
        super(item, recurse, did);

        Check.notNull(type, "type"); //$NON-NLS-1$
        Check.isTrue(type != ItemType.ANY, "type must not be ItemType.ANY"); //$NON-NLS-1$

        this.type = type;
    }

    public TypedItemSpec(final String item, final RecursionType recurse, final ItemType type) {
        this(item, recurse, 0, type);
    }

    /**
     * @return the type of this item (never <code>null</code>)
     */
    public ItemType getType() {
        return type;
    }
}
