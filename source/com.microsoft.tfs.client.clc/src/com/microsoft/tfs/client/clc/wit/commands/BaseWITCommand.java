// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;

import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.commands.Command;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.exceptions.MissingRequiredOptionException;
import com.microsoft.tfs.client.clc.wit.options.OptionProject;
import com.microsoft.tfs.client.clc.wit.options.OptionType;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.core.exceptions.InputValidationException;

public abstract class BaseWITCommand extends Command {
    public static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

    protected WorkItem getWorkItemByID(final int id)
        throws MalformedURLException,
            ArgumentException,
            LicenseException,
            CLCException {
        final TFSTeamProjectCollection tfs = createConnection(true);
        final WorkItemClient client = (WorkItemClient) tfs.getClient(WorkItemClient.class);

        final WorkItem workItem = client.getWorkItemByID(id);
        if (workItem == null) {
            final String messageFormat = Messages.getString("BaseWITCommand.WorkItemDoesNotExistOrNoPermissionFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, Integer.toString(id));

            throw new CLCException(message);
        }

        return workItem;
    }

    protected Project loadProjectFromOptions(final TFSTeamProjectCollection connection)
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        if (findOptionType(OptionProject.class) == null) {
            throw new MissingRequiredOptionException(Messages.getString("BaseWITCommand.ProjectOptionIsRequired")); //$NON-NLS-1$
        }
        final String teamProjectName = ((OptionProject) findOptionType(OptionProject.class)).getValue();

        final WorkItemClient client = (WorkItemClient) connection.getClient(WorkItemClient.class);

        final Project project = client.getProjects().get(teamProjectName);
        if (project == null) {
            final String messageFormat =
                Messages.getString("BaseWITCommand.TeamProjectNameNotFoundOnServerNamesAreFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, teamProjectName);

            final StringBuffer sb = new StringBuffer(message);
            sb.append(NEWLINE);

            for (final Project p : client.getProjects()) {
                sb.append("  " + p.getName()).append(NEWLINE); //$NON-NLS-1$
            }

            throw new CLCException(sb.toString());
        }

        return project;
    }

    protected WorkItemType loadTypeFromOptions(final Project project)
        throws MissingRequiredOptionException,
            CLCException {
        if (findOptionType(OptionType.class) == null) {
            throw new MissingRequiredOptionException(Messages.getString("BaseWITCommand.TypeOptionRequired")); //$NON-NLS-1$
        }
        final String typeName = ((OptionType) findOptionType(OptionType.class)).getValue();

        final WorkItemType type = project.getWorkItemTypes().get(typeName);
        if (type == null) {
            final String messageFormat =
                Messages.getString("BaseWITCommand.WorkItemTypeNotValidForProjectNamesAreFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, typeName, project.getName());

            final StringBuffer sb = new StringBuffer(message);
            sb.append(NEWLINE);

            final WorkItemType[] types = project.getWorkItemTypes().getTypes();
            for (int i = 0; i < types.length; i++) {
                sb.append("  " + types[i].getName()).append(NEWLINE); //$NON-NLS-1$
            }

            throw new CLCException(sb.toString());
        }

        return type;
    }
}
