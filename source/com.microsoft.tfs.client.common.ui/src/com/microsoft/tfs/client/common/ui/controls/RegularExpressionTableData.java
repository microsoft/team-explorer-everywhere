// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls;

import com.microsoft.tfs.util.Check;

/**
 * Holds regular expression strings for a {@link RegularExpressionTable}. Other
 * qualifiers could be added later (case sensitivity, etc.).
 */
public class RegularExpressionTableData {
    private String expression;

    public RegularExpressionTableData(final String expression) {
        Check.notNull(expression, "expression"); //$NON-NLS-1$
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(final String expression) {
        Check.notNull(expression, "expression"); //$NON-NLS-1$
        this.expression = expression;
    }
}
