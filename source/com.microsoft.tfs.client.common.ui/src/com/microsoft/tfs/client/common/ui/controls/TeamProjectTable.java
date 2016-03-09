// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter;
import com.microsoft.tfs.client.common.ui.framework.helper.TableViewerSorter.CategoryProvider;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.util.Check;

/**
 * A table of team projects.
 * <p>
 * If constructed with the parameter to show the "Select All" item, a special
 * tri-state checkbox element is shown in the table at all times to handle
 * multi-checking. That element will not be returned by {@link #getProjects()}
 * or {@link #getCheckedProjects()}, and it is not included in the counts
 * returned by {@link #getCheckedProjectsCount()} and {@link #getProjectCount()}
 * . However, the "Select All" special item <em>is</em> included in the items
 * returned by {@link #getElements()}, {@link #getCheckedElements()},
 * {@link #getCount()}, and all the other "Element"-wise methods.
 * <p>
 *
 * @threadsafety unknown
 */
public class TeamProjectTable extends TableControl {
    private static final ProjectInfo SELECT_ALL_SPECIAL_PROJECT_INFO =
        new ProjectInfo(
            Messages.getString("TeamProjectTable.SelectAll"), //$NON-NLS-1$
            "TeamProjectTable://SpecialItems/SelectAll"); //$NON-NLS-1$

    private static final String PROJECT_COLUMN = "project"; //$NON-NLS-1$
    // private static final String VERSION_CONTROL_COLUMN = "versionControl";
    // //$NON-NLS-1$

    private SourceControlCapabilityFlags sourceControlCapabilityFlagsFilter = SourceControlCapabilityFlags.NONE;

    public TeamProjectTable(final Composite parent, final int style, final boolean showIcons) {
        super(parent, (SWT.CHECK | style), ProjectInfo.class, null);

        final TableColumnData[] columnData = new TableColumnData[] {
            new TableColumnData(Messages.getString("TeamProjectTable.ColumnNameProject"), 100, 1.00F, PROJECT_COLUMN), //$NON-NLS-1$
        };

        setupTable(false, false, columnData);

        final TableViewerColumn projectColumnViewer = new TableViewerColumn(getViewer(), SWT.None, 0);
        projectColumnViewer.setLabelProvider(new ProjectColumnLabelProvider(showIcons));

        setUseDefaultContentProvider();
        setEnableTooltips(false);

        /*
         * Ensure the select all item sorts first.
         */
        final TableViewerSorter sorter = new TableViewerSorter(getViewer());
        sorter.setCategoryProvider(new CategoryProvider() {
            @Override
            public int getCategory(final Object element) {
                if (element == SELECT_ALL_SPECIAL_PROJECT_INFO) {
                    return 0;
                }

                return 1;
            }
        });

        sorter.setComparator(PROJECT_COLUMN, new Comparator<ProjectInfo>() {

            @Override
            public int compare(final ProjectInfo o1, final ProjectInfo o2) {
                if (o1 == SELECT_ALL_SPECIAL_PROJECT_INFO) {
                    return -1;
                }

                if (o2 == SELECT_ALL_SPECIAL_PROJECT_INFO) {
                    return 1;
                }

                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        getViewer().setSorter(sorter);

        getViewer().setComparer(new IElementComparer() {
            @Override
            public boolean equals(final Object a, final Object b) {
                if (a instanceof ProjectInfo && b instanceof ProjectInfo) {
                    return ((ProjectInfo) a).getGUID().equals(((ProjectInfo) b).getGUID());
                }

                return a.equals(b);
            }

            @Override
            public int hashCode(final Object element) {
                if (element instanceof ProjectInfo) {
                    return ((ProjectInfo) element).getGUID().hashCode();
                }

                return element.toString().hashCode();
            }
        });

        ((CheckboxTableViewer) getViewer()).addCheckStateListener(new ICheckStateListener() {
            @Override
            public void checkStateChanged(final CheckStateChangedEvent event) {
                final CheckboxTableViewer viewer = (CheckboxTableViewer) getViewer();

                /*
                 * "Select all" item was checked: apply check state to all the
                 * other elements.
                 */
                if (event.getElement().equals(SELECT_ALL_SPECIAL_PROJECT_INFO)) {
                    if (event.getChecked()) {
                        setCheckedElements(removeNotValidVC(getProjects()));
                    } else {
                        setCheckedElements(new ProjectInfo[0]);
                    }
                } else {
                    if (!hasValidVC(event.getElement())) {
                        viewer.setChecked(event.getElement(), false);
                        setCheckedElements(removeNotValidVC(getCheckedElements()));
                    }
                }

                updateSelectAllCheckState();
            }
        });
    }

    public SourceControlCapabilityFlags getSourceControlCapabilityFlags() {
        return sourceControlCapabilityFlagsFilter;
    }

    public void setSourceControlCapabilityFlags(final SourceControlCapabilityFlags sourceControlCapabilityFlagsFilter) {
        this.sourceControlCapabilityFlagsFilter = sourceControlCapabilityFlagsFilter;
    }

    public void setProjects(final ProjectInfo[] projects) {
        setElements(addSelectAllItem(removeNotValidVC(projects)));
        updateSelectAllCheckState();
    }

    public ProjectInfo[] getProjects() {
        return removeSelectAllItem((ProjectInfo[]) getElements());
    }

    public int getProjectCount() {
        /*
         * Use the filtered projects list (doesn't contain "select all").
         */
        return getProjects().length;
    }

    public void setCheckedProjects(final ProjectInfo[] projects) {
        final ProjectInfo[] filteredProjects = removeSelectAllItem(projects);

        setCheckedElements(filteredProjects);
        updateSelectAllCheckState();
    }

    public ProjectInfo[] getCheckedProjects() {
        return removeSelectAllItem((ProjectInfo[]) getCheckedElements());
    }

    private ProjectInfo[] removeNotValidVC(final ProjectInfo[] projects) {
        final List<ProjectInfo> filteredProjects = new ArrayList<ProjectInfo>();

        if (projects != null) {
            for (final ProjectInfo project : projects) {
                if (hasValidVC(project)) {
                    filteredProjects.add(project);
                }
            }
        }

        return filteredProjects.toArray(new ProjectInfo[filteredProjects.size()]);
    }

    private ProjectInfo[] removeNotValidVC(final Object[] projects) {
        final List<ProjectInfo> filteredProjects = new ArrayList<ProjectInfo>();

        if (projects != null) {
            for (final Object project : projects) {
                if (hasValidVC(project)) {
                    filteredProjects.add((ProjectInfo) project);
                }
            }
        }

        return filteredProjects.toArray(new ProjectInfo[filteredProjects.size()]);
    }

    private int getValidCheckedProjectsCount() {
        /*
         * Use the filtered projects list (doesn't contain "select all" and
         * projects with not valid version control).
         */
        return removeNotValidVC(getCheckedProjects()).length;
    }

    @Override
    public void addCheckStateListener(final ICheckStateListener listener) {
        ((CheckboxTableViewer) getViewer()).addCheckStateListener(listener);
    }

    @Override
    public void removeCheckStateListener(final ICheckStateListener listener) {
        ((CheckboxTableViewer) getViewer()).removeCheckStateListener(listener);
    }

    @Override
    public void dispose() {
        getViewer().getLabelProvider().dispose();
        super.dispose();
    }

    /**
     * @param projects
     *        an array of {@link ProjectInfo} (may be <code>null</code>)
     * @return a new projects array with the items in the given projects array,
     *         minus the "select all" item (if it was present).
     *         <code>null</code> if the given array was <code>null</code>
     */
    public ProjectInfo[] removeSelectAllItem(final ProjectInfo[] projects) {
        final List<ProjectInfo> ret = new ArrayList<ProjectInfo>();

        if (projects != null) {
            for (final ProjectInfo project : projects) {
                if (!SELECT_ALL_SPECIAL_PROJECT_INFO.equals(project)) {
                    ret.add(project);
                }
            }
        }
        return ret.toArray(new ProjectInfo[ret.size()]);
    }

    /**
     * @param projects
     *        an array of {@link ProjectInfo} to add a "select all" element to
     *        (may be <code>null</code>)
     * @return a new projects array with at most one "select all" item present
     *         (never <code>null</code>)
     */
    public ProjectInfo[] addSelectAllItem(final ProjectInfo[] projects) {
        if (projects == null || projects.length == 0) {
            return projects;
        }

        // Check if it already contains one
        for (int i = 0; i < projects.length; i++) {
            if (SELECT_ALL_SPECIAL_PROJECT_INFO.equals(projects[i])) {
                return projects;
            }
        }

        final ProjectInfo[] copy = new ProjectInfo[projects.length + 1];
        for (int i = 0; i < projects.length; i++) {
            copy[i] = projects[i];
        }
        copy[copy.length - 1] = SELECT_ALL_SPECIAL_PROJECT_INFO;

        return copy;
    }

    /**
     * Updates the tri-state "select all" element based on the size of the set
     * of currently checked (non-special) items in the table. If all non-special
     * items are checked, the "select all" box is checked and ungrayed. If no
     * non-special items are checked, the "select all" box is unchecked and
     * ungrayed. If some but not all non-special items are checked, the
     * "select all" box is checked and grayed.
     */
    private void updateSelectAllCheckState() {
        final CheckboxTableViewer viewer = ((CheckboxTableViewer) getViewer());

        // Use the filtered (non-special items removed) counts
        final int checkedCount = getValidCheckedProjectsCount();
        final int elementCount = removeNotValidVC(getProjects()).length;

        if (checkedCount == elementCount && elementCount > 0) {
            viewer.setChecked(SELECT_ALL_SPECIAL_PROJECT_INFO, true);
            viewer.setGrayed(SELECT_ALL_SPECIAL_PROJECT_INFO, false);
        } else if (checkedCount == 0) {
            viewer.setChecked(SELECT_ALL_SPECIAL_PROJECT_INFO, false);
            viewer.setGrayed(SELECT_ALL_SPECIAL_PROJECT_INFO, false);
        } else {
            viewer.setChecked(SELECT_ALL_SPECIAL_PROJECT_INFO, true);
            viewer.setGrayed(SELECT_ALL_SPECIAL_PROJECT_INFO, true);
        }
    }

    public SourceControlCapabilityFlags getProjectSourceControlCapabilityFlags(final Object element) {
        final ProjectInfo project = (ProjectInfo) element;
        return project.getSourceControlCapabilityFlags();
    }

    private boolean isGitProject(final Object element) {
        return getProjectSourceControlCapabilityFlags(element).contains(SourceControlCapabilityFlags.GIT);
    }

    private boolean isTfsProject(final Object element) {
        return getProjectSourceControlCapabilityFlags(element).contains(SourceControlCapabilityFlags.TFS);
    }

    private boolean hasValidVC(final Object element) {
        Check.notNull(sourceControlCapabilityFlagsFilter, "sourceControlCapabilityFlagsFilter"); //$NON-NLS-1$

        final SourceControlCapabilityFlags projectSourceControlCapabilityFlags =
            getProjectSourceControlCapabilityFlags(element);

        return sourceControlCapabilityFlagsFilter.containsAny(projectSourceControlCapabilityFlags);
    }

    private class ProjectColumnLabelProvider extends CellLabelProvider {
        private final boolean showIcons;

        public ProjectColumnLabelProvider(final boolean showIcons) {
            super();
            this.showIcons = showIcons;
        }

        private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

        public Image getColumnImage(final Object element) {
            if (showIcons) {
                if (isTfsProject(element)) {
                    return imageHelper.getImage("images/common/tfs_project.png"); //$NON-NLS-1$
                } else if (isGitProject(element)) {
                    return imageHelper.getImage("images/common/git_repo.png"); //$NON-NLS-1$
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            final ProjectInfo project = (ProjectInfo) element;

            cell.setText(project.getName());

            if (project != SELECT_ALL_SPECIAL_PROJECT_INFO) {
                cell.setImage(getColumnImage(project));

                if (!hasValidVC(project)) {
                    setCellForeground(cell, SWT.COLOR_GRAY);
                }
            }
        }

        @Override
        public void dispose() {
            imageHelper.dispose();
        }
    }

    private void setCellForeground(final ViewerCell cell, final int color) {
        cell.setForeground(getDisplay().getSystemColor(color));
    }
}
