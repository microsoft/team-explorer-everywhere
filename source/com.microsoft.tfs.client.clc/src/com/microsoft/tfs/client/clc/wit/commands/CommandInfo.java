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
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.core.exceptions.InputValidationException;

public class CommandInfo extends BaseWITCommand {

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandInfo.HelpText1"), //$NON-NLS-1$
        };
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        return new AcceptedOptionSet[] {
            /*
             * wit info [/project:projectName]
             */
            new AcceptedOptionSet(new Class[] {
                OptionProject.class
            }, null)
        };
    }

    @Override
    public void run()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        if (findOptionType(OptionProject.class) != null) {
            runProjectMode();
        } else {
            runGlobalMode();
        }
    }

    public void runProjectMode()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        final TFSTeamProjectCollection tfs = createConnection(true);
        final Project project = loadProjectFromOptions(tfs);

        final WorkItemType[] types = project.getWorkItemTypes().getTypes();

        final String messageFormat = Messages.getString("CommandInfo.WorkItemTypesForProjectOnServerFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, project.getName(), tfs.getName());

        getDisplay().getPrintStream().println(message);
        getDisplay().getPrintStream().println();

        final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());
        table.setOverallIndent(2);
        table.setWrapColumnText(true);
        table.setHeadingsVisible(true);

        final TextOutputTable.Column[] columns = new TextOutputTable.Column[] {
            new TextOutputTable.Column(Messages.getString("CommandInfo.WorkItemType"), Sizing.TIGHT) //$NON-NLS-1$
        };

        table.setColumns(columns);

        for (int i = 0; i < types.length; i++) {
            table.addRow(new String[] {
                types[i].getName()
            });
        }

        table.print(getDisplay().getPrintStream());
    }

    public void runGlobalMode()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        final TFSConnection tfs = createConnection(true);
        final WorkItemClient client = (WorkItemClient) tfs.getClient(WorkItemClient.class);

        final String messageFormat = Messages.getString("CommandInfo.TeamProjectsForServerFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, tfs.getName());

        getDisplay().getPrintStream().println(message);
        getDisplay().getPrintStream().println();

        final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());
        table.setOverallIndent(2);
        table.setWrapColumnText(true);
        table.setHeadingsVisible(true);

        final TextOutputTable.Column[] columns = new TextOutputTable.Column[] {
            new TextOutputTable.Column(Messages.getString("CommandInfo.TeamProject"), Sizing.TIGHT) //$NON-NLS-1$
        };

        table.setColumns(columns);

        for (final Project project : client.getProjects()) {
            table.addRow(new String[] {
                project.getName()
            });
        }

        table.print(getDisplay().getPrintStream());
    }
}
