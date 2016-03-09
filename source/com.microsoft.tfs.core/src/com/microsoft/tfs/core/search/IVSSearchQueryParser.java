// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.search;

public interface IVSSearchQueryParser {
    IVSSearchQuery parse(String searchString);

    String buildSearchString(IVSSearchQuery searchQuery);

    String buildSearchStringFromTokens(int tokens, IVSSearchToken[] searchTokens);

    IVSSearchToken getSearchToken(String tokenText);

    /**
     * @param filterTokenType
     *        one of the values defined by {@link VSSearchFilterTokenType}
     */
    IVSSearchFilterToken getSearchFilterToken(String filterField, String filterValue, int filterTokenType);
}
