// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;

public abstract class SingleValueOption extends Option {
    private String _value;

    public SingleValueOption() {
        super();
    }

    /**
     * String values that are valid values for this option. If null is returned,
     * any value is permitted. Derived classes are encouraged to define public
     * static string members and return those values in the array.
     *
     * @return an array of strings that are valid options for this option, null
     *         if all values are permitted.
     */
    protected abstract String[] getValidOptionValues();

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.clc.options.Option#parseValues(java.lang.String)
     */
    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        if (optionValueString == null) {
            final String messageFormat = Messages.getString("SingleValueOption.OptionRequiresSingleValueFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getMatchedAlias());
            throw new InvalidOptionValueException(message);
        }

        /*
         * If this class declares an array of valid values, check them. If the
         * array is null, all strings are valid, so skip the check.
         */
        final String[] validValues = getValidOptionValues();
        if (validValues != null) {
            boolean foundMatch = false;
            for (int i = 0; i < validValues.length; i++) {
                if (validValues[i].equalsIgnoreCase(optionValueString) == true) {
                    foundMatch = true;
                    break;
                }
            }

            if (foundMatch == false) {
                final String messageFormat =
                    Messages.getString("SingleValueOption.OptionRequiresOneOfTheFollowingFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    getMatchedAlias(),
                    Option.makePrettyHelpValuesString(validValues));
                throw new InvalidOptionValueException(message);
            }
        }

        _value = optionValueString;
    }

    /**
     * Gets the single value of this option.
     *
     * @return the value of this option.
     */
    public String getValue() {
        return _value;
    }

    /**
     * Tests whether the given string matches this option's value string.
     *
     * @param s
     *        the string to compare to this option's value string.
     * @return true if the strings match (case is ignored), false if they
     *         differ.
     */
    public boolean matchesValue(final String s) {
        return s.equalsIgnoreCase(_value);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.options.Option#getSyntaxString()
     */
    @Override
    public String getSyntaxString() {
        String validValuesString = "<value>"; //$NON-NLS-1$

        final String[] validValues = getValidOptionValues();
        if (validValues != null) {
            final StringBuffer sb = new StringBuffer();
            for (int i = 0; i < validValues.length; i++) {
                if (sb.length() > 0) {
                    sb.append("|"); //$NON-NLS-1$
                }

                sb.append(validValues[i]);
            }
            validValuesString = sb.toString();
        }

        return OptionsMap.getPreferredOptionPrefix() + getMatchedAlias() + ":" + validValuesString; //$NON-NLS-1$
    }
}
