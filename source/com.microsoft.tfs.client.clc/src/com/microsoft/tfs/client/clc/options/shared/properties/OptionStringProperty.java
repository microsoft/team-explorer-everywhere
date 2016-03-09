// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.options.shared.properties;

import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;

public class OptionStringProperty extends OptionPropertyBase {
    public OptionStringProperty() {
        super();
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        super.parseValues(optionValueString);

        /*
         * Don't parse for a reset option.
         */
        if (isDelete()) {
            return;
        }

        /*
         * Currently there's nothing to validate here. Any string will do.
         */
    }

    @Override
    protected String getPropertyValueSyntaxPart() {
        return "<value>"; //$NON-NLS-1$
    }
}
