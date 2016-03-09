// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.wit.util.WorkItemEditSupport;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.exceptions.InputValidationException;

public class CommandEdit extends BaseWITCommand {
    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandEdit.HelpText1") //$NON-NLS-1$
        };
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        return new AcceptedOptionSet[] {
            new AcceptedOptionSet(WorkItemEditSupport.EDIT_OPTIONAL_OPTION_TYPES, "<id> [fieldName=value]...") //$NON-NLS-1$
        };
    }

    @Override
    public void run()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        if (getFreeArguments().length < 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandEdit.MustSpecifyWorkItemID")); //$NON-NLS-1$
        }
        int id = -1;
        try {
            id = Integer.parseInt(getFreeArguments()[0]);
        } catch (final NumberFormatException ex) {
            final String messageFormat = Messages.getString("CommandEdit.ArgumentIsNotAWorkItemIDFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, getFreeArguments()[0]);
            throw new InvalidFreeArgumentException(message);
        }

        final TFSTeamProjectCollection tfs = createConnection(true);
        final WorkItemClient client = (WorkItemClient) tfs.getClient(WorkItemClient.class);

        final WorkItem workItem = client.getWorkItemByID(id);
        if (workItem == null) {
            final String messageFormat = Messages.getString("CommandEdit.WorkItemDoesNotExistOrNoPermissionFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(id));
            throw new CLCException(message);
        }
        workItem.open();

        final String[] fieldValuePairs = new String[getFreeArguments().length - 1];
        System.arraycopy(getFreeArguments(), 1, fieldValuePairs, 0, fieldValuePairs.length);

        setExitCode(WorkItemEditSupport.editWorkItem(workItem, fieldValuePairs, this, getDisplay()));
    }
}
