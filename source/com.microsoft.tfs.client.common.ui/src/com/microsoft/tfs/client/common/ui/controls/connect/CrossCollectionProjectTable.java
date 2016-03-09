// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.util.Comparator;

import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;

/**
 * A table of team projects for all collections/accounts.
 * <p>
 * <p>
 *
 * @threadsafety unknown
 */
public class CrossCollectionProjectTable extends TableControl {
    private static final String PROJECT_COLUMN = "project"; //$NON-NLS-1$
    private static final String COLLECTION_COLUMN = "collection"; //$NON-NLS-1$
    private static final String ACCOUNT_COLUMN = "account"; //$NON-NLS-1$

    private SourceControlCapabilityFlags sourceControlCapabilityFlagsFilter = SourceControlCapabilityFlags.NONE;

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private CrossCollectionProjectInfo[] unfilteredProjects;
    private final boolean showAccountColumn;

    public CrossCollectionProjectTable(final Composite parent, final int style, boolean showAccountColumn) {
        super(parent, (SWT.FULL_SELECTION | style), CrossCollectionProjectInfo.class, null);

        this.showAccountColumn = showAccountColumn;
        final TableColumnData[] columnData = new TableColumnData[2];
        columnData[0] = new TableColumnData(
            Messages.getString("CrossCollectionProjectTable.ProjectColumnHeader"), //$NON-NLS-1$
            -1,
            0.50F,
            PROJECT_COLUMN);
        if (showAccountColumn) {
            columnData[1] = new TableColumnData(
                Messages.getString("CrossCollectionProjectTable.AccountColumnHeader"), //$NON-NLS-1$
                -1,
                0.50F,
                ACCOUNT_COLUMN);
        } else {
            columnData[1] = new TableColumnData(
                Messages.getString("CrossCollectionProjectTable.CollectionColumnHeader"), //$NON-NLS-1$
                -1,
                0.50F,
                COLLECTION_COLUMN);
        }

        setupTable(true, false, columnData);
        setUseDefaultContentProvider();
        setUseDefaultLabelProvider();
        setEnableTooltips(false);

        final TableViewerSorter sorter = new TableViewerSorter(getViewer());
        sorter.setComparator(PROJECT_COLUMN, new Comparator<CrossCollectionProjectInfo>() {
            @Override
            public int compare(final CrossCollectionProjectInfo o1, final CrossCollectionProjectInfo o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        getViewer().setSorter(sorter);

        getViewer().setComparer(new IElementComparer() {
            @Override
            public boolean equals(final Object a, final Object b) {
                if (a instanceof CrossCollectionProjectInfo && b instanceof CrossCollectionProjectInfo) {
                    return ((CrossCollectionProjectInfo) a).getGUID().equals(
                        ((CrossCollectionProjectInfo) b).getGUID());
                }

                return a.equals(b);
            }

            @Override
            public int hashCode(final Object element) {
                if (element instanceof CrossCollectionProjectInfo) {
                    return ((CrossCollectionProjectInfo) element).getGUID().hashCode();
                }

                return element.hashCode();
            }
        });
    }

    public void applyFilter(final String filter) {
        if (filter != null && filter.trim().length() > 0) {
            final String filter2 = filter.trim().toLowerCase();
            getViewer().setFilters(new ViewerFilter[] {
                new ViewerFilter() {
                    @Override
                    public boolean select(Viewer viewer, Object parentElement, Object element) {
                        CrossCollectionProjectInfo info = (CrossCollectionProjectInfo) element;
                        if (info.getName().toLowerCase().contains(filter2)) {
                            return true;
                        } else if (showAccountColumn && info.getAccountName().toLowerCase().contains(filter2)) {
                            return true;
                        } else if (!showAccountColumn && info.getCollectionName().toLowerCase().contains(filter2)) {
                            return true;
                        }
                        return false;
                    }
                }
            });
        } else {
            getViewer().resetFilters();
        }
    }

    public SourceControlCapabilityFlags getSourceControlCapabilityFlags() {
        return sourceControlCapabilityFlagsFilter;
    }

    public void setSourceControlCapabilityFlags(final SourceControlCapabilityFlags sourceControlCapabilityFlagsFilter) {
        this.sourceControlCapabilityFlagsFilter = sourceControlCapabilityFlagsFilter;
    }

    public void setProjects(final CrossCollectionProjectInfo[] projects) {
        unfilteredProjects = projects;
        setElements(unfilteredProjects);
    }

    public CrossCollectionProjectInfo getSelectedProject() {
        return (CrossCollectionProjectInfo) this.getSelectedElement();
    }

    @Override
    protected String getColumnText(final Object element, final String columnPropertyName) {
        final CrossCollectionProjectInfo project = (CrossCollectionProjectInfo) element;
        if (ACCOUNT_COLUMN.equalsIgnoreCase(columnPropertyName)) {
            return project.getAccountName();
        } else if (COLLECTION_COLUMN.equalsIgnoreCase(columnPropertyName)) {
            return project.getCollectionName();
        } else if (PROJECT_COLUMN.equalsIgnoreCase(columnPropertyName)) {
            return project.getName();
        }

        return null;
    }

    @Override
    protected Image getColumnImage(final Object element, final String columnPropertyName) {
        final CrossCollectionProjectInfo project = (CrossCollectionProjectInfo) element;
        if (PROJECT_COLUMN.equalsIgnoreCase(columnPropertyName)) {
            if (project.isTfsProject()) {
                return imageHelper.getImage("images/common/tfs_project.png"); //$NON-NLS-1$
            } else if (project.isGitProject()) {
                return imageHelper.getImage("images/common/git_repo.png"); //$NON-NLS-1$
            }
        }

        return null;
    }

    @Override
    public void dispose() {
        imageHelper.dispose();
        super.dispose();
    }

    public static class CrossCollectionProjectInfo extends ProjectInfo {
        private String collectionName;
        private String accountName;
        private TFSTeamProjectCollection collection;

        public CrossCollectionProjectInfo(
            TFSTeamProjectCollection collection,
            String name,
            String uri,
            String collectionName,
            String accountName) {
            super(
                name != null ? name : "", //$NON-NLS-1$
                uri != null ? uri : ""); //$NON-NLS-1$
            this.collectionName = collectionName != null ? collectionName : ""; //$NON-NLS-1$
            this.accountName = accountName != null ? accountName : ""; //$NON-NLS-1$
            this.collection = collection;
        }

        public TFSTeamProjectCollection getCollection() {
            return collection;
        }

        public String getCollectionName() {
            return collectionName;
        }

        public String getAccountName() {
            return accountName;
        }

        public boolean isGitProject() {
            return getSourceControlCapabilityFlags().contains(SourceControlCapabilityFlags.GIT);
        }

        public boolean isTfsProject() {
            return getSourceControlCapabilityFlags().contains(SourceControlCapabilityFlags.TFS);
        }
    }
}
