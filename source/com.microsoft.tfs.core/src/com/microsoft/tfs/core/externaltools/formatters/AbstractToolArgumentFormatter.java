// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.externaltools.formatters;

import com.microsoft.tfs.core.externaltools.ExternalTool;
import com.microsoft.tfs.util.Check;

/**
 * Formats command line arguments for launching an external tool process.
 *
 * @since TEE-SDK-10.1
 */
public class AbstractToolArgumentFormatter {
    /**
     * Formats the given tool's arguments with the given substitute strings. If
     * too few substitutions are given for the placeholders in the arguments,
     * the arguments simply retain the placeholder strings. If too many
     * substitution strings are given, the extras are ignored.
     *
     * @param tool
     *        the {@link ExternalTool} whose arguments should be formatted with
     *        the given substitute strings (must not be <code>null</code>)
     * @param substitutes
     *        the strings to put in place of the substitution strings in the
     *        tool's arguments. "%1" in all arguments will be replaced by
     *        substitutes[0], "%2" by substitutes[1], etc. (must not be
     *        <code>null</code>) (members must not be <code>null</code>)
     * @return the tool's arguments with the given strings substituted
     */
    public final String[] formatArguments(final ExternalTool tool, final String[] substitutes) {
        Check.notNull(tool, "tool"); //$NON-NLS-1$
        Check.notNull(substitutes, "substitutes"); //$NON-NLS-1$

        final String[] arguments = tool.getArguments();

        /*
         * Because replaceAll uses $ to backreferences regex matches, we need to
         * escape them, and escape \s (since that's the escape char)
         */
        final String[] escapedSubstitutes = new String[substitutes.length];
        for (int i = 0; i < escapedSubstitutes.length; i++) {
            escapedSubstitutes[i] = escapeForRegularExpression(substitutes[i]);
        }

        /*
         * In each argument, try to replace all "%n" tokens with the replacement
         * data.
         */
        final String[] ret = new String[arguments.length];

        for (int i = 0; i < ret.length; i++) {
            /*
             * Start with the original argument string.
             */
            ret[i] = arguments[i];

            /*
             * Replace every occurrence of the substitutes.
             */
            for (int j = 0; j < escapedSubstitutes.length; j++) {
                final String token = "%" + (j + 1); //$NON-NLS-1$
                ret[i] = ret[i].replaceAll(token, escapedSubstitutes[j]);
            }
        }

        return ret;
    }

    /**
     * Escapes some characters (\, $) in the given string so it can be used for
     * {@link String#replaceAll(String, String)}.
     *
     * @param string
     *        the string to escape characters in (must not be <code>null</code>)
     * @return the given string with backslashes and dollar signs escaped with
     *         more backslashes
     */
    private static String escapeForRegularExpression(final String string) {
        Check.notNull(string, "string"); //$NON-NLS-1$
        return (string.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\\\$")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
}
