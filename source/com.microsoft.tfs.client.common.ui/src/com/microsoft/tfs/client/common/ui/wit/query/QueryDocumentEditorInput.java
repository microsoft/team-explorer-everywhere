// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.query;

import java.io.File;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.wit.qe.QueryEditor;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.util.Check;

public class QueryDocumentEditorInput implements IEditorInput {
    private final TFSServer server;
    private final String targetEditorId;
    private final QueryDocument queryDocument;

    public QueryDocumentEditorInput(
        final TFSServer server,
        final QueryDocument queryDocument,
        final String targetEditorId) {
        Check.notNull(server, "server"); //$NON-NLS-1$
        Check.notNull(queryDocument, "queryDocument"); //$NON-NLS-1$
        Check.notNull(targetEditorId, "targetEditorId"); //$NON-NLS-1$

        this.server = server;
        this.queryDocument = queryDocument;
        this.targetEditorId = targetEditorId;
    }

    public TFSServer getServer() {
        return server;
    }

    public QueryDocument getQueryDocument() {
        return queryDocument;
    }

    public String getTargetEditorID() {
        return targetEditorId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof QueryDocumentEditorInput) {
            final QueryDocumentEditorInput other = (QueryDocumentEditorInput) obj;
            return targetEditorId.equals(other.targetEditorId) && queryDocument.equals(other.queryDocument);
        }
        if (obj instanceof IPathEditorInput
            && queryDocument.getFile() != null
            && targetEditorId.equals(QueryEditor.ID)) {
            final IPathEditorInput pathEditorInput = (IPathEditorInput) obj;
            final File file = pathEditorInput.getPath().toFile();
            return queryDocument.getFile().equals(file);
        }

        return false;
    }

    @Override
    public String toString() {
        return "QueryDocumentEditorInput, QD=" + queryDocument; //$NON-NLS-1$
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        if (queryDocument.getFile() != null) {
            return queryDocument.getFile().getName();
        }
        return queryDocument.getName();
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        if (queryDocument.getFile() != null) {
            return queryDocument.getFile().getAbsolutePath();
        }
        return queryDocument.getHierarchicalPath();
    }

    @Override
    public Object getAdapter(final Class adapter) {
        if (QueryDocument.class == adapter) {
            return queryDocument;
        }
        return Platform.getAdapterManager().getAdapter(this, adapter);
    }
}
