// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.wit.options.OptionProject;
import com.microsoft.tfs.client.clc.wit.options.OptionType;
import com.microsoft.tfs.client.clc.wit.util.WorkItemEditSupport;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.core.exceptions.InputValidationException;

public class CommandCreate extends BaseWITCommand {
    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandCreate.HelpText1") //$NON-NLS-1$
        };
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        return new AcceptedOptionSet[] {
            new AcceptedOptionSet(WorkItemEditSupport.CREATE_OPTIONAL_OPTION_TYPES, "[fieldName=value]...", new Class[] //$NON-NLS-1$
            {
                OptionProject.class,
                OptionType.class
            })
        };
    }

    @Override
    public void run()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        final TFSTeamProjectCollection tfs = createConnection(true);
        final Project project = loadProjectFromOptions(tfs);
        final WorkItemType type = loadTypeFromOptions(project);

        final WorkItemClient client = (WorkItemClient) tfs.getClient(WorkItemClient.class);

        final WorkItem workItem = client.newWorkItem(type);

        final String messageFormat = Messages.getString("CommandCreate.CreatedWorkItemOfTypeInProjectFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, type.getName(), project.getName());

        getDisplay().getPrintStream().println(message);

        setExitCode(WorkItemEditSupport.editWorkItem(workItem, getFreeArguments(), this, getDisplay()));
    }
}
