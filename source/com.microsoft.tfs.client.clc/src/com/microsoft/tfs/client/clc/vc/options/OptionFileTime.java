// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.SingleValueOption;
import com.microsoft.tfs.core.clients.versioncontrol.WorkspaceOptions;

public final class OptionFileTime extends SingleValueOption {
    public final static String CURRENT = "current"; //$NON-NLS-1$
    public final static String CHECKIN = "checkin"; //$NON-NLS-1$

    public OptionFileTime() {
        super();
    }

    @Override
    protected String[] getValidOptionValues() {
        return new String[] {
            OptionFileTime.CURRENT,
            OptionFileTime.CHECKIN
        };
    }

    /**
     * Updates the given {@link WorkspaceOptions} with this option instance's
     * value.
     *
     * @param currentOptions
     *        the options to update (if <code>null</code>,
     *        {@link WorkspaceOptions#NONE} is used)
     * @return the new options
     * @throws InvalidOptionValueException
     *         if the value is unknown
     */
    public WorkspaceOptions updateWorkspaceOptions(WorkspaceOptions currentOptions) throws InvalidOptionValueException {
        if (currentOptions == null) {
            currentOptions = WorkspaceOptions.NONE;
        }

        final String s = getValue();

        if (s.equalsIgnoreCase(CURRENT)) {
            return currentOptions.remove(WorkspaceOptions.SET_FILE_TO_CHECKIN);
        } else if (s.equalsIgnoreCase(CHECKIN)) {
            return currentOptions.combine(WorkspaceOptions.SET_FILE_TO_CHECKIN);
        }

        /*
         * This shouldn't happen because we report explicit valid option values.
         */
        throw new InvalidOptionValueException(
            MessageFormat.format("Option ''{0}'' does not support value ''{1}''.", getMatchedAlias(), s)); //$NON-NLS-1$
    }
}
