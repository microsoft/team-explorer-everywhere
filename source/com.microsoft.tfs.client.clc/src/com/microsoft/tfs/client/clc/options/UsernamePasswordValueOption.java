// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.util.Check;

public abstract class UsernamePasswordValueOption extends Option {
    private String username;
    private String password;

    public UsernamePasswordValueOption() {
        super();
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        if (optionValueString == null) {
            throwUsernamePasswordStyleException(null);
        }

        /*
         * Find only the first comma, so they can appear in the password.
         */
        final int commaIndex = optionValueString.indexOf(',');

        /*
         * Parse the first part into username and domain.
         */
        username = (commaIndex == -1) ? optionValueString : optionValueString.substring(0, commaIndex);

        /*
         * Username is required. (Note: domain is *not* required.)
         */
        if (username == null || username.length() == 0) {
            throwUsernamePasswordStyleException(null);
        }

        /*
         * If there's no comma at all, store a null password. Otherwise store
         * the password string. If there is a password, add it, otherwise add an
         * empty string.
         */
        if (commaIndex == -1) {
            password = null;
        } else {
            password = optionValueString.substring(commaIndex + 1);
        }
    }

    /**
     * @return the username parsed from the option value. Null before
     *         {@link #parseValues(String)} has been called.
     */
    public String getUsername() {
        Check.notNull(username, "username"); //$NON-NLS-1$
        return username;
    }

    /**
     * @return the password parsed from the option value. Null before
     *         {@link #parseValues(String)} has been called, and null after the
     *         call if no password was supplied. An empty password is different
     *         from no password, and is returned as an empty string.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Throws an error about invalid username and password syntax, integrating
     * text from an optional cause.
     *
     * @param cause
     *        the cause of the problem, or null if no specific underlying cause.
     * @throws InvalidOptionValueException
     */
    private void throwUsernamePasswordStyleException(final Throwable cause) throws InvalidOptionValueException {
        final char preferredPrefix = OptionsMap.getPreferredOptionPrefix();

        final String causeMessage = (cause != null) ? cause.getLocalizedMessage() : ""; //$NON-NLS-1$

        final String messageFormat = Messages.getString("UsernamePasswordValueOption.LoginHelpFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, causeMessage, getMatchedAlias(), preferredPrefix);

        throw new InvalidOptionValueException(message);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.options.Option#getSyntaxString()
     */
    @Override
    public String getSyntaxString() {
        return "[" //$NON-NLS-1$
            + OptionsMap.getPreferredOptionPrefix()
            + getMatchedAlias()
            + ":domain\\username,password]" //$NON-NLS-1$
            + " | " //$NON-NLS-1$
            + "[" //$NON-NLS-1$
            + OptionsMap.getPreferredOptionPrefix()
            + getMatchedAlias()
            + ":username@domain,password]"; //$NON-NLS-1$
    }
}
