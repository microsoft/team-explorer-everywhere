// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.common;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.controls.workspaces.WorkspacesControl;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.WizardPageValidatorBinding;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.util.Check;

/**
 * A simple wizard page that requires users to select a workspace. May be used
 * as-is or extended, perhaps to perform some additional page-finished logic.
 * (see {@link ShareWizardWorkspacePage} for an example.)
 */
public class WizardWorkspacePage extends ExtendedWizardPage {
    public static final String PAGE_NAME = "WizardWorkspacePage"; //$NON-NLS-1$

    private String explanationText;

    private Label explanationTextControl;
    private WorkspacesControl workspacesControl;

    public WizardWorkspacePage() {
        super(
            PAGE_NAME,
            Messages.getString("WizardWorkspacePage.PageTitle"), //$NON-NLS-1$
            Messages.getString("WizardWorkspacePage.PageDescription")); //$NON-NLS-1$
    }

    protected WizardWorkspacePage(final String pageName, final String title, final String description) {
        super(pageName, title, description);
    }

    /**
     * Note that there is currently no way to clear the receiver's text without
     * keeping the whitespace where the label would be drawn (and the spacing
     * between the label and the control.)
     */
    protected void setText(final String text) {
        explanationText = text;

        /* Redraw if we've already layed this out */
        if (explanationTextControl != null
            && !explanationTextControl.isDisposed()
            && getControl() instanceof Composite) {
            /*
             * TODO: allow clearing text here and relayout w/o the label control
             * on the page.
             */
            explanationTextControl.setText(text);
            ((Composite) getControl()).layout(true);
        }
    }

    protected String getText() {
        return explanationText;
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        /*
         * Use a SizeConstrainedComposite to encourage the label control to wrap
         * instead of expanding to fill the entire display.
         */
        final SizeConstrainedComposite container = new SizeConstrainedComposite(parent, SWT.NONE);
        container.setDefaultSize(getShell().getSize().x, SWT.DEFAULT);
        setControl(container);

        final GridLayout layout = new GridLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        container.setLayout(layout);

        if (explanationText != null) {
            explanationTextControl = new Label(container, SWT.WRAP);
            explanationTextControl.setText(explanationText);

            GridDataBuilder.newInstance().hGrab().hFill().applyTo(explanationTextControl);
        }

        workspacesControl = new WorkspacesControl(container, SWT.NONE, getCommandExecutor());

        if (getExtendedWizard().enableNext(this)) {
            workspacesControl.getWorkspacesTable().addDoubleClickListener(new IDoubleClickListener() {
                @Override
                public void doubleClick(final DoubleClickEvent event) {
                    final IWizardPage nextPage = getNextPage();
                    if (nextPage != null) {
                        getContainer().showPage(nextPage);
                    }
                }
            });
        }

        GridDataBuilder workspacesControlData = GridDataBuilder.newInstance().grab().fill();

        if (explanationText != null) {
            workspacesControlData = workspacesControlData.vIndent(12);
        }

        workspacesControlData.applyTo(workspacesControl);

        new WizardPageValidatorBinding(this).bind(workspacesControl);
    }

    protected Workspace getSelectedWorkspace() {
        Check.notNull(workspacesControl, "workspacesControl"); //$NON-NLS-1$
        Check.isTrue(!workspacesControl.isDisposed(), "!workspacesControl.isDisposed()"); //$NON-NLS-1$

        return workspacesControl.getWorkspacesTable().getSelectedWorkspace();
    }

    @Override
    protected void refresh() {
        getExtendedWizard().removePageData(Workspace.class);

        final TFSTeamProjectCollection connection =
            (TFSTeamProjectCollection) getExtendedWizard().getPageData(TFSTeamProjectCollection.class);
        workspacesControl.refresh(connection, true, true);
    }

    @Override
    protected boolean onPageFinished() {
        getExtendedWizard().setPageData(Workspace.class, getSelectedWorkspace());
        return true;
    }
}
