// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.wit.form.FormBuildException;
import com.microsoft.tfs.client.common.ui.wit.form.FormGroupData;
import com.microsoft.tfs.client.common.ui.wit.form.FormGroupLayout;
import com.microsoft.tfs.client.common.ui.wit.form.VerticalStackControl;
import com.microsoft.tfs.core.clients.workitem.form.WIFormColumn;
import com.microsoft.tfs.core.clients.workitem.form.WIFormGroup;

public class GroupControl extends BaseWorkItemControl {
    private Composite composite;
    private WIFormGroup groupDescription;

    private final List verticalStackControls = new ArrayList();

    @Override
    protected void hookInit() {
        groupDescription = (WIFormGroup) getFormElement();
    }

    @Override
    public int getMinimumRequiredColumnCount() {
        return 1;
    }

    @Override
    public void addToComposite(final Composite parent) {
        populate(parent);

        final int numColumns = ((GridLayout) parent.getLayout()).numColumns;
        if (wantsVerticalFill() || (groupDescription.getParentElement() instanceof WIFormColumn)) {
            composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, numColumns, 1));
        } else {
            composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, numColumns, 1));
        }
    }

    @Override
    public boolean wantsVerticalFill() {
        if (!isFormElementLastAmongSiblings()) {
            return false;
        }

        if (groupDescription.getLabel() != null) {
            return false;
        }

        for (final Iterator it = verticalStackControls.iterator(); it.hasNext();) {
            final VerticalStackControl vsc = (VerticalStackControl) it.next();
            if (vsc.wantsVerticalFill()) {
                return true;
            }
        }

        return false;
    }

    private void populate(final Composite parent) {
        final WIFormColumn[] columns = groupDescription.getColumnChildren();

        if (groupDescription.getLabel() != null) {
            composite = new Group(parent, SWT.NONE);
            ((Group) composite).setText(groupDescription.getLabel());
        } else {
            composite = new Composite(parent, SWT.NONE);
        }
        getDebuggingContext().debug(composite, groupDescription);
        getWorkItemEditorContextMenu().setMenuOnControl(composite);

        final int margin = (groupDescription.getLabel() == null ? 0 : 5);

        final FormGroupLayout layout = new FormGroupLayout();
        layout.marginWidth = margin;
        layout.marginHeight = margin;
        getDebuggingContext().setupFormGroupLayout(layout);
        composite.setLayout(layout);

        for (int i = 0; i < columns.length; i++) {
            final VerticalStackControl vsc =
                new VerticalStackControl(composite, SWT.NONE, columns[i], getFormContext());

            verticalStackControls.add(vsc);

            final Integer percentWidth = columns[i].getPercentWidth();
            final Integer fixedWidth = columns[i].getFixedWidth();

            if (percentWidth == null && fixedWidth == null) {
                throw new FormBuildException(Messages.getString("GroupControl.NoColumnWidthSpecified")); //$NON-NLS-1$
            }
            if (percentWidth != null && fixedWidth != null) {
                throw new FormBuildException(Messages.getString("GroupControl.InvalidColumnWidthSpecified")); //$NON-NLS-1$
            }

            if (percentWidth != null) {
                vsc.setLayoutData(new FormGroupData(percentWidth.intValue(), true));
            } else {
                vsc.setLayoutData(new FormGroupData(fixedWidth.intValue(), false));
            }
        }
    }
}
