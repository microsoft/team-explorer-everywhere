// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options;

import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.util.Check;

/**
 *         An option is a request to a command to modify its execution behavior.
 *         Don't extend this class directly. Instead, extend NoValueOption,
 *         SingleValueOption, MultipleValueOption, or
 *         UsernamePasswordValueOption.
 *
 *         This class is thread-safe.
 */
public abstract class Option {
    /**
     * The alias under which this option was invoked.
     */
    private String alias;

    /**
     * We save this so we can reconstruct command-lines exactly as the user
     * typed them.
     */
    private String userText;

    /**
     * Zero argument constructor so we can create new instances of this class
     * dynamically.
     */
    public Option() {
    }

    /**
     * Gets the syntax string for this option when shown in a non-command-bound
     * context (global options). Most options display the same syntax string for
     * non-command-bound and command-bound and need only implement this method
     * (not {@link #getSyntaxString(Class)}).
     *
     * @return a string with a syntax hint for this option (never
     *         <code>null</code>)
     */
    public abstract String getSyntaxString();

    /**
     * Gets the syntax string for this option when shown in a command-bound
     * context.
     * <p>
     * Most derived option classes won't have a specific command-bound string
     * and can simply implement {@link #getSyntaxString()}.
     *
     * @param commandClass
     *        the command class to get syntax for (must not be <code>null</code>
     *        )
     * @return a string with a syntax hint for this option, or <code>null</code>
     *         if this option does not have a specific command-bound syntax and
     *         the string from {@link #getSyntaxString()} should be displayed in
     *         the command-bound context
     */
    public String getSyntaxString(final Class<? extends Command> commandClass) {
        return null;
    }

    /**
     * Parses an option value string into the correct field values for this
     * option instance.
     *
     * @param optionValueString
     *        the option value string to parse. If the derived Option class
     *        doesn't support a null option value string, it will throw an
     *        InvalidOptionValueException.
     * @throws InvalidOptionValueException
     *         if the option value string cannot be parsed.
     */
    public abstract void parseValues(String optionValueString) throws InvalidOptionValueException;

    /**
     * Store the exact name the user used for this option, so we can reconstruct
     * command lines.
     *
     * @param userText
     *        the text the user typed to invoke this option.
     */
    public final void setUserText(final String userText) {
        Check.notNull(userText, "userText"); //$NON-NLS-1$
        this.userText = userText;
    }

    public final String getUserText() {
        return userText;
    }

    /**
     * Store the alias that was matched to create this option.
     *
     * @param alias
     *        the alias that was matched to create this option.
     */
    public final void setMatchedAlias(final String alias) {
        Check.notNull(alias, "alias"); //$NON-NLS-1$
        this.alias = alias;
    }

    public final String getMatchedAlias() {
        return alias;
    }

    @Override
    public String toString() {
        return getMatchedAlias();
    }

    protected static String makePrettyHelpValuesString(final String[] validValues) {
        if (validValues == null) {
            return ""; //$NON-NLS-1$
        }

        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < validValues.length; i++) {
            if (i > 0) {
                sb.append("|"); //$NON-NLS-1$
            }

            sb.append(validValues[i]);
        }

        return sb.toString();
    }
}
