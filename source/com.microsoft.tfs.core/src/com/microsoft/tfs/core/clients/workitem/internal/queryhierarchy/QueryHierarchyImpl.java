// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.queryhierarchy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.webservices.IdentityDescriptor;
import com.microsoft.tfs.core.clients.webservices.IdentityHelper;
import com.microsoft.tfs.core.clients.workitem.exceptions.QueryHierarchyException;
import com.microsoft.tfs.core.clients.workitem.exceptions.UnauthorizedAccessException;
import com.microsoft.tfs.core.clients.workitem.exceptions.ValidationException;
import com.microsoft.tfs.core.clients.workitem.internal.project.ProjectImpl;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryBucket;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryImpl;
import com.microsoft.tfs.core.clients.workitem.internal.query.StoredQueryProviderImpl;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.GetStoredQueryItemRowSet;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.GetStoredQueryItemsRowSetHandler;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.RowSetParser;
import com.microsoft.tfs.core.clients.workitem.internal.update.QueryHierarchyBatchUpdatePackage;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.core.clients.workitem.query.StoredQuery;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryHierarchy;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.exceptions.mappers.WorkItemExceptionMapper;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;

public class QueryHierarchyImpl extends QueryFolderImpl implements QueryHierarchy {
    public static final String PRIVATE_QUERY_FOLDER_NAME =
        Messages.getString("QueryHierarchy.PrivateQueryFolderDisplayName"); //$NON-NLS-1$
    public static final String PUBLIC_QUERY_FOLDER_NAME =
        Messages.getString("QueryHierarchy.PublicQueryFolderDisplayName"); //$NON-NLS-1$

    private static final boolean incrementalRefresh = false;

    private long rowVersion;

    public QueryHierarchyImpl(final Project project) {
        super(project.getName(), null, GUID.EMPTY, null);

        setID(GUID.EMPTY);
        setProject(project);

        updateQueries(false);
    }

    @Override
    public boolean supportsFolders() {
        return getQueryHierarchyProvider().supportsFolders();
    }

    @Override
    public boolean supportsPermissions() {
        return getQueryHierarchyProvider().supportsPermissions();
    }

    @Override
    public QueryItem find(final GUID id) {
        Check.notNull(id, "id"); //$NON-NLS-1$

        final Object itemLock = new Object();
        final QueryItem[] item = new QueryItem[1];

        runWithLock(new Runnable() {
            @Override
            public void run() {
                synchronized (itemLock) {
                    item[0] = findInternal(QueryHierarchyImpl.this, id);
                }
            }
        });

        synchronized (itemLock) {
            return item[0];
        }
    }

    private QueryItem findInternal(final QueryFolder root, final GUID id) {
        final QueryItem[] items = root.getItems();

        for (int i = 0; i < items.length; i++) {
            if (items[i].getID().equals(id)) {
                return items[i];
            }

            if (items[i] instanceof QueryFolder) {
                final QueryItem item = findInternal((QueryFolder) items[i], id);

                if (item != null) {
                    return item;
                }
            }
        }

        return null;
    }

    @Override
    public void refresh() {
        runWithLock(new Runnable() {
            @Override
            public void run() {
                resetInternal();
                updateQueries(true);
            }
        });

        fireHierarchyRefreshedEvent();
    }

    private void updateQueries(final boolean refresh) {
        /* String guid to QueryItem map */
        final Map<GUID, QueryItem> existingQueryItemsByGuid = new HashMap<GUID, QueryItem>();

        populateGUIDDictionary(this, existingQueryItemsByGuid);

        if (supportsFolders()) {
            try {
                final long rowVersion = incrementalRefresh ? rowVersion : 0L;

                final Map<GUID, QueryItem> unmatchedItems = new HashMap<GUID, QueryItem>(existingQueryItemsByGuid);

                /* Start: ClientService#GetStoredQueryItems */
                final DOMAnyContentType payload =
                    (DOMAnyContentType) getProject().getWITContext().getProxy3().getStoredQueryItems(
                        rowVersion,
                        getProject().getID(),
                        new DOMAnyContentType());

                /* Handle each table */
                final Element[] payloadElements = payload.getElements();
                for (int i = 0; i < payloadElements.length; i++) {
                    final RowSetParser parser = new RowSetParser();
                    final GetStoredQueryItemsRowSetHandler handler = new GetStoredQueryItemsRowSetHandler();

                    parser.parse(payloadElements[i], handler);

                    processQueries(refresh, existingQueryItemsByGuid, unmatchedItems, handler.getRowSets(), i == 0);
                }
                /* End: ClientService#GetStoredQueryItems */

                final boolean supportsPermissions =
                    getProject().getWITContext().getQueryHierarchyProvider().supportsPermissions();

                if ((refresh && supportsPermissions) && !incrementalRefresh) {
                    for (final Iterator<QueryItem> i = unmatchedItems.values().iterator(); i.hasNext();) {
                        final QueryItem item = i.next();

                        if ((item != null)
                            && (!(item instanceof QueryFolderImpl) || !((QueryFolderImpl) item).isRootNode())) {
                            if (item.getParent() instanceof QueryFolderImpl) {
                                ((QueryFolderImpl) item.getParent()).deleteInternal(item, false);
                            }

                            if (item instanceof QueryItemImpl) {
                                ((QueryItemImpl) item).setDeleted(true);
                            }
                        }
                    }
                }
                return;
            } catch (final RuntimeException e) {
                throw WorkItemExceptionMapper.map(e);
            }
        }

        updateQueriesFromOldServer(refresh);
    }

    private QueryFolder processQueries(
        final boolean isRefresh,
        final Map<GUID, QueryItem> existingItemsByGuid,
        final Map<GUID, QueryItem> unmatchedItems,
        final GetStoredQueryItemRowSet[] storedQueryItemsRowSets,
        final boolean isPublic) {
        final QueryFolder folder = this;

        /* GUID to ArrayList of QueryItems */
        final Map<GUID, List<QueryItem>> waitingForParentList = new HashMap<GUID, List<QueryItem>>();

        final boolean supportsPermissions =
            getProject().getWITContext().getQueryHierarchyProvider().supportsPermissions();

        for (int i = 0; i < storedQueryItemsRowSets.length; i++) {
            final GetStoredQueryItemRowSet itemRowSet = storedQueryItemsRowSets[i];

            IdentityDescriptor ownerDescriptor = null;

            if (supportsPermissions
                && itemRowSet.getOwnerIdentifier() != null
                && itemRowSet.getOwnerIdentifier() != null) {
                ownerDescriptor = new IdentityDescriptor(itemRowSet.getOwnerType(), itemRowSet.getOwnerIdentifier());
            }

            final GUID id = new GUID(itemRowSet.getID());
            String name = itemRowSet.getName();
            GUID parentId;

            if (itemRowSet.getParentID() != null && !itemRowSet.getParentID().equals("")) //$NON-NLS-1$
            {
                parentId = new GUID(itemRowSet.getParentID());
            } else {
                parentId = GUID.EMPTY;

                if (itemRowSet.getName().equals("")) //$NON-NLS-1$
                {
                    name = isPublic ? PUBLIC_QUERY_FOLDER_NAME : PRIVATE_QUERY_FOLDER_NAME;
                }
            }

            if (itemRowSet.getCacheStamp() > rowVersion) {
                rowVersion = itemRowSet.getCacheStamp();
            }

            QueryItem item = null;
            if (existingItemsByGuid.containsKey(id)) {
                item = existingItemsByGuid.get(id);
                unmatchedItems.remove(id);
            }

            if (itemRowSet.isDeleted()) {
                if (item != null) {
                    if (item.getParent() instanceof QueryFolderImpl) {
                        ((QueryFolderImpl) item.getParent()).deleteInternal(item, false);
                    }

                    if (item instanceof QueryItemImpl) {
                        ((QueryItemImpl) item).setDeleted(true);
                    }
                }
            } else {
                addUpdateItem(
                    name,
                    id,
                    parentId,
                    item,
                    itemRowSet.getText(),
                    itemRowSet.isFolder(),
                    isPublic,
                    ownerDescriptor,
                    existingItemsByGuid,
                    waitingForParentList);
            }
        }
        return folder;
    }

    private void addUpdateItem(
        final String name,
        final GUID id,
        final GUID parentId,
        final QueryItem existingItem,
        final String queryText,
        final boolean isFolder,
        final boolean isPublic,
        final IdentityDescriptor ownerDescriptor,
        final Map<GUID, QueryItem> existingItemsByGuid,
        final Map<GUID, List<QueryItem>> waitingForParentList) {
        QueryFolder parent = null;
        QueryItem item;

        if (existingItemsByGuid.containsKey(parentId)) {
            parent = (QueryFolder) existingItemsByGuid.get(parentId);
        }

        if (existingItem == null) {
            if (isFolder) {
                item = createFolder(
                    name,
                    parent,
                    id,
                    isPublic,
                    ownerDescriptor,
                    waitingForParentList,
                    existingItemsByGuid);
            } else {
                item = new QueryDefinitionImpl(name, queryText, parent, id, ownerDescriptor);
            }
        } else {
            item = existingItem;

            if ((parent != null) && !parent.containsID(existingItem.getID()) && parent instanceof QueryFolderImpl) {
                ((QueryFolderImpl) parent).addInternal(existingItem, false);
            }

            if (!existingItem.getName().equals(name)) {
                if (parent != null && parent instanceof QueryFolderImpl) {
                    ((QueryFolderImpl) parent).updateName(existingItem, name);
                } else {
                    existingItem.setName(name);
                }
            }

            existingItem.setOwnerDescriptor(ownerDescriptor);

            if (!isFolder && existingItem instanceof QueryDefinitionImpl) {
                ((QueryDefinitionImpl) existingItem).setQueryTextProtected(queryText);
            }

            if (item instanceof QueryItemImpl) {
                ((QueryItemImpl) item).resetDirty();
            }
        }

        if (parent == null) {
            List<QueryItem> waitingList;

            if (!waitingForParentList.containsKey(parentId)) {
                waitingList = new ArrayList<QueryItem>();
                waitingForParentList.put(parentId, waitingList);
            } else {
                waitingList = waitingForParentList.get(parentId);
            }

            waitingList.add(item);
        }
    }

    private QueryFolder createFolder(
        final String name,
        final QueryFolder parent,
        final GUID id,
        final boolean isPublic,
        final IdentityDescriptor ownerDescriptor,
        final Map<GUID, List<QueryItem>> waitingForParentList,
        final Map<GUID, QueryItem> existingItemsByGuid) {
        final QueryFolderImpl folder = new QueryFolderImpl(name, parent, id, ownerDescriptor);
        folder.setProject(getProject());
        folder.setPersonal(!isPublic);
        existingItemsByGuid.put(id, folder);

        if (waitingForParentList.containsKey(id)) {
            final List<QueryItem> waitingList = waitingForParentList.get(id);

            for (final Iterator<QueryItem> i = waitingList.iterator(); i.hasNext();) {
                final QueryItem item = i.next();

                folder.addInternal(item, false);

                if (item instanceof QueryItemImpl) {
                    ((QueryItemImpl) item).resetDirty();
                }
            }

            waitingForParentList.remove(id);
        }
        return folder;
    }

    /**
     * Populate a Map from guid to the corresponding QueryItem
     *
     * @param folder
     *        The folder to populate from
     * @param existingQueryItemsByGuid
     *        A map from (String) guid to QueryItem that will be populated
     */
    private void populateGUIDDictionary(final QueryFolder folder, final Map<GUID, QueryItem> existingQueryItemsByGuid) {
        existingQueryItemsByGuid.put(folder.getID(), folder);

        final QueryItem[] folderItems = folder.getItems();

        for (int i = 0; i < folderItems.length; i++) {
            if (folderItems[i] instanceof QueryFolder) {
                populateGUIDDictionary((QueryFolder) folderItems[i], existingQueryItemsByGuid);
            } else {
                existingQueryItemsByGuid.put(folderItems[i].getID(), folderItems[i]);
            }
        }
    }

    private void updateQueriesFromOldServer(final boolean refresh) {
        QueryFolder privateFolder, publicFolder;

        if (!containsName(PRIVATE_QUERY_FOLDER_NAME)) {
            privateFolder = new QueryFolderImpl(PRIVATE_QUERY_FOLDER_NAME, this, GUID.newGUID(), null);
            ((QueryFolderImpl) privateFolder).setPersonal(true);
        } else {
            privateFolder = (QueryFolderImpl) getItemByName(PRIVATE_QUERY_FOLDER_NAME);
        }

        if (!containsName(PUBLIC_QUERY_FOLDER_NAME)) {
            publicFolder = new QueryFolderImpl(PUBLIC_QUERY_FOLDER_NAME, this, GUID.newGUID(), null);
            ((QueryFolderImpl) publicFolder).setPersonal(false);
        } else {
            publicFolder = (QueryFolderImpl) getItemByName(PUBLIC_QUERY_FOLDER_NAME);
        }

        /* Guid -> QueryItem map */
        final Map<GUID, QueryItem> existingQueryItemsByGuid = new HashMap<GUID, QueryItem>();
        final Map<GUID, List<QueryItem>> waitingForParentList = new HashMap<GUID, List<QueryItem>>();

        populateGUIDDictionary(this, existingQueryItemsByGuid);

        final StoredQueryBucket queryBucket =
            getProject().getWITContext().getQueryProvider().getQueryBucket(getProject().getID());

        if (refresh) {
            queryBucket.refresh();
        }

        /* Guid to stored query map */
        final Map<GUID, StoredQuery> storedQueryMap = new HashMap<GUID, StoredQuery>();

        for (final Iterator i = queryBucket.getQueryList().iterator(); i.hasNext();) {
            final StoredQuery query = (StoredQuery) i.next();

            storedQueryMap.put(query.getQueryGUID(), query);

            final QueryFolder parent =
                (query.getQueryScope().equals(QueryScope.PRIVATE)) ? privateFolder : publicFolder;

            QueryItem existingItem = null;

            if (publicFolder.containsID(query.getQueryGUID())) {
                existingItem = publicFolder.getItemByID(query.getQueryGUID());
            } else if (privateFolder.containsID(query.getQueryGUID())) {
                existingItem = privateFolder.getItemByID(query.getQueryGUID());
            }

            addUpdateItem(
                query.getName(),
                query.getQueryGUID(),
                parent.getID(),
                existingItem,
                query.getQueryText(),
                false,
                query.getQueryScope() == QueryScope.PUBLIC,
                null,
                existingQueryItemsByGuid,
                waitingForParentList);
        }

        for (final Iterator<QueryItem> i = existingQueryItemsByGuid.values().iterator(); i.hasNext();) {
            final QueryItem item = i.next();

            if (!storedQueryMap.containsKey(item.getID()) && item instanceof QueryDefinition) {
                if (item.getParent() instanceof QueryFolderImpl) {
                    ((QueryFolderImpl) item.getParent()).deleteInternal(item, false);
                }

                if (item instanceof QueryItemImpl) {
                    ((QueryItemImpl) item).setDeleted(true);
                }
            }
        }
    }

    @Override
    public void reset() {
        runWithLock(new Runnable() {
            @Override
            public void run() {
                resetInternal();
            }
        });

        fireHierarchyResetEvent();
    }

    @Override
    public void save() {
        runWithLock(new Runnable() {
            @Override
            public void run() {
                validate(getProject().getWITContext());

                if (supportsFolders()) {
                    saveToNewServer();
                } else {
                    saveToOldServer();
                }
            }
        });
    }

    private void saveToNewServer() {
        final List<QueryItem> itemsToSave = new ArrayList<QueryItem>();

        getDirtyItems(this, itemsToSave);

        if (itemsToSave.size() != 0) {
            saveQueryItems(itemsToSave);
            saveCompleted(itemsToSave);
        }
    }

    private void getDirtyItems(final QueryItem item, final List<QueryItem> itemsToSave) {
        if (item.isDirty() && (!item.isNew() || !item.isDeleted())) {
            itemsToSave.add(item);
        }

        if (item instanceof QueryFolder) {
            final QueryFolder folder = (QueryFolder) item;
            final QueryItem[] removedItems =
                (folder instanceof QueryFolderImpl) ? ((QueryFolderImpl) folder).getRemovedItems() : null;

            if (!folder.isDeleted() && removedItems != null) {
                for (int i = 0; i < removedItems.length; i++) {
                    if (removedItems[i].isDeleted() && !removedItems[i].isNew()) {
                        itemsToSave.add(removedItems[i]);
                    }
                }
            }

            final QueryItem[] children = folder.getItems();

            for (int i = 0; i < children.length; i++) {
                getDirtyItems(children[i], itemsToSave);
            }
        }
    }

    private void saveQueryItems(final List<QueryItem> itemsToSave) {
        final QueryHierarchyBatchUpdatePackage update =
            new QueryHierarchyBatchUpdatePackage(getProject().getWITContext());

        for (final Iterator<QueryItem> i = itemsToSave.iterator(); i.hasNext();) {
            final QueryItem item = i.next();

            if (item.isDeleted()) {
                update.deleteQueryItem(item);
            } else if (item.isNew()) {
                update.insertQueryItem(item);
            } else {
                update.updateQueryItem(item);
            }
        }

        update.update();
    }

    private void saveCompleted(final List<QueryItem> itemsToSave) {
        for (final Iterator<QueryItem> i = itemsToSave.iterator(); i.hasNext();) {
            final QueryItem item = i.next();

            if (item instanceof QueryItemImpl) {
                ((QueryItemImpl) item).onSaveCompleted();
            }
        }
    }

    private void saveToOldServer() {
        final List<QueryItem> itemsToSave = new ArrayList<QueryItem>();

        getDirtyItems(this, itemsToSave);

        try {
            for (final Iterator<QueryItem> i = itemsToSave.iterator(); i.hasNext();) {
                final QueryItem item = i.next();

                if (item instanceof QueryDefinition) {
                    saveQueryToOldServer((QueryDefinition) item);

                    if (item instanceof QueryDefinitionImpl) {
                        ((QueryDefinitionImpl) item).onSaveCompleted();
                    }
                }
            }
        } catch (final ValidationException e) {
            if (e.getType() == ValidationException.Type.NOT_UNIQUE_STORED_QUERY) {
                throw new QueryHierarchyException(
                    Messages.getString("QueryHierarchy.AnotherStoredQueryExistsWithTheSameName"), //$NON-NLS-1$
                    e,
                    QueryHierarchyException.Type.NAME_CONFLICTS_WITH_EXISTING_ITEM);
            }

            throw e;
        } catch (final UnauthorizedAccessException e) {
            throw new QueryHierarchyException(
                Messages.getString("QueryHierarchy.StoredQueryDoesNotExistOrNoPermission"), //$NON-NLS-1$
                e,
                QueryHierarchyException.Type.DENIED_OR_NOT_EXIST);
        }
    }

    private void saveQueryToOldServer(final QueryDefinition query) {
        final Project project = getProject();
        final StoredQueryProviderImpl queryProvider = project.getWITContext().getQueryProvider();
        final StoredQueryBucket queryBucket = queryProvider.getQueryBucket(project.getID());

        if (query.isNew()) {
            if (!query.isDeleted()) {
                addStoredQuery(query, queryProvider);
            }
        } else {
            StoredQueryBucket originalQueryBucket = queryBucket;

            if (query.getParent() != query.getOriginalParent() && query.getOriginalParent() != null) {
                originalQueryBucket = queryProvider.getQueryBucket(query.getOriginalParent().getProject().getID());
            }

            StoredQuery storedQuery = null;
            for (final Iterator<StoredQuery> i = originalQueryBucket.getQueryList().iterator(); i.hasNext();) {
                final StoredQuery testQuery = i.next();

                if (testQuery.getQueryGUID().equals(query.getID())) {
                    storedQuery = testQuery;
                }
            }

            if (query.isDeleted()) {
                queryProvider.deleteStoredQuery((StoredQueryImpl) storedQuery);
            } else {
                if (query.getParent() != query.getOriginalParent()) {
                    addStoredQuery(query, queryProvider);

                    try {
                        queryProvider.deleteStoredQuery((StoredQueryImpl) storedQuery);
                        return;
                    } catch (final RuntimeException e) {
                        final QueryFolder originalParent = query.getOriginalParent();

                        if (query instanceof QueryDefinitionImpl) {
                            ((QueryDefinitionImpl) query).resetDirty();
                        }

                        if (query.getParent() instanceof QueryFolderImpl) {
                            ((QueryFolderImpl) query.getParent()).onAddSaved(query);
                        }

                        if (originalParent instanceof QueryFolderImpl) {
                            ((QueryFolderImpl) originalParent).updateCollectionsForRemove(query);
                        }

                        new QueryDefinitionImpl(
                            storedQuery.getName(),
                            storedQuery.getQueryText(),
                            originalParent,
                            storedQuery.getQueryGUID(),
                            IdentityHelper.createDescriptorFromSID(storedQuery.getOwner()));

                        throw e;
                    }
                }

                storedQuery.setName(query.getName());
                storedQuery.setQueryText(query.getQueryText());
                storedQuery.update();
            }
        }
    }

    private void addStoredQuery(final QueryDefinition query, final StoredQueryProviderImpl storedQueryProvider) {
        QueryScope queryScope;

        if (query.getParent().getName().equals(PRIVATE_QUERY_FOLDER_NAME)) {
            queryScope = QueryScope.PRIVATE;
        } else {
            queryScope = QueryScope.PUBLIC;
        }

        final Project project = getProject();

        final StoredQueryImpl storedQuery =
            new StoredQueryImpl(queryScope, query.getName(), query.getQueryText(), null);
        storedQuery.setProjectID(project.getID());
        storedQuery.setWITContext(project.getWITContext());
        storedQuery.setQueryProvider(storedQueryProvider);
        storedQuery.setProject((ProjectImpl) project);

        storedQueryProvider.addStoredQuery(storedQuery);

        if (query instanceof QueryDefinitionImpl) {
            ((QueryDefinitionImpl) query).setID(storedQuery.getQueryGUID());
        }
    }

    private void fireHierarchyRefreshedEvent() {
    }

    private void fireHierarchyResetEvent() {
    }
}