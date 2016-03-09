// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    public static NodeSelect parseSyntax(final String input) {
        final List<Node> lexems = parseLexems(input);
        final Scanner scanner = new Scanner(lexems);
        final NodeSelect selectNode = scanner.scan();
        scanner.checkTail();
        return selectNode;
    }

    public static List<Node> parseLexems(final String input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null"); //$NON-NLS-1$
        }

        final List<Node> lexems = new ArrayList<Node>();
        final int length = input.length(); // "num1"
        int currentPos = 0; // "num2"

        while (currentPos < length) {
            /*
             * skip over any whitespace characters
             */
            if (Character.isWhitespace(input.charAt(currentPos))) {
                ++currentPos;
                continue;
            }

            /*
             * name (unbracketed): starts with a letter or underscore
             */
            if (Character.isLetter(input.charAt(currentPos)) || input.charAt(currentPos) == '_') {
                Node node;
                Boolean flag;
                final int startIndex = currentPos;
                while ((currentPos < length)
                    && ((Character.isLetterOrDigit(input.charAt(currentPos)) || (input.charAt(currentPos) == '_'))
                        || (input.charAt(currentPos) == '.'))) {
                    currentPos++;
                }
                if (input.charAt(currentPos - 1) == '.') {
                    currentPos--;
                }
                final String val = input.substring(startIndex, currentPos);
                flag = Tools.TranslateBoolToken(val);
                if (flag != null) {
                    node = new NodeBoolValue(flag.booleanValue());
                } else {
                    node = new NodeName(val);
                }
                node.setStartOffset(startIndex);
                node.setEndOffset(currentPos);
                lexems.add(node);
                continue;
            }

            /*
             * variable (AKA macro): starts with a '@' character
             */
            if (input.charAt(currentPos) == '@') {
                /*
                 * move the current position past the '@' character
                 */
                final int startIx = ++currentPos;

                /*
                 * read characters until we reach one that is not a letter or
                 * digit
                 */
                while ((currentPos < length) && Character.isLetterOrDigit(input.charAt(currentPos))) {
                    ++currentPos;
                }

                final NodeVariable node = new NodeVariable(input.substring(startIx, currentPos));
                node.setStartOffset(startIx - 1);
                node.setEndOffset(currentPos);
                lexems.add(node);
                continue;
            }

            /*
             * name (bracketed): starts with an opening square bracket
             */
            if (input.charAt(currentPos) == '[') {
                final int num5 = ++currentPos;
                int num6 = length;
                boolean flag2 = true;
                while (currentPos < length) {
                    if (input.charAt(currentPos) == ']') {
                        num6 = currentPos++;
                        flag2 = false;
                        break;
                    }
                    currentPos++;
                }
                final NodeName name = new NodeName(input.substring(num5, num6));
                name.setStartOffset(num5 - 1);
                name.setEndOffset(num6 + 1);
                if (flag2) {
                    throw new SyntaxException(name, SyntaxError.EXPECTING_CLOSING_SQUARE_BRACKET);
                }
                if (num5 == num6) {
                    throw new SyntaxException(name, SyntaxError.EMPTY_NAME);
                }
                lexems.add(name);
                continue;
            }

            /*
             * number: starts with a digit, or a '-' or '+' followed immediately
             * by a digit
             */
            if ((((input.charAt(currentPos) == '-') || (input.charAt(currentPos) == '+')) && (currentPos + 1) < length)
                && Character.isDigit(input.charAt(currentPos + 1)) || Character.isDigit(input.charAt(currentPos))) {
                final int startIx = currentPos;

                /*
                 * read past the starting '-' or '+' character if there is one
                 */
                if (input.charAt(currentPos) == '-' || input.charAt(currentPos) == '+') {
                    currentPos++;
                }

                /*
                 * read characters until we reach one that is not a digit
                 */
                while ((currentPos < length) && Character.isDigit(input.charAt(currentPos))) {
                    currentPos++;
                }

                /*
                 * if the next character is a '.' character ...
                 */
                if ((currentPos < length) && input.charAt(currentPos) == '.') {
                    /*
                     * ... move past it, and read characters until we reach one
                     * that is not a digit
                     */
                    ++currentPos;
                    while ((currentPos < length) && Character.isDigit(input.charAt(currentPos))) {
                        ++currentPos;
                    }
                }

                /*
                 * if the next character is a 'e' or 'E' character ...
                 */
                if ((currentPos < length) && (input.charAt(currentPos) == 'e' || input.charAt(currentPos) == 'E')) {
                    /*
                     * ... move past it ...
                     */
                    ++currentPos;

                    /*
                     * ... read past a single '-' or '+' character, if one
                     * exists ...
                     */
                    if ((currentPos < length) && (input.charAt(currentPos) == '-' || input.charAt(currentPos) == '+')) {
                        ++currentPos;
                    }

                    /*
                     * ... and read characters until we reach one that is not a
                     * digit
                     */
                    while ((currentPos < length) && Character.isDigit(input.charAt(currentPos))) {
                        ++currentPos;
                    }
                }

                final NodeNumber node = new NodeNumber(input.substring(startIx, currentPos));
                node.setStartOffset(startIx);
                node.setEndOffset(currentPos);
                lexems.add(node);
                continue;
            }

            /*
             * string (AKA quoted identifier): starts with a single-quote or a
             * double-quote character
             */
            if (input.charAt(currentPos) == '\'' || input.charAt(currentPos) == '\"') {
                /*
                 * read past and record the starting quote character
                 */
                final char quoteCharacter = input.charAt(currentPos++);

                final int startIx = currentPos;
                int endIx = length;

                /*
                 * read characters until we reach a closing quote character
                 */
                boolean didNotFindClosingQuoteCharacter = true;
                while (currentPos < length) {
                    if (input.charAt(currentPos) == quoteCharacter) {
                        /*
                         * we have to handle this case carefully: this could be
                         * a closing quote character, or it could be an embedded
                         * (escaped) quote character
                         */

                        /*
                         * move past it ...
                         */
                        currentPos++;

                        /*
                         * ... if we're at the end of the string, or if the next
                         * character is NOT the quote character, then it really
                         * was a closing quote character
                         */
                        if (currentPos == length || input.charAt(currentPos) != quoteCharacter) {
                            endIx = currentPos - 1;
                            didNotFindClosingQuoteCharacter = false;
                            break;
                        }
                    }
                    currentPos++;
                }

                String stringValue = input.substring(startIx, endIx);

                /*
                 * replace any embedded (escaped) quote characters in the string
                 * with their non-escaped equivalents
                 */
                stringValue = stringValue.replaceAll(
                    ("\\" + quoteCharacter + "\\" + quoteCharacter), //$NON-NLS-1$ //$NON-NLS-2$
                    String.valueOf(quoteCharacter));

                final NodeString node = new NodeString(stringValue);
                node.setStartOffset(startIx - 1);
                node.setEndOffset(endIx + 1);
                if (didNotFindClosingQuoteCharacter) {
                    throw new SyntaxException(node, SyntaxError.EXPECTING_CLOSING_QUOTE);
                }
                lexems.add(node);
                continue;
            }

            /*
             * two-character long operator
             */
            if ((currentPos + 1 < length) && (
            // "<=" or "<>"
            (input.charAt(currentPos) == '<' && (input.charAt(currentPos + 1) == '=')
                || (input.charAt(currentPos + 1) == '>')) ||

            // ">="
                (input.charAt(currentPos) == '>' && input.charAt(currentPos + 1) == '=')
                ||

            // "!="
                (input.charAt(currentPos) == '!' && input.charAt(currentPos + 1) == '=') ||

            // "==", "=<" , or "=>"
                (input.charAt(currentPos) == '='
                    && (input.charAt(currentPos + 1) == '='
                        || input.charAt(currentPos + 1) == '<'
                        || input.charAt(currentPos + 1) == '>'))
                ||

            // "&&"
                (input.charAt(currentPos) == '&' && input.charAt(currentPos + 1) == '&') ||

            // "||"
                (input.charAt(currentPos) == '|' && input.charAt(currentPos + 1) == '|'))) {
                final NodeOperation node = new NodeOperation(input.substring(currentPos, currentPos + 2));
                node.setStartOffset(currentPos);
                node.setEndOffset(currentPos + 2);
                lexems.add(node);
                currentPos += 2;
            }

            /*
             * one-character long operator
             */
            else {
                final NodeOperation node = new NodeOperation(input.substring(currentPos, currentPos + 1));
                node.setStartOffset(currentPos);
                node.setEndOffset(currentPos + 1);
                lexems.add(node);
                ++currentPos;
            }
        }

        return lexems;
    }
}
