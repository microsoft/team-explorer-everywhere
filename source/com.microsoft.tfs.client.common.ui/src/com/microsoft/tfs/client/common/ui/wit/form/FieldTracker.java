// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;

public class FieldTracker {
    private static final Log log = LogFactory.getLog(FieldTracker.class);

    public static interface FocusReceiver {
        public boolean setFocus();
    }

    private static class FieldInfo {
        public FocusReceiver focusReceiver;

        public FieldInfo(final Field field) {
        }
    }

    /*
     * Field -> FieldInfo
     */
    private final Map fields = new HashMap();

    /*
     * keeps track of the order in which the Fields are added to this
     * FieldTracker
     */
    private final List insertionOrder = new ArrayList();

    /*
     * the child FieldTrackers of this FieldTracker
     */
    private final List children = new ArrayList();

    public FieldTracker() {

    }

    public FieldTracker(final FieldTracker parent) {
        synchronized (parent) {
            parent.children.add(this);
        }
    }

    public synchronized void addField(final Field field) {
        if (contains(field, true)) {
            final String messageFormat = "Duplicate field {0} ignored by field tracker"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, field.getReferenceName());
            log.warn(message);
            return;
        }

        fields.put(field, new FieldInfo(field));
        if (insertionOrder.contains(field)) {
            insertionOrder.remove(field);
        }
        insertionOrder.add(field);
    }

    public synchronized boolean contains(final Field field, final boolean includeChildren) {
        boolean contained = fields.containsKey(field);
        if (!contained && includeChildren) {
            for (final Iterator it = children.iterator(); it.hasNext();) {
                final FieldTracker child = (FieldTracker) it.next();
                contained = child.contains(field, true);
                if (contained) {
                    break;
                }
            }
        }
        return contained;
    }

    public synchronized void setFocusReceiver(final Field field, final FocusReceiver focusReceiver) {
        if (!fields.containsKey(field)) {
            final String messageFormat = "this FieldTracker does not contain the field [{0}]"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, field);
            throw new IllegalArgumentException(message);
        }
        ((FieldInfo) fields.get(field)).focusReceiver = focusReceiver;
    }

    public synchronized Field findFirstInvalidField() {
        for (final Iterator it = insertionOrder.iterator(); it.hasNext();) {
            final Field field = (Field) it.next();
            if (field.getStatus() != FieldStatus.VALID) {
                return field;
            }
        }

        for (final Iterator it = children.iterator(); it.hasNext();) {
            final FieldTracker child = (FieldTracker) it.next();
            final Field field = child.findFirstInvalidField();
            if (field != null) {
                return field;
            }
        }

        return null;
    }

    public synchronized boolean setFocusToFirstInvalidField() {
        for (final Iterator it = insertionOrder.iterator(); it.hasNext();) {
            final Field field = (Field) it.next();
            if (field.getStatus() != FieldStatus.VALID) {
                final FieldInfo fieldInfo = (FieldInfo) fields.get(field);
                if (fieldInfo.focusReceiver != null) {
                    if (fieldInfo.focusReceiver.setFocus()) {
                        return true;
                    }
                }
            }
        }

        for (final Iterator it = children.iterator(); it.hasNext();) {
            final FieldTracker child = (FieldTracker) it.next();
            if (child.setFocusToFirstInvalidField()) {
                return true;
            }
        }

        return false;
    }

    public String getMessageFromFirstInvalidField(final WorkItem workItem) {
        Field field = findFirstInvalidField();

        if (field == null && workItem != null) {
            for (final Iterator it = workItem.getFields().iterator(); it.hasNext();) {
                final Field nonContainedField = (Field) it.next();
                if (nonContainedField.getStatus() != FieldStatus.VALID) {
                    field = nonContainedField;
                    break;
                }
            }
        }

        if (field != null) {
            return field.getStatus().getInvalidMessage(field);
        }

        return null;
    }
}
