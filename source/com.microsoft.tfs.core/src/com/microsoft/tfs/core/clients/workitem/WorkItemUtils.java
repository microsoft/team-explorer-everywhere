// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem;

import com.microsoft.tfs.core.clients.workitem.link.LinkFactory;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;

/**
 * Utilities for working with work items.
 *
 * @since TEE-SDK-10.1
 */
public class WorkItemUtils {
    public static WorkItem createRelatedWorkItem(
        final WorkItem inputWorkItem,
        final WorkItemType workItemType,
        final int linkTypeId,
        String newTitle,
        final String comment,
        final boolean readOnly) {
        final WorkItem newWorkItem = inputWorkItem.getClient().newWorkItem(workItemType);

        if (newTitle == null) {
            newTitle = ""; //$NON-NLS-1$
        }

        newWorkItem.getFields().getField(CoreFieldReferenceNames.TITLE).setValue(newTitle);

        newWorkItem.getFields().getField(CoreFieldReferenceNames.AREA_ID).setValue(
            inputWorkItem.getFields().getField(CoreFieldReferenceNames.AREA_ID).getValue());

        newWorkItem.getFields().getField(CoreFieldReferenceNames.ASSIGNED_TO).setValue(
            inputWorkItem.getFields().getField(CoreFieldReferenceNames.ASSIGNED_TO).getValue());

        newWorkItem.getFields().getField(CoreFieldReferenceNames.ITERATION_ID).setValue(
            inputWorkItem.getFields().getField(CoreFieldReferenceNames.ITERATION_ID).getValue());

        final RelatedLink relatedLink =
            LinkFactory.newRelatedLink(newWorkItem, inputWorkItem, linkTypeId, comment, readOnly);
        newWorkItem.getLinks().add(relatedLink);

        return newWorkItem;
    }

    /**
     * For Visual Studio compatibility: converts a Double-type to a String, but
     * ensures that whole numbers have no decimal point.
     *
     * This is for rule engine parsing and display. Ie, when we have a double
     * value "0", we display it as "0.0", while .NET (and C# in general)
     * displays "0". This leads to interop headaches, particularly with
     * ALLOWEDVALUES fields.
     *
     * @param value
     *        A Double to convert to a String
     * @return A String representation of a Double
     */
    public static String doubleToString(final Double value) {
        if (value == null) {
            return null;
        }

        if (value.intValue() == value.doubleValue()) {
            return Integer.toString(value.intValue());
        }

        return value.toString();
    }

    /**
     * For Visual Studio compatibility. Takes an Object and returns a string
     * representation. Generally calls Object.toString(), unless we know that
     * TFS wants a particular type formatted a particular way.
     *
     * @param value
     *        An object
     * @return The string representation of an object
     */
    public static String objectToString(final Object value) {
        if (value instanceof Double) {
            return doubleToString((Double) value);
        } else {
            return value.toString();
        }
    }
}
