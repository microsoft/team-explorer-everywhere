// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.SingleValueOption;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

public final class OptionStopAt extends SingleValueOption {
    private VersionSpec version;

    @Override
    protected String[] getValidOptionValues() {
        /*
         * null means that all values are permitted for this option.
         */
        return null;
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        /*
         * Let the superclass parse the values into a single string.
         */
        super.parseValues(optionValueString);

        try {
            version = VersionSpec.parseSingleVersionFromSpec(getValue(), VersionControlConstants.AUTHENTICATED_USER);
        } catch (final Exception e) {
            final String messageFormat = Messages.getString("OptionStopAt.OptionRequiresVersionSpecFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getMatchedAlias(), e.getLocalizedMessage());

            throw new InvalidOptionValueException(message);
        }
    }

    /**
     * Gets the version spec parsed from the option value.
     *
     * @return the version spec parsed for this option; null if none parsed.
     */
    public VersionSpec getParsedVersionSpec() {
        return version;
    }
}
