// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.connect.CrossCollectionProjectTable.CrossCollectionProjectInfo;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

public class CrossCollectionProjectSelectControl extends BaseControl {
    private static final Log log = LogFactory.getLog(CrossCollectionProjectSelectControl.class);

    public static final String PROJECT_TABLE_ID = "CrossCollectionProjectSelectControl.projectTable"; //$NON-NLS-1$

    private final Text filterBox;
    private final Timer filterTimer;
    private FilterTask filterTask;
    private final CrossCollectionProjectTable table;

    private final SingleListenerFacade listeners = new SingleListenerFacade(ProjectSelectionChangedListener.class);

    public CrossCollectionProjectSelectControl(
        final Composite parent,
        final int style,
        final SourceControlCapabilityFlags sourceControlCapabilityFlags) {
        super(parent, style);

        final GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        // Create timer for filtering the list
        filterTimer = new Timer(false);

        // Create the filter text box
        filterBox = new Text(this, SWT.BORDER);
        filterBox.setMessage(Messages.getString("CrossCollectionProjectSelectControl.FilterHintText")); //$NON-NLS-1$
        filterBox.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                startTimer();
            }
        });
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(filterBox);

        table = new CrossCollectionProjectTable(this, SWT.NONE, false);
        table.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                ((ProjectSelectionChangedListener) listeners.getListener()).onProjectSelectionChanged(
                    new ProjectSelectionChangedEvent(table.getSelectedProject()));
            }
        });
        GridDataBuilder.newInstance().grab().hHint(getVerticalSpacing() * 30).fill().applyTo(table);
    }

    private void startTimer() {
        // Cancel any existing task
        if (filterTask != null) {
            filterTask.cancel();
        }
        // Create a new task
        filterTask = new FilterTask();
        // Schedule the task
        filterTimer.schedule(filterTask, 400);
    }

    // Call this when the owning control is going away
    public void stopTimer() {
        // Cancel any existing task
        if (filterTask != null) {
            filterTask.cancel();
        }
    }

    private class FilterTask extends TimerTask {
        @Override
        public void run() {
            UIHelpers.runOnUIThread(true, new Runnable() {
                @Override
                public void run() {
                    table.applyFilter(filterBox.getText());
                }
            });
        }
    }

    public CrossCollectionProjectInfo getSelectedProject() {
        return table.getSelectedProject();
    }

    public void addListener(final ProjectSelectionChangedListener listener) {
        listeners.addListener(listener);
    }

    public void removeListener(final ProjectSelectionChangedListener listener) {
        listeners.removeListener(listener);
    }

    public void refresh(final List<CrossCollectionProjectInfo> projects) {
        if (projects == null) {
            table.setProjects(null);
            return;
        }

        table.setProjects(projects.toArray(new CrossCollectionProjectInfo[projects.size()]));
    }

    public interface ProjectSelectionChangedListener {
        public void onProjectSelectionChanged(ProjectSelectionChangedEvent event);
    }

    public final class ProjectSelectionChangedEvent {
        private final CrossCollectionProjectInfo selectedProject;

        private ProjectSelectionChangedEvent(final CrossCollectionProjectInfo selectedProject) {
            this.selectedProject = selectedProject;
        }

        public CrossCollectionProjectInfo getSelectedProject() {
            return selectedProject;
        }
    }
}
