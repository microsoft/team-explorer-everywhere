// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.search.internal;

import com.microsoft.tfs.core.search.IVSSearchFilterToken;

public class VSSearchFilterToken extends VSSearchToken implements IVSSearchFilterToken {
    private final String filterField;
    private final String filterValue;
    private final int filterSeparatorPosition;
    private final int filterTokenType;

    public VSSearchFilterToken(
        final String originalTokenText,
        final int tokenStartPosition,
        final String parsedTokenText,
        final String filterField,
        final String filterValue,
        final int filterTokenType,
        final int filterSeparatorPosition,
        final int parseError) {
        super(originalTokenText, tokenStartPosition, parsedTokenText, parseError);

        this.filterField = filterField;
        this.filterValue = filterValue;
        this.filterSeparatorPosition = filterSeparatorPosition;
        this.filterTokenType = filterTokenType;
    }

    @Override
    public String getFilterField() {
        return filterField;
    }

    @Override
    public String getFilterValue() {
        return filterValue;
    }

    @Override
    public int getFilterSeparatorPosition() {
        return filterSeparatorPosition;
    }

    @Override
    public int getFilterTokenType() {
        return filterTokenType;
    }
}