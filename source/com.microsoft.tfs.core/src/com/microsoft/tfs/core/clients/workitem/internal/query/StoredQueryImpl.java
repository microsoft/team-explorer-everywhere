// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.query;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;

import com.microsoft.tfs.core.clients.workitem.WorkItemQueryUtils;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.project.ProjectImpl;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeSelect;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.Parser;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.SyntaxException;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.WIQLAdapter;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.InvalidQueryTextException;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.query.StoredQueryMaxLengths;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;

public class StoredQueryImpl implements StoredQuery {
    private Date creationTime;
    private boolean deleted;
    private String queryDescription;
    private String initialQueryDescription;
    private GUID guid;
    private boolean dirty;
    private boolean saved;
    private Date lastWriteTime;
    private String owner;
    ProjectImpl project;
    int projectId;
    private String queryName;
    private String initialQueryName;
    StoredQueryProviderImpl queryProvider;
    private QueryScope queryScope;
    private String queryText;
    private String initialQueryText;
    private long rowVersion;
    WITContext witContext;
    private Boolean isParsable = null;

    public StoredQueryImpl(
        final GUID guid,
        final String queryName,
        final String queryText,
        final QueryScope queryScope,
        final int projectId,
        final ProjectImpl project,
        final boolean deleted,
        final WITContext witContext) {
        this.guid = guid;
        this.queryName = queryName;
        this.queryText = queryText;
        this.queryScope = queryScope;
        this.projectId = projectId;
        this.project = project;
        this.deleted = deleted;

        this.witContext = witContext;

        initialQueryName = queryName;
        initialQueryText = queryText;

        saved = true;
    }

    /**
     * Used when creating a stored query with data from the server (2008)
     */
    public StoredQueryImpl(
        final GUID guid,
        final String queryName,
        final String queryText,
        final String queryDescription,
        final String owner,
        final Date creationTime,
        final Date lastWriteTime,
        final QueryScope queryScope,
        final int projectId,
        final ProjectImpl project,
        final boolean deleted,
        final long rowVersion,
        final StoredQueryProviderImpl queryProvider,
        final WITContext witContext) {
        this.guid = guid;
        this.queryName = queryName;
        this.queryText = queryText;
        this.queryDescription = queryDescription;
        this.owner = owner;
        this.creationTime = creationTime;
        this.lastWriteTime = lastWriteTime;
        this.queryScope = queryScope;
        this.projectId = projectId;
        this.project = project;
        this.deleted = deleted;
        this.rowVersion = rowVersion;
        this.queryProvider = queryProvider;
        this.witContext = witContext;

        initialQueryName = queryName;
        initialQueryText = queryText;
        initialQueryDescription = queryDescription;

        saved = true;
    }

    /**
     * Used when a StoredQuery is created through the OM.
     */
    public StoredQueryImpl(final QueryScope scope, final String name, final String text, final String description)
        throws InvalidQueryTextException {
        setName(name);
        setDescription(description);
        setQueryText(text);
        setQueryScope(scope);

        initialQueryName = queryName;
        initialQueryText = queryText;
        initialQueryDescription = queryDescription;

        saved = false;

        /*
         * NOTE: the MS code also sets the owner field here, using the
         * "System.Security.Principal.WindowsIdentity" class
         */
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof StoredQueryImpl) {
            final StoredQueryImpl other = (StoredQueryImpl) obj;
            if (guid == null) {
                /*
                 * The GUID is null when the StoredQuery is newly created on the
                 * client side. In this case equality is instance equality.
                 */
                return this == other;
            }

            return guid.equals(other.guid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (guid == null ? 0 : guid.hashCode());
    }

    @Override
    public int compareTo(final StoredQuery other) {
        if (this == other) {
            return 0;
        }

        int compValue = queryScope.getValue() - other.getQueryScope().getValue();

        if (compValue == 0) {
            /*
             * I18N: need to use a java.text.Collator with a specified Locale
             */
            compValue = queryName.compareToIgnoreCase(other.getName());
        }

        return compValue;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0} / {1} / {2}", queryName, queryScope, (guid == null ? "<no guid>" : guid)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /***************************************************************************
     * START of implementation of StoredQuery interface
     **************************************************************************/

    @Override
    public void reset() {
        queryName = initialQueryName;
        queryText = initialQueryText;
        queryDescription = initialQueryDescription;
        dirty = false;
    }

    @Override
    public void update() {
        if (queryProvider == null) {
            throw new IllegalStateException("cannot update an unassociated query"); //$NON-NLS-1$
        }

        if (dirty) {
            queryProvider.updateStoredQuery(this);
        }
        saved = true;
        dirty = false;

        initialQueryName = queryName;
        initialQueryText = queryText;
        initialQueryDescription = queryDescription;
    }

    @Override
    public Date getCreationTime() {
        return creationTime;
    }

    @Override
    public String getDescription() {
        return queryDescription;
    }

    @Override
    public void setDescription(String description) {
        if (saved && !descriptionsEqual(queryDescription, description)) {
            dirty = true;
        }
        if (description == null) {
            description = ""; //$NON-NLS-1$
        }
        if (description.length() > StoredQueryMaxLengths.DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException(MessageFormat.format(
                "description is longer than the max length of {0}", //$NON-NLS-1$
                StoredQueryMaxLengths.DESCRIPTION_MAX_LENGTH));
        }
        queryDescription = description;
    }

    @Override
    public boolean isSaved() {
        if (saved) {
            return !dirty;
        }
        return false;
    }

    @Override
    public boolean isParsable() {
        // Lazily determine this property based on the query text. This is used
        // when populating the Team Explorer. For some queries (such as those
        // from a TFS2010 server) the parser will not be able to handle them and
        // so we will want to decorate the UI with a little error icon.

        if (isParsable == null) {
            try {
                Parser.parseSyntax(getQueryText());
                isParsable = Boolean.TRUE;
            } catch (final SyntaxException e) {
                isParsable = Boolean.FALSE;
            }
        }
        return isParsable.booleanValue();
    }

    @Override
    public Date getLastWriteTime() {
        return lastWriteTime;
    }

    @Override
    public String getName() {
        return queryName;
    }

    @Override
    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null"); //$NON-NLS-1$
        }
        name = name.trim();
        if (name.length() < 1) {
            throw new IllegalArgumentException("name must not be empty"); //$NON-NLS-1$
        }
        if (name.length() > StoredQueryMaxLengths.QUERY_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(MessageFormat.format(
                "name is longer than the max length of {0}", //$NON-NLS-1$
                StoredQueryMaxLengths.QUERY_NAME_MAX_LENGTH));
        }
        if (saved && queryName.compareToIgnoreCase(name) != 0) {
            dirty = true;
        }

        queryName = name;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public GUID getQueryGUID() {
        return guid;
    }

    @Override
    public QueryScope getQueryScope() {
        return queryScope;
    }

    @Override
    public void setQueryScope(final QueryScope scope) {
        if (saved) {
            throw new IllegalStateException("cannot change query scope once saved"); //$NON-NLS-1$
        }
        queryScope = scope;
    }

    @Override
    public String getQueryText() {
        return queryText;
    }

    @Override
    public void setQueryText(String text) throws InvalidQueryTextException {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null"); //$NON-NLS-1$
        }
        text = text.trim();
        if (text.length() < 1) {
            throw new IllegalArgumentException("text must not be empty"); //$NON-NLS-1$
        }
        validateWIQL(witContext, text);
        if (saved && queryText.compareToIgnoreCase(text) != 0) {
            dirty = true;
            isParsable = null;
        }
        queryText = text;
    }

    @Override
    public Query createQuery(final Map<String, Object> queryContext) {
        if (witContext == null) {
            throw new IllegalStateException("unassociated stored query"); //$NON-NLS-1$
        }

        return new QueryImpl(witContext, getQueryText(), queryContext);
    }

    @Override
    public WorkItemCollection runQuery(final Map<String, Object> queryContext) {
        return createQuery(queryContext).runQuery();
    }

    /***************************************************************************
     * END of implementation of StoredQuery interface
     **************************************************************************/

    private boolean descriptionsEqual(final String s1, final String s2) {
        if (s1 == null) {
            return s2 == null;
        }

        /*
         * I18N: need to use a Collator with a specified Locale
         */
        return s1.equalsIgnoreCase(s2);
    }

    public static void validateWIQL(final WITContext witContext, final String queryText)
        throws InvalidQueryTextException {
        NodeSelect selectNode;
        try {
            selectNode = Parser.parseSyntax(queryText);
            if (witContext == null) {
                selectNode.bind(null, null, null);
            } else {
                final WIQLAdapter psExternal = new WIQLAdapter(witContext);
                psExternal.setContext(WorkItemQueryUtils.makeContext(StringUtil.EMPTY, StringUtil.EMPTY));
                selectNode.bind(psExternal, null, null);
            }
        } catch (final SyntaxException ex) {
            throw new InvalidQueryTextException(ex.getDetails(), queryText, ex);
        }
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public void updateAfterSave(final GUID guid, final Date updateTime) {
        this.guid = guid;
        lastWriteTime = updateTime;
        creationTime = updateTime;
        saved = true;
        dirty = false;
    }

    public void updateAfterUpdate(final Date updateTime) {
        lastWriteTime = updateTime;
    }

    public int getProjectID() {
        return projectId;
    }

    public void setProjectID(final int projectId) {
        this.projectId = projectId;
    }

    public void setProject(final ProjectImpl project) {
        this.project = project;
    }

    public void setWITContext(final WITContext witContext) {
        this.witContext = witContext;
    }

    public void setQueryProvider(final StoredQueryProviderImpl queryProvider) {
        this.queryProvider = queryProvider;
    }
}
