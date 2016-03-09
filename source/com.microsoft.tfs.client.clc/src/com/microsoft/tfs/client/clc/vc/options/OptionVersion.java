// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.SingleValueOption;
import com.microsoft.tfs.core.clients.versioncontrol.VersionControlConstants;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

public class OptionVersion extends SingleValueOption {
    VersionSpec[] _versions = null;

    public OptionVersion() {
        super();
    }

    @Override
    protected String[] getValidOptionValues() {
        /*
         * null means that all values are permitted for this option.
         */
        return null;
    }

    /**
     * Derived classes can override to change parsing behavior.
     *
     * @return true if {@link #parseValues(String)} should allow multiple specs
     *         as a range, <code>false</code> if it should not
     */
    protected boolean allowParseVersionRange() {
        return true;
    }

    @Override
    public void parseValues(final String optionValueString) throws InvalidOptionValueException {
        /*
         * Let the superclass parse the values into a single string.
         */
        super.parseValues(optionValueString);

        try {
            _versions = VersionSpec.parseMultipleVersionsFromSpec(
                getValue(),
                VersionControlConstants.AUTHENTICATED_USER,
                allowParseVersionRange());
        } catch (final Exception e) {
            final String messageFormat = Messages.getString("OptionVersion.OptionRequiresVersionSpecFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getMatchedAlias(), e.getLocalizedMessage());

            throw new InvalidOptionValueException(message);
        }
    }

    /**
     * Gets the version specs parsed from the option value.
     *
     * @return the version specs parsed for this option; null if none parsed.
     */
    public VersionSpec[] getParsedVersionSpecs() {
        return _versions;
    }
}
