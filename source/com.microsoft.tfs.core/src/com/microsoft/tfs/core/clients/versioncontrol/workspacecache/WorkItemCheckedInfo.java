// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.workspacecache;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkItemCheckinInfo;
import com.microsoft.tfs.core.clients.workitem.CheckinWorkItemAction;
import com.microsoft.tfs.core.ws.runtime.xml.XMLConvert;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.xml.DOMUtils;

/**
 * Encapsulate supplementary info about a work item. An example of a supplement
 * information is the work item checked/unchecked state.
 *
 * The checked state of a work item is sticky. Once its state is changed, the
 * new state will be persisted between invocations.
 */
public class WorkItemCheckedInfo implements Cloneable {
    private static final Log log = LogFactory.getLog(WorkItemCheckedInfo.class);

    public static final String XML_TAG_WORK_ITEM_CHECKED_INFO = "WorkItemsCheckedInfo"; //$NON-NLS-1$
    private static final String XML_TAG_WORK_ITEM = "WorkItem"; //$NON-NLS-1$
    private static final String XML_TAG_ATTRID = "Id"; //$NON-NLS-1$
    private static final String XML_TAG_ATTR_CHECKED = "Checked"; //$NON-NLS-1$
    private static final String XML_TAG_ATTR_ACTION = "Action"; //$NON-NLS-1$

    // Work item ID.
    private final int id;

    // Sticky checked state.
    private final boolean checked;

    private final CheckinWorkItemAction action;

    /**
     * Create a new work item supplement info.
     *
     * @param id
     *        th work item id
     * @param checkedOnOff
     *        checked state: on/off
     * @param action
     *        action to perform on the work item (associate, resolve)
     */
    public WorkItemCheckedInfo(final int id, final boolean checkedOnOff, final CheckinWorkItemAction action) {
        this.id = id;
        this.checked = checkedOnOff;
        this.action = action;
    }

    /**
     * Helper to convert from {@link WorkItemCheckinInfo} to the
     * {@link WorkItemCheckedInfo} objects we persist.
     */
    public static WorkItemCheckedInfo[] fromCheckinInfo(final WorkItemCheckinInfo[] checkinInfo) {
        if (checkinInfo == null) {
            return new WorkItemCheckedInfo[0];
        }

        final WorkItemCheckedInfo[] checkedInfo = new WorkItemCheckedInfo[checkinInfo.length];
        for (int i = 0; i < checkedInfo.length; i++) {
            Check.notNull(checkinInfo[i], "checkinInfo[i]"); //$NON-NLS-1$

            checkedInfo[i] =
                new WorkItemCheckedInfo(checkinInfo[i].getWorkItem().getID(), true, checkinInfo[i].getAction());
        }
        return checkedInfo;
    }

    /**
     * Parse the XML doc for the persistent work items checked info.
     *
     * @param XML
     *        doc
     * @return list of persistent work items checked info, null for none
     */
    public static WorkItemCheckedInfo[] loadFromXML(final Document doc) {
        // Locate the work items checked info tag.
        final Element element = DOMUtils.getFirstChildElement(doc, XML_TAG_WORK_ITEM_CHECKED_INFO);
        if (element == null) {
            return null;
        }
        return loadFromXML(element);
    }

    /**
     * Parse the XML doc for the persistent work items checked info.
     *
     * @param listRoot
     *        XML node containing the work items checked info
     * @return list of persistent work items checked info, null for empty list
     */
    public static WorkItemCheckedInfo[] loadFromXML(final Element listRoot) {
        final Element[] childNodes = DOMUtils.getChildElements(listRoot);

        // Easy case for empty list.
        if (childNodes.length == 0) {
            return null;
        }

        // Allocate the result list.
        final List<WorkItemCheckedInfo> list = new ArrayList<WorkItemCheckedInfo>(childNodes.length);

        // Loop thru all children nodes which are the item themselves.
        for (final Node node : childNodes) {
            int id = 0;
            if (node.getAttributes().getNamedItem(XML_TAG_ATTRID) != null) {
                try {
                    id = Integer.parseInt(node.getAttributes().getNamedItem(XML_TAG_ATTRID).getNodeValue());
                } catch (final NumberFormatException e) {
                    log.warn(MessageFormat.format(
                        "Error parsing work item ID {0}, ignoring", //$NON-NLS-1$
                        node.getAttributes().getNamedItem(XML_TAG_ATTRID).getNodeValue()), e);

                    // Skip this item
                    continue;
                }
            }

            boolean checkedOnOff = false;
            if (node.getAttributes().getNamedItem(XML_TAG_ATTR_CHECKED) != null) {
                checkedOnOff = "1".equals(node.getAttributes().getNamedItem(XML_TAG_ATTR_CHECKED).getNodeValue()); //$NON-NLS-1$
            }

            CheckinWorkItemAction action = CheckinWorkItemAction.NONE;
            if (node.getAttributes().getNamedItem(XML_TAG_ATTR_ACTION) != null) {
                action = actionFromString(node.getAttributes().getNamedItem(XML_TAG_ATTR_ACTION).getNodeValue());
            }

            // Add item to list.
            list.add(new WorkItemCheckedInfo(id, checkedOnOff, action));
        }

        return list.toArray(new WorkItemCheckedInfo[list.size()]);
    }

    /**
     * Save a list of work items checked info as XML.
     *
     * @param parentNode
     *        XML parent node
     * @param list
     *        checked info list
     */
    public static void saveAsXML(final Element parentNode, final WorkItemCheckedInfo[] list) {
        // XML document format:
        // <Parent Node>
        // <WorkItemsCheckedInfo>
        // <Item ID="45" Checked="1">
        // <Item ID="99" Checked="0">
        // </WorkItemsCheckedInfo>
        // </Parent Node>

        // Sanity check and also an easy case for empty list.
        if (list == null || list.length == 0) {
            return;
        }

        // Create the work item list node.
        final Element listRoot = DOMUtils.appendChild(parentNode, XML_TAG_WORK_ITEM_CHECKED_INFO);

        // Loop and insert the work items checked info.
        for (final WorkItemCheckedInfo checkedInfo : list) {
            final Element itemNode = DOMUtils.appendChild(listRoot, XML_TAG_WORK_ITEM);

            itemNode.setAttribute(XML_TAG_ATTRID, XMLConvert.toString(checkedInfo.getID()));
            itemNode.setAttribute(XML_TAG_ATTR_CHECKED, checkedInfo.isChecked() ? "1" : "0"); //$NON-NLS-1$//$NON-NLS-2$
            itemNode.setAttribute(XML_TAG_ATTR_ACTION, actionToString(checkedInfo.getCheckinAction()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkItemCheckedInfo clone() {
        return new WorkItemCheckedInfo(id, checked, action);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof WorkItemCheckedInfo == false) {
            return false;
        }

        final WorkItemCheckedInfo other = (WorkItemCheckedInfo) obj;

        if (id != other.id) {
            return false;
        }

        if (checked != other.checked) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + (id);
        result = result * 37 + (checked ? 1 : 0);

        return result;
    }

    public int getID() {
        return id;
    }

    public boolean isChecked() {
        return this.checked;
    }

    public CheckinWorkItemAction getCheckinAction() {
        return this.action;
    }

    private static CheckinWorkItemAction actionFromString(final String action) {
        // Use locale-invariant compare

        if ("Associate".compareToIgnoreCase(action) == 0) //$NON-NLS-1$
        {
            return CheckinWorkItemAction.ASSOCIATE;
        }

        if ("Resolve".compareToIgnoreCase(action) == 0) //$NON-NLS-1$
        {
            return CheckinWorkItemAction.RESOLVE;
        }

        return CheckinWorkItemAction.NONE;
    }

    private static String actionToString(final CheckinWorkItemAction action) {
        if (action == CheckinWorkItemAction.RESOLVE) {
            return "Resolve"; //$NON-NLS-1$
        }

        if (action == CheckinWorkItemAction.ASSOCIATE) {
            return "Associate"; //$NON-NLS-1$
        }

        return "None"; //$NON-NLS-1$
    }
}