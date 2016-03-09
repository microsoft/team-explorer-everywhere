// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.commands.shared;

import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.ExitCode;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.prompt.Prompt;
import com.microsoft.tfs.client.clc.vc.options.OptionAccept;
import com.microsoft.tfs.client.clc.vc.options.OptionNoPrompt;
import com.microsoft.tfs.client.common.license.LicenseManager;
import com.microsoft.tfs.client.common.util.EULAText;

public final class CommandEULA extends Command {
    public CommandEULA() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.microsoft.tfs.client.clc.Command#run()
     */
    @Override
    public void run() throws ArgumentException, CLCException {
        if (LicenseManager.getInstance().isEULAAccepted()) {
            getDisplay().printErrorLine(Messages.getString("CommandEula.AlreadyAcceptedEULA")); //$NON-NLS-1$
            setExitCode(ExitCode.PARTIAL_SUCCESS);
            return;
        }

        if (findOptionType(OptionNoPrompt.class) != null) {
            getDisplay().printErrorLine(Messages.getString("CommandEula.CannotUseNopromptWithEULA")); //$NON-NLS-1$
            setExitCode(ExitCode.FAILURE);
            return;
        }

        if (findOptionType(OptionAccept.class) == null) {
            final String eulaText = EULAText.getEULAText();

            if (eulaText == null) {
                getDisplay().printErrorLine(Messages.getString("CommandEula.CouldNotLoadEULA")); //$NON-NLS-1$
                setExitCode(ExitCode.FAILURE);
                return;
            }

            final String[] eulaLines = eulaText.split("\n"); //$NON-NLS-1$
            paginate(eulaLines);

            final String defaultYesChar = Messages.getString("CommandEula.AcceptEULAResponseYesChar"); //$NON-NLS-1$
            final String defaultNoChar = Messages.getString("CommandEula.AcceptEULAResponseNoChar"); //$NON-NLS-1$

            final String acceptance = Prompt.readLine(
                getDisplay(),
                getInput(),
                MessageFormat.format(
                    Messages.getString("CommandEula.DoYouAcceptEULAFormat"), //$NON-NLS-1$
                    defaultYesChar,
                    defaultNoChar),
                true);

            if (acceptance == null || acceptance.toLowerCase().startsWith(defaultYesChar) == false) {
                setExitCode(ExitCode.FAILURE);
                return;
            }
        }

        LicenseManager.getInstance().setEULAAccepted(true);
        LicenseManager.getInstance().write();
    }

    private void paginate(final String[] lines) {
        int rows = getDisplay().getHeight();

        if (rows < 3) {
            rows = Integer.MAX_VALUE;
        }

        for (int i = 0; i < lines.length; i++) {
            getDisplay().printLine(lines[i]);

            if (i > 0 && (i % (rows - 3)) == 0) {
                getDisplay().printLine(""); //$NON-NLS-1$
                Prompt.readLine(
                    getDisplay(),
                    getInput(),
                    Messages.getString("CommandEula.PressEnterToContinue"), //$NON-NLS-1$
                    false);
            }
        }

        getDisplay().printLine(""); //$NON-NLS-1$
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        return new AcceptedOptionSet[] {
            new AcceptedOptionSet(new Class[] {
                OptionAccept.class
            }, "") //$NON-NLS-1$
        };
    }

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandEula.HelpText1") //$NON-NLS-1$
        };
    }
}
