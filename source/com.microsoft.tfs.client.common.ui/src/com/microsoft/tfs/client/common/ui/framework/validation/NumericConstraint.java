// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.validation;

public interface NumericConstraint {
    public static NumericConstraint EXACTLY_ONE = new NumericConstraint() {
        @Override
        public boolean passes(final int size) {
            return size == 1;
        }
    };

    public static NumericConstraint ONE_OR_MORE = new NumericConstraint() {
        @Override
        public boolean passes(final int size) {
            return size > 0;
        }
    };

    public boolean passes(int size);
}
