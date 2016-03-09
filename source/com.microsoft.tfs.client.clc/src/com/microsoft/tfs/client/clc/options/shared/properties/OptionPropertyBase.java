// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared.properties;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.SingleValueOption;
import com.microsoft.tfs.util.Check;

public abstract class OptionPropertyBase extends SingleValueOption {
    private static final char SEPARATOR = '=';

    private String propertyName = ""; //$NON-NLS-1$
    private String propertyValue = ""; //$NON-NLS-1$
    private boolean prompt = false;

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.options.SingleValueOption#
     * getValidOptionValues ()
     */
    @Override
    protected String[] getValidOptionValues() {
        /*
         * We do our own parsing and validation, so accept all values.
         */
        return null;
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        super.parseValues(optionValueString);

        final String keyValuePair = getValue();

        final int separatorIndex = keyValuePair.indexOf(SEPARATOR);

        /* Key name must be specified. */
        if (separatorIndex == 0) {
            final String messageFormat = Messages.getString("OptionPropertyBase.InvalidPropertyPairFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, keyValuePair);
            throw new InvalidOptionValueException(message);
        }
        /*
         * No value (no equals sign, ie "-string:keyName" means prompt for the
         * value
         */
        else if (separatorIndex == -1) {
            propertyName = keyValuePair;
            propertyValue = null;
            prompt = true;
        } else {
            propertyName = keyValuePair.substring(0, separatorIndex);
            propertyValue = keyValuePair.substring(separatorIndex + 1, keyValuePair.length());
        }
    }

    public void setPropertyValue(final String propertyValue) throws InvalidOptionValueException {
        Check.notNull(propertyValue, "propertyValue"); //$NON-NLS-1$

        this.propertyValue = propertyValue;
    }

    @Override
    public String getSyntaxString() {
        return OptionsMap.getPreferredOptionPrefix()
            + getMatchedAlias()
            + ":<property>[=[" //$NON-NLS-1$
            + getPropertyValueSyntaxPart()
            + "]]..."; //$NON-NLS-1$
    }

    /**
     * @return the parsed property name.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * @return the parsed property value string, empty if the user supplied no
     *         value.
     */
    public String getPropertyValue() {
        return propertyValue;
    }

    /**
     * @return true if no property value was supplied by the user, meaning this
     *         option should delete the property. False otherwise.
     */
    public boolean isDelete() {
        return propertyValue != null && propertyValue.length() == 0;
    }

    /**
     * @return true if the value must be accepted from the user after a prompt.
     *         False otherwise.
     */
    public boolean isPrompt() {
        return prompt;
    }

    protected abstract String getPropertyValueSyntaxPart();
}
