// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.search.internal;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.core.search.IVSSearchQuery;
import com.microsoft.tfs.core.search.IVSSearchToken;
import com.microsoft.tfs.core.search.VSSearchParseError;
import com.microsoft.tfs.core.search.internal.VSSearchQueryParser.TokenFoundCallback;

/**
 * A search query object as returned by the search query parser
 */
public class VSSearchQuery implements IVSSearchQuery {
    private final List<IVSSearchToken> searchTokens = new ArrayList<IVSSearchToken>();
    private final String searchString;
    private int parseError;

    /**
     * The class does lazy parsing of the search string. This hold whether the
     * search string was parsed yet (on first access of the tokens collection or
     * the ParseError members)
     */
    private boolean stringParsed;

    public VSSearchQuery(final String searchString) {
        this.searchString = searchString;
        this.parseError = VSSearchParseError.NONE;
    }

    public boolean isStringParsed() {
        return stringParsed;
    }

    public void setStringParsed(final boolean stringParsed) {
        this.stringParsed = stringParsed;
    }

    @Override
    public String getSearchString() {
        return searchString;
    }

    @Override
    public int getParseError() {
        // Make sure the string is parsed
        ensureSearchStringParsed();
        return parseError;
    }

    public void setParseError(final int parseError) {
        this.parseError = parseError;
    }

    @Override
    public int getTokens(final int maxTokens, final IVSSearchToken[] rgpSearchTokens) {
        // Make sure the string is parsed
        ensureSearchStringParsed();

        // Return the token count if we are not called to return actual tokens
        if (rgpSearchTokens == null) {
            // Asking explicitly for tokens count but providing no space will
            // result in exception
            if (maxTokens > 0) {
                throw new IllegalArgumentException("maxTokens must be 0 when rgpSearchTokens is null"); //$NON-NLS-1$
            }

            return searchTokens.size();
        }

        // Asking for more elements that we have space provided to store will
        // result in exception
        if (maxTokens > rgpSearchTokens.length) {
            throw new IllegalArgumentException("maxTokens must not be greater than rgpSearchTokens.length"); //$NON-NLS-1$
        }

        // Otherwise return the requested tokens (or as many as we have
        // available)
        final int tokensReturned = Math.min(maxTokens, searchTokens.size());
        for (int i = 0; i < tokensReturned; i++) {
            rgpSearchTokens[i] = searchTokens.get(i);
        }
        return tokensReturned;
    }

    private void ensureSearchStringParsed() {
        // If we already parsed the string, there is nothing else to do
        if (isStringParsed()) {
            return;
        }

        // It's now time to do lazy parsing of the string
        // Call the parser to parse the string and return the tokens found.
        VSSearchQueryParser.parseSearchString(searchString, new TokenFoundCallback() {
            @Override
            public void call(final IVSSearchToken searchToken) {
                // Add the token to our list
                searchTokens.add(searchToken);
                // And update the global parse errors for this token, too
                parseError |= searchToken.getParseError();
            }
        });

        // Mark that we parsed the string
        setStringParsed(true);
    }
}