// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.wit.form.DebuggingContext;
import com.microsoft.tfs.client.common.ui.wit.form.FieldTracker;
import com.microsoft.tfs.client.common.ui.wit.form.FormContext;
import com.microsoft.tfs.client.common.ui.wit.form.WorkItemEditorContextMenu;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;

public abstract class BaseWorkItemControl implements IWorkItemControl {
    private WIFormElement formElement;
    private FormContext formContext;

    @Override
    public final void init(final WIFormElement formElement, final FormContext formContext) {
        this.formElement = formElement;
        this.formContext = formContext;

        hookInit();
    }

    protected String getFieldDataAsString(final String fieldName) {
        if (fieldName == null) {
            return ""; //$NON-NLS-1$
        }

        final Object value = formContext.getWorkItem().getFields().getField(fieldName).getValue();

        if (value == null) {
            return ""; //$NON-NLS-1$
        }
        return value.toString();
    }

    @Override
    public boolean wantsVerticalFill() {
        return false;
    }

    protected void hookInit() {

    }

    protected FieldTracker getFieldTracker() {
        return formContext.getFieldTracker();
    }

    protected DebuggingContext getDebuggingContext() {
        return formContext.getDebuggingContext();
    }

    protected WIFormElement getFormElement() {
        return formElement;
    }

    protected TFSServer getServer() {
        return formContext.getServer();
    }

    protected WorkItem getWorkItem() {
        return formContext.getWorkItem();
    }

    protected FormContext getFormContext() {
        return formContext;
    }

    protected WorkItemEditorContextMenu getWorkItemEditorContextMenu() {
        return formContext.getWorkItemEditorContextMenu();
    }

    protected boolean isFormElementLastAmongSiblings() {
        final WIFormElement[] siblings = formElement.getParentElement().getChildElements();

        return siblings[siblings.length - 1] == formElement;
    }
}
