// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.search;

public interface IVSSearchQuery {
    String getSearchString();

    /**
     * @return a bitfield whose fields are defined by {@link VSSearchParseError}
     */
    int getParseError();

    int getTokens(int maxTokens, IVSSearchToken[] rgpSearchTokens);
}
