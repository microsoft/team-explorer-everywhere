// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.changes;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;

import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.core.clients.versioncontrol.path.LocalPath;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.tasks.CanceledException;

public class ChangeItem implements IAdaptable {
    private final Object element;
    private final ChangeItemType type;
    private final TFSRepository repository;
    private final String name;
    private final String folder;
    private final ChangeType changeType;
    private final int version;
    private final ItemType itemType;
    private final String sourceServerItem;
    private final String serverItem;
    private final int itemId;
    private final int encoding;
    private final PropertyValue[] properties;

    public ChangeItem(final PendingChange change, final ChangeItemType type, final TFSRepository repository) {
        element = change;
        this.type = type;
        this.repository = repository;

        if (change.getLocalItem() == null) {
            name = ServerPath.getFileName(change.getServerItem());
            folder = ServerPath.getParent(change.getServerItem());
        } else {
            name = LocalPath.getFileName(change.getLocalItem());
            folder = LocalPath.getDirectory(change.getLocalItem());
        }

        changeType = change.getChangeType();
        version = change.getVersion();
        itemType = change.getItemType();
        serverItem = change.getServerItem();

        if (change.getChangeType().contains(ChangeType.RENAME) && change.getSourceServerItem() != null) {
            sourceServerItem = change.getSourceServerItem();
        } else {
            sourceServerItem = null;
        }

        itemId = change.getItemID();
        encoding = change.getEncoding();
        properties = change.getPropertyValues();
    }

    public ChangeItem(final Change change, final TFSRepository repository) {
        element = change;
        type = ChangeItemType.CHANGESET;
        this.repository = repository;

        name = ServerPath.getFileName(change.getItem().getServerItem());
        folder = ServerPath.getParent(change.getItem().getServerItem());
        changeType = change.getChangeType();
        version = change.getItem().getChangeSetID();
        itemType = change.getItem().getItemType();
        sourceServerItem = null;
        serverItem = change.getItem().getServerItem();
        itemId = change.getItem().getItemID();
        encoding = change.getItem().getEncoding().getCodePage();
        properties = change.getItem().getPropertyValues();
    }

    @Override
    public Object getAdapter(final Class adapter) {
        if (adapter.isInstance(element)) {
            return element;
        }

        return Platform.getAdapterManager().getAdapter(this, adapter);
    }

    public PendingChange getPendingChange() {
        return (PendingChange) element;
    }

    public Change getChange() {
        return (Change) element;
    }

    public PropertyValue[] getPropertyValues() {
        return properties;
    }

    public String getLocalOrServerItem() {
        if (element instanceof Change) {
            return ((Change) element).getItem().getServerItem();
        }

        String item = ((PendingChange) element).getLocalItem();
        if (item == null) {
            item = ((PendingChange) element).getServerItem();
        }

        return item;
    }

    public ChangeItemType getType() {
        return type;
    }

    public TFSRepository getRepository() {
        return repository;
    }

    public String getName() {
        return name;
    }

    public String getFolder() {
        return folder;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public int getVersion() {
        return version;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public String getSourceServerItem() {
        return sourceServerItem;
    }

    public String getServerItem() {
        return serverItem;
    }

    public int getItemID() {
        return itemId;
    }

    public int getEncoding() {
        return encoding;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if ((obj instanceof ChangeItem) == false) {
            return false;
        }

        final ChangeItem other = (ChangeItem) obj;

        return other.element.equals(element);
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }

    /**
     * Gets the {@link PendingChange} objects from the given {@link ChangeItem}s
     * by calling {@link #getPendingChange()} on each.
     *
     * @param changeItems
     *        the change items to get from (must not be <code>null</code>)
     * @return the {@link PendingChange} items
     */
    public static PendingChange[] getPendingChanges(final ChangeItem[] changeItems) {
        final PendingChange[] pendingChanges = new PendingChange[changeItems.length];

        for (int i = 0; i < changeItems.length; i++) {
            pendingChanges[i] = changeItems[i].getPendingChange();
        }

        return pendingChanges;
    }

    /*
     *
     * @return true if this ChangeItem is unchanged, false otherwise
     *
     * This method works for both local workspace and server workspace, throws
     * CanceledException if the progress monitor gets cancelled.
     */
    public boolean isUnchanged() throws CanceledException {
        final Workspace workspace = repository.getWorkspace();
        Check.notNull(workspace, "workspace"); //$NON-NLS-1$
        return getPendingChange().isUnchanged(workspace);
    }
}
