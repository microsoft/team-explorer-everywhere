// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeListener;

public class FieldChangeListenerSupport {
    private final Set listeners = new HashSet();
    private final Field field;

    public FieldChangeListenerSupport(final Field field) {
        this.field = field;
    }

    public synchronized void addFieldChangeListener(final FieldChangeListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeFieldChangeListener(final FieldChangeListener listener) {
        listeners.remove(listener);
    }

    public synchronized void fireListeners(final Object source) {
        final FieldChangeEvent event = new FieldChangeEvent();
        event.source = source;
        event.field = field;

        for (final Iterator it = listeners.iterator(); it.hasNext();) {
            ((FieldChangeListener) it.next()).fieldChanged(event);
        }
    }
}
