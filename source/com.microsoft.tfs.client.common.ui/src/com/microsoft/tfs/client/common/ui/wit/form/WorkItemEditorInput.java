// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WorkItemHelpers;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.util.Check;

public class WorkItemEditorInput implements IEditorInput {
    private TFSServer server;
    private final WorkItem workItem;
    private final int documentNumber;

    public WorkItemEditorInput(final TFSServer server, final WorkItem workItem) {
        this(server, workItem, -1);
    }

    public WorkItemEditorInput(final TFSServer server, final WorkItem workItem, final int documentNumber) {
        Check.notNull(server, "server"); //$NON-NLS-1$
        Check.notNull(workItem, "workItem"); //$NON-NLS-1$

        this.server = server;
        this.workItem = workItem;
        this.documentNumber = documentNumber;
    }

    public int getWorkItemID() {
        return workItem.getID();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof WorkItemEditorInput) {
            final WorkItemEditorInput other = (WorkItemEditorInput) obj;
            return workItem.equals(other.workItem);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return workItem.hashCode();
    }

    /**
     * Warning: should only be used when reconnected (returning online).
     */
    public void setServer(final TFSServer server) {
        this.server = server;
    }

    public TFSServer getServer() {
        return server;
    }

    public WorkItem getWorkItem() {
        return workItem;
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
        return WorkItemHelpers.getWorkItemDocumentName(
            workItem.getType(),
            workItem.getFields().getID(),
            documentNumber);
    }

    public boolean isNewDocument() {
        return documentNumber > 0;
    }

    public int getDocumentNumber() {
        return documentNumber;
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
}
