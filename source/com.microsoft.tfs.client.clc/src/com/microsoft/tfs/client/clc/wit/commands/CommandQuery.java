// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.clc.wit.commands;

import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.HashMap;

import com.microsoft.tfs.client.clc.AcceptedOptionSet;
import com.microsoft.tfs.client.clc.Messages;
import com.microsoft.tfs.client.clc.exceptions.ArgumentException;
import com.microsoft.tfs.client.clc.exceptions.CLCException;
import com.microsoft.tfs.client.clc.exceptions.InvalidFreeArgumentException;
import com.microsoft.tfs.client.clc.exceptions.LicenseException;
import com.microsoft.tfs.client.clc.options.shared.OptionDelete;
import com.microsoft.tfs.client.clc.wit.options.OptionCreate;
import com.microsoft.tfs.client.clc.wit.options.OptionForce;
import com.microsoft.tfs.client.clc.wit.options.OptionList;
import com.microsoft.tfs.client.clc.wit.options.OptionPrivate;
import com.microsoft.tfs.client.clc.wit.options.OptionProject;
import com.microsoft.tfs.client.clc.wit.options.OptionPublic;
import com.microsoft.tfs.client.clc.wit.options.OptionShowQuery;
import com.microsoft.tfs.client.clc.wit.options.OptionUpdate;
import com.microsoft.tfs.console.TextOutputTable;
import com.microsoft.tfs.console.TextOutputTable.Column.Sizing;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.InvalidQueryTextException;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.query.StoredQueryCollection;
import com.microsoft.tfs.core.clients.workitem.query.StoredQueryFactory;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.exceptions.InputValidationException;
import com.microsoft.tfs.util.StringUtil;

public class CommandQuery extends BaseWITCommand {

    @Override
    public String[] getCommandHelpText() {
        return new String[] {
            Messages.getString("CommandQuery.HelpText1"), //$NON-NLS-1$
            Messages.getString("CommandQuery.HelpText2") //$NON-NLS-1$
        };
    }

    @Override
    public AcceptedOptionSet[] getSupportedOptionSets() {
        return new AcceptedOptionSet[] {
            /*
             * wit query /list [/public] [/private] /project:projectName
             */
            new AcceptedOptionSet(new Class[] {
                OptionPublic.class,
                OptionPrivate.class
            }, null, new Class[] {
                OptionList.class,
                OptionProject.class
            }),

            /*
             * wit query [/showquery] [/public] [/private] /project:projectName
             * <queryName>
             */
            new AcceptedOptionSet(new Class[] {
                OptionPublic.class,
                OptionPrivate.class,
                OptionShowQuery.class
            }, "<queryName>", new Class[] //$NON-NLS-1$
            {
                OptionProject.class
            }),

            /*
             * wit query /create [/public] [/private] [/force]
             * /project:projectName <queryName> <wiql>
             */
            new AcceptedOptionSet(new Class[] {
                OptionPublic.class,
                OptionPrivate.class,
                OptionForce.class
            }, "<queryName> <wiql>", new Class[] //$NON-NLS-1$
            {
                OptionCreate.class,
                OptionProject.class
            }),

            /*
             * wit query /delete [/public] [/private] /project:projectName
             * <queryName>
             */
            new AcceptedOptionSet(new Class[] {
                OptionPublic.class,
                OptionPrivate.class
            }, "<queryName>", new Class[] //$NON-NLS-1$
            {
                OptionDelete.class,
                OptionProject.class
            }),

            /*
             * wit query /update [/public] [/private] [/force]
             * /project:projectName <queryName> <wiql>
             */
            new AcceptedOptionSet(new Class[] {
                OptionPublic.class,
                OptionPrivate.class,
                OptionForce.class
            }, "<queryName> <wiql>", new Class[] //$NON-NLS-1$
            {
                OptionUpdate.class,
                OptionProject.class
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
        if (findOptionType(OptionList.class) != null) {
            runListMode();
        } else if (findOptionType(OptionCreate.class) != null) {
            runCreateMode();
        } else if (findOptionType(OptionDelete.class) != null) {
            runDeleteMode();
        } else if (findOptionType(OptionUpdate.class) != null) {
            runUpdateMode();
        } else {
            final boolean showQueryOnly = (findOptionType(OptionShowQuery.class) != null);
            runQueryMode(showQueryOnly);
        }
    }

    private void runUpdateMode()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        if (getFreeArguments().length != 2) {
            throw new InvalidFreeArgumentException(
                Messages.getString("CommandQuery.ExactlyOneQueryNameAndWIQLRequired")); //$NON-NLS-1$
        }
        String queryName = getFreeArguments()[0];
        queryName = displayNametoBackCompatName(queryName);

        final String wiql = getFreeArguments()[1];

        final TFSTeamProjectCollection tfs = createConnection(true);
        final Project project = loadProjectFromOptions(tfs);
        tfs.getClient(WorkItemClient.class);

        final StoredQuery query = findQuery(queryName, project, tfs);

        try {
            query.setQueryText(wiql);
        } catch (final Exception ex) {
            final StringBuffer sb = new StringBuffer();
            sb.append(Messages.getString("CommandQuery.ErrorInWIQLForQuery")).append(NEWLINE); //$NON-NLS-1$
            sb.append(wiql).append(NEWLINE).append(NEWLINE);
            sb.append(ex.getMessage());
            throw new CLCException(sb.toString());
        }

        query.update();

        final String messageFormat = Messages.getString("CommandQuery.UpdatedQueryInProjectFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, query.getName(), project.getName());

        getDisplay().getPrintStream().println(message);
    }

    private void runDeleteMode()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        final TFSTeamProjectCollection tfs = createConnection(true);
        final Project project = loadProjectFromOptions(tfs);

        if (getFreeArguments().length != 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandQuery.ExactlyOneQueryNameRequired")); //$NON-NLS-1$
        }
        final String queryName = getFreeArguments()[0];

        final StoredQuery query = findQuery(queryName, project, tfs);

        project.getStoredQueries().remove(query);

        final String messageFormat = Messages.getString("CommandQuery.DeletedQueryInProjectFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, query.getName(), project.getName());

        getDisplay().getPrintStream().println(message);
    }

    private void runCreateMode()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        if (getFreeArguments().length != 2) {
            throw new InvalidFreeArgumentException(
                Messages.getString("CommandQuery.ExactlyOneQueryNameAndWIQLRequired")); //$NON-NLS-1$
        }
        final String queryName = getFreeArguments()[0];
        final String wiql = getFreeArguments()[1];

        boolean isPublic = false;

        if (findOptionType(OptionPublic.class) != null) {
            isPublic = true;
        }

        final TFSTeamProjectCollection tfs = createConnection(true);
        final Project project = loadProjectFromOptions(tfs);
        tfs.getClient(WorkItemClient.class);

        StoredQuery storedQuery = null;

        try {
            storedQuery = StoredQueryFactory.newStoredQuery(
                isPublic ? QueryScope.PUBLIC : QueryScope.PRIVATE,
                queryName,
                wiql,
                null);
        } catch (final InvalidQueryTextException ex) {
            final StringBuffer sb = new StringBuffer();
            sb.append(Messages.getString("CommandQuery.ErrorInWIQLForQuery")).append(NEWLINE); //$NON-NLS-1$
            sb.append(wiql).append(NEWLINE).append(NEWLINE);
            sb.append(ex.getMessage());
            throw new CLCException(sb.toString());
        }

        project.getStoredQueries().add(storedQuery);

        final String messageFormat = Messages.getString("CommandQuery.CreatedQueryInProjectFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, queryName, project.getName());

        getDisplay().getPrintStream().println(message);
        getDisplay().getPrintStream().println(
            Messages.getString("CommandQuery.QueryGUIDColon") + storedQuery.getQueryGUID()); //$NON-NLS-1$
        getDisplay().getPrintStream().println(
            Messages.getString("CommandQuery.CreationDateTimeColon") + storedQuery.getCreationTime()); //$NON-NLS-1$
    }

    private void runQueryMode(final boolean showQueryOnly)
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        final TFSTeamProjectCollection tfs = createConnection(true);
        final Project project = loadProjectFromOptions(tfs);

        if (getFreeArguments().length != 1) {
            throw new InvalidFreeArgumentException(Messages.getString("CommandQuery.ExactlyOneQueryNameRequired")); //$NON-NLS-1$
        }
        String queryName = getFreeArguments()[0];
        queryName = displayNametoBackCompatName(queryName);

        final StoredQuery query = findQuery(queryName, project, tfs);

        if (showQueryOnly) {
            final String messageFormat;

            if (query.getQueryScope() == QueryScope.PUBLIC) {
                messageFormat = Messages.getString("CommandQuery.PublicQueryInProjectOnServerFormat"); //$NON-NLS-1$
            } else {
                messageFormat = Messages.getString("CommandQuery.PrivateQueryInProjectOnServerFormat"); //$NON-NLS-1$
            }

            final String message = MessageFormat.format(
                messageFormat,
                backCompatNameToDisplayName(query.getName()),
                project.getName(),
                tfs.getName());

            getDisplay().getPrintStream().println(message);
            getDisplay().getPrintStream().println();
            getDisplay().getPrintStream().println(query.getQueryText());
        } else {
            try {
                final WorkItemCollection results = query.runQuery(new HashMap<String, Object>());
                displayQueryResults(queryName, results);
            } catch (final InvalidQueryTextException ex) {
                final String messageFormat =
                    Messages.getString("CommandQuery.QueryCannotBeRunBecauseOfErrorInQueryFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(
                    messageFormat,
                    backCompatNameToDisplayName(query.getName()),
                    ex.getLocalizedMessage());

                log.warn(message, ex);
                throw new CLCException(message);
            }
        }
    }

    private void displayQueryResults(final String queryName, final WorkItemCollection results)
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {

        final String messageFormat = Messages.getString("CommandQuery.QueryResultsFormat"); //$NON-NLS-1$
        final String message =
            MessageFormat.format(messageFormat, results.size(), backCompatNameToDisplayName(queryName));

        getDisplay().printLine(message);
        getDisplay().printLine(""); //$NON-NLS-1$

        final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());
        table.setOverallIndent(2);
        table.setWrapColumnText(true);
        table.setHeadingsVisible(true);

        final TextOutputTable.Column[] columns = new TextOutputTable.Column[results.getDisplayFieldList().getSize()];

        for (int i = 0; i < results.getDisplayFieldList().getSize(); i++) {
            /*
             * Use Pressure.LOW for all columns, because low-pressure columns
             * get their complete contents printed (not truncated or wrapped),
             * and many queries return so many columns that all screen space is
             * used up. Better to have too much output (user can scroll or use a
             * larger terminal) than not enough.
             */
            columns[i] = new TextOutputTable.Column(results.getDisplayFieldList().getField(i).getName(), Sizing.TIGHT);
        }
        table.setColumns(columns);

        for (int i = 0; i < results.size(); i++) {
            final WorkItem workItem = results.getWorkItem(i);
            final String[] rowData = new String[columns.length];
            for (int j = 0; j < rowData.length; j++) {
                final Object value = workItem.getFields().getField(
                    results.getDisplayFieldList().getField(j).getReferenceName()).getValue();
                rowData[j] = (value == null ? null : value.toString());
            }
            table.addRow(rowData);
        }

        table.print(getDisplay().getPrintStream());
    }

    private StoredQuery findQuery(final String queryName, final Project project, final TFSTeamProjectCollection tfs)
        throws CLCException {
        final StoredQueryCollection collection = project.getStoredQueries();

        StoredQuery query = null;

        /*
         * If the /public option was explictly specified, OR the /private option
         * was NOT explictly specified, we assume a public query
         */
        if (findOptionType(OptionPublic.class) != null || findOptionType(OptionPrivate.class) == null) {
            query = collection.getQueryByNameAndScope(queryName, QueryScope.PUBLIC);
        }

        /*
         * If the /private option was explictly specified, OR the above public
         * search didn't match a query, we try to find a private query that
         * matches
         */
        if (query == null
            && (findOptionType(OptionPrivate.class) != null || findOptionType(OptionPublic.class) == null)) {
            query = collection.getQueryByNameAndScope(queryName, QueryScope.PRIVATE);
        }

        if (query == null) {
            final String messageFormat = Messages.getString("CommandQuery.QueryNotFoundUseListToQueryNamesFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, queryName);

            throw new CLCException(message);
        }

        return query;
    }

    private void runListMode()
        throws ArgumentException,
            MalformedURLException,
            CLCException,
            LicenseException,
            InputValidationException {
        final TFSTeamProjectCollection tfs = createConnection(true);
        final Project project = loadProjectFromOptions(tfs);

        final StoredQueryCollection collection = project.getStoredQueries();

        final StoredQuery[] publicQueries = collection.getQueriesByScope(QueryScope.PUBLIC);
        final StoredQuery[] privateQueries = collection.getQueriesByScope(QueryScope.PRIVATE);

        boolean showPublic = publicQueries.length > 0;
        boolean showPrivate = privateQueries.length > 0;

        if (findOptionType(OptionPrivate.class) != null && findOptionType(OptionPublic.class) == null) {
            showPublic = false;
        }

        if (findOptionType(OptionPublic.class) != null && findOptionType(OptionPrivate.class) == null) {
            showPrivate = false;
        }

        final TextOutputTable table = new TextOutputTable(getDisplay().getWidth());

        /*
         * Use Pressure.LOW for all columns, because low-pressure columns get
         * their complete contents printed (not truncated or wrapped), and many
         * queries return so many columns that all screen space is used up.
         * Better to have too much output (user can scroll or use a larger
         * terminal) than not enough.
         */
        table.setColumns(new TextOutputTable.Column[] {

            new TextOutputTable.Column(Messages.getString("CommandQuery.QueryName"), Sizing.TIGHT), //$NON-NLS-1$
            new TextOutputTable.Column(Messages.getString("CommandQuery.TeamProject"), Sizing.TIGHT), //$NON-NLS-1$
            new TextOutputTable.Column(Messages.getString("CommandQuery.CreatedDate"), Sizing.TIGHT), //$NON-NLS-1$
            new TextOutputTable.Column(Messages.getString("CommandQuery.Description"), Sizing.TIGHT) //$NON-NLS-1$
        });

        table.setOverallIndent(2);
        table.setWrapColumnText(true);
        table.setHeadingsVisible(true);

        if (showPublic) {
            final String messageFormat = Messages.getString("CommandQuery.PublicQueriesForTeamProjectFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, project.getName());

            getDisplay().printLine(message);
            getDisplay().printLine(""); //$NON-NLS-1$
            loadTable(table, publicQueries, project.getName());
            table.print(getDisplay().getPrintStream());
            table.clearRows();
        }

        if (showPrivate) {
            if (privateQueries.length > 0) {
                if (showPublic) {
                    getDisplay().printLine(""); //$NON-NLS-1$
                }

                final String messageFormat = Messages.getString("CommandQuery.PrivateQueriesForTeamProjectFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, project.getName());

                getDisplay().printLine(message);
                getDisplay().printLine(""); //$NON-NLS-1$
                loadTable(table, privateQueries, project.getName());
                table.print(getDisplay().getPrintStream());
                table.clearRows();
            }
        }
    }

    private void loadTable(final TextOutputTable table, final StoredQuery[] queries, final String projectName) {
        String name;
        for (int i = 0; i < queries.length; i++) {
            name = backCompatNameToDisplayName(queries[i].getName());
            table.addRow(new String[] {
                name,
                projectName,
                queries[i].getCreationTime().toString(),
                queries[i].getDescription()
            });
        }
    }

    /**
     * Method to work around the query names that are expected when working in
     * back-compat mode against stored queries.
     */
    private String displayNametoBackCompatName(String queryName) {
        final int lastBackSlash = queryName.lastIndexOf('\\');
        if (lastBackSlash > 0 && lastBackSlash < queryName.length() - 1) {
            queryName = (char) 0x00ab + queryName;
            queryName = queryName.substring(0, lastBackSlash + 1)
                + (char) 0x00bb
                + ' '
                + queryName.substring(lastBackSlash + 2);
            queryName = queryName.replace('\\', (char) 0x2044);
        }

        return queryName;
    }

    /**
     * Method to work around the query names that are expected when working in
     * back-compat mode against stored queries.
     */
    private String backCompatNameToDisplayName(String queryName) {
        if (queryName.indexOf((char) 0x00ab) >= 0) {
            queryName = queryName.substring(queryName.indexOf((char) 0x00ab) + 1);
            queryName = StringUtil.replace(queryName, new String(new char[] {
                0x00bb,
                ' '
            }), "\\"); //$NON-NLS-1$
            queryName = queryName.replace((char) 0x2044, '\\');
        }
        return queryName;
    }
}
