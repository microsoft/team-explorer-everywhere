// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.link;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.internal.update.UpdateXMLConstants;
import com.microsoft.tfs.core.clients.workitem.link.Link;
import com.microsoft.tfs.core.clients.workitem.link.RegisteredLinkTypeNames;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;
import com.microsoft.tfs.util.xml.DOMUtils;

public class RelatedLinkImpl extends LinkImpl implements RelatedLink {
    private WorkItem sourceWorkItem;
    private final int targetWorkItemId;
    private final int workItemLinkTypeId;
    private WorkItem targetWorkItem;

    public RelatedLinkImpl(
        final WorkItem sourceWorkItem,
        final int targetWorkItemId,
        final int workItemLinkTypeId,
        final String comment,
        final boolean newComponent,
        final boolean readOnly) {
        super(new RegisteredLinkTypeImpl(RegisteredLinkTypeNames.WORKITEM), comment, -1, newComponent, readOnly);
        this.sourceWorkItem = sourceWorkItem;
        this.targetWorkItemId = targetWorkItemId;
        this.workItemLinkTypeId = workItemLinkTypeId;
    }

    @Override
    public LinkImpl cloneLink() {
        return new RelatedLinkImpl(
            sourceWorkItem,
            targetWorkItemId,
            workItemLinkTypeId,
            getComment(),
            true,
            isReadOnly());
    }

    @Override
    public WorkItem getSourceWorkItem() {
        return sourceWorkItem;
    }

    @Override
    public int getTargetWorkItemID() {
        return targetWorkItemId;
    }

    @Override
    public int getWorkItemLinkTypeID() {
        return workItemLinkTypeId;
    }

    @Override
    public WorkItem getTargetWorkItem() {
        return targetWorkItem;
    }

    @Override
    public void setSourceWorkItem(final WorkItem value) {
        sourceWorkItem = value;
    }

    public void setWorkItem(final WorkItem value) {
        targetWorkItem = value;
    }

    @Override
    public boolean isEquivalent(final Link link) {
        if (link instanceof RelatedLinkImpl) {
            final RelatedLinkImpl other = (RelatedLinkImpl) link;
            return sourceWorkItem.getID() == other.sourceWorkItem.getID()
                && targetWorkItemId == other.targetWorkItemId
                && workItemLinkTypeId == other.workItemLinkTypeId;
        }
        return false;
    }

    @Override
    protected String getFallbackDescription() {
        return String.valueOf(targetWorkItemId);
    }

    @Override
    protected void createXMLForAdd(final Element parentElement) {
        Element element;

        if (workItemLinkTypeId == 0) {
            // old related link type (WIT v1 and v2)
            element = DOMUtils.appendChild(parentElement, UpdateXMLConstants.ELEMENT_NAME_CREATE_RELATION);
            element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_WORK_ITEM_ID, String.valueOf(targetWorkItemId));
        } else {
            // work item link (WIT v3)
            final Element rootElement = DOMUtils.getRootElement(parentElement);
            final int sourceWorkItemId =
                (sourceWorkItem.getID() == 0) ? sourceWorkItem.getTemporaryID() : sourceWorkItem.getID();
            element = DOMUtils.appendChild(rootElement, UpdateXMLConstants.ELEMENT_NAME_INSERT_WORKITEM_LINK);
            element.setAttribute(
                UpdateXMLConstants.ATTRIBUTE_NAME_WORKITEM_LINK_SOURCE_ID,
                String.valueOf(sourceWorkItemId));
            element.setAttribute(
                UpdateXMLConstants.ATTRIBUTE_NAME_WORKITEM_LINK_TARGET_ID,
                String.valueOf(targetWorkItemId));
            element.setAttribute(
                UpdateXMLConstants.ATTRIBUTE_NAME_WORKITEM_LINK_TYPE,
                String.valueOf(workItemLinkTypeId));
            element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_WORKITEM_LINK_AUTOMERGE, String.valueOf(true));
        }

        if (getComment() != null && getComment().trim().length() > 0) {
            element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_COMMENT, getComment().trim());
        }
    }

    @Override
    protected void createXMLForRemove(final Element parentElement) {
        if (workItemLinkTypeId == 0) {
            // old related link type (WIT v1 and v2)
            final Element element =
                DOMUtils.appendChild(parentElement, UpdateXMLConstants.ELEMENT_NAME_REMOVE_RELATION);
            element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_WORK_ITEM_ID, String.valueOf(targetWorkItemId));
        } else {
            // work item link (WIT v3)
            final Element rootElement = DOMUtils.getRootElement(parentElement);
            final int sourceWorkItemId =
                (sourceWorkItem.getID() == 0) ? sourceWorkItem.getTemporaryID() : sourceWorkItem.getID();
            final Element element =
                DOMUtils.appendChild(rootElement, UpdateXMLConstants.ELEMENT_NAME_DELETE_WORKITEM_LINK);
            element.setAttribute(
                UpdateXMLConstants.ATTRIBUTE_NAME_WORKITEM_LINK_SOURCE_ID,
                String.valueOf(sourceWorkItemId));
            element.setAttribute(
                UpdateXMLConstants.ATTRIBUTE_NAME_WORKITEM_LINK_TARGET_ID,
                String.valueOf(targetWorkItemId));
            element.setAttribute(
                UpdateXMLConstants.ATTRIBUTE_NAME_WORKITEM_LINK_TYPE,
                String.valueOf(workItemLinkTypeId));
            element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_WORKITEM_LINK_AUTOMERGE, String.valueOf(true));
        }
    }

    @Override
    protected String getInsertTagName() {
        return null;
    }
}
