// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.sharewizard;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.table.TableColumnData;
import com.microsoft.tfs.client.common.ui.framework.table.TableControl;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.util.Check;

/**
 * This wizard page is shown when the user has an error with their working
 * folder mappings: in particular, when they have some selected projects for
 * sharing mapped, but others remain unmapped. We do not know how to cope in
 * this situation (allowing them to map some projects but not others.)
 */

public class ShareWizardWorkingFolderErrorPage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "ShareWizardWorkingFolderErrorPage"; //$NON-NLS-1$

    public static final String ERROR_TABLE_ID = "ShareWizardWorkingFolderErrorPage.errorTable"; //$NON-NLS-1$

    private ShareWizardWorkingFolderErrorTable errorTable;

    public ShareWizardWorkingFolderErrorPage() {
        super(PAGE_NAME, Messages.getString("ShareWizardWorkingFolderErrorPage.PageName"), ""); //$NON-NLS-1$ //$NON-NLS-2$
        setPageComplete(false);
        setErrorMessage(Messages.getString("ShareWizardWorkingFolderErrorPage.ErrorMessageText")); //$NON-NLS-1$
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final SizeConstrainedComposite container = new SizeConstrainedComposite(parent, SWT.NONE);
        setControl(container);

        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        /*
         * Set the growing containers default size to the wizard size. This will
         * encourage the multiline text control to wrap (which it tends not to
         * do, it will only wrap at your display's width.)
         */
        container.setDefaultSize(getShell().getSize().x, SWT.DEFAULT);

        final Label errorText = new Label(container, SWT.WRAP | SWT.READ_ONLY);

        final String messageFormat = Messages.getString("ShareWizardWorkingFolderErrorPage.ErrorLabelTextFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, ProductInformation.getCurrent().toString());
        errorText.setText(message);

        GridDataBuilder.newInstance().hGrab().applyTo(errorText);

        errorTable = new ShareWizardWorkingFolderErrorTable(container, SWT.NONE);
        errorTable.setText(Messages.getString("ShareWizardWorkingFolderErrorPage.SelectedProjectsLabelText")); //$NON-NLS-1$
        AutomationIDHelper.setWidgetID(errorTable.getTable(), ERROR_TABLE_ID);
        GridDataBuilder.newInstance().hGrab().hFill().vGrab().vFill().vIndent(12).applyTo(errorTable);
    }

    @Override
    public void refresh() {
        final Workspace workspace = (Workspace) getExtendedWizard().getPageData(Workspace.class);
        final IProject[] projects = (IProject[]) getExtendedWizard().getPageData(IProject.class);
        final Map<IProject, String> workfoldMap = (Map<IProject, String>) getExtendedWizard().getPageData(
            ShareWizardWorkspacePage.PROJECT_WORKING_FOLDER_MAP);

        errorTable.setWorkspace(workspace);
        errorTable.setWorkingFolderMap(workfoldMap);
        errorTable.setProjects(projects);
    }

    private class ShareWizardWorkingFolderErrorTable extends TableControl {
        private Workspace workspace = null;
        private Map<IProject, String> workfoldMap = new HashMap<IProject, String>();

        protected ShareWizardWorkingFolderErrorTable(final Composite parent, final int style) {
            super(parent, style | SWT.FULL_SELECTION, IProject.class, null);

            final TableColumnData[] columnData = new TableColumnData[] {
                new TableColumnData(
                    Messages.getString("ShareWizardWorkingFolderErrorPage.ProjectColumnName"), //$NON-NLS-1$
                    15,
                    0.15F,
                    "project"), //$NON-NLS-1$
                new TableColumnData(
                    Messages.getString("ShareWizardWorkingFolderErrorPage.LocalPathColumName"), //$NON-NLS-1$
                    40,
                    0.40F,
                    "localpath"), //$NON-NLS-1$
                new TableColumnData(
                    Messages.getString("ShareWizardWorkingFolderErrorPage.ServerPathColumnName"), //$NON-NLS-1$
                    45,
                    0.45F,
                    "serverpath") //$NON-NLS-1$
            };

            setupTable(true, true, columnData);
            setUseViewerDefaults();
            setEnableTooltips(false);
        }

        @Override
        protected String getColumnText(final Object element, final String propertyName) {
            final IProject shareProject = (IProject) element;

            if (propertyName.equals("project")) //$NON-NLS-1$
            {
                return shareProject.getProject().getName();
            } else if (propertyName.equals("localpath")) //$NON-NLS-1$
            {
                return shareProject.getLocation().toOSString();
            } else if (propertyName.equals("serverpath")) //$NON-NLS-1$
            {
                final String workingFolder = workfoldMap.get(shareProject);

                if (workspace == null) {
                    final String messageFormat =
                        Messages.getString("ShareWizardWorkingFolderErrorPage.UnknownParentMapFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, workingFolder);
                    return message;

                }

                if (workingFolder == null) {
                    return Messages.getString("ShareWizardWorkingFolderErrorPage.NotMapped"); //$NON-NLS-1$
                }

                final String mappedPath = workspace.getMappedServerPath(shareProject.getLocation().toOSString());
                return (mappedPath == null) ? Messages.getString("ShareWizardWorkingFolderErrorPage.Cloaked") //$NON-NLS-1$
                    : mappedPath;
            }

            return Messages.getString("ShareWizardWorkingFolderErrorPage.Unknown"); //$NON-NLS-1$
        }

        public void setProjects(final IProject[] projects) {
            Check.notNull(projects, "projects"); //$NON-NLS-1$

            setElements(projects);
        }

        public void setWorkingFolderMap(final Map<IProject, String> workfoldMap) {
            Check.notNull(workfoldMap, "workfoldMap"); //$NON-NLS-1$

            this.workfoldMap = workfoldMap;
        }

        public void setWorkspace(final Workspace workspace) {
            this.workspace = workspace;
        }
    }
}
