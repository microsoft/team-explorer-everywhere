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
import com.microsoft.tfs.jni.PlatformMiscUtils;

public class ScriptCommandChangeDirectory extends ScriptCommand {
    public ScriptCommandChangeDirectory() {
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
                Messages.getString("ScriptCommandChangeDirectory.CommandRequiresOneDirectoryFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getCanonicalName());

            throw new InvalidFreeArgumentException(message);
        }

        if (PlatformMiscUtils.getInstance().changeCurrentDirectory(args[0]) == false) {
            final String messageFormat = Messages.getString("ScriptCommandChangeDirectory.ChangeDirectoryFailedFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, args[0]);

            throw new CLCException(message);
        }
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
        optionSets[0] = new AcceptedOptionSet("<directory>"); //$NON-NLS-1$
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
            Messages.getString("ScriptCommandChangeDirectory.HelpText1") //$NON-NLS-1$
        };
    }

}
