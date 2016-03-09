// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.search.internal;

import com.microsoft.tfs.core.search.IVSSearchToken;

public class VSSearchToken implements IVSSearchToken {
    private final String originalTokenText;
    private final int tokenStartPosition;
    private final String parsedTokenText;
    private final int parseError;

    public VSSearchToken(
        final String originalTokenText,
        final int tokenStartPosition,
        final String parsedTokenText,
        final int parseError) {
        this.originalTokenText = originalTokenText;
        this.tokenStartPosition = tokenStartPosition;
        this.parsedTokenText = parsedTokenText;
        this.parseError = parseError;
    }

    @Override
    public String getOriginalTokenText() {
        return originalTokenText;
    }

    @Override
    public int getTokenStartPosition() {
        return tokenStartPosition;
    }

    @Override
    public String getParsedTokenText() {
        return parsedTokenText;
    }

    @Override
    public int getParseError() {
        return parseError;
    }
}
