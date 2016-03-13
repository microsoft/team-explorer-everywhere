// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query;

import java.io.File;
import java.net.URI;
import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.internal.query.qe.WIQLTranslator;
import com.microsoft.tfs.core.clients.workitem.internal.queryhierarchy.QueryDefinitionUtil;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.Direction;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeFieldName;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.NodeSelect;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.Parser;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.SyntaxException;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.WIQLAdapter;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.WIQLConstants;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.qe.DisplayField;
import com.microsoft.tfs.core.clients.workitem.query.qe.DisplayFieldCollection;
import com.microsoft.tfs.core.clients.workitem.query.qe.ResultOptions;
import com.microsoft.tfs.core.clients.workitem.query.qe.SortField;
import com.microsoft.tfs.core.clients.workitem.query.qe.SortFieldCollection;
import com.microsoft.tfs.core.clients.workitem.query.qe.WIQLOperators;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.LinkQueryMode;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryDefinition;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolder;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryFolderUtil;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryItem;
import com.microsoft.tfs.core.clients.workitem.queryhierarchy.QueryType;
import com.microsoft.tfs.core.util.MementoRepository;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.listeners.ListenerList;
import com.microsoft.tfs.util.listeners.StandardListenerList;

/**
 * <p>
 * A QueryDocument is used in the UI to represent a work item query. Some
 * QueryDocuments conceptually represent other significant objects in the
 * system. For example, {@link QueryDocument}s can be used to represent
 * {@link StoredQuery}s or WIQ files on disk. However, some
 * {@link QueryDocument}s represent nothing, such as a {@link QueryDocument}
 * used to create a new query or a {@link QueryDocument} used to represent a
 * power search.
 *
 * <p>
 * A {@link QueryDocument} is not immutable. However, once created, a
 * {@link QueryDocument} will only ever represent a single conceptual item. For
 * example, a {@link QueryDocument} instance that represents a
 * {@link StoredQuery} will never change and start representing a WIQ file.
 *
 * <p>
 * {@link QueryDocument}s can be obtained from the {@link QueryDocument}
 * service. The QueryDocumentService is also used to manage the lifecycle of
 * {@link QueryDocument}s that represent persistent items like
 * {@link StoredQuery}s and WIQ files.
 *
 * @since TEE-SDK-10.1
 */
public class QueryDocument {
    private static final String SELECT_FORMAT_STRING = "{0} FROM {1} {2} {3} {4}"; //$NON-NLS-1$
    private static final String WHERE_CLAUSE_FORMAT = "{0} {1}"; //$NON-NLS-1$
    private static final String WHERE_STRING = "WHERE"; //$NON-NLS-1$
    private static final String MODE_CLAUSE_FORMAT = "mode({0})"; //$NON-NLS-1$

    public static ResultOptions getDefaultResultOptions(
        final FieldDefinitionCollection fieldDefinitions,
        final QueryDocument queryDocument) {
        final ResultOptions options = new ResultOptions(queryDocument);
        getDefaultDisplayFields(options.getDisplayFields(), fieldDefinitions);
        getDefaultSortFields(options.getSortFields(), fieldDefinitions);
        return options;
    }

    private static void getDefaultDisplayFields(
        final DisplayFieldCollection dfc,
        final FieldDefinitionCollection fieldDefinitions) {
        dfc.clear();
        dfc.add(
            new DisplayField(
                DisplayField.getLocalizedFieldName(CoreFieldReferenceNames.ID, fieldDefinitions),
                ResultOptions.getDefaultColumnWidth(CoreFieldReferenceNames.ID, fieldDefinitions)));
        dfc.add(
            new DisplayField(
                DisplayField.getLocalizedFieldName(CoreFieldReferenceNames.TITLE, fieldDefinitions),
                ResultOptions.getDefaultColumnWidth(CoreFieldReferenceNames.TITLE, fieldDefinitions)));
    }

    private static void getDefaultSortFields(
        final SortFieldCollection sfc,
        final FieldDefinitionCollection fieldDefinitions) {
        sfc.clear();
        sfc.add(new SortField(DisplayField.getLocalizedFieldName(CoreFieldReferenceNames.ID, fieldDefinitions), true));
    }

    /*
     * tracks the dirty status of this QueryDocument
     */
    private boolean dirty = false;
    private final ListenerList dirtyListeners = new StandardListenerList();

    /*
     * holds save listeners
     */
    private final ListenerList saveListeners = new StandardListenerList();

    /*
     * holds the identity of this QueryDocument
     */
    private GUID guid;
    private File file;

    private GUID parentGuid = GUID.EMPTY;

    private String description;
    private String filterExpression;
    private String name;
    private ResultOptions resultOptions;
    private final QueryScope scope;

    private GUID saveParent;
    private String saveDescription;
    private String saveFilterExpression;
    private String saveName;
    private ResultOptions saveResultOptions;
    private QueryScope saveScope;
    private LinkQueryMode queryMode;
    private LinkQueryMode saveQueryMode;
    private QueryType queryType;
    private QueryType saveQueryType;

    /*
     * The work item client associated with this QueryDocument.
     */
    private final WorkItemClient workItemClient;

    /*
     * Usually non-null. Will be null if this QueryDocument represents a WIQ
     * file that does not specify a project.
     */
    private String projectName;

    /*
     * That's not clear why team name is needed in the document. It does not
     * make any sense to change user's selection of either a team or a project
     * when loading WIQ from file, but we try to mimic VS behavior as much as
     * possible.
     */
    private String teamName;

    public QueryDocument(final WorkItemClient workItemClient) {
        Check.notNull(workItemClient, "workItemClient"); //$NON-NLS-1$

        queryType = QueryType.LIST;
        saveQueryType = QueryType.LIST;

        queryMode = LinkQueryMode.WORK_ITEMS;
        saveQueryMode = LinkQueryMode.WORK_ITEMS;

        this.workItemClient = workItemClient;

        resultOptions = getDefaultResultOptions(null, this);
        scope = QueryScope.PRIVATE;
        dirty = false;
    }

    public void setFile(final File file) {
        this.file = file;
        guid = null;
    }

    public void setGUID(final GUID guid) {
        this.guid = guid;
        file = null;
    }

    public void restoreGUIDAndFile(final GUID guid, final File file) {
        this.guid = guid;
        this.file = file;
    }

    public void setParentGUID(final GUID parentGuid) {
        this.parentGuid = parentGuid;
        dirty = true;
        notifyDirtyListeners();
    }

    public void load() {
        try {
            if ((file != null || guid != null) && getResultOptions() != null) {
                getResultOptions().getDisplayFields().clear();
                getResultOptions().getSortFields().clear();
            }
            if (file != null) {
                loadFromFile();
            } else if (guid != null) {
                loadFromServer();
            }
            clearDirty();
            saveState();
        } catch (final SyntaxException se) {
            // WIQL Syntax exception raised when parsing WIQL - throw nicer
            // error.
            throw new WorkItemException(
                Messages.getString("QueryDocument.ClientDoesNotSupportQueryType"), //$NON-NLS-1$
                265000);
        }
    }

    private void loadFromFile() {
        /*
         * TODO: what if the wiq document doesn't parse?
         */
        final WIQDocument wiqDocument = WIQDocument.load(file);

        if (wiqDocument.getTeamFoundationServer() == null || wiqDocument.getTeamProject() == null) {
            throw new RuntimeException(Messages.getString("QueryDocument.FileDoesNotSpecifyServerOrProject")); //$NON-NLS-1$
        }

        final URI currentServer = URIUtils.toLowerCase(workItemClient.getConnection().getBaseURI());
        final URI documentServer = URIUtils.toLowerCase(
            URIUtils.ensurePathHasTrailingSlash(URIUtils.newURI(wiqDocument.getTeamFoundationServer())));

        if (!currentServer.equals(documentServer)) {
            throw new RuntimeException(Messages.getString("QueryDocument.ServerDoesNotMatchCurrentTfs")); //$NON-NLS-1$
        }

        final String documentProject = wiqDocument.getTeamProject();
        final Project project = workItemClient.getProjects().get(documentProject);
        if (project == null) {
            final String messageFormat = Messages.getString("QueryDocument.ProjectDoesNotExistFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, documentProject);
            throw new RuntimeException(message);
        }

        setProjectName(project.getName());

        final String documentTeamName = wiqDocument.getTeamName();
        setTeamName(documentTeamName);

        /*
         * TODO: what if the wiq document specifies an empty wiql string?
         */
        setDocumentFieldsFromWIQL(wiqDocument.getWIQL());

        String nameWithoutExtension = file.getName();
        nameWithoutExtension = nameWithoutExtension.substring(0, nameWithoutExtension.lastIndexOf(".")); //$NON-NLS-1$
        setName(nameWithoutExtension);
    }

    private void loadFromServer() {
        final QueryDefinition definition =
            (QueryDefinition) workItemClient.getProjects().get(getProjectName()).getQueryHierarchy().find(guid);

        if (definition.getQueryText() == null || definition.getQueryText().equals("")) //$NON-NLS-1$
        {
            throw new WorkItemException(Messages.getString("QueryDocument.QueryDefinitionDoesNoHaveAssocQuery")); //$NON-NLS-1$
        }

        setDocumentFieldsFromWIQL(definition.getQueryText());
        setName(definition.getName());
        setParentGUID(definition.getParent().getID());
    }

    public void save() {
        if (file != null) {
            saveToFile();
        } else {
            saveToServer();
        }

        ResultOptionsColumnWidthPersistence.persist(new MementoRepository(
            workItemClient.getConnection().getPersistenceStoreProvider().getCachePersistenceStore()), this);

        dirty = false;
        notifyDirtyListeners();

        notifySaveListeners();

        saveState();
    }

    private void saveToFile() {
        final String queryText = getQueryText();
        final String teamName;
        if (StringUtil.isNullOrEmpty(queryText) || !queryText.contains(WIQLOperators.MACRO_CURRENT_ITERATION)) {
            teamName = null;
        } else {
            teamName = getTeamName();
        }

        final WIQDocument wiqDocument = new WIQDocument(
            queryText,
            getWorkItemClient().getConnection().getBaseURI().toString(),
            getProjectName(),
            teamName);

        wiqDocument.save(file);
    }

    private void saveToServer() {
        QueryDefinition queryDefinition;
        if (guid == null) {
            final QueryItem queryItem = getExistingQueryByName(getName());

            if (queryItem == null || !(queryItem instanceof QueryDefinition)) {
                final QueryFolder parent = getFolder(getParentGUID(), getProjectName());
                queryDefinition = parent.newDefinition(name, getQueryText());
            } else {
                queryDefinition = (QueryDefinition) queryItem;
            }
        } else {
            queryDefinition = getQueryDefinition();

            /*
             * The query definition will be null if the query was deleted by
             * another user, or if permissions don't allow this user to access
             * it.
             */
            if (queryDefinition == null) {
                throw new WorkItemException(
                    MessageFormat.format(
                        Messages.getString("QueryDocument.QueryCouldNotSaveToSpecifiedLocationFormat"), //$NON-NLS-1$
                        name));
            }
        }

        queryDefinition.setName(name);
        queryDefinition.setQueryText(getQueryText());

        try {
            workItemClient.getProjects().get(getProjectName()).getQueryHierarchy().save();
        } catch (final RuntimeException e) {
            workItemClient.getProjects().get(getProjectName()).getQueryHierarchy().reset();
            throw e;
        }
    }

    private QueryFolder getFolder(final GUID id, final String project) {
        final QueryItem possibleFolder = workItemClient.getProjects().get(project).getQueryHierarchy().find(id);

        if (possibleFolder instanceof QueryFolder) {
            return ((QueryFolder) possibleFolder);
        }

        return null;
    }

    public QueryItem getExistingQueryByName(final String name) {
        final QueryItem parent = workItemClient.getProjects().get(projectName).getQueryHierarchy().find(parentGuid);

        if (parent instanceof QueryFolder && ((QueryFolder) parent).containsName(name)) {
            return ((QueryFolder) parent).getItemByName(name);
        }

        return null;
    }

    private QueryDefinition getQueryDefinition() {
        if (guid != null) {
            final QueryItem queryItem =
                workItemClient.getProjects().get(getProjectName()).getQueryHierarchy().find(guid);

            if (queryItem instanceof QueryDefinition) {
                return (QueryDefinition) queryItem;
            }
        }
        return null;
    }

    public QueryScope getQueryScope() {
        QueryItem queryDefinition = getQueryDefinition();

        if (queryDefinition == null) {
            queryDefinition = getParentFolder();
        }

        if ((queryDefinition != null) && !queryDefinition.isPersonal()) {
            return QueryScope.PUBLIC;
        }

        return QueryScope.PRIVATE;
    }

    private QueryFolder getParentFolder() {
        return getFolder(getParentGUID(), getProjectName());
    }

    public void setQueryScope(final QueryScope scope) {
        if (!getQueryScope().equals(scope)) {
            final QueryFolder defaultParent = WorkItemClient.getDefaultParent(
                workItemClient.getProjects().get(getProjectName()),
                (scope == QueryScope.PUBLIC));

            parentGuid = defaultParent.getID();
            dirty = true;
            notifyDirtyListeners();
        }
    }

    public ResultOptions getResultOptions() {
        return resultOptions;
    }

    public void setResultOptions(final ResultOptions resultOptions) {
        Check.notNull(resultOptions, "resultOptions"); //$NON-NLS-1$

        int changeType =
            ResultOptions.CHANGE_TYPE_SORT | ResultOptions.CHANGE_TYPE_COLUMNS | ResultOptions.CHANGE_TYPE_WIDTHS;
        if (this.resultOptions != null) {
            changeType = ResultOptions.determineChange(this.resultOptions, resultOptions);
        }
        this.resultOptions = resultOptions;
        if (changeType != ResultOptions.CHANGE_TYPE_WIDTHS && changeType != ResultOptions.CHANGE_TYPE_NONE) {
            dirty = true;
            notifyDirtyListeners();
        }
    }

    public String getFilterExpression() {
        if (filterExpression == null) {
            return ""; //$NON-NLS-1$
        }
        return filterExpression;
    }

    public void setFilterExpression(final String filterExpression) {
        final boolean equal =
            (this.filterExpression == null ? filterExpression == null : this.filterExpression.equals(filterExpression));
        if (!equal) {
            this.filterExpression = filterExpression;
            dirty = true;
            notifyDirtyListeners();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        /*
         * TODO: validate input (eg QuerySaveAsDialog.IsNameValid())
         */
        this.name = name;
        dirty = true;
        notifyDirtyListeners();
    }

    /**
     * Update the document name without marking the document dirty.
     *
     * @param name
     *        The new name for the document.
     */
    public void updateName(final String name) {
        this.name = name;
    }

    public String getHierarchicalPath() {
        final QueryFolder parentFolder = getParentFolder();
        final StringBuffer sb = new StringBuffer();
        sb.append(QueryFolderUtil.getHierarchicalPath(parentFolder));
        if (sb.length() > 0) {
            sb.append(QueryFolderUtil.PATH_HIERARCHY_SEPARATOR);
        }
        sb.append(getName());
        return sb.toString();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
        dirty = true;
        notifyDirtyListeners();
    }

    public String getQueryText() {
        return generateWIQL(getResultOptions());
    }

    public void setQueryText(final String queryText) {
        setDocumentFieldsFromWIQL(queryText);
        dirty = true;
        notifyDirtyListeners();
    }

    public LinkQueryMode getQueryMode() {
        return queryMode;
    }

    public void setQueryMode(final LinkQueryMode queryMode) {
        this.queryMode = queryMode;
        dirty = true;
        notifyDirtyListeners();
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public void setQueryType(final QueryType queryType) {
        this.queryType = queryType;
        resultOptions.onQueryTypeChanged(workItemClient.getFieldDefinitions());

        dirty = true;
        notifyDirtyListeners();
    }

    public void reset() {
        restoreState();
    }

    public GUID getGUID() {
        return guid;
    }

    public GUID getParentGUID() {
        return parentGuid;
    }

    public File getFile() {
        return file;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(final String projectName) {
        this.projectName = projectName;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(final String teamName) {
        this.teamName = teamName;
    }

    public WorkItemClient getWorkItemClient() {
        return workItemClient;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void clearDirty() {
        dirty = false;
        notifyDirtyListeners();
    }

    public void addDirtyListener(final QueryDocumentDirtyListener listener) {
        dirtyListeners.addListener(listener);
    }

    public void removeDirtyListener(final QueryDocumentDirtyListener listener) {
        dirtyListeners.removeListener(listener);
    }

    private void notifyDirtyListeners() {
        final QueryDocumentDirtyListener[] listeners = (QueryDocumentDirtyListener[]) dirtyListeners.getListeners(
            new QueryDocumentDirtyListener[dirtyListeners.size()]);

        for (int i = 0; i < listeners.length; i++) {
            listeners[i].dirtyStateChanged(this);
        }
    }

    public void addSaveListener(final QueryDocumentSaveListener listener) {
        saveListeners.addListener(listener);
    }

    public void removeSaveListener(final QueryDocumentSaveListener listener) {
        saveListeners.removeListener(listener);
    }

    private void notifySaveListeners() {
        final QueryDocumentSaveListener[] listeners = (QueryDocumentSaveListener[]) saveListeners.getListeners(
            new QueryDocumentSaveListener[saveListeners.size()]);

        for (int i = 0; i < listeners.length; i++) {
            listeners[i].onQueryDocumentSaved(this);
        }
    }

    private String generateWIQL(final ResultOptions resultOptions) {
        return generateWIQL(resultOptions, getFilterExpression());
    }

    private String generateWIQL(final ResultOptions resultOptions, String whereClause) {
        if (whereClause.trim().length() > 0) {
            whereClause = MessageFormat.format(WHERE_CLAUSE_FORMAT, new Object[] {
                WHERE_STRING,
                whereClause
            });
        }

        final String tableName = LinkQueryMode.WORK_ITEMS.equals(queryMode) ? WIQLConstants.WORK_ITEM_TABLE
            : WIQLConstants.WORK_ITEM_LINK_TABLE;

        String mode = ""; //$NON-NLS-1$
        if (LinkQueryMode.LINKS_MUST_CONTAIN.equals(queryMode)) {
            mode = WIQLConstants.MUST_CONTAIN;
        } else if (LinkQueryMode.LINKS_MUST_CONTAIN.equals(queryMode)) {
            mode = WIQLConstants.MUST_CONTAIN;
        } else if (LinkQueryMode.LINKS_MAY_CONTAIN.equals(queryMode)) {
            mode = WIQLConstants.MAY_CONTAIN;
        } else if (LinkQueryMode.LINKS_DOES_NOT_CONTAIN.equals(queryMode)) {
            mode = WIQLConstants.DOES_NOT_CONTAIN;
        } else if (LinkQueryMode.LINKS_RECURSIVE.equals(queryMode)) {
            mode = WIQLConstants.RECURSIVE;
        }

        final String modeClause = (mode.length() > 0) ? MessageFormat.format(MODE_CLAUSE_FORMAT, new Object[] {
            mode
        }) : ""; //$NON-NLS-1$

        return MessageFormat.format(SELECT_FORMAT_STRING, new Object[] {
            resultOptions.getSelectClause(workItemClient.getFieldDefinitions()),
            tableName,
            whereClause,
            resultOptions.getOrderByClause(workItemClient.getFieldDefinitions()),
            modeClause
        });
    }

    private void saveState() {
        saveParent = parentGuid;
        saveScope = scope;
        saveResultOptions = new ResultOptions(resultOptions, true, this);
        saveQueryType = getQueryType();
        saveQueryMode = getQueryMode();
        saveFilterExpression = filterExpression;
        saveName = name;
        saveDescription = description;
    }

    private void restoreState() {
        setParentGUID(saveParent);
        setQueryScope(saveScope);
        setResultOptions(saveResultOptions);
        setFilterExpression(saveFilterExpression);
        setName(saveName);
        setDescription(saveDescription);
        setQueryType(saveQueryType);
        setQueryMode(saveQueryMode);

        dirty = false;
        notifyDirtyListeners();
    }

    public boolean isLinkQuery() {
        return !LinkQueryMode.WORK_ITEMS.equals(queryMode);
    }

    public boolean isTreeQuery() {
        return LinkQueryMode.LINKS_RECURSIVE.equals(queryMode);
    }

    private void setDocumentFieldsFromWIQL(final String wiql) {
        if (wiql != null && wiql.length() > 0) {
            final NodeSelect select = Parser.parseSyntax(wiql);
            getResultOptions().getDisplayFields().clear();
            getResultOptions().getSortFields().clear();
            if (select.getWhere() != null) {
                setFilterExpression(WIQLTranslator.wiqlNodeAsString(select.getWhere(), wiql));
            } else {
                setFilterExpression(""); //$NON-NLS-1$
            }
            queryMode = WIQLAdapter.getQueryMode(select);
            queryType = QueryDefinitionUtil.getQueryType(queryMode);

            if (select.getFields() != null) {
                for (int i = 0; i < select.getFields().getCount(); i++) {
                    final NodeFieldName node = select.getFields().getNodeFieldNameItem(i);
                    final String name =
                        DisplayField.getLocalizedFieldName(node.getValue(), workItemClient.getFieldDefinitions());
                    getResultOptions().getDisplayFields().add(
                        new DisplayField(
                            name,
                            ResultOptions.getDefaultColumnWidth(name, workItemClient.getFieldDefinitions())));
                }
            }

            if (guid != null || file != null) {
                ResultOptionsColumnWidthPersistence.restore(
                    new MementoRepository(
                        workItemClient.getConnection().getPersistenceStoreProvider().getCachePersistenceStore()),
                    this);
            }

            if (select.getOrderBy() != null) {
                for (int i = 0; i < select.getOrderBy().getCount(); i++) {
                    final NodeFieldName node = select.getOrderBy().getNodeFieldNameItem(i);
                    final String name =
                        DisplayField.getLocalizedFieldName(node.getValue(), workItemClient.getFieldDefinitions());
                    getResultOptions().getSortFields().add(
                        new SortField(name, node.getDirection() != Direction.DESCENDING));
                }
            }
        } else {
            getResultOptions().getDisplayFields().clear();
            getResultOptions().getSortFields().clear();
            setFilterExpression(""); //$NON-NLS-1$
        }
    }
}
