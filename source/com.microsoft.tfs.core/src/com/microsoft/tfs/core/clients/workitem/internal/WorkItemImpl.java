// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.clients.registration.ToolNames;
import com.microsoft.tfs.core.clients.versioncontrol.events.EventSource;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateListener;
import com.microsoft.tfs.core.clients.workitem.events.WorkItemSaveEvent;
import com.microsoft.tfs.core.clients.workitem.exceptions.DeniedOrNotExistException;
import com.microsoft.tfs.core.clients.workitem.exceptions.UnableToSaveException;
import com.microsoft.tfs.core.clients.workitem.fields.FieldCollection;
import com.microsoft.tfs.core.clients.workitem.files.AttachmentCollection;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldModificationType;
import com.microsoft.tfs.core.clients.workitem.internal.files.AttachmentCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.link.LinkCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.node.NodeImpl;
import com.microsoft.tfs.core.clients.workitem.internal.revision.RevisionCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.revision.RevisionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.GetResultsRowSetHandler;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.RevisionsRowSetHandler;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.RowSetParser;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.WorkItemFilesRowSetHandler;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.WorkItemRelationsRowSetHandler;
import com.microsoft.tfs.core.clients.workitem.internal.rowset.WorkItemTextsRowSetHandler;
import com.microsoft.tfs.core.clients.workitem.internal.rules.IRuleTarget;
import com.microsoft.tfs.core.clients.workitem.internal.rules.IRuleTargetField;
import com.microsoft.tfs.core.clients.workitem.internal.rules.RuleEngine;
import com.microsoft.tfs.core.clients.workitem.internal.update.WorkItemUpdatePackage;
import com.microsoft.tfs.core.clients.workitem.internal.wittype.WorkItemTypeImpl;
import com.microsoft.tfs.core.clients.workitem.link.LinkCollection;
import com.microsoft.tfs.core.clients.workitem.link.LinkFactory;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.revision.RevisionCollection;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.core.ws.runtime.types.AnyContentType;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.core.ws.runtime.types.StaxAnyContentType;

import ms.tfs.workitemtracking.clientservices._03._ClientService2Soap_GetWorkItemResponse;
import ms.tfs.workitemtracking.clientservices._03._ClientService3Soap_GetWorkItemResponse;
import ms.tfs.workitemtracking.clientservices._03._ClientService5Soap_GetWorkItemResponse;

/*
 * TODO: make this class thread safe, or document that it is not.
 */
public class WorkItemImpl implements WorkItem, IRuleTarget {
    private static final Log log = LogFactory.getLog(WorkItemImpl.class);

    /*
     * Tracks the "edit state" of this work item. Right now this is a binary
     * value - the work item is either open for editing or it is not.
     *
     * An edit state is basically defined by two things: which attributes of a
     * work item have been retrieved from the server, and whether or not the
     * rule engine has been initially run on the current field values.
     *
     * When open is false, the work item has some but not all fields retrieved
     * from the server (using the Page webservice) and no links and no file
     * attachments. The rule engine has not been run. This is the case when a
     * WorkItem object is constructed as part of query results.
     *
     * When open is true, the work item has all links, file attachments, and
     * fields (retrieved by a call to GetWorkItem webservice) and the rule
     * engine has been initially run.
     *
     * Visual Studio's WIT/PS object model is more sophisticated here. They
     * offer four different edit states: none, partial, readonly, and full. None
     * is the same as open == false. Full is the same as open == true. Partial
     * means that most or all field values have been retrieved (using the Page
     * webservice) but no links or attachments, and no rules have been run.
     * Readonly is the same as open == true except that no rules are run, and
     * all fields are marked readonly.
     */
    private boolean open = false;

    /*
     * we track the current dirty and valid states so we can avoid notifying
     * state listeners if the state has not changed
     *
     * see the comment in fireStateListenersIfNeeded()
     */
    private boolean currentDirtyState = false;

    /*
     * the WIT context
     */
    private final WITContext witContext;

    /*
     * the four collections that hold the data in this work item
     */
    private final FieldCollectionImpl fieldCollection;
    private final LinkCollectionImpl linkCollection;
    private final AttachmentCollectionImpl attachmentCollection;
    private final RevisionCollectionImpl revisionCollection;

    /*
     * Temporary work item id is supplied with the SOAP XML when saving a new
     * work item. If the work item has links the link will specify this TempId
     * as the source id of the link.
     */
    private int tempId = 0;
    private static int TEMPID = 0;
    private static Object TEMPIDLOCK = new Object();

    /*
     * the state listeners for this work item
     */
    private final WorkItemStateListenerSupport stateListeners = new WorkItemStateListenerSupport(this);

    public WorkItemImpl(final WITContext witContext) {
        /*
         * Note that the constructor for a work item is basically a no-op.
         * Simply constructing a work item object does not set any initial field
         * values, and no round trips to the server are initiated.
         */

        this.witContext = witContext;

        /*
         * Initialize the four data collections. Note that construction of these
         * objects does nothing except for store references to the objects
         * passed in the constructors.
         */
        linkCollection = new LinkCollectionImpl(this);
        attachmentCollection = new AttachmentCollectionImpl(this);
        revisionCollection = new RevisionCollectionImpl();
        fieldCollection = new FieldCollectionImpl(this, witContext);

        /*
         * The edit state of a work item when initially constructed is open ==
         * false. To change the edit state, the open() method must be called.
         */
        open = false;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof WorkItemImpl) {
            final WorkItemImpl other = (WorkItemImpl) obj;
            if (fieldCollection.getID() == 0) {
                /*
                 * a new (id == 0) work item is only equal to another work item
                 * if they are exactly the same object
                 */
                return this == other;
            } else {
                /*
                 * normal case: two work items are equal if their IDs are equal
                 */
                return fieldCollection.getID() == other.fieldCollection.getID();
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return fieldCollection.getID();
    }

    @Override
    public String toString() {
        final String messageFormat = Messages.getString("WorkItemImpl.WorkItemIdWithHexFormat"); //$NON-NLS-1$
        return MessageFormat.format(messageFormat, getID(), Integer.toHexString(System.identityHashCode(this)));
    }

    /***************************************************************************
     * START of implementation of WorkItem interface
     **************************************************************************/

    @Override
    public WorkItemClient getClient() {
        return witContext.getClient();
    }

    @Override
    public void open() {
        if (open) {
            /*
             * already open - do nothing
             */
            return;
        }

        /*
         * sanity check
         */
        if (!fieldCollection.hasField(WorkItemFieldIDs.ID)) {
            throw new IllegalStateException(Messages.getString("WorkItemImpl.AttemptToOpenItemWithNoIdField")); //$NON-NLS-1$
        }

        /*
         * sanity check
         */
        if (fieldCollection.getFieldInternal(WorkItemFieldIDs.ID).getValue() == null) {
            throw new IllegalStateException(Messages.getString("WorkItemImpl.AttemptToOpenItemWithNoIdValue")); //$NON-NLS-1$
        }

        final int id = fieldCollection.getID();

        /*
         * If this is not a new work item (id != 0), we call the GetWorkItem web
         * service as part of open().
         */
        if (id != 0) {
            log.info(MessageFormat.format("Opening work item {0} from server", Integer.toString(id))); //$NON-NLS-1$

            final AnyContentType metadata;
            final String dbStamp;
            final DOMAnyContentType domContent;
            if (witContext.isVersion2()) {
                final _ClientService2Soap_GetWorkItemResponse response = witContext.getProxy().getWorkItem(
                    id,
                    0,
                    0,
                    null,
                    true,
                    witContext.getMetadataUpdateHandler().getHaveEntries(),
                    new DOMAnyContentType(),
                    new StaxAnyContentType());

                metadata = response.getMetadata();
                dbStamp = response.getDbStamp();
                domContent = (DOMAnyContentType) response.getWorkItem();
            } else if (witContext.isVersion3()) {
                final _ClientService3Soap_GetWorkItemResponse response = witContext.getProxy3().getWorkItem(
                    id,
                    0,
                    0,
                    null,
                    true,
                    witContext.getMetadataUpdateHandler().getHaveEntries(),
                    new DOMAnyContentType(),
                    new StaxAnyContentType());

                metadata = response.getMetadata();
                dbStamp = response.getDbStamp();
                domContent = (DOMAnyContentType) response.getWorkItem();
            } else {
                final _ClientService5Soap_GetWorkItemResponse response = witContext.getProxy5().getWorkItem(
                    id,
                    0,
                    0,
                    null,
                    true,
                    witContext.getMetadataUpdateHandler().getHaveEntries(),
                    new DOMAnyContentType(),
                    new StaxAnyContentType());

                metadata = response.getMetadata();
                dbStamp = response.getDbStamp();
                domContent = (DOMAnyContentType) response.getWorkItem();
            }

            /*
             * do the incremental metadata update
             */
            witContext.getMetadataUpdateHandler().updateMetadata(metadata, dbStamp);

            /*
             * Dispose of the StaxAnyContentType.
             */
            metadata.dispose();

            /*
             * callback to the collections before populating them: -- the field
             * collection could hold old values from query results -- the reason
             * for doing this to the other collections isn't as good - basically
             * we could be in the case where revert() was called followed by
             * open() in which case we want to clear out old data
             *
             * this code will need to be rethought once more of the work item
             * lifecycle is clear
             */
            fieldCollection.reset();
            fieldCollection.ensureAllFieldsInWIPhysicalType();
            linkCollection.internalClear();
            attachmentCollection.internalClear();
            revisionCollection.internalClear();

            /*
             * the open webservice method gives us back 6 rowsets (we ignore the
             * 3rd rowset as it's not used in v1)
             */
            final Element workItemInfoTable = domContent.getElements()[0];
            final Element revisionsTable = domContent.getElements()[1];
            // Ignore Keywords table domContent.getElements()[2];
            final Element textsTable = domContent.getElements()[3];
            final Element filesTable = domContent.getElements()[4];
            final Element relationsTable = domContent.getElements()[5];
            // Only on Rosario or greater
            // Element relationRevisions = domContent.getElements()[6];

            /*
             * prepare a rowset parser
             */
            final RowSetParser parser = new RowSetParser();

            /*
             * parse in the WorkItemInfo rowset
             */
            final GetResultsRowSetHandler getTableHandler = new GetResultsRowSetHandler(this);
            parser.parse(workItemInfoTable, getTableHandler);
            if (!getTableHandler.parsedRow()) {
                /*
                 * if we get here, it means the get webservice method didn't
                 * return work item data back, either because the ID is invalid
                 * or the user doesn't have access to that ID
                 */
                throw new DeniedOrNotExistException(
                    MessageFormat.format(
                        Messages.getString("WorkItemImpl.DoesNotExistOrAccessDeniedFormat"), //$NON-NLS-1$
                        Integer.toString(getID())));
            }

            /*
             * parse in the remaining rowsets
             */
            parser.parse(revisionsTable, new RevisionsRowSetHandler(this));
            parser.parse(textsTable, new WorkItemTextsRowSetHandler(this, witContext.getMetadata()));
            parser.parse(filesTable, new WorkItemFilesRowSetHandler(this, witContext.getMetadata()));
            parser.parse(relationsTable, new WorkItemRelationsRowSetHandler(this, witContext.getMetadata()));
        }

        /*
         * If we supported the "readonly" edit state, one of the parts of that
         * support would involve skipping this call for that edit state.
         */
        runRulesForOpen();

        /*
         * We are now in the full open edit state.
         */
        open = true;
    }

    @Override
    public void save() throws UnableToSaveException {
        if (!open) {
            throw new IllegalStateException(Messages.getString("WorkItemImpl.CannotSave")); //$NON-NLS-1$
        }

        internalSave();

        fireStateListenersIfNeeded();
        stateListeners.fireSaved();

        getClient().getEventEngine().fireWorkItemSaveEvent(new WorkItemSaveEvent(EventSource.newFromHere(), this));
    }

    @Override
    public String getNextState(final String action) {
        return getTypeInternal().getNextState(
            (String) getFields().getField(CoreFieldReferenceNames.STATE).getValue(),
            action);
    }

    @Override
    public Project getProject() {
        return getType().getProject();
    }

    @Override
    public WorkItemType getType() {
        final int areaId = getFields().getAreaID();
        final String workItemTypeName = getFields().getWorkItemType();

        NodeImpl node = witContext.getRootNode().findNodeDownwards(areaId);
        node = node.getProjectNodeParent();

        final String projectName = node.getName();

        return getClient().getProjects().get(projectName).getWorkItemTypes().get(workItemTypeName);
    }

    @Override
    public void reset() {
        if (!open) {
            /*
             * If the work item is not open, reset() is a no-op.
             */
            return;
        }

        WorkItemType type = null;
        final boolean newlyCreated = (fieldCollection.getID() == 0);
        if (newlyCreated) {
            /*
             * save the type - once we call fieldCollection.reset() calling
             * getType() will not work
             */
            type = getType();
        }

        fieldCollection.reset();
        linkCollection.reset();
        attachmentCollection.reset();

        if (newlyCreated) {
            /*
             * Calling fieldCollection.reset() reverts all fields to their
             * original values. In the newly created work item case, this will
             * mean that every field is null (since there are no original values
             * in the newly created case). In this special case we re-initialize
             * the new work item.
             */
            witContext.initNewWorkItem(this, type);
        }

        /*
         * The open rules must be run again.
         */
        runRulesForOpen();

        fireStateListenersIfNeeded();
    }

    @Override
    public void syncToLatest() {
        /*
         * SyncToLatest is only supported for work items that are not newly
         * created.
         */
        if (fieldCollection.getID() == 0) {
            /*
             * I18N
             */
            throw new UnsupportedOperationException(
                Messages.getString("WorkItemImpl.CannotPerformOperationItemNotSaved")); //$NON-NLS-1$
        }

        open = false;

        open();

        fireStateListenersIfNeeded();

        stateListeners.fireSynchedToLatest();

        fieldCollection.fireFieldChangeListeners();
    }

    @Override
    public boolean isValid() {
        return fieldCollection.isValid();
    }

    @Override
    public boolean isDirty() {
        /*
         * normal case: if any of fields, links, attachments are dirty, the work
         * item is dirty
         */
        return fieldCollection.isDirty() || linkCollection.isDirty() || attachmentCollection.isDirty();
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public AttachmentCollection getAttachments() {
        return attachmentCollection;
    }

    @Override
    public FieldCollection getFields() {
        return fieldCollection;
    }

    @Override
    public RevisionCollection getRevisions() {
        return revisionCollection;
    }

    @Override
    public LinkCollection getLinks() {
        return linkCollection;
    }

    @Override
    public void addWorkItemStateListener(final WorkItemStateListener listener) {
        stateListeners.addListener(listener);
    }

    @Override
    public void removeWorkItemStateListener(final WorkItemStateListener listener) {
        stateListeners.removeListener(listener);
    }

    @Override
    public String getURI() {
        final ArtifactID id = new ArtifactID(
            ToolNames.WORK_ITEM_TRACKING,
            InternalWorkItemConstants.WORK_ITEM_ARTIFACT_TYPE,
            String.valueOf(getFields().getID()));
        return id.encodeURI();
    }

    @Override
    public String getTitle() {
        return (String) getFields().getField(CoreFieldReferenceNames.TITLE).getValue();
    }

    @Override
    public void setTitle(final String title) {
        getFields().getField(CoreFieldReferenceNames.TITLE).setValue(title);
    }

    @Override
    public WorkItem copy() {
        return copy(getType());
    }

    @Override
    public WorkItem copy(final WorkItemType targetType) {
        if (!open) {
            throw new UnsupportedOperationException(Messages.getString("WorkItemImpl.CopyNotSupportedWhenItemNotOpen")); //$NON-NLS-1$
        }

        final WorkItemImpl newWorkItem = new WorkItemImpl(witContext);
        witContext.initNewWorkItem(newWorkItem, targetType);

        fieldCollection.copy(newWorkItem.fieldCollection);
        linkCollection.copy(newWorkItem.linkCollection);

        /*
         * file attachments are not copied as per the MS implementation
         */

        /*
         * If the source work item (this) is not new (id != 0), then add a
         * related link between the source and target work items.
         */
        if (getFields().getID() != 0) {
            final RelatedLink relatedLink = LinkFactory.newRelatedLink(newWorkItem, this, null, false);
            newWorkItem.linkCollection.add(relatedLink);
        }

        newWorkItem.open();

        return newWorkItem;
    }

    /***************************************************************************
     * END of implementation of WorkItem interface
     **************************************************************************/

    /***************************************************************************
     * START of implementation of internal (WorkItemImpl) methods
     **************************************************************************/

    private void runRulesForOpen() {
        /*
         * Run the initial set of rules.
         */
        final RuleEngine engine = new RuleEngine(this, witContext);
        engine.open();

        /*
         * set the changed by field to the current user
         */
        final String changedBy = getContext().getCurrentUserDisplayName();
        getFieldsInternal().getFieldInternal(WorkItemFieldIDs.CHANGED_BY).setValue(
            changedBy,
            FieldModificationType.INTERNAL_MODEL);
    }

    public TFSTeamProjectCollection getConnection() {
        return witContext.getConnection();
    }

    public WorkItemTypeImpl getTypeInternal() {
        return (WorkItemTypeImpl) getType();
    }

    public WITContext getContext() {
        return witContext;
    }

    @Override
    public int getID() {
        return fieldCollection.getID();
    }

    @Override
    public int getTemporaryID() {
        if (tempId == 0) {
            synchronized (TEMPIDLOCK) {
                tempId = --TEMPID;
            }
        }
        return tempId;
    }

    private void internalSave() throws UnableToSaveException {
        attachmentCollection.preSave();

        final int id = getID();

        if (id == 0) {
            log.info(MessageFormat.format("Creating new work item of type {0}", getType().getName())); //$NON-NLS-1$
        } else {
            log.info(MessageFormat.format("Updating work item {0}", Integer.toString(getID()))); //$NON-NLS-1$
        }

        final WorkItemUpdatePackage updatePackage = new WorkItemUpdatePackage(this, witContext);

        updatePackage.update();

        final RevisionImpl newRevision = RevisionImpl.createFromFieldCollection(
            fieldCollection,
            witContext,
            revisionCollection.size(),
            revisionCollection);
        revisionCollection.addRevisionToEnd(newRevision);

        fieldCollection.resetAfterUpdate();
        linkCollection.update();
        attachmentCollection.update();

        /*
         * TODO should we be calling runRulesForOpen() instead?
         */
        final RuleEngine engine = new RuleEngine(this, witContext);
        engine.open();

        fireStateListenersIfNeeded();
    }

    public AttachmentCollectionImpl getAttachmentsInternal() {
        return attachmentCollection;
    }

    public FieldCollectionImpl getFieldsInternal() {
        return fieldCollection;
    }

    public LinkCollectionImpl getLinksInternal() {
        return linkCollection;
    }

    public RevisionCollectionImpl getRevisionsInternal() {
        return revisionCollection;
    }

    public void fireStateListenersIfNeeded() {
        final boolean newDirtyState = isDirty();
        if (currentDirtyState != newDirtyState) {
            currentDirtyState = newDirtyState;
            stateListeners.fireDirtyStateChanged(newDirtyState);
        }

        /*
         * The original intention of this method was to only fire the state
         * listeners when there was a state change - for example, when dirty had
         * changed from false to true, and not just when there was a possibility
         * of a state change.
         *
         * However, it turns out that part of the work item UI needs something a
         * little more sophisticated than that. For the form, we want to show an
         * error message for the first invalid field. This means that the UI
         * needs to know not just when the valid state has changed, but even
         * when the valid state stays the same it needs to get a notification.
         * For example, when the reason a field is invalid has changed the UI
         * needs to update a message.
         *
         * For now, the fix is to just fire the valid listeners every time there
         * is a potential valid state change. This isn't the best long term fix
         * and hopefully we can come up with something better in the future,
         * either by making the UI or the core more sophisticated.
         */

        /*
         * boolean newValidState = isValid(); if (currentValidState !=
         * newValidState) { currentValidState = newValidState;
         * stateListeners.fireValidStateChanged(newValidState); }
         */
        stateListeners.fireValidStateChanged(isValid());
    }

    public String getUpdateXMLForDebugging() {
        return new WorkItemUpdatePackage(this, witContext).getUpdateXML();
    }

    /***************************************************************************
     * START of implementation of IRuleTarget interface
     **************************************************************************/

    @Override
    public int getAreaID() {
        return fieldCollection.getAreaID();
    }

    @Override
    public IRuleTargetField getRuleTargetField(final int fieldId) {
        return fieldCollection.getFieldInternal(fieldId);
    }

    /***************************************************************************
     * END of implementation of IRuleTarget interface
     **************************************************************************/
}
