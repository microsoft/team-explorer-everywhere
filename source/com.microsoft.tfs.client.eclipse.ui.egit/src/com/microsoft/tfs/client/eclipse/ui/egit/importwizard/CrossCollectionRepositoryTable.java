// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.egit.importwizard;

import java.util.Comparator;

import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.git.json.TfsGitRepositoryJson;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.connect.CrossCollectionProjectTable.CrossCollectionProjectInfo;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.eclipse.ui.egit.Messages;
import com.microsoft.tfs.core.TFSTeamProjectCollection;

/**
 * A table of repositories for all collections/accounts and team projects.
 * <p>
 * <p>
 *
 * @threadsafety unknown
 */
public class CrossCollectionRepositoryTable extends TableControl {
    private static final String PROJECT_COLUMN = "project"; //$NON-NLS-1$
    private static final String URI_COLUMN = "uri"; //$NON-NLS-1$
    private static final String REPO_COLUMN = "repo"; //$NON-NLS-1$

    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    public CrossCollectionRepositoryTable(final Composite parent, final int style) {
        super(parent, (SWT.FULL_SELECTION | style), CrossCollectionProjectInfo.class, null);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(
                Messages.getString("CrossCollectionRepositoryTable.RepositoryColumnHeader"), //$NON-NLS-1$
                -1,
                0.25F,
                REPO_COLUMN),
            new TableColumnData(
                Messages.getString("CrossCollectionRepositoryTable.ProjectColumnHeader"), //$NON-NLS-1$
                -1,
                0.25F,
                PROJECT_COLUMN),
            new TableColumnData(
                Messages.getString("CrossCollectionRepositoryTable.UriColumnHeader"), //$NON-NLS-1$
                -1,
                0.50F,
                URI_COLUMN)
        };

        setupTable(true, false, columnData);
        setUseDefaultContentProvider();
        setUseDefaultLabelProvider();
        setEnableTooltips(false);

        /*
         * Ensure the select all item sorts first.
         */
        final TableViewerSorter sorter = new TableViewerSorter(getViewer());
        sorter.setComparator(REPO_COLUMN, new Comparator<CrossCollectionRepositoryInfo>() {
            @Override
            public int compare(final CrossCollectionRepositoryInfo o1, final CrossCollectionRepositoryInfo o2) {
                return o1.getRepositoryName().compareToIgnoreCase(o2.getRepositoryName());
            }
        });

        getViewer().setSorter(sorter);

        getViewer().setComparer(new IElementComparer() {
            @Override
            public boolean equals(final Object a, final Object b) {
                if (a instanceof CrossCollectionRepositoryInfo && b instanceof CrossCollectionRepositoryInfo) {
                    return ((CrossCollectionRepositoryInfo) a).getRepositoryGUID().equals(
                        ((CrossCollectionRepositoryInfo) b).getRepositoryGUID());
                }

                return a.equals(b);
            }

            @Override
            public int hashCode(final Object element) {
                if (element instanceof CrossCollectionRepositoryInfo) {
                    return ((CrossCollectionRepositoryInfo) element).getRepositoryGUID().hashCode();
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
                        final CrossCollectionRepositoryInfo repo = (CrossCollectionRepositoryInfo) element;
                        return (repo.getRepositoryName().toLowerCase().contains(filter2)
                            || repo.getURI().toLowerCase().contains(filter2)
                            || repo.getName().toLowerCase().contains(filter2));
                    }
                }
            });
        } else {
            getViewer().resetFilters();
        }
    }

    public void setRepositories(final CrossCollectionRepositoryInfo[] repositories) {
        setElements(repositories);
    }

    public CrossCollectionRepositoryInfo getSelectedRepository() {
        return (CrossCollectionRepositoryInfo) this.getSelectedElement();
    }

    @Override
    protected String getColumnText(final Object element, final String columnPropertyName) {
        final CrossCollectionRepositoryInfo repo = (CrossCollectionRepositoryInfo) element;
        if (REPO_COLUMN.equalsIgnoreCase(columnPropertyName)) {
            return repo.getRepositoryName();
        } else if (URI_COLUMN.equalsIgnoreCase(columnPropertyName)) {
            return repo.getRepositoryURI();
        } else if (PROJECT_COLUMN.equalsIgnoreCase(columnPropertyName)) {
            return repo.getName();
        }

        return null;
    }

    @Override
    protected Image getColumnImage(final Object element, final String columnPropertyName) {
        final CrossCollectionRepositoryInfo repo = (CrossCollectionRepositoryInfo) element;
        if (PROJECT_COLUMN.equalsIgnoreCase(columnPropertyName)) {
            if (repo.isTfsProject()) {
                return imageHelper.getImage("images/common/tfs_project.png"); //$NON-NLS-1$
            } else if (repo.isGitProject()) {
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

    public static class CrossCollectionRepositoryInfo extends CrossCollectionProjectInfo {
        private final static String TEAM_PROJECT_URI_PREFIX = "vstfs:///Classification/TeamProject/"; //$NON-NLS-1$
        private final TfsGitRepositoryJson gitRepository;

        public CrossCollectionRepositoryInfo(TFSTeamProjectCollection collection, TfsGitRepositoryJson gitRepository) {
            super(
                collection,
                gitRepository.getTeamProject().getName(),
                TEAM_PROJECT_URI_PREFIX + gitRepository.getTeamProject().getId(),
                collection.getName(),
                collection.getBaseURI().getHost());
            this.gitRepository = gitRepository;
        }

        public TfsGitRepositoryJson getRepository() {
            return gitRepository;
        }

        public String getRepositoryName() {
            return gitRepository.getName();
        }

        public String getRepositoryGUID() {
            return gitRepository.getId();
        }

        public String getRepositoryURI() {
            return gitRepository.getRemoteUrl();
        }

    }
}
