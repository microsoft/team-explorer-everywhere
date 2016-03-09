// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.internal.localworkspace;

import com.microsoft.tfs.util.BitField;

public class ParsedItemSpecOptions extends BitField {
    public static final ParsedItemSpecOptions NONE = new ParsedItemSpecOptions(0);
    public static final ParsedItemSpecOptions INCLUDE_DELETED = new ParsedItemSpecOptions(1);

    private ParsedItemSpecOptions(final int value) {
        super(value);
    }

    public boolean contains(final ParsedItemSpecOptions other) {
        return containsInternal(other);
    }
}
