// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.core.exceptions.InputValidationException;

public class ScriptCommandSetNoPrompt extends ScriptCommand {
    public ScriptCommandSetNoPrompt() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.commands.Command#run()
     */
    @Override
    public void run()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        final String[] args = getFreeArguments();

        if (args.length != 1) {
            final String messageFormat =
                Messages.getString("ScriptCommandSetNoPrompt.CommandRequiresBooleanArgumentFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        /*
         * This command only exists to parse ported scripts correctly.
         */
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.clc.commands.Command#getSupportedOptionSets()
     */
    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        final AcceptedOptionSet[] optionSets = new AcceptedOptionSet[1];
        optionSets[0] = new AcceptedOptionSet("true|false"); //$NON-NLS-1$
        return optionSets;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.commands.Command#getCommandHelpText()
     */
    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("ScriptCommandSetNoPrompt.HelpText1") //$NON-NLS-1$
        };
    }

}
