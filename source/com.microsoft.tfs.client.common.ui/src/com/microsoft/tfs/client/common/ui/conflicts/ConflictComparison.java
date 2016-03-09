// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.conflicts;

import com.microsoft.tfs.util.Check;

public class ConflictComparison {
    private final ConflictComparisonOption[] options;
    private final ConflictComparisonDescription[] descriptions;

    public ConflictComparison(final ConflictComparisonOption[] options) {
        this(options, new ConflictComparisonDescription[0]);
    }

    public ConflictComparison(
        final ConflictComparisonOption[] options,
        final ConflictComparisonDescription[] descriptions) {
        Check.notNull(options, "options"); //$NON-NLS-1$
        Check.notNull(descriptions, "descriptions"); //$NON-NLS-1$

        this.options = options;
        this.descriptions = descriptions;
    }

    public ConflictComparisonOption[] getOptions() {
        return options;
    }

    public ConflictComparisonDescription[] getDescriptions() {
        return descriptions;
    }

}
