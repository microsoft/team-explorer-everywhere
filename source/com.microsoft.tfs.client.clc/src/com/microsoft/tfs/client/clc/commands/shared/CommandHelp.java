// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.commands.shared;

import java.net.MalformedURLException;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Help;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.options.shared.OptionListExitCodes;

public final class CommandHelp extends Command {
    public CommandHelp() {
        super();
    }

    @Override
    public void run() throws ArgumentException, MalformedURLException, CLCException {
        final String[] args = getFreeArguments();

        if (findOptionType(OptionListExitCodes.class) != null) {
            Help.showExitCodes(getDisplay());
        } else if (args.length == 0) {
            /*
             * No arguments, show general help.
             */
            Help.show((Command) null, getDisplay());
        } else if (args.length == 1) {
            Help.show(args[0], getDisplay());
        } else {
            final String messageFormat = Messages.getString("CommandHelp.HelpRequiresACommandFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.commands.Command#getValidOptions()
     */
    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet(new Class[] {
            OptionListExitCodes.class
        }, "[<command>]"); //$NON-NLS-1$
        return optionSets;
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandHelp.HelpText1") //$NON-NLS-1$
        };
    }
}
