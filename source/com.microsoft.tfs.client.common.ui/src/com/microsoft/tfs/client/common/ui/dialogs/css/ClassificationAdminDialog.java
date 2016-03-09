// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.css;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolBar;

import com.microsoft.tfs.client.common.commands.css.GetClassificationNodesCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.CommonStructureControl;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.DeleteNodeAction;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.DemoteNodeAction;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.MoveDownNodeAction;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.MoveUpNodeAction;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.NewNodeAction;
import com.microsoft.tfs.client.common.ui.dialogs.css.actions.PromoteNodeAction;
import com.microsoft.tfs.client.common.ui.framework.command.BusyIndicatorCommandExecutor;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.TFSTeamProjectCollectionFormatter;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.CommonStructureClient;
import com.microsoft.tfs.util.Check;

/**
 * Dialog to edit areas and iterations.
 */
public class ClassificationAdminDialog extends BaseDialog {
    private final TFSTeamProjectCollection connection;
    private final CommonStructureClient css;
    private final String projectName;
    private final String projectUri;

    public ClassificationAdminDialog(
        final Shell parentShell,
        final TFSTeamProjectCollection connection,
        final String projectName,
        final String projectUri) {
        super(parentShell);

        Check.notNull(connection, "connection"); //$NON-NLS-1$
        Check.notNull(projectName, "projectName"); //$NON-NLS-1$
        Check.notNull(projectUri, "projectUri"); //$NON-NLS-1$

        this.connection = connection;
        this.projectName = projectName;
        this.projectUri = projectUri;

        css = (CommonStructureClient) connection.getClient(CommonStructureClient.class);

        setOptionIncludeDefaultButtons(false);
        addButtonDescription(
            IDialogConstants.OK_ID,
            Messages.getString("ClassificationAdminDialog.CloseButtonText"), //$NON-NLS-1$
            true);
    }

    @Override
    protected void hookAddToDialogArea(final Composite composite) {
        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        final Label tfsPrompt =
            SWTUtil.createLabel(composite, Messages.getString("ClassificationAdminDialog.TfsLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hAlignPrompt().applyTo(tfsPrompt);

        final Label tfsLabel = SWTUtil.createLabel(composite, TFSTeamProjectCollectionFormatter.getLabel(connection));
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(tfsLabel);

        final Label tfsProjectPrompt =
            SWTUtil.createLabel(composite, Messages.getString("ClassificationAdminDialog.ProjectLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hAlignPrompt().applyTo(tfsProjectPrompt);

        final Label teamProjectLabel = SWTUtil.createLabel(composite, projectName);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(teamProjectLabel);

        final TabFolder tabControl = new TabFolder(composite, SWT.TOP);
        GridDataBuilder.newInstance().hSpan(layout).grab().fill().applyTo(tabControl);
        ControlSize.setCharSizeHints(tabControl, 60, 15);

        final TabItem areaTab =
            SWTUtil.createTabItem(tabControl, Messages.getString("ClassificationAdminDialog.AreaTabText")); //$NON-NLS-1$
        final Composite areaComposite = new Composite(tabControl, SWT.NONE);

        final GridLayout areaCompositeLayout = new GridLayout();
        areaCompositeLayout.marginHeight = 0;
        areaCompositeLayout.marginWidth = 0;
        areaCompositeLayout.verticalSpacing = 0;
        areaComposite.setLayout(areaCompositeLayout);
        areaTab.setControl(areaComposite);

        final TabItem iterationTab =
            SWTUtil.createTabItem(tabControl, Messages.getString("ClassificationAdminDialog.IterationTabText")); //$NON-NLS-1$
        final Composite iterationComposite = new Composite(tabControl, SWT.NONE);

        final GridLayout iterationCompositeLayout = new GridLayout();
        iterationCompositeLayout.marginHeight = 0;
        iterationCompositeLayout.marginWidth = 0;
        iterationCompositeLayout.verticalSpacing = 0;
        iterationComposite.setLayout(iterationCompositeLayout);
        iterationTab.setControl(iterationComposite);

        final ToolBarManager areaToolbar = createToolBar(areaComposite);
        final ToolBarManager iterationToolbar = createToolBar(iterationComposite);

        final Label areaSeparator = new Label(areaComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(areaSeparator);

        final Label iterationSeparator = new Label(iterationComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(iterationSeparator);

        final CommonStructureControl areaControl = new CommonStructureControl(areaComposite, SWT.SINGLE, css);
        final CommonStructureControl iterationControl = new CommonStructureControl(iterationComposite, SWT.SINGLE, css);

        GridDataBuilder.newInstance().fill().grab().applyTo(areaControl);
        GridDataBuilder.newInstance().fill().grab().applyTo(iterationControl);

        final BusyIndicatorCommandExecutor executor = new BusyIndicatorCommandExecutor(getShell());
        final GetClassificationNodesCommand command = new GetClassificationNodesCommand(css, projectUri);
        final IStatus status = executor.execute(command);
        if (!status.isOK()) {
            return;
        }

        areaControl.setRootNode(command.getAreas());
        areaControl.getTreeViewer().setSelection(new StructuredSelection(areaControl.getRootNode()));
        iterationControl.setRootNode(command.getIterations());
        iterationControl.getTreeViewer().setSelection(new StructuredSelection(iterationControl.getRootNode()));

        addActions(areaToolbar, areaControl);
        addActions(iterationToolbar, iterationControl);
    }

    private ToolBarManager createToolBar(final Composite parent) {
        final ToolBar toolBar = new ToolBar(parent, SWT.HORIZONTAL);
        GridDataBuilder.newInstance().hFill().applyTo(toolBar);
        final ToolBarManager manager = new ToolBarManager(toolBar);

        return manager;
    }

    private void addActions(final ToolBarManager manager, final CommonStructureControl cssControl) {
        manager.add(new NewNodeAction(cssControl));
        manager.add(new DeleteNodeAction(cssControl));
        manager.add(new Separator());
        manager.add(new MoveUpNodeAction(cssControl));
        manager.add(new MoveDownNodeAction(cssControl));
        manager.add(new PromoteNodeAction(cssControl));
        manager.add(new DemoteNodeAction(cssControl));

        manager.update(true);
    }

    /*
     * (non-Javadoc)
     *
     * @seecom.microsoft.tfs.client.common.ui.shared.dialog.BaseDialog#
     * provideDialogTitle()
     */
    @Override
    protected String provideDialogTitle() {
        return Messages.getString("ClassificationAdminDialog.DialogTitle"); //$NON-NLS-1$
    }
}
