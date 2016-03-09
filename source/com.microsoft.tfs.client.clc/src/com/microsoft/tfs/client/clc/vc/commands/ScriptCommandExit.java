// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.vc.commands;

import java.net.MalformedURLException;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.core.exceptions.InputValidationException;

public class ScriptCommandExit extends ScriptCommand {
    public ScriptCommandExit() {
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
        // This is turned into ExitCode.SUCCESS by the caller.
        setExitCode(ExitCode.SUCCESS_BUT_STOP_NOW);
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
        optionSets[0] = new AcceptedOptionSet();
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
            Messages.getString("ScriptCommandExit.HelpText1") //$NON-NLS-1$
        };
    }

}
