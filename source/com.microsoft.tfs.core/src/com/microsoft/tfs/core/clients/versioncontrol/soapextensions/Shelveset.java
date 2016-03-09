// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.soapextensions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.microsoft.tfs.core.artifact.ArtifactID;
import com.microsoft.tfs.core.artifact.ArtifactIDFactory;
import com.microsoft.tfs.core.clients.versioncontrol.PropertyUtils;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.WorkItemCheckedInfo;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.internal.wrappers.WebServiceObjectWrapper;
import com.microsoft.tfs.core.internal.wrappers.WrapperUtils;
import com.microsoft.tfs.util.Check;

import ms.tfs.versioncontrol.clientservices._03._PropertyValue;
import ms.tfs.versioncontrol.clientservices._03._Shelveset;
import ms.tfs.versioncontrol.clientservices._03._VersionControlLink;

/**
 * A server-side collection of pending changes and associated metadata.
 *
 * @threadsafety thread-compatible
 * @since TEE-SDK-10.1
 */
public final class Shelveset extends WebServiceObjectWrapper {
    private WorkItemCheckinInfo[] workItemInfo = null;

    public Shelveset() {
        this(new _Shelveset());
    }

    public Shelveset(final _Shelveset shelveset) {
        super(shelveset);

        final String displayName = shelveset.getOwnerdisp();
        if (displayName == null || displayName.length() == 0) {
            shelveset.setOwnerdisp(shelveset.getOwner());
        }
    }

    public Shelveset(
        final String name,
        final String owner,
        final String ownerDisplayName,
        final String comment,
        final String policyOverrideComment,
        final CheckinNote checkinNote,
        final VersionControlLink[] links,
        final Calendar creationDate,
        final boolean changesExcluded,
        final PropertyValue[] propertyValues) {
        this(
            new _Shelveset(
                creationDate,
                name,
                owner,
                ownerDisplayName,
                owner,
                changesExcluded,
                comment,
                policyOverrideComment,
                (checkinNote) != null ? checkinNote.getWebServiceObject() : null,
                (_VersionControlLink[]) WrapperUtils.unwrap(_VersionControlLink.class, links),
                (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, propertyValues)));
    }

    public Shelveset(
        final String name,
        final String owner,
        final String ownerDisplayName,
        final String comment,
        final String policyOverrideComment,
        final CheckinNote checkinNote,
        final WorkItemCheckedInfo[] checkedInfo,
        final Calendar creationDate,
        final boolean changesExcluded,
        final PropertyValue[] propertyValues) {
        this(
            new _Shelveset(
                creationDate,
                name,
                owner,
                ownerDisplayName,
                owner,
                changesExcluded,
                comment,
                policyOverrideComment,
                (checkinNote) != null ? checkinNote.getWebServiceObject() : null,
                createVersionControlLinks(checkedInfo),
                (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, propertyValues)));
    }

    public Shelveset(
        final String name,
        final String owner,
        final String ownerDisplayName,
        final String comment,
        final String policyOverrideComment,
        final CheckinNote checkinNote,
        final WorkItemCheckinInfo[] checkedInfo,
        final Calendar creationDate,
        final boolean changesExcluded,
        final PropertyValue[] propertyValues) {
        this(
            new _Shelveset(
                creationDate,
                name,
                owner,
                ownerDisplayName,
                owner,
                changesExcluded,
                comment,
                policyOverrideComment,
                (checkinNote) != null ? checkinNote.getWebServiceObject() : null,
                createVersionControlLinks(checkedInfo),
                (_PropertyValue[]) WrapperUtils.unwrap(_PropertyValue.class, propertyValues)));
    }

    private static _VersionControlLink[] createVersionControlLinks(final WorkItemCheckedInfo[] checkedInfo) {
        if (checkedInfo == null || checkedInfo.length == 0) {
            return null;
        }

        final List<_VersionControlLink> links = new ArrayList<_VersionControlLink>();

        for (int i = 0; i < checkedInfo.length; i++) {
            VersionControlLinkType linkType;

            if (checkedInfo[i].getCheckinAction() == CheckinWorkItemAction.ASSOCIATE) {
                linkType = VersionControlLinkType.ASSOCIATE;
            } else if (checkedInfo[i].getCheckinAction() == CheckinWorkItemAction.RESOLVE) {
                linkType = VersionControlLinkType.RESOLVE;
            } else {
                continue;
            }

            final ArtifactID artifactID = ArtifactIDFactory.newWorkItemArtifactID(checkedInfo[i].getID());

            links.add(new _VersionControlLink(linkType.getValue(), artifactID.encodeURI()));
        }

        return links.toArray(new _VersionControlLink[links.size()]);
    }

    private static _VersionControlLink[] createVersionControlLinks(final WorkItemCheckinInfo[] checkinInfo) {
        if (checkinInfo == null || checkinInfo.length == 0) {
            return null;
        }

        final List<_VersionControlLink> links = new ArrayList<_VersionControlLink>();

        for (int i = 0; i < checkinInfo.length; i++) {
            VersionControlLinkType linkType;

            if (checkinInfo[i].getAction() == CheckinWorkItemAction.ASSOCIATE) {
                linkType = VersionControlLinkType.ASSOCIATE;
            } else if (checkinInfo[i].getAction() == CheckinWorkItemAction.RESOLVE) {
                linkType = VersionControlLinkType.RESOLVE;
            } else {
                continue;
            }

            links.add(new _VersionControlLink(linkType.getValue(), checkinInfo[i].getWorkItem().getURI()));
        }

        return links.toArray(new _VersionControlLink[links.size()]);
    }

    /**
     * Gets the web service object this class wraps. The returned object should
     * not be modified.
     *
     * @return the web service object this class wraps.
     */
    public _Shelveset getWebServiceObject() {
        return (_Shelveset) webServiceObject;
    }

    /**
     * @return the shelveset name.
     */
    public String getName() {
        return getWebServiceObject().getName();
    }

    /**
     * @return the owner name.
     */
    public String getOwnerName() {
        return getWebServiceObject().getOwner();
    }

    /**
     * Set the owner name.
     */
    public void setOwnerName(final String owner) {
        getWebServiceObject().setOwner(owner);
    }

    /**
     * @return the owner display name.
     */
    public String getOwnerDisplayName() {
        // Show the display format if we have one. If this is a newly created
        // shelveset we probably don't so just return the unique format.
        final String displayName = getWebServiceObject().getOwnerdisp();
        if (displayName != null && displayName.length() > 0) {
            return getWebServiceObject().getOwnerdisp();
        }

        return getOwnerName();
    }

    /**
     * Set the owner display name.
     */
    public void setOwnerDisplayName(final String owner) {
        getWebServiceObject().setOwnerdisp(owner);
    }

    /**
     * @return the date and time that the shelveset was shelved.
     */
    public Calendar getCreationDate() {
        return getWebServiceObject().getDate();
    }

    /**
     * @return the comment that describes the shelveset.
     */
    public String getComment() {
        return getWebServiceObject().getComment();
    }

    /**
     * @return the check-in note that is associated with this shelveset.
     */
    public CheckinNote getCheckinNote() {
        return new CheckinNote(getWebServiceObject().getCheckinNote());
    }

    /**
     * @return user-supplied comment that describes why the policy failed.
     */
    public String getPolicyOverrideComment() {
        return getWebServiceObject().getPolicyOverrideComment();
    }

    public PropertyValue[] getPropertyValues() {
        // TODO remove the selectUnique
        return PropertyUtils.selectUnique(
            (PropertyValue[]) WrapperUtils.wrap(PropertyValue.class, getWebServiceObject().getProperties()));
    }

    /**
     * Gets brief work item information including only the IDs and the actions
     * on each included with the shelveset. Unlike
     * {@link #getWorkItemInfo(WorkItemClient)}, this method does not access the
     * server.
     *
     * @returns the work items that are associated with the shelveset
     */
    public synchronized WorkItemCheckedInfo[] getBriefWorkItemInfo() {
        final List<WorkItemCheckedInfo> workItemList = new ArrayList<WorkItemCheckedInfo>();

        final _VersionControlLink[] links = getWebServiceObject().getLinks();
        if (links != null) {
            for (final _VersionControlLink _link : links) {
                final VersionControlLink link = new VersionControlLink(_link);

                if (link.getLinkType() == VersionControlLinkType.ASSOCIATE
                    || link.getLinkType() == VersionControlLinkType.RESOLVE) {
                    final ArtifactID artifactID = new ArtifactID(link.getURL());
                    final int id = Integer.parseInt(artifactID.getToolSpecificID());

                    CheckinWorkItemAction action = CheckinWorkItemAction.NONE;

                    if (link.getLinkType() == VersionControlLinkType.ASSOCIATE) {
                        action = CheckinWorkItemAction.ASSOCIATE;
                    } else if (link.getLinkType() == VersionControlLinkType.RESOLVE) {
                        action = CheckinWorkItemAction.RESOLVE;
                    }

                    workItemList.add(new WorkItemCheckedInfo(id, true, action));
                }
            }
        }
        return workItemList.toArray(new WorkItemCheckedInfo[workItemList.size()]);
    }

    /**
     * Gets work items associated with the shelveset.
     *
     * @param workItemClient
     *        a work item client to resolve information with (must not be
     *        <code>null</code>)
     * @return the work items that are associated with the shelveset
     */
    public synchronized WorkItemCheckinInfo[] getWorkItemInfo(final WorkItemClient workItemClient) {
        Check.notNull(workItemClient, "workItemClient"); //$NON-NLS-1$

        if (workItemInfo == null) {
            final _VersionControlLink[] links = getWebServiceObject().getLinks();
            if (links != null) {
                workItemInfo = new WorkItemCheckinInfo[links.length];

                for (int i = 0; i < links.length; i++) {
                    final VersionControlLink link = new VersionControlLink(links[i]);

                    if (link.getLinkType() == VersionControlLinkType.ASSOCIATE
                        || link.getLinkType() == VersionControlLinkType.RESOLVE) {
                        final ArtifactID linkArtifact = new ArtifactID(link.getURL());
                        final WorkItem wi =
                            workItemClient.getWorkItemByID(Integer.parseInt(linkArtifact.getToolSpecificID()));

                        CheckinWorkItemAction action = CheckinWorkItemAction.NONE;

                        if (link.getLinkType() == VersionControlLinkType.ASSOCIATE) {
                            action = CheckinWorkItemAction.ASSOCIATE;
                        } else if (link.getLinkType() == VersionControlLinkType.RESOLVE) {
                            action = CheckinWorkItemAction.RESOLVE;
                        }

                        workItemInfo[i] = new WorkItemCheckinInfo(wi, action);
                    }
                }
            } else {
                workItemInfo = new WorkItemCheckinInfo[0];
            }
        }

        return workItemInfo;
    }

    /**
     * @return <code>true</code> if some changes were excluded from this
     *         shelveset, <code>false</code> otherwise
     * @since TFS 2008
     */
    public boolean areChangesExcluded() {
        return getWebServiceObject().isCe();
    }
}
