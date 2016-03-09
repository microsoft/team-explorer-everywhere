// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLayout;
import com.microsoft.tfs.util.Check;

public class FormContext {
    private final TFSServer server;
    private final WorkItem workItem;
    private final WIFormLayout formLayoutDescription;
    private final WorkItemEditorContextMenu workItemEditorContextMenu;
    private final DebuggingContext debuggingContext;
    private final List fieldTrackerStack = new ArrayList();

    public FormContext(
        final TFSServer server,
        final WorkItem workItem,
        final WIFormLayout formLayoutDescription,
        final FieldTracker fieldTracker,
        final WorkItemEditorContextMenu workItemEditorContextMenu,
        final DebuggingContext debuggingContext) {
        Check.notNull(server, "server"); //$NON-NLS-1$

        this.server = server;
        this.workItem = workItem;
        this.formLayoutDescription = formLayoutDescription;
        this.workItemEditorContextMenu = workItemEditorContextMenu;
        this.debuggingContext = debuggingContext;

        pushFieldTracker(fieldTracker);
    }

    public DebuggingContext getDebuggingContext() {
        return debuggingContext;
    }

    public FieldTracker getFieldTracker() {
        return (FieldTracker) fieldTrackerStack.get(0);
    }

    public void pushFieldTracker(final FieldTracker fieldTracker) {
        fieldTrackerStack.add(0, fieldTracker);
    }

    public FieldTracker popFieldTracker() {
        return (FieldTracker) fieldTrackerStack.remove(0);
    }

    public WIFormLayout getFormLayoutDescription() {
        return formLayoutDescription;
    }

    public TFSServer getServer() {
        return server;
    }

    public WorkItem getWorkItem() {
        return workItem;
    }

    public WorkItemEditorContextMenu getWorkItemEditorContextMenu() {
        return workItemEditorContextMenu;
    }
}
