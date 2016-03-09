// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.wit;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.client.common.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public class QueryDocumentService {
    private static final Log log = LogFactory.getLog(QueryDocumentService.class);
    private static final String DEFAULT_TEAM_QUERY =
        "SELECT [System.Id], [System.WorkItemType], [System.Title], [System.AssignedTo], [System.State] FROM WorkItems WHERE [System.TeamProject] = @project and [System.WorkItemType] <> '' and [System.State] <> '' order by [System.Id]"; //$NON-NLS-1$

    private final WorkItemClient workItemClient;

    private final List<QueryDocumentWrapper> queries = new ArrayList<QueryDocumentWrapper>();
    private final Object queriesLock = new Object();

    private int nextDocumentNumber = 0;
    private final Object nextNumberLock = new Object();

    public QueryDocumentService(final TFSTeamProjectCollection connection) {
        Check.notNull(connection, "connection"); //$NON-NLS-1$

        workItemClient = connection.getWorkItemClient();
    }

    public QueryDocumentService(final WorkItemClient workItemClient) {
        Check.notNull(workItemClient, "workItemClient"); //$NON-NLS-1$

        this.workItemClient = workItemClient;
    }

    private int getNextDocumentNumber() {
        synchronized (nextNumberLock) {
            return ++nextDocumentNumber;
        }
    }

    public QueryDocument createNewQueryDocument(final String projectName, final QueryFolder parent) {
        return createNewQueryDocument(DEFAULT_TEAM_QUERY, projectName, parent);
    }

    public QueryDocument createNewQueryDocument(final String projectName, final QueryScope queryScope) {
        return createNewQueryDocument(DEFAULT_TEAM_QUERY, projectName, getDefaultParent(projectName, queryScope));
    }

    public QueryDocument createNewQueryDocument(
        final String queryText,
        final String projectName,
        final QueryScope queryScope) {
        return createNewQueryDocument(queryText, projectName, getDefaultParent(projectName, queryScope));
    }

    public QueryDocument createNewQueryDocument(
        final String queryText,
        final String projectName,
        final QueryFolder parent) {
        Check.notNull(projectName, "projectName"); //$NON-NLS-1$
        Check.notNull(parent, "parent"); //$NON-NLS-1$

        final Project project = workItemClient.getProjects().get(projectName);
        final QueryDocument document = new QueryDocument(workItemClient);

        final String newQueryNameFormat = Messages.getString("QueryDocumentService.DeafultNewQueryNameFormat"); //$NON-NLS-1$
        final String newQueryName = MessageFormat.format(newQueryNameFormat, Integer.toString(getNextDocumentNumber()));

        document.setName(newQueryName);
        document.setProjectName(project.getName());
        document.setQueryText(queryText);
        document.setParentGUID(parent.getID());
        document.clearDirty();
        document.load();

        synchronized (queriesLock) {
            queries.add(new QueryDocumentWrapper(document));
        }

        return document;
    }

    private QueryFolder getDefaultParent(final String projectName, final QueryScope queryScope) {
        final Project project = workItemClient.getProjects().get(projectName);

        final QueryItem[] roots = project.getQueryHierarchy().getItems();

        for (int i = 0; i < roots.length; i++) {
            if (roots[i] instanceof QueryFolder && roots[i].isPersonal() == (queryScope == QueryScope.PRIVATE)) {
                return (QueryFolder) roots[i];
            }
        }

        return null;
    }

    public boolean hasQueryDocumentForStoredQuery(final GUID guid) {
        Check.notNull(guid, "guid"); //$NON-NLS-1$

        synchronized (queriesLock) {
            for (final Iterator<QueryDocumentWrapper> it = queries.iterator(); it.hasNext();) {
                final QueryDocumentWrapper wrapper = it.next();
                if (guid.equals(wrapper.queryDocument.getGUID())) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized QueryDocument getQueryDocumentForStoredQuery(final Project project, final GUID guid) {
        Check.notNull(project, "project"); //$NON-NLS-1$
        Check.notNull(guid, "guid"); //$NON-NLS-1$

        synchronized (queriesLock) {
            for (final Iterator<QueryDocumentWrapper> it = queries.iterator(); it.hasNext();) {
                final QueryDocumentWrapper wrapper = it.next();

                if (guid.equals(wrapper.queryDocument.getGUID())) {
                    log.trace("getQueryDocumentForStoredQuery(" //$NON-NLS-1$
                        + guid
                        + "): returning cached QueryDocument: " //$NON-NLS-1$
                        + wrapper.queryDocument);
                    return wrapper.queryDocument;
                }
            }

            final QueryDocument queryDocument = new QueryDocument(workItemClient);
            queryDocument.setGUID(guid);
            queryDocument.setProjectName(project.getName());
            queryDocument.load();

            queries.add(new QueryDocumentWrapper(queryDocument));
            log.trace("getQueryDocumentForStoredQuery(" + guid + "): returning new QueryDocument: " + queryDocument); //$NON-NLS-1$ //$NON-NLS-2$
            return queryDocument;
        }
    }

    public boolean hasQueryDocumentForFile(final File file) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        synchronized (queriesLock) {
            for (final Iterator<QueryDocumentWrapper> it = queries.iterator(); it.hasNext();) {
                final QueryDocumentWrapper wrapper = it.next();

                if (file.equals(wrapper.queryDocument.getFile())) {
                    return true;
                }
            }
        }
        return false;
    }

    public QueryDocument getQueryDocumentForFile(final File file) {
        Check.notNull(file, "file"); //$NON-NLS-1$

        synchronized (queriesLock) {
            for (final Iterator<QueryDocumentWrapper> it = queries.iterator(); it.hasNext();) {
                final QueryDocumentWrapper wrapper = it.next();

                if (file.equals(wrapper.queryDocument.getFile())) {
                    log.trace("getQueryDocumentForFile(" //$NON-NLS-1$
                        + file.getAbsolutePath()
                        + "): returning cached QueryDocument: " //$NON-NLS-1$
                        + wrapper.queryDocument);
                    return wrapper.queryDocument;
                }
            }

            final QueryDocument queryDocument = new QueryDocument(workItemClient);
            queryDocument.setFile(file);
            queryDocument.load();

            queries.add(new QueryDocumentWrapper(queryDocument));
            log.trace("getQueryDocumentForFile(" //$NON-NLS-1$
                + file.getAbsolutePath()
                + "): returning new QueryDocument: " //$NON-NLS-1$
                + queryDocument);
            return queryDocument;
        }
    }

    public void incrementReferences(final QueryDocument queryDocument) {
        synchronized (queriesLock) {
            final QueryDocumentWrapper wrapper = findQueryDocumentWrapper(queryDocument);
            if (wrapper != null) {
                ++wrapper.refCount;

                if (log.isTraceEnabled()) {
                    log.trace("incrementReferences(" + queryDocument + "): " + wrapper.refCount); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }
    }

    public int decrementReferences(final QueryDocument queryDocument) {
        synchronized (queriesLock) {
            final QueryDocumentWrapper wrapper = findQueryDocumentWrapper(queryDocument);
            if (wrapper != null) {
                --wrapper.refCount;

                if (wrapper.refCount <= 0) {
                    queries.remove(wrapper);
                }
                if (log.isTraceEnabled()) {
                    log.trace("decrementReferences(" + queryDocument + "): " + wrapper.refCount); //$NON-NLS-1$ //$NON-NLS-2$
                }
                return wrapper.refCount;
            }
            return -1;
        }
    }

    private QueryDocumentWrapper findQueryDocumentWrapper(final QueryDocument queryDocument) {
        synchronized (queriesLock) {
            for (final Iterator<QueryDocumentWrapper> it = queries.iterator(); it.hasNext();) {
                final QueryDocumentWrapper queryDocumentWrapper = it.next();

                if (queryDocumentWrapper.queryDocument == queryDocument) {
                    return queryDocumentWrapper;
                }
            }
        }
        return null;
    }

    private static class QueryDocumentWrapper {
        public QueryDocument queryDocument;
        public int refCount;

        public QueryDocumentWrapper(final QueryDocument queryDocument) {
            this.queryDocument = queryDocument;
            refCount = 0;
        }
    }
}
