// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.sections;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import com.microsoft.tfs.client.common.ui.buildmanager.BuildManagerEvent;
import com.microsoft.tfs.client.common.ui.buildmanager.BuildManagerListener;
import com.microsoft.tfs.client.common.ui.buildmanager.BuildPropertyChangedEvent;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.table.tooltip.TableTooltipLabelManager;
import com.microsoft.tfs.client.common.ui.framework.table.tooltip.TableTooltipLabelProvider;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildConstants;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildImageHelper;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.BuildHelpers;
import com.microsoft.tfs.client.common.ui.teambuild.teamexplorer.helpers.TimeSpanHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.sections.TeamExplorerBaseSection;
import com.microsoft.tfs.client.common.ui.views.TeamExplorerView;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildDetailSpec;
import com.microsoft.tfs.core.clients.build.IBuildQueryResult;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.IQueuedBuildQueryResult;
import com.microsoft.tfs.core.clients.build.IQueuedBuildSpec;
import com.microsoft.tfs.core.clients.build.flags.BuildStatus;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;

public class TeamExplorerBuildsMyBuildsSection extends TeamExplorerBaseSection {
    private final TeamBuildImageHelper imageHelper = new TeamBuildImageHelper();

    private final BuildManagerListener buildManagerListener = new MyBuildManagerListener();

    private final int MAX_COMPLETED_BUILDS = 5;
    private final BuildStatus COMPLETED_BUILD_STATES =
        BuildStatus.FAILED.combine(BuildStatus.PARTIALLY_SUCCEEDED).combine(BuildStatus.SUCCEEDED).combine(
            BuildStatus.STOPPED);

    private TeamExplorerContext context;
    private FormToolkit toolkit;

    Object[] buildItems;

    private Composite composite;
    private TableViewer tableViewer;

    private final Object refreshLock = new Object();
    private boolean refreshInProgress = false;
    private boolean refreshThreadStop = false;
    private long refreshLastTime = 0;
    private volatile int refreshInterval = (30 * 1000);

    @Override
    public boolean initializeInBackground(final TeamExplorerContext context) {
        return true;
    }

    @Override
    public void initialize(final IProgressMonitor monitor, final TeamExplorerContext context) {
        this.context = context;
        loadBuildItems();
    }

    private void refreshInternal() {
        final int before = buildItems == null ? 0 : buildItems.length;

        try {
            loadBuildItems();
        } finally {
            synchronized (refreshLock) {
                refreshInProgress = false;
                refreshLastTime = System.currentTimeMillis();
            }
        }

        final int after = buildItems == null ? 0 : buildItems.length;

        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                if (before != after) {
                    final Section section = (Section) composite.getParent();

                    if (section != null && !section.isDisposed()) {
                        section.setExpanded(false);

                        composite.getChildren()[0].dispose();

                        if (after == 0) {
                            createEmptyLabel(toolkit, composite);
                        } else {
                            createTableViewer(toolkit, composite);
                        }

                        section.setExpanded(true);
                    }
                } else if (before > 0) {
                    if (tableViewer != null && !tableViewer.getControl().isDisposed()) {
                        tableViewer.setInput(buildItems);
                    }
                }
            }
        });
    }

    private void loadBuildItems() {
        final IBuildServer buildServer = context.getBuildServer();
        if (buildServer == null) {
            return;
        }

        // Create a calendar for 7 days ago.
        final Calendar beginDate = Calendar.getInstance();
        beginDate.add(Calendar.DAY_OF_YEAR, -7);

        final String projectName = context.getCurrentProjectInfo().getName();
        final String uniqueUserName = context.getServer().getConnection().getAuthenticatedIdentity().getUniqueName();

        // Query for the queued builds.
        final IQueuedBuildSpec queueSpec = buildServer.createBuildQueueSpec(projectName);
        final IQueuedBuildQueryResult[] queuedResult = buildServer.queryQueuedBuilds(new IQueuedBuildSpec[] {
            queueSpec
        });

        // Query for the completed builds.
        final IBuildDetailSpec detailSpec = buildServer.createBuildDetailSpec(projectName);
        detailSpec.setMinFinishTime(beginDate);
        detailSpec.setRequestedFor(uniqueUserName);
        detailSpec.setStatus(COMPLETED_BUILD_STATES);
        final IBuildQueryResult completedResult = buildServer.queryBuilds(detailSpec);

        final List<Object> list = new ArrayList<Object>();
        final IQueuedBuild[] queuedBuilds = queuedResult[0].getQueuedBuilds();
        Arrays.sort(queuedBuilds, new Comparator<IQueuedBuild>() {
            @Override
            public int compare(final IQueuedBuild o1, final IQueuedBuild o2) {
                return o2.getQueueTime().compareTo(o1.getQueueTime());
            }
        });

        for (final IQueuedBuild queuedBuild : queuedResult[0].getQueuedBuilds()) {
            if (queuedBuild.getRequestedFor().equals(uniqueUserName)) {
                list.add(queuedBuild);
            }
        }

        final IBuildDetail[] completedBuilds = completedResult.getBuilds();
        Arrays.sort(completedBuilds, new Comparator<IBuildDetail>() {
            @Override
            public int compare(final IBuildDetail o1, final IBuildDetail o2) {
                return o2.getFinishTime().compareTo(o1.getFinishTime());
            }
        });

        for (int i = 0; i < MAX_COMPLETED_BUILDS && i < completedBuilds.length; i++) {
            list.add(completedBuilds[i]);
        }

        buildItems = list.toArray();
    }

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        this.toolkit = toolkit;
        composite = toolkit.createComposite(parent);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 1, true, 0, 5);

        if (!context.isConnected()) {
            createDisconnectedContent(toolkit, composite);
            return composite;
        } else if (buildItems.length == 0) {
            createEmptyLabel(toolkit, composite);
        } else {
            createTableViewer(toolkit, composite);
        }

        // Create a background worker thread that will perform an automatic
        // refresh for this section.
        final Thread refreshThread = new Thread(new MyBuildsRefreshWorker());
        refreshThread.setName("My Builds Auto Refresh"); //$NON-NLS-1$
        refreshThread.start();

        // Add build changes listeners to allow refresh updates in the UI.
        BuildHelpers.getBuildManager().addBuildManagerListener(buildManagerListener);

        // Handle disposal of this control.
        composite.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                refreshThreadStop = true;
                refreshThread.interrupt();
                imageHelper.dispose();
                BuildHelpers.getBuildManager().removeBuildManagerListener(buildManagerListener);
            }
        });

        return composite;
    }

    private void createEmptyLabel(final FormToolkit toolkit, final Composite parent) {
        final String message = Messages.getString("TeamExplorerBuildsMyBuildsSection.YouHaveNoBuilds"); //$NON-NLS-1$
        final Label label = toolkit.createLabel(composite, message);
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(label);
    }

    private void createTableViewer(final FormToolkit toolkit, final Composite parent) {
        tableViewer = new TableViewer(composite, SWT.MULTI | SWT.FULL_SELECTION | SWT.NO_SCROLL);
        tableViewer.setContentProvider(new MyBuildsContentProvider());
        tableViewer.setLabelProvider(new MyBuildsLabelProvider());
        tableViewer.addDoubleClickListener(new MyBuildsDoubleClickListener());
        tableViewer.setInput(buildItems);
        GridDataBuilder.newInstance().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableViewer.getControl());

        // hook up tooltips
        final TableTooltipLabelManager tooltipManager =
            new TableTooltipLabelManager(tableViewer.getTable(), new MyBuildsTooltipProvider(), false);

        tooltipManager.addTooltipManager();

        registerContextMenu(context, tableViewer.getControl(), tableViewer);
    }

    private class MyBuildsTooltipProvider implements TableTooltipLabelProvider {
        @Override
        public String getTooltipText(final Object element, final int columnIndex) {
            if (element instanceof IBuildDetail) {
                final IBuildDetail detail = (IBuildDetail) element;
                final String format = Messages.getString("TeamExplorerBuildsMyBuildsSection.BuildDetailTooltipFormat"); //$NON-NLS-1$

                return MessageFormat.format(
                    format, //
                    detail.getBuildNumber(),
                    detail.getBuildDefinition().getName(),
                    detail.getBuildServer().getDisplayText(detail.getStatus()),
                    TimeSpanHelpers.ago(detail.getFinishTime()),
                    detail.getRequestedBy(),
                    detail.getBuildServer().getDisplayText(detail.getReason()),
                    TeamBuildConstants.DATE_FORMAT.format(detail.getStartTime().getTime()),
                    TeamBuildConstants.DATE_FORMAT.format(detail.getFinishTime().getTime()),
                    TimeSpanHelpers.duration(detail.getStartTime(), detail.getFinishTime()));
            } else if (element instanceof IQueuedBuild) {
                final IQueuedBuild queued = (IQueuedBuild) element;
                final IBuildDetail detail = queued.getBuild();

                if (detail != null) {
                    final String format =
                        Messages.getString("TeamExplorerBuildsMyBuildsSection.QueuedBuildTooltipFormat"); //$NON-NLS-1$

                    return MessageFormat.format(
                        format, //
                        detail.getBuildNumber(),
                        queued.getBuildDefinition().getName(),
                        queued.getBuildServer().getDisplayText(queued.getStatus()),
                        queued.getRequestedBy(),
                        queued.getBuildServer().getDisplayText(queued.getReason()));
                } else if (queued.getStatus().contains(QueueStatus.QUEUED)) {
                    final String format =
                        Messages.getString("TeamExplorerBuildsMyBuildsSection.QueuedBuildItemTooltipFormat"); //$NON-NLS-1$

                    return MessageFormat.format(
                        format, //
                        queued.getBuildDefinition().getName(),
                        queued.getBuildDefinition().getName(),
                        queued.getQueuePosition(),
                        queued.getBuildServer().getDisplayText(queued.getPriority()),
                        queued.getRequestedBy(),
                        queued.getBuildServer().getDisplayText(queued.getReason()));
                } else {
                    final String format =
                        Messages.getString("TeamExplorerBuildsMyBuildsSection.PostponedBuildTooltipFormat"); //$NON-NLS-1$

                    return MessageFormat.format(
                        format, //
                        queued.getBuildDefinition().getName(),
                        queued.getBuildDefinition().getName(),
                        queued.getBuildServer().getDisplayText(queued.getStatus()),
                        TimeSpanHelpers.ago(queued.getQueueTime()),
                        queued.getRequestedBy(),
                        queued.getBuildServer().getDisplayText(queued.getReason()));
                }
            }

            return null;
        }
    }

    private class MyBuildsContentProvider extends ContentProviderAdapter {
        @Override
        public Object[] getElements(final Object inputElement) {
            return (Object[]) inputElement;
        }
    }

    private class MyBuildsLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            if (columnIndex > 0) {
                return null;
            }

            if (element instanceof IQueuedBuild) {
                final IQueuedBuild build = (IQueuedBuild) element;
                return imageHelper.getStatusImage(build.getStatus());
            } else if (element instanceof IBuildDetail) {
                final IBuildDetail detail = (IBuildDetail) element;
                return imageHelper.getStatusImage(detail.getStatus());
            } else {
                return null;
            }
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            if (columnIndex > 0) {
                return null;
            }

            if (element instanceof IQueuedBuild) {
                final IQueuedBuild queuedBuild = (IQueuedBuild) element;
                final IBuildDetail detail = queuedBuild.getBuild();

                if (detail != null && detail.getStartTime() != null && detail.getStartTime().getTimeInMillis() > 0) {
                    final String format = Messages.getString("TeamExplorerBuildsMyBuildsSection.QueuedBuildItemFormat"); //$NON-NLS-1$
                    final String ago = TimeSpanHelpers.ago(detail.getStartTime());
                    return MessageFormat.format(format, detail.getBuildNumber(), ago);
                } else if (queuedBuild.getStatus().contains(QueueStatus.QUEUED)) {
                    final String format = Messages.getString("TeamExplorerBuildsMyBuildsSection.QueuedItemFormat"); //$NON-NLS-1$
                    final String priority = queuedBuild.getBuildServer().getDisplayText(queuedBuild.getPriority());
                    return MessageFormat.format(
                        format,
                        queuedBuild.getBuildDefinition().getName(),
                        queuedBuild.getQueuePosition(),
                        priority);
                } else if (queuedBuild.getStatus().contains(QueueStatus.POSTPONED)) {
                    final String format = Messages.getString("TeamExplorerBuildsMyBuildsSection.BuildPostponedFormat"); //$NON-NLS-1$
                    final String ago = TimeSpanHelpers.ago(queuedBuild.getQueueTime());
                    return MessageFormat.format(format, queuedBuild.getBuildDefinition().getName(), ago);
                } else {
                    final String format = Messages.getString("TeamExplorerBuildsMyBuildsSection.BuildStartingFormat"); //$NON-NLS-1$
                    return MessageFormat.format(format, queuedBuild.getBuildDefinition().getName());
                }
            } else if (element instanceof IBuildDetail) {
                final IBuildDetail detail = (IBuildDetail) element;

                final String format = Messages.getString("TeamExplorerBuildsMyBuildsSection.MyBuildsItemFormat"); //$NON-NLS-1$
                final String ago = TimeSpanHelpers.ago(detail.getFinishTime());
                return MessageFormat.format(format, detail.getBuildNumber(), ago);
            }

            return ""; //$NON-NLS-1$
        }
    }

    private class MyBuildsDoubleClickListener implements IDoubleClickListener {
        @Override
        public void doubleClick(final DoubleClickEvent event) {
            final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            final Object element = selection.getFirstElement();

            if (element instanceof IBuildDetail) {
                final IBuildDetail buildDetail = (IBuildDetail) element;
                BuildHelpers.viewBuildReport(tableViewer.getControl().getShell(), buildDetail);
            } else if (element instanceof IQueuedBuild) {
                final IQueuedBuild queuedBuild = (IQueuedBuild) element;
                if (queuedBuild.getBuild() != null) {
                    final IBuildDetail buildDetail = queuedBuild.getBuild();
                    BuildHelpers.viewBuildReport(tableViewer.getControl().getShell(), buildDetail);
                }
            }
        }
    }

    private class MyBuildManagerListener implements BuildManagerListener {
        @Override
        public void onBuildDetailsChanged(final BuildManagerEvent event) {
            if (event.getSource() instanceof TeamExplorerView) {
                return;
            }

            for (final IBuildDetail changedBuildDetail : event.getBuildDetails()) {
                for (final Object buildItem : buildItems) {
                    if (buildItem instanceof IBuildDetail) {
                        final IBuildDetail buildDetail = (IBuildDetail) buildItem;
                        if (buildDetail.getBuildNumber().equals(changedBuildDetail.getBuildNumber())) {
                            refreshInternal();
                        }
                    }
                }
            }
        }

        @Override
        public void onBuildQueued(final BuildManagerEvent event) {
            refreshInternal();
        }

        @Override
        public void onBuildDeleted(final BuildManagerEvent event) {
            refreshInternal();
        }

        @Override
        public void onBuildsDeleted(final BuildManagerEvent event) {
            refreshInternal();
        }

        @Override
        public void onBuildPropertyChanged(final BuildPropertyChangedEvent event) {
            refreshInternal();
        }

        @Override
        public void onBuildStopped(final BuildManagerEvent event) {
            refreshInternal();
        }

        @Override
        public void onBuildPostponedOrResumed(final BuildManagerEvent event) {
            refreshInternal();
        }

        @Override
        public void onBuildPrioritiesChanged(final BuildManagerEvent event) {
            refreshInternal();
        }
    }

    private class MyBuildsRefreshWorker implements Runnable {
        private final Log log = LogFactory.getLog(MyBuildsRefreshWorker.class);

        @Override
        public void run() {
            log.info("Starting MY BUILDS refresh worker"); //$NON-NLS-1$

            while (!refreshThreadStop) {
                boolean doRefresh = false;
                long sleepTime = 0;

                synchronized (refreshLock) {
                    /*
                     * If another refresh is not in progress and we're past time
                     * for a refresh, then schedule one.
                     */
                    final long currentTime = System.currentTimeMillis();

                    if (refreshInProgress) {
                        /*
                         * If there's a refresh going on (manual refresh) defer
                         * until the refresh interval and check again
                         */
                        sleepTime = refreshInterval;
                    } else if (refreshLastTime + refreshInterval <= currentTime) {
                        /* Otherwise, it's time to do a refresh */
                        doRefresh = true;
                        refreshInProgress = true;
                        sleepTime = refreshInterval;
                    } else {
                        /*
                         * Otherwise, we got woken up before a refresh was due,
                         * simply sleep until we think it's time to do the
                         * refresh
                         */
                        sleepTime = (refreshLastTime + refreshInterval) - currentTime;
                    }
                }

                if (doRefresh) {
                    final Job refreshJob =
                        new Job(Messages.getString("TeamExplorerBuildsMyBuildsSection.RefreshMyBuilds")) //$NON-NLS-1$
                    {
                            @Override
                            protected IStatus run(final IProgressMonitor monitor) {
                                try {
                                    refreshInternal();
                                } catch (final Exception e) {
                                    log.debug("Exception during auto-refresh", e); //$NON-NLS-1$
                                }
                                return Status.OK_STATUS;
                            }
                        };

                    refreshJob.schedule();
                }

                try {
                    Thread.sleep(sleepTime);
                } catch (final InterruptedException e) {
                }
            }
        }
    }
}
