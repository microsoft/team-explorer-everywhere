// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.options;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.OptionsMap;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.vc.commands.CommandRollback;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;

/**
 * Effectively an alias for {@link OptionVersion}, but provides different help
 * guidance depending on context (command).
 */
public final class OptionChangeset extends OptionVersion {
    public OptionChangeset() {
        super();
    }

    @Override
    protected boolean allowParseVersionRange() {
        /*
         * Would be nice to make this conditional, but we don't have the Command
         * class during parsing and this is probably the only option that would
         * need that context.
         */
        return true;
    }

    @Override
    public String getSyntaxString() {
        // This option isn't available globally, so this string really doesn't
        // matter.
        return super.getSyntaxString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSyntaxString(final Class<? extends Command> commandClass) {
        /*
         * This option is a bit special because it wants to document itself as
         * requiring ranges for some commands, requiring a single value for
         * others.
         */
        if (commandClass.equals(CommandRollback.class)) {
            return MessageFormat.format(
                Messages.getString("OptionChangeset.SyntaxStringChangesetRangeFormat"), //$NON-NLS-1$
                OptionsMap.getPreferredOptionPrefix(),
                getMatchedAlias(),
                VersionSpec.RANGE_DELIMITER);
        } else {
            return MessageFormat.format(
                Messages.getString("OptionChangeset.SyntaxStringSingleChangesetFormat"), //$NON-NLS-1$
                OptionsMap.getPreferredOptionPrefix(),
                getMatchedAlias());
        }
    }
}
