// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.compare;

import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

public class CompareSaveEvent {
    private final CompareSaveNode node;
    private final ISaveableCompareElement element;

    public CompareSaveEvent(final CompareSaveNode node, final ISaveableCompareElement element) {
        Check.notNull(node, "node"); //$NON-NLS-1$
        Check.notNull(element, "element"); //$NON-NLS-1$

        this.node = node;
        this.element = element;
    }

    public CompareSaveNode getNode() {
        return node;
    }

    public ISaveableCompareElement getElement() {
        return element;
    }

    public static final class CompareSaveNode extends TypesafeEnum {
        public static CompareSaveNode ANCESTOR = new CompareSaveNode(0);
        public static CompareSaveNode ORIGINAL = new CompareSaveNode(1);
        public static CompareSaveNode MODIFIED = new CompareSaveNode(2);
        public static CompareSaveNode UNKNOWN = new CompareSaveNode(3);

        private CompareSaveNode(final int id) {
            super(id);
        }
    }
}
