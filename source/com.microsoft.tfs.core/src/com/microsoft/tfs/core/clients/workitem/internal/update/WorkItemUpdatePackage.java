// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.update;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldType;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldImpl;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldModificationType;
import com.microsoft.tfs.core.clients.workitem.internal.fields.ServerComputedFieldType;
import com.microsoft.tfs.core.ws.runtime.types.DOMAnyContentType;
import com.microsoft.tfs.util.xml.DOMSerializeUtils;
import com.microsoft.tfs.util.xml.DOMUtils;

/**
 * This class encapsulates the Package XML blob that's sent to the server when a
 * work item is updated. Given an open work item and a WITContext, it will build
 * an in-memory representation of the update package XML (DOM tree).
 */
public class WorkItemUpdatePackage extends BaseUpdatePackage {
    private final WorkItemImpl workItem;

    /*
     * the <UpdateWorkItem> or <InsertWorkItem> element, child of <Package>
     */
    private final Element workItemElement;

    /*
     * the <Columns> element, child of <UpdateWorkItem>
     */
    private Element columnsElement;

    /*
     * the <ComputedColumns> element, child of <UpdateWorkItem>
     */
    private Element computedColumnsElement;

    private final Map<String, List<ElementHandler>> updateElementNamesToHandlerLists =
        new HashMap<String, List<ElementHandler>>();

    public WorkItemUpdatePackage(final WorkItemImpl workItem, final WITContext context) {
        super(context);
        this.workItem = workItem;

        workItemElement = createWorkItemElement();
        processFields();
        addHardcodedComputedColumns();
        processLinks();
        processAttachments();

        // Work item links are handled separate from work items in WIT version
        // 3. An update which only effects a work item link will result in an
        // empty set of changes under the work item [note, the computed columns
        // element is always generated, so a workItemElement with only one child
        // is considered empty). WIT server will throw if we send an empty work
        // item element in version 3.
        if (context.isVersion3OrHigher() && workItemElement.getChildNodes().getLength() == 1) {
            getRoot().removeChild(workItemElement);
        }
    }

    private Element createWorkItemElement() {
        Element element;

        if (workItem.getFields().getID() == 0) {
            /*
             * this is a new workitem, so we create an <InsertWorkItem> element
             */
            element = DOMUtils.appendChild(getRoot(), UpdateXMLConstants.ELEMENT_NAME_INSERT_WORK_ITEM);
            element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_TEMPID, String.valueOf(-workItem.getTemporaryID()));
        } else {
            /*
             * this is an existing workitem, so we create an <UpdateWorkItem>
             * element
             */
            element = DOMUtils.appendChild(getRoot(), UpdateXMLConstants.ELEMENT_NAME_UPDATE_WORK_ITEM);

            element.setAttribute(
                UpdateXMLConstants.ATTRIBUTE_NAME_WORK_ITEM_ID,
                String.valueOf(workItem.getFields().getID()));
            element.setAttribute(
                UpdateXMLConstants.ATTRIBUTE_NAME_REVISION,
                String.valueOf(workItem.getFields().getField(CoreFieldReferenceNames.REVISION).getOriginalValue()));
        }

        element.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_OBJECT_TYPE, UpdateXMLConstants.WORK_ITEM_OBJECT_TYPE);
        return element;
    }

    private void processFields() {
        for (final Iterator<Field> it = workItem.getFields().iterator(); it.hasNext();) {
            final FieldImpl field = (FieldImpl) it.next();

            if (field.isComputed() || !field.isDirty()) {
                continue;
            }

            if (field.getFieldDefinitionInternal().isLargeText()) {
                processLargeTextField(field);
            } else {
                processNonLargeTextField(field);
            }
        }
    }

    private void processLargeTextField(final FieldImpl field) {
        final String value = field.getNewValueAsString();
        Element insertText;

        if (value != null) {
            insertText =
                DOMUtils.appendChildWithText(workItemElement, UpdateXMLConstants.ELEMENT_NAME_INSERT_TEXT, value);
        } else {
            /*
             * If the field value is null ("empty"), we indicate this to the
             * server by using an empty (except for attributes) <InsertText>
             * element.
             */
            insertText = DOMUtils.appendChild(workItemElement, UpdateXMLConstants.ELEMENT_NAME_INSERT_TEXT);
        }

        insertText.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_FIELD_NAME, field.getReferenceName());
        insertText.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_FIELD_DISPLAY_NAME, field.getName());
    }

    private void processNonLargeTextField(final FieldImpl field) {
        if (columnsElement == null) {
            columnsElement = DOMUtils.appendChild(workItemElement, UpdateXMLConstants.ELEMENT_NAME_COLUMNS);
        }

        if (field.getServerComputedType() != null) {
            final Element c = DOMUtils.appendChild(columnsElement, UpdateXMLConstants.ELEMENT_NAME_COLUMN);
            c.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_COLUMN, field.getReferenceName());

            final ServerComputedFieldType type = field.getServerComputedType();
            if (type == ServerComputedFieldType.DATE_TIME || type == ServerComputedFieldType.RANDOM_GUID) {
                c.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_TYPE, field.getServerComputedType().getName());
            } else {
                /*
                 * for server computed current user fields, we leave the type
                 * unspecified (defaults to String, which is the only valid
                 * field type for server computed currentuser), and specify the
                 * current user in the value child
                 */
                DOMUtils.appendChildWithText(
                    c,
                    UpdateXMLConstants.ELEMENT_NAME_VALUE,
                    getContext().getCurrentUserDisplayName());
            }

            /*
             * Add the field to the computed columns element.
             */
            if (computedColumnsElement == null) {
                computedColumnsElement =
                    DOMUtils.appendChild(workItemElement, UpdateXMLConstants.ELEMENT_NAME_COMPUTED_COLUMNS);
            }
            DOMUtils.appendChild(computedColumnsElement, UpdateXMLConstants.ELEMENT_NAME_COMPUTED_COLUMN).setAttribute(
                UpdateXMLConstants.ATTRIBUTE_NAME_COLUMN,
                field.getReferenceName());
        } else {
            final String value = field.getNewValueAsString();
            final Element c = DOMUtils.appendChild(columnsElement, UpdateXMLConstants.ELEMENT_NAME_COLUMN);
            c.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_COLUMN, field.getReferenceName());

            final FieldType type = field.getFieldDefinitionInternal().getFieldType();
            if (type == FieldType.STRING) {
                c.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_TYPE, UpdateXMLConstants.TYPE_STRING);
            } else if (type == FieldType.INTEGER) {
                c.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_TYPE, UpdateXMLConstants.TYPE_NUMBER);
            } else if (type == FieldType.DATETIME) {
                c.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_TYPE, UpdateXMLConstants.TYPE_DATETIME);
            } else if (type == FieldType.DOUBLE) {
                c.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_TYPE, UpdateXMLConstants.TYPE_DOUBLE);
            } else if (type == FieldType.GUID) {
                c.setAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_TYPE, UpdateXMLConstants.TYPE_GUID);
            }

            if (value != null) {
                DOMUtils.appendChildWithText(c, UpdateXMLConstants.ELEMENT_NAME_VALUE, value);
            } else {
                /*
                 * To send a null ("empty") value for a field to the server, we
                 * send an empty <value> element.
                 */
                DOMUtils.appendChild(c, UpdateXMLConstants.ELEMENT_NAME_VALUE);
            }
        }
    }

    private void addHardcodedComputedColumns() {
        if (computedColumnsElement == null) {
            computedColumnsElement =
                DOMUtils.appendChild(workItemElement, UpdateXMLConstants.ELEMENT_NAME_COMPUTED_COLUMNS);
        }

        DOMUtils.appendChild(computedColumnsElement, UpdateXMLConstants.ELEMENT_NAME_COMPUTED_COLUMN).setAttribute(
            UpdateXMLConstants.ATTRIBUTE_NAME_COLUMN,
            CoreFieldReferenceNames.REVISED_DATE);

        DOMUtils.appendChild(computedColumnsElement, UpdateXMLConstants.ELEMENT_NAME_COMPUTED_COLUMN).setAttribute(
            UpdateXMLConstants.ATTRIBUTE_NAME_COLUMN,
            CoreFieldReferenceNames.CHANGED_DATE);

        DOMUtils.appendChild(computedColumnsElement, UpdateXMLConstants.ELEMENT_NAME_COMPUTED_COLUMN).setAttribute(
            UpdateXMLConstants.ATTRIBUTE_NAME_COLUMN,
            CoreFieldReferenceNames.PERSON_ID);

        if (workItem.getClient().getFieldDefinitions().contains(CoreFieldReferenceNames.AUTHORIZED_DATE)) {
            DOMUtils.appendChild(computedColumnsElement, UpdateXMLConstants.ELEMENT_NAME_COMPUTED_COLUMN).setAttribute(
                UpdateXMLConstants.ATTRIBUTE_NAME_COLUMN,
                CoreFieldReferenceNames.AUTHORIZED_DATE);
        }
    }

    private void processLinks() {
        addElementHandlers(workItem.getLinksInternal().addUpdateXML(workItemElement));
    }

    private void processAttachments() {
        addElementHandlers(workItem.getAttachmentsInternal().addUpdateXML(workItemElement));
    }

    private void addElementHandlers(final ElementHandler[] handlers) {
        if (handlers == null) {
            return;
        }
        for (int i = 0; i < handlers.length; i++) {
            List<ElementHandler> handlerList = updateElementNamesToHandlerLists.get(handlers[i].getElementName());
            if (handlerList == null) {
                handlerList = new ArrayList<ElementHandler>();
                updateElementNamesToHandlerLists.put(handlers[i].getElementName(), handlerList);
            }
            handlerList.add(handlers[i]);
        }
    }

    @Override
    protected void handleUpdateResponse(final DOMAnyContentType response) {
        final Element updateResultsElement = response.getElements()[0];

        Element responseElement;
        if (workItem.getFields().getID() == 0) {
            /*
             * new work item, we expect an InsertWorkItem element from the
             * server
             */
            responseElement = (Element) updateResultsElement.getElementsByTagName(
                UpdateXMLConstants.ELEMENT_NAME_INSERT_WORK_ITEM).item(0);
            if (responseElement == null) {
                try {
                    throw new RuntimeException(MessageFormat.format(
                        "expected an InsertWorkItem element in [{0}]", //$NON-NLS-1$
                        DOMSerializeUtils.toString(updateResultsElement)));
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            /*
             * normal case, we expect an UpdateWorkItem element from the server
             */
            responseElement = (Element) updateResultsElement.getElementsByTagName(
                UpdateXMLConstants.ELEMENT_NAME_UPDATE_WORK_ITEM).item(0);

            if (responseElement == null) {
                /*
                 * Only work item links were modified on the work item. No more
                 * work needs to be done.
                 */
                return;
            }
        }

        /*
         * update the work item's revision
         */
        final String newRevision = responseElement.getAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_REVISION);
        workItem.getFieldsInternal().getFieldInternal(WorkItemFieldIDs.REVISION).setValue(
            newRevision,
            FieldModificationType.SERVER);

        /*
         * if the work item was new and is now saved, update the ID
         */
        if (workItem.getFields().getID() == 0) {
            final String newId = responseElement.getAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_ID);
            workItem.getFieldsInternal().getFieldInternal(WorkItemFieldIDs.ID).setValue(
                newId,
                FieldModificationType.SERVER);
        }

        /*
         * process any computed columns
         */
        final Element computedColumnsElement =
            (Element) updateResultsElement.getElementsByTagName(UpdateXMLConstants.ELEMENT_NAME_COMPUTED_COLUMNS).item(
                0);
        if (computedColumnsElement != null) {
            final NodeList computedColumns =
                computedColumnsElement.getElementsByTagName(UpdateXMLConstants.ELEMENT_NAME_COMPUTED_COLUMN);
            for (int i = 0; i < computedColumns.getLength(); i++) {
                final Element computedColumnElement = (Element) computedColumns.item(i);
                final String fieldReferenceName =
                    computedColumnElement.getAttribute(UpdateXMLConstants.ATTRIBUTE_NAME_COLUMN);
                final String value =
                    computedColumnElement.getElementsByTagName(UpdateXMLConstants.ELEMENT_NAME_VALUE).item(
                        0).getChildNodes().item(0).getNodeValue();

                final FieldImpl field = workItem.getFieldsInternal().getFieldInternal(fieldReferenceName);
                field.setValue(value, FieldModificationType.SERVER);
            }
        }

        /*
         * process any custom handlers
         */
        for (final Iterator<String> it = updateElementNamesToHandlerLists.keySet().iterator(); it.hasNext();) {
            final String elementName = it.next();
            final List<ElementHandler> handlerList = updateElementNamesToHandlerLists.get(elementName);
            final NodeList elements = updateResultsElement.getElementsByTagName(elementName);
            for (int i = 0; i < elements.getLength(); i++) {
                final Element element = (Element) elements.item(i);
                final ElementHandler handler = handlerList.get(i);
                handler.handle(element);
            }
        }
    }
}
