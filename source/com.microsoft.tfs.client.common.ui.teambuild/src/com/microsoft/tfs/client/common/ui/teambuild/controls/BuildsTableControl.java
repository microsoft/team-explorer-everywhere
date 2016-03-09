// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.microsoft.tfs.client.common.ui.framework.command.UICommandExecutorFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter.SortDirection;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildConstants;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildImageHelper;
import com.microsoft.tfs.client.common.ui.teambuild.commands.SaveBuildDetailCommand;
import com.microsoft.tfs.core.clients.build.IBuildDetail;
import com.microsoft.tfs.core.clients.build.IBuildServer;
import com.microsoft.tfs.core.clients.build.internal.TeamBuildCache;
import com.microsoft.tfs.util.Check;

public class BuildsTableControl extends TableControl {
    public static final String BUILDS_TABLE_ID =
        "com.microsoft.tfs.test.ui.build.editors.BuildsTableControlWrapper.table"; //$NON-NLS-1$

    private static final int KEEP_FOREVER_COLUMN = 0;
    private static final String KEEP_FOREVER_COLUMN_PROPERTY = "keepForever"; //$NON-NLS-1$
    private static final int REASON_COLUMN = 1;
    private static final String REASON_PROPERTY = "reason"; //$NON-NLS-1$
    private static final int STATUS_COLUMN = 2;
    private static final String STATUS_COLUMN_PROPERTY = "status"; //$NON-NLS-1$
    private static final int NAME_COLUMN = 3;
    private static final String NAME_COLUMN_PROPERTY = "name"; //$NON-NLS-1$
    private static final int BUILD_DEFINITION_COLUMN = 4;
    private static final String BUILD_DEFINITION_COLUMN_PROPERTY = "buildDefinition"; //$NON-NLS-1$
    private static final int BUILD_QUALITY_COLUMN = 5;
    private static final String BUILD_QUALITY_COLUMN_PROPERTY = "quality"; //$NON-NLS-1$
    private static final int DATE_COMPLETED_COLUMN = 6;
    private static final String DATE_COMPLETED_COLUMN_PROPERTY = "dateCompleted"; //$NON-NLS-1$
    private static final int REQUESTED_BY_COLUMN = 7;
    private static final String REQUESTED_BY_COLUMN_PROPERTY = "requestedBy"; //$NON-NLS-1$

    private static final String[] COLUMN_PROPERTIES = new String[] {
        KEEP_FOREVER_COLUMN_PROPERTY,
        REASON_PROPERTY,
        STATUS_COLUMN_PROPERTY,
        NAME_COLUMN_PROPERTY,
        BUILD_DEFINITION_COLUMN_PROPERTY,
        BUILD_QUALITY_COLUMN_PROPERTY,
        DATE_COMPLETED_COLUMN_PROPERTY,
        REQUESTED_BY_COLUMN_PROPERTY
    };

    private final Image statusHeaderImage;
    private final Image keepHeaderImage;
    private final Image reasonHeaderImage;
    private IBuildServer buildServer;
    public String[] buildQualities;
    public ComboBoxCellEditor qualityCellEditor;
    private String teamProject;

    public BuildsTableControl(
        final Composite parent,
        final int style,
        final IBuildServer buildServer,
        final String teamProject) {
        super(parent, style, IBuildDetail.class, null);
        this.buildServer = buildServer;
        this.teamProject = teamProject;
        statusHeaderImage = AbstractUIPlugin.imageDescriptorFromPlugin(
            TFSTeamBuildPlugin.PLUGIN_ID,
            "/icons/ColumnHeaderStatus.gif").createImage(); //$NON-NLS-1$
        keepHeaderImage = AbstractUIPlugin.imageDescriptorFromPlugin(
            TFSTeamBuildPlugin.PLUGIN_ID,
            "/icons/ColumnHeaderKeepForever.gif").createImage(); //$NON-NLS-1$
        reasonHeaderImage = AbstractUIPlugin.imageDescriptorFromPlugin(
            TFSTeamBuildPlugin.PLUGIN_ID,
            "/icons/ColumnHeaderReason.gif").createImage(); //$NON-NLS-1$

        final TableColumnData[] columnData = new TableColumnData[] {
            (new TableColumnData(keepHeaderImage, 22, "keepForever")).setResizeable(false), //$NON-NLS-1$
            (new TableColumnData(reasonHeaderImage, 22, "reason")).setResizeable(false), //$NON-NLS-1$
            (new TableColumnData(statusHeaderImage, 22, "status")).setResizeable(false), //$NON-NLS-1$
            new TableColumnData(Messages.getString("BuildsTableControl.NameColumnText"), 200, "name"), //$NON-NLS-1$ //$NON-NLS-2$
            new TableColumnData(
                Messages.getString("BuildsTableControl.BuildDefinitionColumnText"), //$NON-NLS-1$
                150,
                "buildDefinition"), //$NON-NLS-1$
            new TableColumnData(Messages.getString("BuildsTableControl.BuildQualityColumnText"), 100, "buildQuality"), //$NON-NLS-1$ //$NON-NLS-2$
            new TableColumnData(Messages.getString("BuildsTableControl.DateCompletedColumnText"), 100, "dateCompleted"), //$NON-NLS-1$ //$NON-NLS-2$
            new TableColumnData(Messages.getString("BuildsTableControl.RequestedByColumnText"), 120, "requestedBy"), //$NON-NLS-1$ //$NON-NLS-2$
        };
        setupTable(true, false, columnData);

        // Set tooltips for the columns that display only an icon.
        final String[] columnHeaderTooltips = new String[] {
            Messages.getString("BuildsTableControl.ColumnHeaderRetainTooltipText"), //$NON-NLS-1$
            Messages.getString("BuildsTableControl.ColumnHeaderReasonTooltipText"), //$NON-NLS-1$
            Messages.getString("BuildsTableControl.ColumnHeaderStatusTooltipText"), //$NON-NLS-1$
        };
        setTableColumnHeaderTooltips(columnHeaderTooltips);

        setUseDefaultContentProvider();
        getViewer().setSorter(createSorter());
        getViewer().setLabelProvider(new LabelProvider());

        loadBuildQualities();

        getViewer().setColumnProperties(COLUMN_PROPERTIES);
        qualityCellEditor = new ComboBoxCellEditor(getTable(), buildQualities, SWT.READ_ONLY);
        getViewer().setCellEditors(new CellEditor[] {
            null,
            null,
            null,
            null,
            null,
            qualityCellEditor,
            null,
            null
        });
        getViewer().setCellModifier(new BuildTableCellModifier());

        setEnableTooltips(true, true);

        AutomationIDHelper.setWidgetID(this, BUILDS_TABLE_ID);
    }

    public void updateContext(final IBuildServer buildServer, final String teamProject) {
        this.buildServer = buildServer;
        this.teamProject = teamProject;
        loadBuildQualities();
    }

    public void loadBuildQualities() {
        // Setup build qualities editing
        if (buildServer.getBuildServerVersion().isV1()) {
            // TFS2005 Server does not allow empty string for build quality.
            buildQualities = TeamBuildCache.getInstance(buildServer, teamProject).getBuildQualities(false);
        } else {
            // TFS2008 Allows empty string for build quality.
            final String[] temp = TeamBuildCache.getInstance(buildServer, teamProject).getBuildQualities(false);
            buildQualities = new String[temp.length + 1];
            buildQualities[0] = ""; //$NON-NLS-1$
            System.arraycopy(temp, 0, buildQualities, 1, temp.length);
        }

        if (qualityCellEditor != null) {
            qualityCellEditor.setItems(buildQualities);
        }
    }

    private class BuildTableCellModifier implements ICellModifier {
        @Override
        public boolean canModify(final Object element, final String property) {
            return BUILD_QUALITY_COLUMN_PROPERTY.equals(property);
        }

        @Override
        public Object getValue(final Object element, final String property) {
            final IBuildDetail buildDetail = (IBuildDetail) element;

            if (BUILD_QUALITY_COLUMN_PROPERTY.equals(property)) {
                final String buildQuality = buildDetail.getQuality();

                for (int i = 0; i < buildQualities.length; i++) {
                    if (buildQualities[i].equals(buildQuality)) {
                        return new Integer(i);
                    }
                }

                return new Integer(0);
            }
            return null;
        }

        @Override
        public void modify(final Object element, final String property, final Object value) {
            IBuildDetail buildDetail;

            if (element instanceof Item) {
                buildDetail = ((IBuildDetail) ((Item) element).getData());
            } else {
                buildDetail = (IBuildDetail) element;
            }

            if (BUILD_QUALITY_COLUMN_PROPERTY.equals(property)) {
                // We have changed the build quality - update the server then
                // refresh the model
                final int qualityIndex = ((Integer) value).intValue();
                final String buildQuality = buildQualities[qualityIndex];

                final String oldBuildQuality = buildDetail.getQuality();

                if (buildQuality.equals(oldBuildQuality) || (qualityIndex == 0 && oldBuildQuality == null)) {
                    // Avoid server roundtrip when unchanged.
                    return;
                }

                buildDetail.setQuality(buildQuality);

                final SaveBuildDetailCommand command = new SaveBuildDetailCommand(buildDetail);
                final IStatus status =
                    UICommandExecutorFactory.newBusyIndicatorCommandExecutor(getShell()).execute(command);

                if (status.getSeverity() != IStatus.OK) {
                    buildDetail.setQuality(oldBuildQuality);
                    return;
                }

                // Update model
                getViewer().update(buildDetail, null);
            }

        }

    }

    private ViewerSorter createSorter() {
        final TableViewerSorter sorter = new TableViewerSorter(getViewer());

        sorter.setComparator(KEEP_FOREVER_COLUMN, new Comparator<IBuildDetail>() {
            @Override
            public int compare(final IBuildDetail buildDetail1, final IBuildDetail buildDetail2) {
                if (buildDetail1.isKeepForever() == buildDetail2.isKeepForever()) {
                    return 0;
                }
                return buildDetail1.isKeepForever() ? 1 : -1;
            }
        });

        sorter.setComparator(STATUS_COLUMN, new Comparator<IBuildDetail>() {
            @Override
            public int compare(final IBuildDetail buildDetail1, final IBuildDetail buildDetail2) {
                return buildDetail1.getStatus().compareTo(buildDetail2.getStatus());
            }
        });

        sorter.setComparator(DATE_COMPLETED_COLUMN, new Comparator<IBuildDetail>() {
            @Override
            public int compare(final IBuildDetail buildDetail1, final IBuildDetail buildDetail2) {
                return buildDetail1.getFinishTime().getTime().compareTo(buildDetail2.getFinishTime().getTime());
            }
        });

        sorter.sort(DATE_COMPLETED_COLUMN, SortDirection.DESCENDING);

        return sorter;
    }

    public void setBuilds(final IBuildDetail[] builds) {
        setElements(builds);
    }

    public IBuildDetail[] getBuilds() {
        return (IBuildDetail[]) getElements();
    }

    public void setSelectedBuilds(final IBuildDetail[] builds) {
        setSelectedElements(builds);
    }

    public void setSelectedBuild(final IBuildDetail build) {
        setSelectedElement(build);
    }

    public IBuildDetail[] getSelectedBuilds() {
        return (IBuildDetail[]) getSelectedElements();
    }

    public IBuildDetail getSelectedBuild() {
        return (IBuildDetail) getSelectedElement();
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
        keepHeaderImage.dispose();
        reasonHeaderImage.dispose();
    }

    /**
     * @see com.microsoft.tfs.client.common.ui.framework.table.TableControl#getTooltipText(java.lang.Object,
     *      int)
     */
    @Override
    public String getTooltipText(final Object element, final int columnIndex) {
        Check.isTrue(columnIndex != -1, "columnIndex != -1"); //$NON-NLS-1$

        if (buildServer != null && element instanceof IBuildDetail) {
            final IBuildDetail build = (IBuildDetail) element;

            if (columnIndex == KEEP_FOREVER_COLUMN) {
                if (build.isKeepForever()) {
                    return Messages.getString("BuildsTableControl.BuildDetailProtected"); //$NON-NLS-1$
                } else {
                    return Messages.getString("BuildsTableControl.BuildDetailNotProtected"); //$NON-NLS-1$
                }
            } else if (columnIndex == REASON_COLUMN) {
                return buildServer.getDisplayText(build.getReason());
            } else if (columnIndex == STATUS_COLUMN) {
                return buildServer.getDisplayText(build.getStatus());
            }
        }

        return null;
    }

    private static class LabelProvider extends org.eclipse.jface.viewers.LabelProvider implements ITableLabelProvider {
        private final TeamBuildImageHelper imageHelper = new TeamBuildImageHelper();

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            if (element instanceof IBuildDetail) {
                final IBuildDetail buildDetail = (IBuildDetail) element;

                if (columnIndex == REASON_COLUMN) {
                    return imageHelper.getBuildReasonImage(buildDetail.getReason());
                }

                if (columnIndex == KEEP_FOREVER_COLUMN) {
                    return imageHelper.getKeepForeverImage(buildDetail.isKeepForever());
                }

                if (columnIndex == STATUS_COLUMN) {
                    return imageHelper.getStatusImage(buildDetail.getStatus());
                }
            }

            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            if (!(element instanceof IBuildDetail)) {
                return ""; //$NON-NLS-1$
            }
            final IBuildDetail buildDetail = (IBuildDetail) element;
            switch (columnIndex) {
                case KEEP_FOREVER_COLUMN:
                case REASON_COLUMN:
                case STATUS_COLUMN:
                    return ""; //$NON-NLS-1$
                case NAME_COLUMN:
                    return buildDetail.getBuildNumber();
                case BUILD_DEFINITION_COLUMN:
                    if (buildDetail.getBuildDefinition() == null) {
                        return ""; //$NON-NLS-1$
                    }
                    return buildDetail.getBuildDefinition().getName();
                case BUILD_QUALITY_COLUMN:
                    return buildDetail.getQuality() == null ? "" : buildDetail.getQuality(); //$NON-NLS-1$
                case DATE_COMPLETED_COLUMN:
                    if (buildDetail.isBuildFinished()) {
                        return TeamBuildConstants.DATE_FORMAT.format(buildDetail.getFinishTime().getTime());
                    }
                    return ""; //$NON-NLS-1$
                case REQUESTED_BY_COLUMN:
                    String userName = buildDetail.getRequestedFor();
                    if (userName == null || userName.length() == 0) {
                        userName = buildDetail.getRequestedBy();
                    }
                    return userName;
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
     * Trigger the cell editor for the build quality column.
     *
     */
    public void editQuality(final IBuildDetail detail) {
        getViewer().editElement(detail, BUILD_QUALITY_COLUMN);
    }

    public void removeBuilds(final IBuildDetail[] buildsToRemove) {
        final Set<IBuildDetail> builds = new HashSet<IBuildDetail>(Arrays.asList(getBuilds()));
        builds.removeAll(Arrays.asList(buildsToRemove));
        setElements(builds.toArray());
    }

    /**
     * @return the buildServer
     */
    public IBuildServer getBuildServer() {
        return buildServer;
    }

    /**
     * @param buildServer
     *        the buildServer to set
     */
    public void setBuildServer(final IBuildServer buildServer) {
        this.buildServer = buildServer;
    }

    /**
     * Called after one or more build protection properties (
     * "Retain Indefinately") has been toggled. Fire a fake selection change
     * notification so the Eclipse toolbar button and main menu button states
     * will update.
     */
    public void afterToggleProtection() {
        notifySelectionChangedListeners();
    }

}
