// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.rules;

public class MatchPattern {
    private final String pattern;

    public MatchPattern(final String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern must be non-null"); //$NON-NLS-1$
        }
        if (pattern.length() == 0) {
            throw new IllegalArgumentException("pattern must not be an empty string"); //$NON-NLS-1$
        }

        this.pattern = pattern;
    }

    public boolean matches(final String input) {
        /*
         * null input always fails
         */
        if (input == null) {
            return false;
        }

        /*
         * simple optimization - the input must be the same length as the
         * pattern string
         *
         * I18N: is this assumption true for all Locales?
         */
        if (input.length() != pattern.length()) {
            return false;
        }

        for (int ix = 0; ix < input.length(); ix++) {
            final char inputChar = input.charAt(ix);
            final char patternChar = pattern.charAt(ix);
            boolean charsMatch = false;

            switch (patternChar) {
                case 'A':
                case 'a':
                    charsMatch = Character.isLetter(inputChar);
                    break;

                case 'N':
                case 'n':
                    charsMatch = Character.isDigit(inputChar);
                    break;

                case 'X':
                case 'x':
                    charsMatch = Character.isLetterOrDigit(inputChar);
                    break;

                default:
                    /*
                     * I18N: probably need to use a java.text.Collator. The
                     * pattern character and the input character must be the
                     * same ignoring case.
                     */
                    charsMatch = (Character.toLowerCase(inputChar) == Character.toLowerCase(patternChar));
            }

            if (!charsMatch) {
                return false;
            }
        }

        return true;
    }
}
