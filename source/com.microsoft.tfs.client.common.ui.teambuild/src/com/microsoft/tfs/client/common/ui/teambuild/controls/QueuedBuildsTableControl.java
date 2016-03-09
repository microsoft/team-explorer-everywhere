// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls;

import java.util.Comparator;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter.SortDirection;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildConstants;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildImageHelper;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.IQueuedBuild;
import com.microsoft.tfs.core.clients.build.flags.QueueStatus;
import com.microsoft.tfs.util.Check;

public class QueuedBuildsTableControl extends TableControl {
    private final Image reasonHeaderImage;
    private final Image statusHeaderImage;
    private IBuildServer buildServer;

    private static final int REASON_COLUMN = 0;
    private static final int STATUS_COLUMN = 1;
    private static final int BUILD_DEFINITION_COLUMN = 2;
    private static final int PRIORITY_COLUMN = 3;
    private static final int DATE_QUEUED_COLUMN = 4;
    private static final int REQUESTED_BY_COLUMN = 5;
    private static final int BUILD_AGENT_COLUMN = 6;

    public QueuedBuildsTableControl(final Composite parent, final int style, final IBuildServer buildServer) {
        this(parent, style, buildServer, null);
    }

    /**
     * Constructs a {@link QueuedBuildsTableControl}. The {@link IBuildServer}
     * may be <code>null</code> (column text for a V3 build server will be
     * shown); call {@link #setBuildServer(IBuildServer)} to update the control.
     *
     * @param buildServer
     *        the build server to use (may be <code>null</code>)
     */
    public QueuedBuildsTableControl(
        final Composite parent,
        final int style,
        final IBuildServer buildServer,
        final String viewDataKey) {
        super(parent, style, IQueuedBuild.class, viewDataKey);
        this.buildServer = buildServer;

        reasonHeaderImage = AbstractUIPlugin.imageDescriptorFromPlugin(
            TFSTeamBuildPlugin.PLUGIN_ID,
            "/icons/ColumnHeaderReason.gif").createImage(); //$NON-NLS-1$

        statusHeaderImage = AbstractUIPlugin.imageDescriptorFromPlugin(
            TFSTeamBuildPlugin.PLUGIN_ID,
            "/icons/ColumnHeaderStatus.gif").createImage(); //$NON-NLS-1$

        setupTableColumns();

        setUseDefaultContentProvider();
        getViewer().setSorter(createSorter());
        getViewer().setLabelProvider(new LabelProvider());
        setEnableTooltips(true, true);

    }

    /**
     * Called after the list has been refreshed. and selections have been set.
     * The Eclipse editor actions (toolbar and main menu) are listening for
     * selection changes. The button states may need to change due to the
     * refreshed query, so use the selection changed notification to trigger the
     * editor action state updates.
     */
    public void afterQueryRefresh() {
        notifySelectionChangedListeners();
    }

    private void setupTableColumns() {
        // Default to agent
        String controllerOrAgentColumnText = Messages.getString("QueuedBuildsTableControl.BuilAgentColumnText"); //$NON-NLS-1$

        // Use controller if new server
        if (buildServer != null && buildServer.getBuildServerVersion().isV3OrGreater()) {
            controllerOrAgentColumnText = Messages.getString("QueuedBuildsTableControl.BuildControllerColumnText"); //$NON-NLS-1$
        }

        final TableColumnData[] columnData = new TableColumnData[] {
            (new TableColumnData(reasonHeaderImage, 22, "reason")).setResizeable(false), //$NON-NLS-1$
            (new TableColumnData(statusHeaderImage, 22, "status")).setResizeable(false), //$NON-NLS-1$
            new TableColumnData(
                Messages.getString("QueuedBuildsTableControl.BuildDefinitionColumnText"), //$NON-NLS-1$
                300,
                "buildDefinition"), //$NON-NLS-1$
            new TableColumnData(Messages.getString("QueuedBuildsTableControl.PriorityColumnText"), 100, "priority"), //$NON-NLS-1$ //$NON-NLS-2$
            new TableColumnData(Messages.getString("QueuedBuildsTableControl.DateQueuedColumnText"), 100, "dateQueued"), //$NON-NLS-1$ //$NON-NLS-2$
            new TableColumnData(
                Messages.getString("QueuedBuildsTableControl.RequestedByColumnText"), //$NON-NLS-1$
                120,
                "requestedBy"), //$NON-NLS-1$
            new TableColumnData(controllerOrAgentColumnText, 100, "buildAgent") //$NON-NLS-1$
        };

        setupTable(true, false, columnData);

        // Set tooltips for the columns that display only an icon.
        final String[] columnHeaderTooltips = new String[] {
            Messages.getString("QueuedBuildsTableControl.ColumnHeaderReasonTooltipText"), //$NON-NLS-1$
            Messages.getString("QueuedBuildsTableControl.ColumnHeaderStatusTooltipText") //$NON-NLS-1$
        };
        setTableColumnHeaderTooltips(columnHeaderTooltips);
    }

    private ViewerSorter createSorter() {
        final TableViewerSorter sorter = new TableViewerSorter(getViewer());
        sorter.setComparator(STATUS_COLUMN, new Comparator<IQueuedBuild>() {
            @Override
            public int compare(final IQueuedBuild queuedBuild1, final IQueuedBuild queuedBuild2) {
                return queuedBuild1.getStatus().compareTo(queuedBuild2.getStatus());
            }
        });
        sorter.setComparator(DATE_QUEUED_COLUMN, new Comparator<IQueuedBuild>() {
            @Override
            public int compare(final IQueuedBuild queuedBuild1, final IQueuedBuild queuedBuild2) {
                return queuedBuild1.getQueueTime().getTime().compareTo(queuedBuild2.getQueueTime().getTime());
            }
        });

        sorter.sort(DATE_QUEUED_COLUMN, SortDirection.DESCENDING);

        return sorter;
    }

    public void setQueuedBuilds(final IQueuedBuild[] queuedBuilds) {
        setElements(queuedBuilds);
    }

    public IQueuedBuild[] getQueuedBuilds() {
        return (IQueuedBuild[]) getElements();
    }

    public void setSelectedQueuedBuilds(final IQueuedBuild[] queuedBuilds) {
        setSelectedElements(queuedBuilds);
    }

    public void setSelectedQueuedBuild(final IQueuedBuild queuedBuild) {
        setSelectedElement(queuedBuild);
    }

    public IQueuedBuild[] getSelectedQueuedBuilds() {
        return (IQueuedBuild[]) getSelectedElements();
    }

    public IQueuedBuild getSelectedQueuedBuild() {
        return (IQueuedBuild) getSelectedElement();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.shared.table.TableControl#onDisposed()
     */
    @Override
    protected void onDisposed() {
        statusHeaderImage.dispose();
        reasonHeaderImage.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTooltipText(final Object element, final int columnIndex) {
        Check.isTrue(columnIndex != -1, "columnIndex != -1"); //$NON-NLS-1$

        if (buildServer != null && element instanceof IQueuedBuild) {
            final IQueuedBuild queuedBuild = (IQueuedBuild) element;

            if (columnIndex == REASON_COLUMN) {
                return buildServer.getDisplayText(queuedBuild.getReason());
            } else if (columnIndex == STATUS_COLUMN) {
                if (queuedBuild.getStatus().contains(QueueStatus.COMPLETED)) {
                    return buildServer.getDisplayText(queuedBuild.getBuild().getStatus());
                }
                return buildServer.getDisplayText(queuedBuild.getStatus());
            }
        }
        return null;
    }

    private class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements ITableLabelProvider {
        private final TeamBuildImageHelper imageHelper = new TeamBuildImageHelper();

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            if (element instanceof IQueuedBuild) {
                final IQueuedBuild queuedBuild = (IQueuedBuild) element;

                if (columnIndex == REASON_COLUMN) {
                    return imageHelper.getBuildReasonImage(queuedBuild.getReason());
                }

                if (columnIndex == STATUS_COLUMN) {
                    // Build Status Column
                    if (queuedBuild.getStatus().contains(QueueStatus.COMPLETED) && queuedBuild.getBuild() != null) {
                        return imageHelper.getStatusImage(queuedBuild.getBuild().getStatus());
                    }
                    return imageHelper.getStatusImage(queuedBuild.getStatus());
                }
            }

            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            if (!(element instanceof IQueuedBuild)) {
                return ""; //$NON-NLS-1$
            }
            final IQueuedBuild queuedBuild = (IQueuedBuild) element;

            switch (columnIndex) {
                case REASON_COLUMN:
                case STATUS_COLUMN:
                    return ""; //$NON-NLS-1$

                /*
                 * Build definition column will use the queued build's build
                 * definition when there's no build definition available.
                 */
                case BUILD_DEFINITION_COLUMN:
                    if (queuedBuild.getBuild() != null && buildServer != null) {
                        return buildServer.getBuildDefinition(queuedBuild.getBuild().getBuildDefinitionURI()).getName();
                    } else if (queuedBuild.getBuildDefinition() != null) {
                        return queuedBuild.getBuildDefinition().getName();
                    }

                    return ""; //$NON-NLS-1$

                case PRIORITY_COLUMN:
                    return queuedBuild.getPriority().getDisplayText();

                case DATE_QUEUED_COLUMN:
                    return TeamBuildConstants.DATE_FORMAT.format(queuedBuild.getQueueTime().getTime());

                case REQUESTED_BY_COLUMN:
                    String userName = queuedBuild.getRequestedForDisplayName();
                    if (userName == null || userName.length() == 0) {
                        userName = queuedBuild.getRequestedByDisplayName();
                    }
                    if (userName == null || userName.length() == 0) {
                        userName = queuedBuild.getRequestedFor();
                    }
                    if (userName == null || userName.length() == 0) {
                        userName = queuedBuild.getRequestedBy();
                    }
                    return userName;

                case BUILD_AGENT_COLUMN:
                    if (queuedBuild.getBuild() != null && buildServer != null) {
                        return buildServer.getBuildController(
                            queuedBuild.getBuild().getBuildControllerURI(),
                            false).getName();
                    } else if (queuedBuild.getBuildController() != null) {
                        return queuedBuild.getBuildController().getName();
                    }

                    return ""; //$NON-NLS-1$

                default:
                    break;
            }
            return ""; //$NON-NLS-1$
        }

        /**
         * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
         */
        @Override
        public void dispose() {
            super.dispose();
            imageHelper.dispose();
        }

    }

    /**
     * @return the buildServer (may be <code>null</code>)
     */
    public IBuildServer getBuildServer() {
        return buildServer;
    }

    /**
     * @param buildServer
     *        the buildServer to set (may be <code>null</code>)
     */
    public void setBuildServer(final IBuildServer buildServer) {
        this.buildServer = buildServer;

        /*
         * Some table column names may change depending on the version.
         */
        setupTableColumns();
    }

}
