// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.search;

public interface IVSSearchFilterToken extends IVSSearchToken {
    String getFilterField();

    String getFilterValue();

    /**
     * @return a bitfield whose fields are defined by
     *         {@link VSSearchFilterTokenType}
     */
    int getFilterTokenType();

    int getFilterSeparatorPosition();
}
