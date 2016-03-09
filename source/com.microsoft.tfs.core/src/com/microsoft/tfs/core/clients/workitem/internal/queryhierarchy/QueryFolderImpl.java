// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.queryhierarchy;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItemType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public class QueryFolderImpl extends QueryItemImpl implements QueryFolder {
    private final Object lock = new Object();

    /* children QueryItems */
    private final List<QueryItem> items = new ArrayList<QueryItem>();

    /* added and removed items */
    private List<QueryItem> addedItems;
    private List<QueryItem> removedItems;

    private boolean needsSort;

    public QueryFolderImpl(final String name) {
        super(name, null);
    }

    public QueryFolderImpl(final String name, final QueryFolder parent) {
        super(name, parent);
    }

    protected QueryFolderImpl(
        final String name,
        final QueryFolder parent,
        final GUID id,
        final IdentityDescriptor ownerDescriptor) {
        super(name, parent, id, ownerDescriptor);
    }

    @Override
    public QueryFolder newFolder(final String name) {
        return new QueryFolderImpl(name, this);
    }

    @Override
    public QueryDefinition newDefinition(final String name, final String queryText) {
        return new QueryDefinitionImpl(name, queryText, this);
    }

    @Override
    public QueryItem[] getItems() {
        synchronized (lock) {
            if (needsSort) {
                Collections.sort(items, new QueryItemComparator());
                needsSort = false;
            }
        }

        return items.toArray(new QueryItem[items.size()]);
    }

    @Override
    public boolean contains(final QueryItem item) {
        Check.notNull(item, "item"); //$NON-NLS-1$

        synchronized (lock) {
            for (final Iterator<QueryItem> i = items.iterator(); i.hasNext();) {
                if (item.equals(i.next())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean containsID(final GUID id) {
        Check.notNull(id, "id"); //$NON-NLS-1$

        synchronized (lock) {
            for (final Iterator<QueryItem> i = items.iterator(); i.hasNext();) {
                if (id.equals(i.next().getID())) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean containsName(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$

        synchronized (lock) {
            return (findByName(name) != null);
        }
    }

    @Override
    public QueryItem getItemByID(final GUID id) {
        Check.notNull(id, "id"); //$NON-NLS-1$

        synchronized (lock) {
            final QueryItem item = findByID(id);

            if (item != null) {
                return item;
            }
        }

        throw new IllegalArgumentException(Messages.getString("QueryFolder.SpecifiedKeyWasNotFound")); //$NON-NLS-1$
    }

    @Override
    public QueryItem getItemByName(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$

        synchronized (lock) {
            final QueryItem item = findByName(name);

            if (item != null) {
                return item;
            }
        }
        throw new IllegalArgumentException(Messages.getString("QueryFolder.SpecifiedKeyWasNotFound")); //$NON-NLS-1$
    }

    protected boolean isRootNode() {
        if (getProject() != null) {
            final QueryHierarchy queryHierarchy = getProject().getQueryHierarchy();

            if (queryHierarchy.containsID(getID()) || this == queryHierarchy) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void add(final QueryItem item) {
        Check.notNull(item, "item"); //$NON-NLS-1$

        QueryFolder parent = null;

        synchronized (lock) {
            if (item == this) {
                throw new IllegalArgumentException(Messages.getString("QueryFolder.CannotAddSelfAsChild")); //$NON-NLS-1$
            }

            if (item.isDeleted()) {
                throw new IllegalArgumentException(Messages.getString("QueryFolder.CannotAddDeletedItems")); //$NON-NLS-1$
            }

            if (isDeleted()) {
                throw new IllegalArgumentException(Messages.getString("QueryFolder.CannotAddToADeletedFolder")); //$NON-NLS-1$
            }

            if (getProject() != null
                && !getProject().getQueryHierarchy().supportsFolders()
                && item instanceof QueryFolder) {
                throw new IllegalArgumentException(Messages.getString("QueryFolder.ServerDoesNotSupportFolders")); //$NON-NLS-1$
            }

            if (item instanceof QueryFolderImpl && ((QueryFolderImpl) item).isRootNode()) {
                throw new IllegalArgumentException(Messages.getString("QueryFolder.RootNodesMayNotBeModified")); //$NON-NLS-1$
            }

            if (getProject() != null && getProject().getQueryHierarchy() == this) {
                throw new IllegalArgumentException(Messages.getString("QueryFolder.RootNodesMayNotBeModified")); //$NON-NLS-1$
            }

            if (getProject() != null
                && item.getProject() != null
                && getProject().getWorkItemClient() != item.getProject().getWorkItemClient()) {
                throw new IllegalArgumentException(Messages.getString("QueryFolder.AddingAcrossStoresIsProhibited")); //$NON-NLS-1$
            }

            if (getProject() != null
                && item.getProject() != null
                && getProject() != item.getProject()
                && !isNew()
                && !item.isNew()) {
                throw new IllegalArgumentException(Messages.getString("QueryFolder.MovingBetweenProjectsIsProhibited")); //$NON-NLS-1$
            }

            for (QueryFolder folder = getParent(); folder != null; folder = folder.getParent()) {
                if (folder == item) {
                    throw new IllegalArgumentException(Messages.getString("QueryFolder.CannotAddParentAsAChild")); //$NON-NLS-1$
                }
            }

            if (containsID(item.getID())) {
                return;
            }

            checkForDuplicateName(item, item.getName());
            parent = item.getParent();
            addInternal(item, true);
        }

        if (parent != null && parent != this && parent instanceof QueryFolderImpl) {
            ((QueryFolderImpl) parent).onContentsChanged(item, QueryFolderAction.REMOVED);
        }

        onContentsChanged(item, QueryFolderAction.ADDED);
    }

    protected void addInternal(final QueryItem item, final boolean updateChangedLists) {
        if (item.getParent() != null && item.getParent() != this && item.getParent() instanceof QueryFolderImpl) {
            ((QueryFolderImpl) item.getParent()).deleteInternal(item, updateChangedLists);
        }

        items.add(item);
        needsSort = true;

        if (item instanceof QueryItemImpl) {
            ((QueryItemImpl) item).setParent(this);
        }

        if (updateChangedLists) {
            if (removedItems != null && removedItems.contains(item)) {
                removedItems.remove(item);
            } else {
                if (addedItems == null) {
                    addedItems = new ArrayList<QueryItem>();
                }
                addedItems.add(item);
            }
        }
    }

    protected void deleteInternal(final QueryItem item, final boolean updateChangedLists) {
        synchronized (lock) {
            items.remove(item);

            if (updateChangedLists) {
                updateChangedListsOnRemove(item);
            }
        }

        if (item instanceof QueryItemImpl) {
            ((QueryItemImpl) item).setParent(null);
        }
    }

    @Override
    protected void resetInternal() {
        final QueryItem[] items = getItems();

        for (int i = 0; i < items.length; i++) {
            if (items[i] instanceof QueryItemImpl) {
                ((QueryItemImpl) items[i]).resetInternal();
            }
        }

        if (removedItems != null) {
            final QueryItem[] removedItemArray = removedItems.toArray(new QueryItem[removedItems.size()]);

            for (int i = 0; i < removedItemArray.length; i++) {
                if (removedItemArray[i] instanceof QueryItemImpl) {
                    ((QueryItemImpl) removedItemArray[i]).resetInternal();
                }
            }
        }

        super.resetInternal();
    }

    void checkForDuplicateName(final QueryItem item, final String name) {
        Check.notNull(item, "item"); //$NON-NLS-1$
        Check.notNull(name, "name"); //$NON-NLS-1$

        final QueryItem namedItem = findByName(name);

        if (namedItem != null && namedItem != item) {
            throw new WorkItemException(
                MessageFormat.format(Messages.getString("QueryFolder.NameConflictForQueryFormat"), name)); //$NON-NLS-1$
        }
    }

    private QueryItem findByName(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$

        for (final Iterator<QueryItem> i = items.iterator(); i.hasNext();) {
            final QueryItem item = i.next();

            if (item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }

        return null;
    }

    void updateName(final QueryItem item, final String newName) {
        needsSort = true;

        if (item instanceof QueryItemImpl) {
            ((QueryItemImpl) item).setNameInternal(newName);
        }
    }

    private void updateChangedListsOnRemove(final QueryItem item) {
        if ((addedItems != null) && addedItems.contains(item)) {
            addedItems.remove(item);
        } else {
            if (removedItems == null) {
                removedItems = new ArrayList<QueryItem>();
            }

            removedItems.add(item);
        }
    }

    @Override
    protected void onMoveChangedHierarchy() {
        super.onMoveChangedHierarchy();
        updateAttributes(this);
    }

    private void updateAttributes(final QueryFolder queryFolder) {
        final QueryItem[] children = queryFolder.getItems();

        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof QueryItemImpl) {
                ((QueryItemImpl) children[i]).setProject(getProject());
                ((QueryItemImpl) children[i]).setPersonal(isPersonal());
            }

            if (children[i] instanceof QueryFolder) {
                updateAttributes((QueryFolder) children[i]);
            }
        }
    }

    protected void onContentsChanged(final QueryItem item, final QueryFolderAction action) {
        /* Fire contents changed event to listeners */
    }

    private QueryItem findByID(final GUID id) {
        for (final Iterator<QueryItem> i = items.iterator(); i.hasNext();) {
            final QueryItem item = i.next();

            if (item.getID().equals(id)) {
                return item;
            }
        }

        return null;
    }

    protected void updateCollectionsForRemove(final QueryItem item) {
        final QueryItem existingItem = findByID(item.getID());

        if (existingItem == null) {
            removedItems.remove(item);
        } else {
            items.remove(item);
        }
    }

    protected void runWithLock(final Runnable runnable) {
        synchronized (lock) {
            runnable.run();
        }
    }

    protected void onAddSaved(final QueryItem item) {
        addedItems.remove(item);
        fireAdd(item);
    }

    protected void onUpdateSaved(final QueryItem item) {
        fireUpdate(item);
    }

    protected void onDeleteSaved(final QueryItem item) {
        updateCollectionsForRemove(item);
        fireDelete(item);
    }

    private void fireAdd(final QueryItem item) {
        onChangesCommitted(item, QueryFolderAction.ADDED);
    }

    private void fireUpdate(final QueryItem item) {
        onChangesCommitted(item, QueryFolderAction.CHANGED);
    }

    private void fireDelete(final QueryItem item) {
        onChangesCommitted(item, QueryFolderAction.REMOVED);
    }

    private void onChangesCommitted(final QueryItem item, final QueryFolderAction action) {
        /* Fire event */
    }

    @Override
    protected void validate(final WITContext context) {
        final QueryItem[] items = getItems();

        for (int i = 0; i < items.length; i++) {
            if (items[i] instanceof QueryItemImpl) {
                ((QueryItemImpl) items[i]).validate(context);
            }
        }
    }

    protected QueryItem[] getRemovedItems() {
        if (removedItems == null) {
            return new QueryItem[0];
        }

        return removedItems.toArray(new QueryItem[removedItems.size()]);
    }

    @Override
    public QueryItemType getType() {
        if (GUID.EMPTY.toString().replaceAll("-", "").equals(getID())) //$NON-NLS-1$ //$NON-NLS-2$
        {
            return QueryItemType.PROJECT;
        }

        return QueryItemType.QUERY_FOLDER;
    }

    private static final class QueryItemComparator implements Comparator<QueryItem> {
        @Override
        public int compare(final QueryItem item0, final QueryItem item1) {
            final boolean folder0 = (item0 instanceof QueryFolder);
            final boolean folder1 = (item1 instanceof QueryFolder);

            if (folder0 == folder1) {
                return Collator.getInstance().compare(item0.getName(), item1.getName());
            }

            if (folder0) {
                return -1;
            }

            return 1;
        }
    }
}