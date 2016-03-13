// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.search.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.microsoft.tfs.core.search.IVSSearchFilterToken;
import com.microsoft.tfs.core.search.IVSSearchQuery;
import com.microsoft.tfs.core.search.IVSSearchQueryParser;
import com.microsoft.tfs.core.search.IVSSearchToken;
import com.microsoft.tfs.core.search.VSSearchFilterTokenType;
import com.microsoft.tfs.core.search.VSSearchParseError;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.StringUtil;

public class VSSearchQueryParser implements IVSSearchQueryParser {
    public interface TokenFoundCallback {
        void call(IVSSearchToken token);
    }

    private enum AddQuotesMode {
        DEFAULT, IF_EMPTY, ALWAYS
    };

    /**
     * Characters used by the parser's syntax
     */
    private static class SyntaxCharacters {
        // Parser token delimiter, when used outside quotes
        public static final char SPACE_SEPARATOR = ' ';
        // Parser token delimiter, use it to include spaces in the tokens. E.g.
        // these are single tokens: "alpha beta", "Date:last week"
        public static final char QUOTE = '"';
        // Escape character for all parser syntax character. E.g. "\"a\"quoted"
        public static final char ESCAPE = '\\';
        // Filter field delimiter. E.g. "Date:yesterday", "AssignedTo:alin"
        // (matches anything containing alin), etc
        public static final char PARTIAL_MATCH_FILTER_SEPARATOR = ':';
        // Exact/Word-match filter field delimiter. E.g. "AssignedTo:alin" to
        // match Alin but not Alina
        public static final char EXACT_MATCH_FILTER_SEPARATOR = '=';
        // Excludsion filter token, prefixes a filter. E.g. "bugs -A:alin" to
        // search for bugs not assigned to alin, etc
        public static final char EXCLUDE_FILTER = '-';
    }

    @Override
    public IVSSearchQuery parse(final String searchString) {
        Check.notNull(searchString, "searchString"); //$NON-NLS-1$

        // Initialize a query object to do lazy parsing of the search string
        // Tolerate whitespaces or empty strings, we'll just return empty tokens
        // collections

        return new VSSearchQuery(searchString);
    }

    @Override
    public String buildSearchString(final IVSSearchQuery searchQuery) {
        Check.notNull(searchQuery, "searchQuery"); //$NON-NLS-1$

        // Get the tokens count
        final int tokensCount = searchQuery.getTokens(0, null);
        // Get the tokens
        final IVSSearchToken[] tokens = new IVSSearchToken[tokensCount];
        searchQuery.getTokens(tokensCount, tokens);

        return buildSearchStringFromTokens(tokensCount, tokens);
    }

    @Override
    public String buildSearchStringFromTokens(final int tokens, final IVSSearchToken[] searchTokens) {
        Check.notNull(searchTokens, "searchTokens"); //$NON-NLS-1$

        final List<String> tokensTexts = new ArrayList<String>(tokens);
        for (final IVSSearchToken token : searchTokens) {
            tokensTexts.add(VSSearchQueryParser.buildTokenText(token));
        }

        // Assemble the search string from the tokens
        return StringUtil.join(
            tokensTexts.toArray(new String[tokensTexts.size()]),
            Character.toString(VSSearchQueryParser.SyntaxCharacters.SPACE_SEPARATOR));
    }

    @Override
    public IVSSearchToken getSearchToken(final String tokenText) {
        Check.notNull(tokenText, "tokenText"); //$NON-NLS-1$

        final String escapedTokenText = VSSearchQueryParser.escapeString(tokenText, AddQuotesMode.IF_EMPTY);
        return new VSSearchToken(escapedTokenText, 0 /* tokenStart */, tokenText, VSSearchParseError.NONE);
    }

    @Override
    public IVSSearchFilterToken getSearchFilterToken(
        final String filterField,
        final String filterValue,
        final int filterTokenType) {
        Check.notNull(filterField, "filterField"); //$NON-NLS-1$
        Check.notNull(filterValue, "filterValue"); //$NON-NLS-1$

        final String filterTokenText = buildFilterTokenText(
            filterField,
            AddQuotesMode.IF_EMPTY,
            filterValue,
            AddQuotesMode.ALWAYS,
            filterTokenType);

        final int filterSeparator = findFilterSeparator(filterTokenText);
        final AtomicBoolean invalidEscape = new AtomicBoolean();
        final String unescapedTokenText = VSSearchQueryParser.unescapeString(filterTokenText, invalidEscape);

        return new VSSearchFilterToken(
            filterTokenText,
            0 /* tokenStart */,
            unescapedTokenText,
            filterField,
            filterValue,
            filterTokenType,
            filterSeparator,
            VSSearchParseError.NONE);
    }

    /**
     * Returns whether the specified character is a quote
     *
     * @param category
     *        the unicode category ({@link Character#getType(char)})
     */
    public static boolean isQuote(final char ch, final int category) {
        // @formatter:off
        // Notes: a list of usual quotes characters is http://en.wikipedia.org/wiki/Angle_quotes
        // We'll mainly use for quote the " character, whose category is OtherPunctuation
        // We'll also use Initial/FinalQuuotePunctuation with the same meaning, without making any distinction between start/end (which may be reversed in some languages as German)
        // This may not work well for all languages (e.g. Romanian start quote is seen as OpenPunctuation, and also Ja/Ch/Kr quotes are seen as Open/ClosePunctuation) so we have to explicitly check other characters as well
        // U+00AB, U+00BB - De/Ro (these have the right Initial/FinalQuotePunctuation category)
        // U+2039, U+203A  - De (these have the right Initial/FinalQuotePunctuation category)
        // U+2018 – U+201B - Single quote
        // U+201C – U+201E - Double quote
        // also tolerate U+201B and U+201F, they seem in the same ballpark as single and double quotes
        // U+FE41 - U+FE44 - (Ch)
        // U+300C - U+300F - (Ja/Ch/Kr)
        // U+FF02, U+FF07 - fullwidth apostrophe and quotation mark
        // @formatter:on

        return ch == VSSearchQueryParser.SyntaxCharacters.QUOTE
            || category == Character.INITIAL_QUOTE_PUNCTUATION
            || category == Character.FINAL_QUOTE_PUNCTUATION
            || (ch == 0xFF02)
            || (ch == 0xFF07)
            || (ch >= 0x2018 && ch <= 0x201F)
            || (ch >= 0x300C && ch <= 0x300F)
            || (ch >= 0xFE41 && ch <= 0xFE44);
    }

    /**
     * Returns whether the specified character is a space token delimiter
     */
    public static boolean isSpace(final char ch, final int category) {
        return ch == VSSearchQueryParser.SyntaxCharacters.SPACE_SEPARATOR || category == Character.SPACE_SEPARATOR;
    }

    /**
     * Returns whether the specified character is an escape character
     */
    public static boolean isEscape(final char ch) {
        return ch == VSSearchQueryParser.SyntaxCharacters.ESCAPE;
    }

    /**
     * Returns whether the specified character is a filter exclusion character
     */
    public static boolean isExcludeFilter(final char ch) {
        return ch == VSSearchQueryParser.SyntaxCharacters.EXCLUDE_FILTER;
    }

    /**
     * Returns whether the specified character is a filter separator character
     */
    public static boolean isFilterSeparator(final char ch) {
        return ch == VSSearchQueryParser.SyntaxCharacters.PARTIAL_MATCH_FILTER_SEPARATOR
            || ch == VSSearchQueryParser.SyntaxCharacters.EXACT_MATCH_FILTER_SEPARATOR;
    }

    /**
     * Parse a search string and invoke the callback for each token found
     *
     * @param searchString
     *        String to parse
     * @param tokenFoundCallback
     *        Callback function to call for each token found
     */
    public static void parseSearchString(final String searchString, final TokenFoundCallback tokenFoundCallback) {
        // First pass: Separate tokens based on spaces outside of quotes
        // Second pass: remove unescaped quotes
        // Third pass: Search for unescaped : and split string where left=name
        // and right=value
        // Fourth: Unescape escaped characters

        // This function does the first and second logical passes in the loop
        // below (in the same time) and hands off the third and fourth passes to
        // ParseTokenString
        int tokenStart = -1;
        boolean quoteMode = false;
        final int stringLength = searchString.length();
        final StringBuilder unquotedToken = new StringBuilder(stringLength);

        for (int i = 0; i < stringLength; i++) {
            final char ch = searchString.charAt(i);
            final int category = Character.getType(ch);

            if (VSSearchQueryParser.isSpace(ch, category)) {
                // Skip spaces in quotes
                if (quoteMode) {
                    unquotedToken.append(ch);
                    continue;
                }

                // Space outside quotes. If we haven't started yet a token,
                // ignore it.
                if (tokenStart == -1) {
                    continue;
                }

                // Found the end of a token.
                parseTokenString(
                    searchString,
                    tokenStart,
                    i - tokenStart,
                    unquotedToken.toString(),
                    quoteMode,
                    tokenFoundCallback);

                // Prepare for next token
                tokenStart = -1;
                unquotedToken.setLength(0);
            } else {
                // Non-space. Remember this as token start if necessary.
                if (tokenStart == -1) {
                    tokenStart = i;
                }

                if (VSSearchQueryParser.isQuote(ch, category)) {
                    quoteMode = !quoteMode;
                    continue;
                }

                // This character will be present in the unquoted token, too.
                unquotedToken.append(ch);

                if (VSSearchQueryParser.isEscape(ch)) {
                    // Also add next character in the unquoted token if such
                    // character exist
                    if (i + 1 < stringLength) {
                        unquotedToken.append(searchString.charAt(++i));
                    }
                }
            }
        }

        // Add the last token if we have any
        if (tokenStart != -1) {
            parseTokenString(
                searchString,
                tokenStart,
                stringLength - tokenStart,
                unquotedToken.toString(),
                quoteMode,
                tokenFoundCallback);
        }
    }

    /**
     * Parse a token string, create a simple or filter token object and calls
     * the callback
     *
     * @param searchString
     *        The original search string
     * @param tokenStart
     *        The token start position in the search string
     * @param tokenLength
     *        The token length
     * @param unquotedToken
     *        The token string with quotes removed
     * @param quoteMode
     *        Whether quotes were not matched when the token was split
     * @param tokenFoundCallback
     *        Callback to call with the created token
     */
    static void parseTokenString(
        final String searchString,
        final int tokenStart,
        final int tokenLength,
        final String unquotedToken,
        final boolean quoteMode,
        final TokenFoundCallback tokenFoundCallback) {
        IVSSearchToken token = null;
        final String originalTokenText = searchString.substring(tokenStart, tokenStart + tokenLength);
        int parseError = VSSearchParseError.NONE;
        if (quoteMode) {
            parseError |= VSSearchParseError.UNMATCHED_QUOTES;
        }

        // This is Fourth logical pass of the parser, we can do it before the
        // 3rd because the result is used in both cases.
        final AtomicBoolean invalidEscape = new AtomicBoolean(false);
        final String parsedTokenText = VSSearchQueryParser.unescapeString(unquotedToken, invalidEscape);

        if (invalidEscape.get()) {
            parseError |= VSSearchParseError.INVALID_ESCAPE;
        }

        // In the unquoted token text, search for the first filter separator
        // that's not escaped.
        // This is Third logical pass of the parser
        int filterSeparator = findFilterSeparator(unquotedToken);
        if (filterSeparator == -1) {
            token = new VSSearchToken(originalTokenText, tokenStart, parsedTokenText, parseError);
        } else {
            int filterTokenType = VSSearchFilterTokenType.DEFAULT;
            if (unquotedToken.charAt(
                filterSeparator) == VSSearchQueryParser.SyntaxCharacters.EXACT_MATCH_FILTER_SEPARATOR) {
                filterTokenType |= VSSearchFilterTokenType.EXACT_MATCH;
            }

            // This is a filter token
            final AtomicBoolean invalidFilterFieldEscape = new AtomicBoolean(false);
            String filterField = VSSearchQueryParser.unescapeString(
                unquotedToken.substring(0, filterSeparator),
                invalidFilterFieldEscape);

            final AtomicBoolean invalidFilterValueEscape = new AtomicBoolean(false);
            final String filterValue = VSSearchQueryParser.unescapeString(
                unquotedToken.substring(filterSeparator + 1),
                invalidFilterValueEscape);

            if (invalidFilterFieldEscape.get() || invalidFilterValueEscape.get()) {
                parseError |= VSSearchParseError.INVALID_ESCAPE;
            }

            // A filter value that is null, empty or has only whitespaces is not
            // accepted.
            if (filterField == null || filterField.trim().length() == 0) {
                parseError |= VSSearchParseError.EMPTY_FILTER_FIELD;
            } else {
                // If the filter field starts with the exclusion character
                // Indicate this in the token type, remove the character from
                // the filter field and check again if the remaining field is
                // not null, empty or just spaces
                if (VSSearchQueryParser.isExcludeFilter(filterField.charAt(0))) {
                    filterTokenType |= VSSearchFilterTokenType.EXCLUDE;

                    filterField = filterField.substring(1);

                    if (filterField == null || filterField.trim().length() == 0) {
                        parseError |= VSSearchParseError.EMPTY_FILTER_FIELD;
                    }
                }
            }

            // Whereas filter field cannot contain only spaces, the filter value
            // can ( e.g. to search for a string composed only of spaces, quote
            // the spaces like Contains:" " )
            // We'll only indicate an error if the value is empty
            if (filterValue == null || filterValue.length() == 0) {
                parseError |= VSSearchParseError.EMPTY_FILTER_VALUE;
            }

            // Now get the position of the filter separator in the original
            // token text
            filterSeparator = findFilterSeparator(originalTokenText);

            token = new VSSearchFilterToken(
                originalTokenText,
                tokenStart,
                parsedTokenText,
                filterField,
                filterValue,
                filterTokenType,
                filterSeparator,
                parseError);
        }

        // Now call the callback with the token created
        tokenFoundCallback.call(token);
    }

    /**
     * Returns the position of the filter separator in the token string (if the
     * string contains such separator)
     *
     * @param tokenString
     *        The token string
     * @return Separator position or -1 if no separator is found
     */
    public static int findFilterSeparator(final String tokenString) {
        // Since both separator and the escape character are constant
        // characters, we could use a RegExp to make things easier to write, but
        // it's less efficient than just looking ourselves char-by-char.
        final int stringLength = tokenString.length();
        for (int i = 0; i < stringLength; i++) {
            final char ch = tokenString.charAt(i);

            if (VSSearchQueryParser.isEscape(ch)) {
                // Skip both this character and the next one, whatever it may be
                // (if any)
                i++;
                continue;
            }

            if (VSSearchQueryParser.isFilterSeparator(ch)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Unescapes the search string
     *
     * @param tokenString
     *        The token string to unescape
     * @param invalidEscape
     *        Whether the token had an invalid escape sequence
     * @return The unescaped string
     */
    public static String unescapeString(final String tokenString, final AtomicBoolean invalidEscape) {
        invalidEscape.set(false);

        final int stringLength = tokenString.length();
        final StringBuilder unescapedString = new StringBuilder(stringLength);

        for (int i = 0; i < stringLength; i++) {
            final char ch = tokenString.charAt(i);
            if (!VSSearchQueryParser.isEscape(ch)) {
                unescapedString.append(ch);
                continue;
            }

            // Escaped character, see if the next character exists
            if (i + 1 < stringLength) {
                final char ch2 = tokenString.charAt(++i);
                final int category = Character.getType(ch2);

                if (VSSearchQueryParser.isEscape(ch2)
                    || VSSearchQueryParser.isFilterSeparator(ch2)
                    || VSSearchQueryParser.isQuote(ch2, category)) {
                    // Correct escape sequence, keep only the second character
                    unescapedString.append(ch2);
                } else {
                    // The string contains an invalid escape sequence ( only \\,
                    // \", \: are accepted)
                    invalidEscape.set(true);
                    // We'll keep though both characters
                    unescapedString.append(ch);
                    unescapedString.append(ch2);
                }
            } else {
                // The string terminates with escape character that doesn't
                // escape anything.
                invalidEscape.set(true);
                // We'll keep the character though
                unescapedString.append(ch);
            }
        }

        return unescapedString.toString();
    }

    public static String escapeString(final String tokenString, final AddQuotesMode addQuotesMode) {
        return escapeString(tokenString, addQuotesMode, false);
    }

    /**
     * Escape a string (to be used when reconstructing a search string from the
     * search query)
     *
     * @param tokenString
     *        String to escape
     * @param addQuotesMode
     * @param isFilterFieldString
     * @return String with :, \ and quotes characters escaped (escaped with \
     *         character)
     */
    public static String escapeString(
        final String tokenString,
        final AddQuotesMode addQuotesMode,
        final boolean isFilterFieldString) {
        boolean addQuotes = (addQuotesMode == AddQuotesMode.ALWAYS);

        final int stringLength = tokenString.length();
        final StringBuilder escapedString = new StringBuilder(2 * stringLength);

        if (stringLength == 0) {
            // The token is empty, add quotes if necessary around the escaped
            // string to keep this token
            addQuotes = (addQuotesMode != AddQuotesMode.DEFAULT);
        } else {
            for (int i = 0; i < stringLength; i++) {
                final char ch = tokenString.charAt(i);
                final int category = Character.getType(ch);

                if (VSSearchQueryParser.isSpace(ch, category)) {
                    // The token contain spaces, add quotes around the escaped
                    // string
                    addQuotes = true;
                }

                if (VSSearchQueryParser.isEscape(ch)
                    || VSSearchQueryParser.isFilterSeparator(ch)
                    || VSSearchQueryParser.isQuote(ch, category)
                    || (isFilterFieldString && i == 0 && VSSearchQueryParser.isExcludeFilter(ch))) {
                    // Add escape sequence
                    escapedString.append(VSSearchQueryParser.SyntaxCharacters.ESCAPE);
                }

                // Add the original character
                escapedString.append(ch);
            }
        }

        if (addQuotes) {
            escapedString.insert(0, VSSearchQueryParser.SyntaxCharacters.QUOTE);
            escapedString.append(VSSearchQueryParser.SyntaxCharacters.QUOTE);
        }

        return escapedString.toString();
    }

    /**
     * Returns a re-built string for the specified token
     *
     * @param token
     *        Token whose text needs to be rebuilt
     */
    static String buildTokenText(final IVSSearchToken token) {
        final IVSSearchFilterToken filterToken =
            (token instanceof IVSSearchFilterToken) ? (IVSSearchFilterToken) token : null;
        if (filterToken == null) {
            // Simple token
            return VSSearchQueryParser.escapeString(
                token.getParsedTokenText(),
                (token.getOriginalTokenText() == null || token.getOriginalTokenText().length() == 0)
                    ? AddQuotesMode.DEFAULT : AddQuotesMode.IF_EMPTY);
        } else {
            // Take the original field and see if there are non-whitespace
            // characters (skipping possible leading exclusion filter mark)
            String originalField = StringUtil.trimBegin(
                filterToken.getOriginalTokenText().substring(0, filterToken.getFilterSeparatorPosition()));
            if (originalField.length() > 0 && VSSearchQueryParser.isExcludeFilter(originalField.charAt(0))) {
                originalField = originalField.substring(1);
            }

            final boolean originalFieldIsWhitespace = originalField.trim().length() == 0;
            final boolean originalValueIsWhitespace = filterToken.getOriginalTokenText().substring(
                filterToken.getFilterSeparatorPosition() + 1).trim().length() == 0;

            return buildFilterTokenText(
                filterToken.getFilterField(),
                (originalFieldIsWhitespace ? AddQuotesMode.DEFAULT : AddQuotesMode.IF_EMPTY),
                filterToken.getFilterValue(),
                (originalValueIsWhitespace ? AddQuotesMode.DEFAULT : AddQuotesMode.IF_EMPTY),
                filterToken.getFilterTokenType());
        }
    }

    static String buildFilterTokenText(
        final String unescapedFilterField,
        final AddQuotesMode addQuotesModeField,
        final String unescapedFilterValue,
        final AddQuotesMode addQuotesModeValue,
        final int filterTokenType) {
        // Filter field, get the filter parts
        String filterField = VSSearchQueryParser.escapeString(unescapedFilterField, addQuotesModeField, true);
        final String filterValue = VSSearchQueryParser.escapeString(unescapedFilterValue, addQuotesModeValue);

        // If this is an exclusion filter, prefix it with the exclusion
        // character
        if ((filterTokenType & VSSearchFilterTokenType.EXCLUDE) != 0) {
            filterField = VSSearchQueryParser.SyntaxCharacters.EXCLUDE_FILTER + filterField;
        }

        // Separate the field from value with the partial/exact match character
        // as indicated by the token type
        final char filterSeparator = ((filterTokenType & VSSearchFilterTokenType.EXACT_MATCH) != 0)
            ? VSSearchQueryParser.SyntaxCharacters.EXACT_MATCH_FILTER_SEPARATOR
            : VSSearchQueryParser.SyntaxCharacters.PARTIAL_MATCH_FILTER_SEPARATOR;

        // And return the string by joining with a filter separator character
        return StringUtil.join(new String[] {
            filterField,
            filterValue
        }, Character.toString(filterSeparator));
    }
}
