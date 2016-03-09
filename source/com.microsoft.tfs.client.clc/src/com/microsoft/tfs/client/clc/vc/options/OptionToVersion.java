// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;

/**
 * Effectively an alias for {@link OptionVersion}, but provides help text that
 */
public final class OptionToVersion extends OptionVersion {
    public OptionToVersion() {
        super();
    }

    @Override
    protected boolean allowParseVersionRange() {
        return false;
    }

    @Override
    public String getSyntaxString() {
        return MessageFormat.format(
            Messages.getString("OptionToVersion.SyntaxStringFormat"), //$NON-NLS-1$
            OptionsMap.getPreferredOptionPrefix(),
            getMatchedAlias());
    }
}
