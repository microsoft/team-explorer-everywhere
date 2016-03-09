// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.workspacecache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNote;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.CheckinNoteFieldValue;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PendingChange;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Shelveset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.versioncontrol.workspacecache.internal.CheckinItem;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.ws.runtime.xml.XMLConvert;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.xml.DOMUtils;

public class SavedCheckin implements Cloneable {
    public static final String XML_SAVED_CHECKIN = "SavedCheckin"; //$NON-NLS-1$
    private static final String XML_EXCLUDED_ITEMS = "ExcludedItems"; //$NON-NLS-1$
    private static final String XML_COMMENT = "Comment"; //$NON-NLS-1$
    private static final String XML_POLICY_OVERRIDE_COMMENT = "PolicyOverrideComment"; //$NON-NLS-1$
    private static final String XML_ITEM = "Item"; //$NON-NLS-1$
    private static final String XML_SERVER_ITEM = "serverItem"; //$NON-NLS-1$
    private static final String XML_ITEM_ID = "itemId"; //$NON-NLS-1$
    private static final String XML_WORK_ITEMS_CHECKED_INFO = "WorkItemsCheckedInfo"; //$NON-NLS-1$

    private Map<String, CheckinItem> m_excludedItems;
    private String m_comment;
    private CheckinNote m_checkinNotes;
    private WorkItemCheckedInfo[] m_persistentWorkItemsCheckedInfo;
    private String m_policyOverrideComment;

    public SavedCheckin() {
        m_excludedItems = CheckinItem.fromServerPaths(null);
        initialize(null, null, null, null, false);
    }

    public SavedCheckin(
        final String comment,
        final PendingChange[] excludedChanges,
        final CheckinNote checkinNotes,
        final WorkItemCheckedInfo[] workItemCheckedInfo,
        final String policyOverrideComment) {
        m_excludedItems = CheckinItem.fromPendingChanges(excludedChanges);
        initialize(comment, checkinNotes, workItemCheckedInfo, policyOverrideComment, false);
    }

    public SavedCheckin(
        final Collection<String> excludedServerPaths,
        final String comment,
        final CheckinNote checkinNotes,
        final WorkItemCheckedInfo[] workItemCheckedInfo,
        final String policyOverrideComment) {
        m_excludedItems = CheckinItem.fromServerPaths(excludedServerPaths);
        initialize(comment, checkinNotes, workItemCheckedInfo, policyOverrideComment, false);
    }

    private SavedCheckin(
        final Map<String, CheckinItem> excludedItems,
        final String comment,
        final CheckinNote checkinNotes,
        final WorkItemCheckedInfo[] workItemsCheckedInfo,
        final String policyOverrideComment) {
        m_excludedItems = excludedItems;
        initialize(comment, checkinNotes, workItemsCheckedInfo, policyOverrideComment, false);
    }

    public SavedCheckin(final Shelveset shelveset) {
        m_excludedItems = CheckinItem.fromPendingChanges(null);
        initialize(
            shelveset.getComment(),
            shelveset.getCheckinNote(),
            shelveset.getBriefWorkItemInfo(),
            shelveset.getPolicyOverrideComment(),
            false);
    }

    @Override
    public Object clone() {
        final Map<String, CheckinItem> excludedItems = CheckinItem.fromCheckinItems(m_excludedItems);
        CheckinNote checkinNotes = null;
        if (m_checkinNotes != null) {
            checkinNotes = new CheckinNote(m_checkinNotes.getValues());
        }
        WorkItemCheckedInfo[] workItems = null;
        if (m_persistentWorkItemsCheckedInfo != null) {
            workItems = new WorkItemCheckedInfo[m_persistentWorkItemsCheckedInfo.length];
            System.arraycopy(
                m_persistentWorkItemsCheckedInfo,
                0,
                workItems,
                0,
                m_persistentWorkItemsCheckedInfo.length);
        }
        return new SavedCheckin(excludedItems, m_comment, checkinNotes, workItems, m_policyOverrideComment);
    }

    /**
     * Initialized this object from the data specified in an attempted checkin
     *
     * @param comment
     *        Checkin comments for the attempted checkin
     * @param checkinNotes
     *        Any checkin notes specified in the attempted checkin
     * @param workItemsCheckedInfo
     *        Work items checked as part of thes attempted checkin
     * @param policyOverrideComment
     *        Policy override comment (if specified) in the attempted checkin
     * @param mergeWorkItems
     *        true to merge the work items, false to replace them with the new
     *        set
     */
    private void initialize(
        final String comment,
        final CheckinNote checkinNotes,
        final WorkItemCheckedInfo[] workItemsCheckedInfo,
        final String policyOverrideComment,
        final boolean mergeWorkItems) {
        this.m_comment = comment;

        // Do not save any checkin note fields that have empty values as they
        // aren't useful.
        if (checkinNotes != null) {
            final List<CheckinNoteFieldValue> fieldValueList = new ArrayList<CheckinNoteFieldValue>();
            for (final CheckinNoteFieldValue fieldValue : checkinNotes.getValues()) {
                if (fieldValue.getValue() != null && fieldValue.getValue().length() > 0) {
                    fieldValueList.add(fieldValue);
                }
            }

            if (fieldValueList.size() > 0) {
                m_checkinNotes =
                    new CheckinNote(fieldValueList.toArray(new CheckinNoteFieldValue[fieldValueList.size()]));
            }
        }

        if (mergeWorkItems) {
            mergeWorkItems(workItemsCheckedInfo);
        } else {
            m_persistentWorkItemsCheckedInfo = workItemsCheckedInfo;
        }

        m_policyOverrideComment = policyOverrideComment;
    }

    /**
     * Returns true if there if the item is excluded in this checkin.
     *
     * @param targetServerItem
     *        the target item to check
     */
    public boolean isExcluded(final String targetServerItem) {
        Check.notNull(targetServerItem, "targetServerItem"); //$NON-NLS-1$

        return m_excludedItems.containsKey(targetServerItem);
    }

    /**
     * Reinitialize this SavedCheckin with the metadata contined within the
     * specified shelveset and update the check states with the the specified
     * set of pending changes.
     *
     * @param shelveset
     *        The shelveset containing the desired metadata
     * @param unshelvedChanges
     *        The pending changes that where unshelved
     */
    public void mergeShelvesetMetadata(final Shelveset shelveset, final PendingChange[] unshelvedChanges) {
        // Add the unshelved changes as checked items.
        updateCheckinItems(unshelvedChanges, new PendingChange[0]);

        // Reiniailize with the shelveset metadata.
        initialize(
            shelveset.getComment(),
            shelveset.getCheckinNote(),
            shelveset.getBriefWorkItemInfo(),
            shelveset.getPolicyOverrideComment(),
            true);

        /*
         * Remove unshelved changes from the excluded server paths. The
         * justification is if the user just unshelved some changes, it is
         * likely that the user wants to have those changes in the include list.
         */
        if (unshelvedChanges != null) {
            for (final PendingChange change : unshelvedChanges) {
                m_excludedItems.remove(change.getServerItem());
            }
        }
    }

    /**
     * Merge the work items
     *
     * @param workItems
     *        the work items to be merged (may be <code>null</code> or empty)
     */
    public void mergeWorkItems(final WorkItemCheckedInfo[] workItems) {
        // Nothing to merge
        if (workItems == null || workItems.length == 0) {
            return;
        }

        // Combine the two lists into one
        final Map<Integer, WorkItemCheckedInfo> combined = new HashMap<Integer, WorkItemCheckedInfo>();
        if (m_persistentWorkItemsCheckedInfo != null) {
            for (final WorkItemCheckedInfo wiInfo : m_persistentWorkItemsCheckedInfo) {
                combined.put(wiInfo.getID(), wiInfo);
            }
        }

        for (final WorkItemCheckedInfo wiInfo : workItems) {
            combined.put(wiInfo.getID(), wiInfo);
        }

        m_persistentWorkItemsCheckedInfo = combined.values().toArray(new WorkItemCheckedInfo[combined.values().size()]);
    }

    /**
     * Update the saved channel info with the current check states. The
     * specified checked states may be a filtered subset of the overall set of
     * pending changes so this list does not replace the current channel checked
     * states. Instead, this method ensures that the specified checked states
     * exist in saved channel info and the unchecked changes do not.
     *
     * @param checkedPendingChanges
     *        the set of checked pending changes to merge with the overall set
     *        of checked items (may be <code>null</code>)
     * @param uncheckedPendingChanges
     *        the set of unchecked pending changes to remove from the overall
     *        set of checked items (may be <code>null</code>)
     */
    public void updateCheckinItems(
        final PendingChange[] checkedPendingChanges,
        final PendingChange[] uncheckedPendingChanges) {
        if (checkedPendingChanges != null) {
            // remove any checked pending change from our exclusion
            for (final PendingChange checkedPendingChange : checkedPendingChanges) {
                m_excludedItems.remove(checkedPendingChange.getServerItem());
            }
        }

        if (uncheckedPendingChanges != null) {
            // add any unchecked pending change to our exclusions
            for (final PendingChange uncheckedPendingChange : uncheckedPendingChanges) {
                m_excludedItems.put(
                    uncheckedPendingChange.getServerItem(),
                    new CheckinItem(uncheckedPendingChange.getServerItem(), uncheckedPendingChange.getItemID()));
            }
        }
    }

    /**
     * Creates an instance from the XML representation used in the cache file.
     *
     * @param attemptedCheckinNode
     *        the XML node (must not be <code>null</code>)
     * @return an instance of {@link SavedCheckin} created from the XML
     */
    public static SavedCheckin loadFromXML(final Element attemptedCheckinNode) {
        String comment = null;
        String policyOverrideComment = null;
        CheckinNote relnotes = null;
        final Map<String, CheckinItem> excludedItems = new TreeMap<String, CheckinItem>(ServerPath.TOP_DOWN_COMPARATOR);
        WorkItemCheckedInfo[] workItemsCheckedInfo = null;

        for (final Element child : DOMUtils.getChildElements(attemptedCheckinNode)) {
            if (child.getNodeName().equals(CheckinNote.XML_CHECKIN_NOTES)) {
                relnotes = CheckinNote.loadFromXML(child);
            } else if (child.getNodeName().equals(XML_COMMENT)) {
                /*
                 * The VS implementation converts "\n" back to "\r\n" here,
                 * because .NET's XmlReader will have converted "\r\n" to just
                 * "\n" when reading.
                 *
                 * We do the same conversion, even though SWT doesn't care and
                 * it's a bit weird for Unix, but it keeps us working better
                 * side-by-side with VS on Windows.
                 *
                 * When the comment text box is empty in VS (0 chars), it saves
                 * the XML element with a newline and some indent spacing
                 * between the tags. I'm not sure why this is done, but we'll
                 * trim the whitespace before converting newlines so we read
                 * that back as an empty Java string.
                 */
                comment = DOMUtils.getText(child).trim().replace("\n", "\r\n"); //$NON-NLS-1$ //$NON-NLS-2$
            } else if (child.getNodeName().equals(XML_POLICY_OVERRIDE_COMMENT)) {
                /*
                 * See comment above.
                 */
                policyOverrideComment = DOMUtils.getText(child).trim().replace("\n", "\r\n"); //$NON-NLS-1$ //$NON-NLS-2$
                                                                                              // ;
            } else if (child.getNodeName().equals(XML_EXCLUDED_ITEMS)) {
                loadItemsFromXML(child, excludedItems);
            } else if (child.getNodeName().equals(XML_WORK_ITEMS_CHECKED_INFO)) {
                workItemsCheckedInfo = WorkItemCheckedInfo.loadFromXML(child);
            }
        }

        return new SavedCheckin(excludedItems, comment, relnotes, workItemsCheckedInfo, policyOverrideComment);
    }

    /**
     * Loads children named {@value #XML_ITEM} from the given items node.
     *
     * @param itemsNode
     *        the ExcludedItems node (must not be <code>null</code>)
     * @param itemMap
     *        the map to update with items read (must not be <code>null</code>)
     */
    private static void loadItemsFromXML(final Element itemsNode, final Map<String, CheckinItem> itemMap) {
        Check.notNull(itemsNode, "itemsNode"); //$NON-NLS-1$
        Check.notNull(itemMap, "itemMap"); //$NON-NLS-1$

        for (final Element itemNode : DOMUtils.getChildElements(itemsNode, XML_ITEM)) {
            final String serverItem = itemNode.getAttributes().getNamedItem(XML_SERVER_ITEM).getNodeValue();
            final int itemId = XMLConvert.toInt(itemNode.getAttributes().getNamedItem(XML_ITEM_ID).getNodeValue());

            if (serverItem != null && serverItem.length() > 0) {
                itemMap.put(serverItem, new CheckinItem(serverItem, itemId));
            }
        }
    }

    /**
     * Saves this instance to the XML format used in the cache file.
     *
     * @param parent
     *        the XML parent node (must not be <code>null</code>)
     */
    public void saveAsXML(final Element parent) {
        final Element attemptedCheckinNode = DOMUtils.appendChild(parent, XML_SAVED_CHECKIN);

        // Save empty string if no comment for compat with TEE clients before
        // 2012-03-16 (VS also does this)
        DOMUtils.appendChildWithText(attemptedCheckinNode, XML_COMMENT, (m_comment != null) ? m_comment : ""); //$NON-NLS-1$

        // Save empty string if no comment for compat with TEE clients before
        // 2012-03-16 (VS also does this)
        DOMUtils.appendChildWithText(
            attemptedCheckinNode,
            XML_POLICY_OVERRIDE_COMMENT,
            (m_policyOverrideComment != null) ? m_policyOverrideComment : ""); //$NON-NLS-1$

        if (m_checkinNotes != null) {
            m_checkinNotes.saveAsXML(attemptedCheckinNode);
        }

        // Save the persistent work items checked info.
        WorkItemCheckedInfo.saveAsXML(attemptedCheckinNode, m_persistentWorkItemsCheckedInfo);

        // Save the excluded items
        if (m_excludedItems.size() > 0) {
            saveItemsAsXML(attemptedCheckinNode, XML_EXCLUDED_ITEMS, m_excludedItems);
        }
    }

    /**
     * Save the items to Xml Write out the pending change server items, if there
     * are not a huge number of them. We set a limit to prevent the cache file
     * from becoming huge.
     */
    private void saveItemsAsXML(
        final Element attemptedCheckinNode,
        final String tagName,
        final Map<String, CheckinItem> itemList) {
        final Element selectedItems = DOMUtils.appendChild(attemptedCheckinNode, tagName);

        if (itemList.size() < 500) {
            for (final CheckinItem ci : itemList.values()) {
                final Element serverItemNode = DOMUtils.appendChild(selectedItems, XML_ITEM);
                serverItemNode.setAttribute(XML_SERVER_ITEM, ci.getServerItem());
                serverItemNode.setAttribute(XML_ITEM_ID, XMLConvert.toString(ci.getItemID()));
            }
        }
    }

    public String getComment() {
        return m_comment;
    }

    public void setComment(final String comment) {
        m_comment = comment;
    }

    public String getPolicyOverrideComment() {
        return m_policyOverrideComment;
    }

    public void setPolicyOverrideComment(final String policyOverrideComment) {
        this.m_policyOverrideComment = policyOverrideComment;
    }

    public CheckinNote getCheckinNotes() {
        return m_checkinNotes;
    }

    public void setCheckinNotes(final CheckinNote checkinNotes) {
        this.m_checkinNotes = checkinNotes;
    }

    public WorkItemCheckedInfo[] getWorkItemsCheckedInfo() {
        return m_persistentWorkItemsCheckedInfo;
    }

    /**
     * Convenience method to get the checked work item info as
     * {@link WorkItemCheckinInfo} (used for web services) instead of
     * {@link WorkItemCheckedInfo} (used for persistence).
     * <p>
     * The server may be contacted to instantiate {@link WorkItem}s. Permissions
     * may restrict which work items are returned or cause exceptions. Use
     * {@link #getWorkItemsCheckedInfo()} to get the raw persisted information.
     *
     * @param workItemClient
     *        the work item client to use to instantiate {@link WorkItem}s (must
     *        not be <code>null</code>)
     * @return the {@link WorkItemCheckinInfo}s for this server
     */
    public WorkItemCheckinInfo[] getWorkItemsCheckinInfo(final WorkItemClient workItemClient) {
        Check.notNull(workItemClient, "workItemClient"); //$NON-NLS-1$

        return WorkItemCheckinInfo.fromWorkItemCheckedInfo(workItemClient, getWorkItemsCheckedInfo());
    }

    public void setPersistentWorkItemsCheckedInfo(final WorkItemCheckedInfo[] persistentWorkItemsCheckedInfo) {
        this.m_persistentWorkItemsCheckedInfo = persistentWorkItemsCheckedInfo;
    }

    public String[] getExcludedServerPaths() {
        return m_excludedItems.keySet().toArray(new String[m_excludedItems.keySet().size()]);
    }

    public void setExcludedServerPaths(final String[] serverPaths) {
        this.m_excludedItems = CheckinItem.fromServerPaths(Arrays.asList(serverPaths));
    }

    /**
     * @return the {@link WorkItemCheckedInfo} objects in this
     *         {@link SavedCheckin} that have an action of either
     *         {@link CheckinWorkItemAction#RESOLVE} or
     *         {@link CheckinWorkItemAction#ASSOCIATE} (excludes
     *         {@link CheckinWorkItemAction#NONE}).
     */
    public WorkItemCheckedInfo[] getAssociateOrResolveWorkItemsCheckedInfo() {
        final List<WorkItemCheckedInfo> infos = new ArrayList<WorkItemCheckedInfo>();

        if (m_persistentWorkItemsCheckedInfo != null) {
            for (final WorkItemCheckedInfo info : m_persistentWorkItemsCheckedInfo) {
                if (info.getCheckinAction() == CheckinWorkItemAction.RESOLVE
                    || info.getCheckinAction() == CheckinWorkItemAction.ASSOCIATE) {
                    infos.add(info);
                }
            }
        }

        return infos.toArray(new WorkItemCheckedInfo[infos.size()]);
    }
}