// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.wizard.sharewizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.codemarker.CodeMarker;
import com.microsoft.tfs.client.common.codemarker.CodeMarkerDispatch;
import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.controls.vc.ServerItemTreeControl;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.WizardPageValidatorBinding;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.common.ui.vc.serveritem.WorkspaceItemSource;
import com.microsoft.tfs.client.eclipse.ui.Messages;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;
import com.microsoft.tfs.util.valid.ValidatorHelper;

/**
 * Prompts for the destination for one (or more) projects that are being shared
 * to the server.
 */
public class ShareWizardTreePage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "ShareWizardTreePage"; //$NON-NLS-1$
    public static final String SERVER_PATHS = "ShareWizardTreePage.serverPaths"; //$NON-NLS-1$

    public static final CodeMarker CODEMARKER_REFRESH_COMPLETE =
        new CodeMarker("com.microsoft.tfs.client.eclipse.ui.wizard.sharewizard.ShareWizardTreePage#refreshComplete"); //$NON-NLS-1$

    public static final String SELECTED_PATH_TEXT_ID = "ShareWizardTreePage.selectedPathText"; //$NON-NLS-1$

    private SizeConstrainedComposite container;

    private Label descriptionLabel;
    private ShareWizardTreeControl treeControl;

    public ShareWizardTreePage() {
        super(
            PAGE_NAME,
            Messages.getString("ShareWizardTreePage.PageName"), //$NON-NLS-1$
            Messages.getString("ShareWizardTreePage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final IProject[] projects = (IProject[]) getExtendedWizard().getPageData(IProject.class);
        Check.notNull(projects, "projects"); //$NON-NLS-1$

        /*
         * Use a SizeConstrainedComposite to encourage the label control to wrap
         * instead of expanding to fill the entire display.
         */
        container = new SizeConstrainedComposite(parent, SWT.NONE);
        container.setDefaultSize(getShell().getSize().x, SWT.DEFAULT);

        setControl(container);

        final GridLayout layout = new GridLayout();
        layout.marginHeight = getVerticalMargin();
        layout.marginWidth = getHorizontalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        descriptionLabel = new Label(container, SWT.WRAP);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(descriptionLabel);

        treeControl = new ShareWizardTreeControl(container, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().vGrab().vFill().vIndent(12).applyTo(treeControl);

        new WizardPageValidatorBinding(this).bind(treeControl);
    }

    @Override
    protected void refresh() {
        getExtendedWizard().removePageData(SERVER_PATHS);

        final Workspace workspace = (Workspace) getExtendedWizard().getPageData(Workspace.class);
        final IProject[] projects = (IProject[]) getExtendedWizard().getPageData(IProject.class);

        if (projects.length == 1) {
            descriptionLabel.setText(Messages.getString("ShareWizardTreePage.SingleProjectDescriptionLabelText")); //$NON-NLS-1$
        } else {
            descriptionLabel.setText(Messages.getString("ShareWizardTreePage.MultiProjectDescriptionLabelText")); //$NON-NLS-1$
        }

        treeControl.setWorkspace(workspace);
        treeControl.setProjects(projects);
        treeControl.setFocus();

        container.layout(true);

        CodeMarkerDispatch.dispatch(CODEMARKER_REFRESH_COMPLETE);
    }

    @Override
    protected boolean onPageFinished() {
        getExtendedWizard().setPageData(SERVER_PATHS, treeControl.getProjectServerPaths());
        return true;
    }

    /*
     * Note that this tree control handles things slightly differently depending
     * on whether we have single or multiple projects being shared.
     */
    private class ShareWizardTreeControl extends Composite implements Validatable {
        private final ValidatorHelper validator;

        private final ServerItemTreeControl serverItemTreeControl;
        private final Label selectedPathPrompt;
        private final Text selectedPathText;

        private IProject[] projects = new IProject[0];

        private String selectedPath;

        public ShareWizardTreeControl(final Composite parent, final int style) {
            super(parent, style);

            validator = new ValidatorHelper(this);

            final GridLayout layout = new GridLayout(2, false);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.horizontalSpacing = getHorizontalSpacing();
            setLayout(layout);

            final Label folderLabel = new Label(this, SWT.NONE);
            folderLabel.setText(Messages.getString("ShareWizardTreePage.FolderLabelText")); //$NON-NLS-1$
            GridDataBuilder.newInstance().span(2, 1).fill().applyTo(folderLabel);

            serverItemTreeControl = new ServerItemTreeControl(this, SWT.NONE);
            serverItemTreeControl.setVisibleServerItemTypes(ServerItemType.ALL_FOLDERS);
            GridDataBuilder.newInstance().span(2, 1).fill().grab().applyTo(serverItemTreeControl);

            serverItemTreeControl.addSelectionChangedListener(new ISelectionChangedListener() {
                @Override
                public void selectionChanged(final SelectionChangedEvent event) {
                    final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                    onServerItemSelected((TypedServerItem) selection.getFirstElement());
                }
            });

            selectedPathPrompt = new Label(this, SWT.NONE);
            GridDataBuilder.newInstance().vIndent(15).applyTo(selectedPathPrompt);

            selectedPathText = new Text(this, SWT.BORDER);
            AutomationIDHelper.setWidgetID(selectedPathText, SELECTED_PATH_TEXT_ID);
            GridDataBuilder.newInstance().hFill().hGrab().hIndent(getHorizontalSpacing()).vIndent(15).applyTo(
                selectedPathText);

            selectedPathText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(final ModifyEvent e) {
                    onSelectedPathModified(selectedPathText.getText());
                }
            });

            /*
             * initial validation
             */
            onSelectedPathModified(selectedPathText.getText());
        }

        public void setProjects(final IProject[] projects) {
            Check.notNull(projects, "projects"); //$NON-NLS-1$

            this.projects = projects;

            if (projects.length == 1) {
                selectedPathPrompt.setText(Messages.getString("ShareWizardTreePage.SingleProjectPathPrompt")); //$NON-NLS-1$
            } else {
                selectedPathPrompt.setText(Messages.getString("ShareWizardTreePage.MultiProjectPathPrompt")); //$NON-NLS-1$
            }

            layout(true);
        }

        public void setWorkspace(final Workspace workspace) {
            serverItemTreeControl.setServerItemSource(new WorkspaceItemSource(workspace));
        }

        public String getSelectedPath() {
            return selectedPath;
        }

        public String[] getProjectServerPaths() {
            final String[] serverPaths = new String[projects.length];

            /*
             * Handle single vs multiple projects a little differently: since we
             * let users enter a project path for single projects, we do the
             * concatenation of project name in the selection listener. For
             * multiple projects, we always concat the project names here.
             */
            if (projects.length == 1) {
                serverPaths[0] = treeControl.getSelectedPath();
            } else {
                for (int i = 0; i < projects.length; i++) {
                    serverPaths[i] = ServerPath.combine(treeControl.getSelectedPath(), projects[i].getName());
                }
            }

            return serverPaths;
        }

        @Override
        public Validator getValidator() {
            return validator;
        }

        @Override
        public boolean setFocus() {
            return serverItemTreeControl.setFocus();
        }

        protected void onSelectedPathModified(String text) {
            if (text.trim().length() == 0) {
                text = null;
            }
            selectedPath = text;

            if (selectedPath == null || !ServerPath.isServerPath(selectedPath)) {
                validator.setInvalid(Messages.getString("ShareWizardTreePage.MustSelectSingleServer")); //$NON-NLS-1$
            } else if (ServerPath.equals(ServerPath.ROOT, selectedPath)) {
                validator.setInvalid(Messages.getString("ShareWizardTreePage.CannotSpecifyRootPath")); //$NON-NLS-1$
            } else if (projects.length == 1 && ServerPath.isDirectChild(ServerPath.ROOT, selectedPath)) {
                validator.setInvalid(Messages.getString("ShareWizardTreePage.MustSpecifyPathBeneathProject")); //$NON-NLS-1$
            } else {
                validator.setValid();
            }
        }

        private void onServerItemSelected(final TypedServerItem serverItem) {
            if (serverItem == null) {
                selectedPathText.setText(""); //$NON-NLS-1$
            } else {
                /*
                 * When sharing multiple projects, show the parent path. When
                 * sharing a single project, show the resultant path.
                 */

                final String serverPath = serverItem.getServerPath();

                if (projects.length == 1) {
                    selectedPathText.setText(ServerPath.combine(serverPath, projects[0].getName()));
                } else {
                    selectedPathText.setText(serverPath);
                }
            }
        }
    }

}
