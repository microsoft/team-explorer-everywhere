// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.artifact.ArtifactIDFactory;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.query.Query;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._Change;
import ms.tfs.versioncontrol.clientservices._03._Changeset;
import ms.tfs.versioncontrol.clientservices._03._PropertyValue;

/**
 * Represents a set of changes committed to the repository.
 *
 * @since TEE-SDK-10.1
 */
public final class Changeset extends WebServiceObjectWrapper {
    /**
     * Value used as the largest possible changeset number in any TFS
     * repository.
     */
    public static final int MAX = 2147483647;

    /**
     * Contains the change array directly. These are cached in the wrapper for
     * speed, because storing them in the wrapped object means unwrapping them
     * every time {@link #getChanges()} is called.
     * {@link #getWebServiceObject()} will always update the wrapped web service
     * object before it returns it (it will be up-to-date).
     *
     * Constructors must initialize this field from available data.
     */
    private Change[] changes;

    /**
     * Work items associated with this changeset. Populated on demand (and
     * cached) via {@link #getWorkItems(WorkItemClient)}.
     */
    private WorkItem[] workItems = null;

    /**
     * Gets a default date for a {@link Changeset}'s construction, which is the
     * .NET epoch. Use this date for normal changeset construction during
     * checkin.
     */
    public static Calendar getDefaultDate() {
        /*
         * IMPORTANT: TFS 2010 requires CheckinOther permissions if this date is
         * after 1850/01/01 (.NET's "beginning of time"). We use 1/1/1 UTC to
         * match Visual Studio's behavior here, and not require those extra
         * permissions.
         */

        final Calendar cal = new GregorianCalendar(1, Calendar.JANUARY, 1, 0, 0, 0);
        cal.setTimeZone(TimeZone.getTimeZone("GMT")); //$NON-NLS-1$

        return cal;
    }

    public Changeset() {
        this(new _Changeset());
    }

    public Changeset(final _Changeset changeset) {
        super(changeset);

        if (changeset.getChanges() != null) {
            changes = (Change[]) WrapperUtils.wrap(Change.class, changeset.getChanges());
        }

        final String committerDisplayName = changeset.getCmtrdisp();
        if (committerDisplayName == null || committerDisplayName.length() == 0) {
            changeset.setCmtrdisp(changeset.getCmtr());
        }

        final String ownerDisplayName = changeset.getOwnerdisp();
        if (ownerDisplayName == null || ownerDisplayName.length() == 0) {
            changeset.setOwnerdisp(changeset.getOwner());
        }
    }

    public Changeset(final Changeset changeset, final Change change) {
        this(changeset.getWebServiceObject());
        changes = new Change[] {
            change
        };
    }

    public Changeset(
        final String owner,
        final String comment,
        final CheckinNote checkinNote,
        final PolicyOverrideInfo policyOverride) {
        /*
         * Use a null date to let the server fill it in.
         */
        this(null, comment, checkinNote, policyOverride, null, null, null, -1, owner, null, null);
    }

    /**
     * @param date
     *        the date the changeset was created at (when it is being imported
     *        from another version control system, for example), or
     *        <code>null</code> to let the server use the current time. Pass
     *        <code>null</code> for normal check-ins.
     */
    public Changeset(
        final Change[] changes,
        final String comment,
        final CheckinNote checkinNote,
        final PolicyOverrideInfo policyOverride,
        final String committer,
        final String committerDisplayName,
        final Calendar date,
        final int changeSetID,
        final String owner,
        final String ownerDisplayName,
        final PropertyValue[] properties) {
        this(
            new _Changeset(
                committer,
                committerDisplayName,
                date != null ? date : getDefaultDate(),
                changeSetID,
                owner,
                ownerDisplayName,
                comment,
                checkinNote != null ? checkinNote.getWebServiceObject() : null,
                policyOverride != null ? policyOverride.getWebServiceObject() : null,
                properties != null ? (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, properties) : null,
                changes != null ? (_Change[]) WrapperUtils.unwrap(_Change.class, changes) : null));
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Changeset getWebServiceObject() {
        /*
         * Update the changes in the wrapped object.
         */
        getWebServiceObjectInternal().setChanges((_Change[]) WrapperUtils.unwrap(_Change.class, changes));

        return getWebServiceObjectInternal();
    }

    /**
     * Gets the web service object but does not update it.
     *
     * Prefer this method internally.
     */
    private _Changeset getWebServiceObjectInternal() {
        return (_Changeset) webServiceObject;
    }

    /**
     * Set the work items which reference this changeset. A helper method so
     * that UI can query work items independently but set them here.
     *
     * @param workItems
     *        The work items which reference this changeset.
     */
    public synchronized void setWorkItems(final WorkItem[] workItems) {
        Check.notNull(workItems, "workItems"); //$NON-NLS-1$

        this.workItems = workItems;
    }

    /**
     * Gets the work items which reference this changeset.
     *
     * @return the work items which reference this changeset.
     * @throws TECoreException
     *         if this changeset is uncommitted (because uncommitted changesets
     *         cannot have work items which reference them).
     */
    public synchronized WorkItem[] getWorkItems() {
        if (getWebServiceObjectInternal().getCset() < 1) {
            throw new TECoreException(
                Messages.getString("Changeset.AnArtifactURICannotBeCreatedForAnUncommittedChangeset")); //$NON-NLS-1$
        }

        if (workItems == null) {
            return new WorkItem[0];
        }

        return workItems;
    }

    /**
     * Gets the work items which reference this changeset.
     *
     * @param workItemClient
     *        a work item client to use to perform the query.
     * @return the work items which reference this changeset.
     * @throws TECoreException
     *         if this changeset is uncommitted (because uncommitted changesets
     *         cannot have work items which reference them).
     */
    public synchronized WorkItem[] getWorkItems(final WorkItemClient workItemClient) throws TECoreException {
        Check.notNull(workItemClient, "workItemClient"); //$NON-NLS-1$

        if (workItems == null) {
            if (getWebServiceObjectInternal().getCset() < 1) {
                throw new TECoreException(
                    Messages.getString("Changeset.AnArtifactURICannotBeCreatedForAnUncommittedChangeset")); //$NON-NLS-1$
            }

            final Query query = workItemClient.createReferencingQuery(
                ArtifactIDFactory.newChangesetArtifactID(
                    getWebServiceObjectInternal().getCset()).encodeURI().toString());

            query.getDisplayFieldList().add(CoreFieldReferenceNames.WORK_ITEM_TYPE);
            query.getDisplayFieldList().add(CoreFieldReferenceNames.STATE);
            query.getDisplayFieldList().add(CoreFieldReferenceNames.ASSIGNED_TO);
            query.getDisplayFieldList().add(CoreFieldReferenceNames.TITLE);

            final WorkItemCollection itemCollection = query.runQuery();

            if (itemCollection != null) {
                final int size = itemCollection.size();
                workItems = new WorkItem[size];

                for (int i = 0; i < size; i++) {
                    workItems[i] = itemCollection.getWorkItem(i);
                }
            } else {
                workItems = new WorkItem[0];
            }
        }

        return workItems;
    }

    public Change[] getChanges() {
        /*
         * Return the data from the cache field.
         */
        return changes;
    }

    public void setChanges(final Change[] changes) {
        /*
         * Update the cache field.
         */
        this.changes = changes;
    }

    public String getComment() {
        return getWebServiceObjectInternal().getComment();
    }

    public void setComment(final java.lang.String comment) {
        getWebServiceObjectInternal().setComment(comment);
    }

    public CheckinNote getCheckinNote() {
        return new CheckinNote(getWebServiceObjectInternal().getCheckinNote());
    }

    public void setCheckinNote(final CheckinNote checkinNote) {
        getWebServiceObjectInternal().setCheckinNote(checkinNote.getWebServiceObject());
    }

    public PolicyOverrideInfo getPolicyOverride() {
        return new PolicyOverrideInfo(getWebServiceObjectInternal().getPolicyOverride());
    }

    public void setPolicyOverride(final PolicyOverrideInfo policyOverride) {
        getWebServiceObjectInternal().setPolicyOverride(policyOverride.getWebServiceObject());
    }

    public String getCommitter() {
        return getWebServiceObjectInternal().getCmtr();
    }

    public void setCommitter(final String committer) {
        getWebServiceObjectInternal().setCmtr(committer);
    }

    public String getCommitterDisplayName() {
        return getWebServiceObjectInternal().getCmtrdisp();
    }

    public void setCommitterDisplayName(final String committer) {
        getWebServiceObjectInternal().setCmtrdisp(committer);
    }

    public Calendar getDate() {
        return getWebServiceObjectInternal().getDate();
    }

    public void setDate(final Calendar date) {
        getWebServiceObjectInternal().setDate(date);
    }

    public int getChangesetID() {
        return getWebServiceObjectInternal().getCset();
    }

    public void setChangesetID(final int changesetID) {
        getWebServiceObjectInternal().setCset(changesetID);
    }

    public String getOwner() {
        return getWebServiceObjectInternal().getOwner();
    }

    public void setOwner(final String owner) {
        getWebServiceObjectInternal().setOwner(owner);
    }

    public String getOwnerDisplayName() {
        return getWebServiceObjectInternal().getOwnerdisp();
    }

    public void setOwnerDisplayName(final String owner) {
        getWebServiceObjectInternal().setOwnerdisp(owner);
    }

    public PropertyValue[] getPropertyValues() {
        // TODO remove the selectUnique
        return PropertyUtils.selectUnique(
            (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObjectInternal().getProperties()));
    }

    /**
     * Sorts the changes in the changeset.
     */
    public void sortChanges() {
        Arrays.sort(changes);
    }
}
