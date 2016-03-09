// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.queryhierarchy;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryImpl;
import com.microsoft.tfs.core.clients.workitem.query.InvalidQueryTextException;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItemType;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public class QueryDefinitionImpl extends QueryItemImpl implements QueryDefinition {
    private String queryText;
    private String originalQueryText;
    private QueryType type;

    public QueryDefinitionImpl(final String name, final String queryText) {
        this(name, queryText, null);
    }

    public QueryDefinitionImpl(final String name, final String queryText, final QueryFolder parent) {
        super(name, parent);

        try {
            originalQueryText = queryText;
            setQueryTextWithValidation(queryText);
        } catch (final RuntimeException e) {
            if ((parent != null) && parent.contains(this)) {
                delete();
            }
            throw e;
        }
    }

    QueryDefinitionImpl(
        final String name,
        final String queryText,
        final QueryFolder parent,
        final GUID id,
        final IdentityDescriptor ownerDescriptor) {
        super(name, parent, id, ownerDescriptor);

        setQueryTextInternal(queryText);
        originalQueryText = queryText;
    }

    @Override
    protected void resetDirty() {
        originalQueryText = queryText;
        super.resetDirty();
    }

    @Override
    protected void resetInternal() {
        setQueryTextInternal(originalQueryText);
        super.resetInternal();
    }

    protected void setQueryTextProtected(final String queryText) {
        setQueryTextInternal(queryText);
        originalQueryText = queryText;
    }

    private void setQueryTextInternal(final String queryText) {
        this.queryText = queryText;
        type = null;
    }

    private void setQueryTextWithValidation(final String queryText)
        throws WorkItemException,
            InvalidQueryTextException {
        Check.notNull(queryText, "queryText"); //$NON-NLS-1$

        if (isDeleted()) {
            throw new WorkItemException(Messages.getString("QueryDefinition.CannotModifyDeletedItem")); //$NON-NLS-1$
        }

        if (queryText.trim().length() == 0) {
            throw new IllegalArgumentException(Messages.getString("QueryDefinition.QueryTextCannotBeEmpty")); //$NON-NLS-1$
        }

        StoredQueryImpl.validateWIQL(getProject().getWITContext(), queryText);
        setQueryTextInternal(queryText);
    }

    @Override
    protected void validate(final WITContext context) {
        if (isDirty()) {
            StoredQueryImpl.validateWIQL(context, getQueryText());
        }
    }

    @Override
    protected boolean isDirtyShallow() {
        if (!super.isDirtyShallow()) {
            return !queryText.equals(originalQueryText);
        }

        return true;
    }

    @Override
    public String getOriginalQueryText() {
        return originalQueryText;
    }

    @Override
    public String getQueryText() {
        return queryText;
    }

    @Override
    public void setQueryText(final String queryText) throws InvalidQueryTextException {
        setQueryTextWithValidation(queryText);

        if (getParent() != null && getParent() instanceof QueryFolderImpl) {
            ((QueryFolderImpl) getParent()).onContentsChanged(this, QueryFolderAction.CHANGED);
        }
    }

    @Override
    public QueryType getQueryType() {
        if (type == null) {
            type = QueryDefinitionUtil.getQueryType(queryText);
        }

        return type;
    }

    @Override
    protected void onSaveCompleted() {
        originalQueryText = queryText;
        super.onSaveCompleted();
    }

    @Override
    public QueryItemType getType() {
        return QueryItemType.QUERY_DEFINITION;
    }
}