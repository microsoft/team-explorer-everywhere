// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.exceptions.InvalidOptionValueException;
import com.microsoft.tfs.client.clc.options.SingleValueOption;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.LockLevel;

public final class OptionLock extends SingleValueOption {
    /*
     * Like Visual Studio, only these locale-invariant names may be used as
     * option values.
     */

    public final static String NONE = "none"; //$NON-NLS-1$
    public final static String CHECKIN = "checkin"; //$NON-NLS-1$
    public final static String CHECKOUT = "checkout"; //$NON-NLS-1$

    public OptionLock() {
        super();
    }

    @Override
    protected String[] getValidOptionValues() {
        // "Unchanged" is not accepted (like VS).

        return new String[] {
            OptionLock.NONE,
            OptionLock.CHECKIN,
            OptionLock.CHECKOUT
        };
    }

    public LockLevel getValueAsLockLevel() throws InvalidOptionValueException {
        final String s = getValue();

        if (s == null) {
            // This is unlikely to happen, so return unchanged.
            return LockLevel.UNCHANGED;
        } else if (s.equalsIgnoreCase(OptionLock.NONE)) {
            return LockLevel.NONE;
        } else if (s.equalsIgnoreCase(OptionLock.CHECKIN)) {
            return LockLevel.CHECKIN;
        } else if (s.equalsIgnoreCase(OptionLock.CHECKOUT)) {
            return LockLevel.CHECKOUT;
        }

        /*
         * This shouldn't happen because we report explicit valid option values.
         */
        throw new InvalidOptionValueException(
            MessageFormat.format("Option ''{0}'' does not support value ''{1}''.", getMatchedAlias(), s)); //$NON-NLS-1$
    }

}
