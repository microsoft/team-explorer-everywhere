// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateAdapter;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateListener;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeEvent;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeListener;
import com.microsoft.tfs.util.StringUtil;

public class WorkItemFormHeader extends Composite {
    private final WorkItem workItem;
    private final FieldTracker fieldTracker;
    private CLabel label;
    private FieldChangeListener titleFieldChangeListener;
    private WorkItemStateListener workItemStateListener;

    private boolean disconnected = false;

    public WorkItemFormHeader(
        final Composite parent,
        final int style,
        final WorkItem workItem,
        final FieldTracker fieldTracker) {
        super(parent, style);
        this.workItem = workItem;
        this.fieldTracker = fieldTracker;
        populate();
    }

    private void populate() {
        final Color backgroundColor = getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND);

        final FillLayout layout = new FillLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);

        setBackground(backgroundColor);

        label = new CLabel(this, SWT.LEFT);
        label.setBackground(backgroundColor);

        attachWorkItemListeners();

        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                onDispose();
            }
        });
    }

    private void attachWorkItemListeners() {
        titleFieldChangeListener = new TitleChangeListener();
        workItemStateListener = new StateListener();

        workItem.getFields().getField(CoreFieldReferenceNames.TITLE).addFieldChangeListener(titleFieldChangeListener);
        workItem.addWorkItemStateListener(workItemStateListener);
    }

    private void onDispose() {
        workItem.getFields().getField(CoreFieldReferenceNames.TITLE).removeFieldChangeListener(
            titleFieldChangeListener);
        workItem.removeWorkItemStateListener(workItemStateListener);
    }

    private class TitleChangeListener implements FieldChangeListener {
        @Override
        public void fieldChanged(final FieldChangeEvent event) {
            refresh();
        }
    }

    private class StateListener extends WorkItemStateAdapter {
        @Override
        public void dirtyStateChanged(final boolean isDirty, final WorkItem workItem) {
            refresh();
        }

        @Override
        public void validStateChanged(final boolean isValid, final WorkItem workItem) {
            refresh();
        }
    }

    public void refresh() {
        /*
         * header refresh can be triggered by one of four things:
         *
         * 1) external call of this method, normally as part of the initial
         * building of a GUI 2) work item title field change 3) work item dirty
         * state change 4) work item valid state change
         *
         * we're not always guaranteed to be on the UI thread for all these
         * cases, hence the extra safety here
         */

        final String headerText = getHeaderText();

        UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) {
                    return;
                }

                label.setText(headerText);
                if (workItem.isValid()) {
                    label.setImage(null);
                } else {
                    label.setImage(
                        PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
                }
                label.getParent().layout();
            }
        });
    }

    private String getHeaderText() {
        final StringBuffer sb = new StringBuffer();

        /*
         * Internationalization note: format the IDs as strings instead of
         * passing numbers to MessageFormat, which always inserts group
         * separators for the locale (for example, "Work item 1,234").
         */

        final int id = workItem.getFields().getID();
        if (id == 0) {
            /*
             * new work item
             */
            final String messageFormat = Messages.getString("WorkItemFormHeader.NewWorkItemTitleFormat"); //$NON-NLS-1$
            sb.append(MessageFormat.format(messageFormat, workItem.getType().getName()));
        } else {
            if (workItem.isDirty()) {
                final String messageFormat = Messages.getString("WorkItemFormHeader.DirtyExistingWorkItemTitleFormat"); //$NON-NLS-1$
                sb.append(MessageFormat.format(messageFormat, workItem.getType().getName(), Integer.toString(id)));
            } else {
                final String messageFormat = Messages.getString("WorkItemFormHeader.CleanExistingWorkItemTitleFormat"); //$NON-NLS-1$
                sb.append(MessageFormat.format(messageFormat, workItem.getType().getName(), Integer.toString(id)));
            }
        }

        String message = null;
        if (workItem.isValid()) {
            message = (String) workItem.getFields().getField(CoreFieldReferenceNames.TITLE).getValue();
            if (message != null && message.trim().length() == 0) {
                message = null;
            }
        } else {
            message = fieldTracker.getMessageFromFirstInvalidField(workItem);
        }

        // Escape any ampersands
        message = StringUtil.replace(message, "&", "&&"); //$NON-NLS-1$ //$NON-NLS-2$

        if (message != null) {
            sb.append(" : "); //$NON-NLS-1$
            sb.append(message);
        }

        if (disconnected) {
            sb.append(Messages.getString("WorkItemFormHeader.DisconnectedTitleSuffix")); //$NON-NLS-1$
        }

        return sb.toString();
    }

    public void setDisconnected(final boolean disconnected) {
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                WorkItemFormHeader.this.disconnected = disconnected;

                refresh();
            }
        });
    }
}
