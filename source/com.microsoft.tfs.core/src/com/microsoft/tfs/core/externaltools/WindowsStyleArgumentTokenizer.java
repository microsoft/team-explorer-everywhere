// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools;

import java.util.ArrayList;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.externaltools.validators.ExternalToolException;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Methods for tokenizing string that contain arguments to external tools in a
 * style similar to TFS on Windows.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public abstract class WindowsStyleArgumentTokenizer {
    /**
     * This is a specialized hack for UI code that deals with command strings.
     * It's not very useful otherwise. It gets the raw string that is the first
     * token, up until the first whitespace after the token. Includes leading
     * whitespace. More precisely, it's the entire start of the given string
     * that makes up the first token.
     *
     * @param arguments
     *        the arguments to parse
     * @return the first token in the string, including leading whitespace and
     *         all quote and escaped characters
     */
    public static String getRawFirstToken(final String arguments) {
        if (arguments.length() == 0) {
            return arguments;
        }

        /*
         * Copied the loop from tokenize below. This loop does less.
         */

        final StringBuffer currentToken = new StringBuffer();

        /*
         * Copy all initial whitespace. Easier done here so as not to upset the
         * loop.
         */

        int i = 0;

        while (true) {
            final char c = arguments.charAt(i);

            if (c != ' ' && c != '\t') {
                break;
            }

            currentToken.append(c);
            i++;
        }

        boolean inQuote = false; // inside double quotes
        boolean lastQuote = false; // last char was a quote *in a quote*

        for (; i < arguments.length(); i++) {
            final char c = arguments.charAt(i);

            // handling windows quoting rules is INTERESTING!
            // no wait, the other thing... tedious!
            if (c == '"') {
                // the second in a sequence of double-quotes inside a quote (eg,
                // the third double-quote in the string "example""text")
                if (lastQuote) {
                    lastQuote = false;
                }

                // could be an end quote, could be the start of an embedded
                // quote
                else if (inQuote) {
                    lastQuote = true;
                } else {
                    inQuote = true;
                }

                // always write the quote
                currentToken.append(c);

                continue;
            }

            // last char was a quote, this char is not, thus we actually ended
            // the quote on the last character
            if (lastQuote) {
                inQuote = false;
                lastQuote = false;
            }

            // non-quoted spaces separate arguments
            if ((c == ' ' || c == '\t') && !inQuote) {
                // done with token, return it
                return currentToken.toString();
            }

            // otherwise, part of the argument or some leading whitespace
            currentToken.append(c);
        }

        return currentToken.toString();
    }

    /**
     * <p>
     * Tokenizes the argument string into strings to pass to exec in "windows
     * style" -- anything inside paired double quotes is grouped, you may escape
     * double-quotes only inside double-quotes by doubling them. (Confusing,
     * huh?)
     * </p>
     * <p>
     * That is:
     * </p>
     * <ul>
     * <li>"this is a group"</li>
     * <li>"this - "" - is an embedded double-quote"</li>
     * <li>"this - " - is a parser error"</li>
     * </ul>
     *
     * @param arguments
     *        the arguments to tokenize (must not be <code>null</code>)
     * @return a string array containing the grouped arguments
     * @throws ExternalToolException
     *         if the argument string cannot be parsed
     */
    public static String[] tokenizeArguments(final String arguments) throws ExternalToolException {
        Check.notNull(arguments, "arguments"); //$NON-NLS-1$

        // strip leading and trailing whitespace
        final String args = arguments.trim();

        final ArrayList tokens = new ArrayList();
        StringBuffer currentToken = new StringBuffer();

        boolean inQuote = false; // inside double quotes
        boolean lastQuote = false; // last char was a quote *in a quote*

        for (int i = 0; i < args.length(); i++) {
            final char c = args.charAt(i);

            // handling windows quoting rules is INTERESTING!
            // no wait, the other thing... tedious!
            if (c == '"') {
                // the second in a sequence of double-quotes inside a quote (eg,
                // the third double-quote in the string "example""text")
                if (lastQuote) {
                    currentToken.append('"');
                    lastQuote = false;
                }

                // could be an end quote, could be the start of an embedded
                // quote
                else if (inQuote) {
                    lastQuote = true;
                } else {
                    inQuote = true;
                }

                continue;
            }

            // last char was a quote, this char is not, thus we actually ended
            // the quote on the last character
            if (lastQuote) {
                inQuote = false;
                lastQuote = false;
            }

            // non-quoted spaces separate arguments
            if ((c == ' ' || c == '\t') && !inQuote) {
                // Prevent multiple whitespaces in series from creating empty
                // tokens.
                if (currentToken.length() > 0) {
                    tokens.add(currentToken);
                    currentToken = new StringBuffer();
                }
            } else {
                // otherwise, part of the argument
                currentToken.append(c);
            }
        }

        // last character we parsed was a double-quote. must have been a
        // terminator
        if (lastQuote) {
            inQuote = false;
        }

        if (inQuote) {
            throw new ExternalToolException(
                Messages.getString("WindowsStyleArgumentTokenizer.UnterminatedDoubleQuoteInToolArguments")); //$NON-NLS-1$
        }

        // deal with any hangerson
        if (currentToken.length() > 0) {
            tokens.add(currentToken);
        }

        final String[] converted = new String[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            converted[i] = tokens.get(i).toString();
        }

        return converted;
    }
}
