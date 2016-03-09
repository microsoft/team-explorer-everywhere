// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.wit.form.FormContext;
import com.microsoft.tfs.core.clients.workitem.form.WIFormElement;

public interface IWorkItemControl {
    public void init(WIFormElement formElement, FormContext formContext);

    public int getMinimumRequiredColumnCount();

    public void addToComposite(Composite parent);

    public boolean wantsVerticalFill();
}
