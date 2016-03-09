// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.results;

import java.text.MessageFormat;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;

/**
 * A IEditorInput for use with the WITQueryResultsEditor.
 *
 * This editor input is based around a
 * com.microsoft.tfs.core.workitem.query.Query.
 */
public class QueryResultsEditorInput implements IEditorInput {
    /*
     * A Query object. This is the main object that this editor input is based
     * on.
     */
    private final Query query;

    /*
     * An OPTIONAL StoredQuery object. If non-null, this editor input will use
     * the StoredQuery to compute equals(), hashcode(), getName(), and
     * getToolTipText() results.
     */
    private final StoredQuery storedQuery;

    public QueryResultsEditorInput(final Query query) {
        this(query, null);
    }

    public QueryResultsEditorInput(final Query query, final StoredQuery storedQuery) {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null"); //$NON-NLS-1$
        }

        this.query = query;
        this.storedQuery = storedQuery;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof QueryResultsEditorInput && storedQuery != null) {
            final QueryResultsEditorInput other = (QueryResultsEditorInput) obj;
            return storedQuery.equals(other.storedQuery);
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (storedQuery != null) {
            return storedQuery.hashCode();
        }
        return super.hashCode();
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        if (storedQuery != null) {
            final String messageFormat = Messages.getString("QueryResultsEditorInput.NameFormat"); //$NON-NLS-1$
            return MessageFormat.format(messageFormat, storedQuery.getName());
        }
        return Messages.getString("QueryResultsEditorInput.EditorName"); //$NON-NLS-1$
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return getName();
    }

    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }

    public Query getQuery() {
        return query;
    }
}
