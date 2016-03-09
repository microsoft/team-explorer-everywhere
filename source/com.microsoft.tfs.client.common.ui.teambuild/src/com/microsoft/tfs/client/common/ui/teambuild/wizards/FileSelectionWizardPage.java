// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.controls.vc.ServerItemPicker;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemSource;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.client.common.ui.vc.serveritem.TypedServerItem;
import com.microsoft.tfs.client.common.ui.vc.serveritem.VersionedItemSource;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IWorkspaceMapping;
import com.microsoft.tfs.core.clients.build.IWorkspaceTemplate;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;

public abstract class FileSelectionWizardPage extends WizardPage {
    private final IBuildDefinition buildDefinition;

    private Text buildFileText;

    private ServerItemPicker serverItemPicker;
    private ServerItemSource serverItemSource;

    private String buildFileServerPath;

    public FileSelectionWizardPage(
        final String pageName,
        final IBuildDefinition buildDefinition,
        final String title,
        final String description) {
        super("buildFileSelectionPage", title, null); //$NON-NLS-1$
        setDescription(description);
        this.buildDefinition = buildDefinition;
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);
        final GridLayout layout = SWTUtil.gridLayout(composite, 2);

        serverItemSource =
            new VersionedItemSource(buildDefinition.getBuildServer().getConnection(), LatestVersionSpec.INSTANCE);
        serverItemPicker = new ServerItemPicker(composite, SWT.NONE);
        serverItemPicker.setServerItemSource(serverItemSource);
        serverItemPicker.setCurrentFolderPath(ServerPath.ROOT);

        GridDataBuilder.newInstance().hSpan(layout).fill().grab().applyTo(serverItemPicker);

        setControl(composite);

        SWTUtil.createLabel(composite, ""); //$NON-NLS-1$
        SWTUtil.createLabel(composite, ""); //$NON-NLS-1$

        SWTUtil.createLabel(composite, getBuildFileLabel());

        buildFileText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(buildFileText);

        if (buildFileServerPath == null) {
            buildFileServerPath =
                ServerPath.combine(calculateSourceDir(buildDefinition.getWorkspace()), getDefaultBuildFileName());
        }

        serverItemPicker.setSelectedItem(new TypedServerItem(buildFileServerPath, ServerItemType.FILE));
        buildFileText.setText(buildFileServerPath);
        buildFileText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                updatePageComplete();
            }
        });

        serverItemPicker.getServerItemTable().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                final TypedServerItem selectedItem = (TypedServerItem) selection.getFirstElement();
                if (selectedItem == null) {
                    buildFileText.setText(""); //$NON-NLS-1$
                } else {
                    buildFileText.setText(selectedItem.getServerPath());
                }
                updatePageComplete();
            }
        });

        serverItemPicker.getServerItemTable().addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                final TypedServerItem item = (TypedServerItem) selection.getFirstElement();
                if (item != null && item.getType() == ServerItemType.FILE) {
                    final IWizardPage nextPage = getNextPage();
                    if (nextPage != null) {
                        getContainer().showPage(nextPage);
                    }
                }
            }
        });

        // Ensure that selected item is visible in server picker.
        final TableItem[] selectedItems = serverItemPicker.getServerItemTable().getTable().getSelection();
        if (selectedItems != null && selectedItems.length > 0) {
            serverItemPicker.getServerItemTable().getTable().showItem(selectedItems[0]);
        }
    }

    private String calculateSourceDir(final IWorkspaceTemplate workspaceTemplate) {
        final IWorkspaceMapping[] workspaceMappings = workspaceTemplate.getMappings();

        if (workspaceMappings == null || workspaceMappings.length == 0) {
            return ServerPath.combine(ServerPath.ROOT, buildDefinition.getTeamProject());
        }

        return findCommonServerPath(workspaceMappings);
    }

    private void updatePageComplete() {
        buildFileServerPath = buildFileText.getText().trim();

        if (!buildFileServerPath.startsWith(ServerPath.ROOT)) {
            setPageComplete(false);

            final String messageFormat =
                Messages.getString("FileSelectionWizardPage.BuildFilePathMustStartAtRootFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, ServerPath.ROOT);
            setErrorMessage(message);
            return;
        }

        setPageComplete(true);
        setErrorMessage(null);
        setMessage(null);
    }

    private String findCommonServerPath(final IWorkspaceMapping[] workspaceMappings) {

        // Sort into server path order.
        Arrays.sort(workspaceMappings, new Comparator<IWorkspaceMapping>() {
            @Override
            public int compare(final IWorkspaceMapping x, final IWorkspaceMapping y) {
                return ServerPath.compareTopDown(x.getServerItem(), y.getServerItem());
            }
        });

        String serverPath = workspaceMappings[0].getServerItem();

        for (int i = 0; i < workspaceMappings.length; i++) {
            if (!workspaceMappings[i].getServerItem().startsWith(serverPath)) {
                serverPath = ServerPath.getCommonParent(workspaceMappings[i].getServerItem(), serverPath);
            }
        }

        return serverPath;
    }

    public String getBuildFileServerPath() {
        return buildFileServerPath;
    }

    public void setBuildFileServerPath(final String buildFileServerPath) {
        this.buildFileServerPath = buildFileServerPath;
        if (serverItemPicker != null && !serverItemPicker.isDisposed()) {
            serverItemPicker.setSelectedItem(new TypedServerItem(buildFileServerPath, ServerItemType.FILE));
            buildFileText.setText(buildFileServerPath);
        }
    }

    public void setDefaultServerItem(final IWorkspaceTemplate workspaceTemplate) {
        setBuildFileServerPath(ServerPath.combine(calculateSourceDir(workspaceTemplate), getDefaultBuildFileName()));
    }

    public IBuildDefinition getBuildDefinition() {
        return buildDefinition;
    }

    public abstract String getBuildFileLabel();

    public abstract String getDefaultBuildFileName();

}
