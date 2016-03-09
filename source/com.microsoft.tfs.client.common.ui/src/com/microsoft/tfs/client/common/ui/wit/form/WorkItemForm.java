// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateAdapter;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateListener;
import com.microsoft.tfs.core.clients.workitem.form.WIFormDescription;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLayout;
import com.microsoft.tfs.core.clients.workitem.internal.link.LinkCollectionImpl;
import com.microsoft.tfs.core.clients.workitem.link.DescriptionUpdateErrorCallback;
import com.microsoft.tfs.core.clients.workitem.link.DescriptionUpdateFinishedCallback;
import com.microsoft.tfs.core.clients.workitem.link.LinkCollection;
import com.microsoft.tfs.util.Check;

public class WorkItemForm extends Composite {
    private final TFSServer server;
    private final WorkItem workItem;
    private final FieldTracker fieldTracker;
    private WorkItemStateListener workItemStateListener;
    private static final Log log = LogFactory.getLog(WorkItemForm.class);

    public WorkItemForm(
        final Composite parent,
        final int style,
        final TFSServer server,
        final WorkItem workItem,
        final FieldTracker fieldTracker) {
        super(parent, style);

        Check.notNull(server, "server"); //$NON-NLS-1$

        this.server = server;
        this.workItem = workItem;
        this.fieldTracker = fieldTracker;

        populate();
    }

    private void populate() {
        /*
         * get the form description from the work item's type
         */
        final WIFormDescription formDescription = workItem.getType().getFormDescription();

        /*
         * compute the proper layout inside the description to use
         */
        final WIFormLayout formLayoutDescription = Helpers.getLayoutForForm(formDescription);

        /*
         * create the WorkItemEditorContextMenu object, which can supply context
         * menus for child controls
         */
        final WorkItemEditorContextMenu workItemEditorContextMenu =
            new WorkItemEditorContextMenu(server, workItem, getShell(), this);

        /*
         * create a debugging context
         */
        final DebuggingContext debuggingContext = new DebuggingContext(getDisplay());

        /*
         * create the FormContext object, which is a "composite" object
         * containing all of the context of the form. it's passed around among
         * child controls
         */
        final FormContext formContext = new FormContext(
            server,
            workItem,
            formLayoutDescription,
            fieldTracker,
            workItemEditorContextMenu,
            debuggingContext);

        /*
         * set our layout to fill and create a scrolled composite for the form
         * to go into
         */
        setLayout(new FillLayout());
        final ScrolledComposite sc1 = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
        sc1.setExpandHorizontal(true);
        sc1.setExpandVertical(true);

        /*
         * create the vertical stack control for the root layout element
         */
        final VerticalStackControl verticalStackControl =
            new VerticalStackControl(sc1, SWT.NONE, formLayoutDescription, formContext);

        /*
         * set up the scrolled composite with proper min size
         */
        sc1.setContent(verticalStackControl);
        sc1.setMinSize(verticalStackControl.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        /*
         * set focus to the first invalid field (if there is one)
         */
        fieldTracker.setFocusToFirstInvalidField();

        /**
         * Initiate the background job to retrieve work item fields of link
         * targets.
         */
        updateWorkItemLinkTargetsColumns();

        /*
         * This state listener is a kludge until the core WIT OM has better
         * state/listener management. For now we just update the links control
         * on work item save, which is intended to handle the case where you
         * associate a work item with a checkin and that work item is open in an
         * editor.
         *
         * Added an update on synchedToLatest. This catches the case where the
         * links collection is modified, then the work item is reverted.
         */
        workItemStateListener = new WorkItemStateAdapter() {
            @Override
            public void saved(final WorkItem workItem) {
                UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
                    @Override
                    public void run() {
                        updateWorkItemLinkTargetsColumns();
                    }
                });
            }

            @Override
            public void synchedToLatest(final WorkItem workItem) {
                UIHelpers.runOnUIThread(getDisplay(), true, new Runnable() {
                    @Override
                    public void run() {
                        updateWorkItemLinkTargetsColumns();
                    }
                });
            }
        };

        workItem.addWorkItemStateListener(workItemStateListener);

        // Remove the work item state listener when this work item is disposed.
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                workItem.removeWorkItemStateListener(workItemStateListener);
            }
        });
    }

    /**
     * Initiate a background job that will query for the referenced fields of
     * all work item links contained within this work item. Completion of the
     * background job is indicated via a success or failure callback.
     */
    public void updateWorkItemLinkTargetsColumns() {
        // Do nothing if all links have already been updated.
        final LinkCollectionImpl linkCollectionImpl = (LinkCollectionImpl) workItem.getLinks();

        if (linkCollectionImpl.allDescriptionsComputed()) {
            return;
        }

        // Create the runnable which will be initiated by a background job.
        final Runnable runnable = linkCollectionImpl.getDescriptionUpdateRunnable(
            new WorkItemLinkDescriptionUpdateErrorCallback(),
            new WorkItemLinkDescriptionUpdateFinishedCallback(linkCollectionImpl));

        // Create the background job.
        final Job job = new Job(Messages.getString("WorkItemForm.UpdateLinkJobTitle")) //$NON-NLS-1$
        {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                monitor.beginTask(Messages.getString("WorkItemForm.ProgressStatusText"), IProgressMonitor.UNKNOWN); //$NON-NLS-1$

                try {
                    runnable.run();
                    return Status.OK_STATUS;
                } finally {
                    monitor.done();
                }
            }
        };

        // Start the background job.
        job.schedule();
    }

    /**
     * Callback method for the unsuccessful completion of the link update job.
     */
    private class WorkItemLinkDescriptionUpdateErrorCallback implements DescriptionUpdateErrorCallback {
        @Override
        public void onDescriptionUpdateError(final Throwable error) {
            log.error("Error updating link descriptions", error); //$NON-NLS-1$
        }
    }

    /**
     * Callback method for successful completion of the link update job.
     */
    private class WorkItemLinkDescriptionUpdateFinishedCallback implements DescriptionUpdateFinishedCallback {
        private final LinkCollection linkCollection;

        public WorkItemLinkDescriptionUpdateFinishedCallback(final LinkCollection linkCollection) {
            this.linkCollection = linkCollection;
        }

        @Override
        public void onDescriptionUpdateFinished() {
            ((LinkCollectionImpl) linkCollection).linkTargetsUpdated();
        }
    }
}
