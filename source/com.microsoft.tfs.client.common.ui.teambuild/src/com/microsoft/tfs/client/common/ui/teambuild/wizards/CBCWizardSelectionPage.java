// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardSelectionPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.tree.TreeContentProvider;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TeamBuildImageHelper;

public class CBCWizardSelectionPage extends WizardSelectionPage {

    private boolean canFinishEarly = false;
    private TreeViewer wizardTree;
    protected final CreateBuildConfigurationWizardNode[] wizardDescriptors;

    protected CBCWizardSelectionPage(final CreateBuildConfigurationWizardNode[] wizardDescriptors) {
        super("createBuildConfigurationWizardSelectionPage"); //$NON-NLS-1$
        setTitle(Messages.getString("CBCWizardSelectionPage.PageTitle")); //$NON-NLS-1$
        setDescription(Messages.getString("CBCWizardSelectionPage.PageDescription")); //$NON-NLS-1$
        this.wizardDescriptors = wizardDescriptors;
    }

    /**
     * Makes the next page visible.
     */
    public void advanceToNextPageOrFinish() {
        if (canFlipToNextPage()) {
            getContainer().showPage(getNextPage());
        } else if (canFinishEarly()) {
            if (getWizard().performFinish()) {
                ((WizardDialog) getContainer()).close();
            }
        }
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite outerContainer = SWTUtil.createComposite(parent);
        SWTUtil.gridLayout(outerContainer, 1);
        final Label wizardLabel =
            SWTUtil.createLabel(outerContainer, Messages.getString("CBCWizardSelectionPage.WizardLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().fill().applyTo(wizardLabel);

        final Composite innerContainer = SWTUtil.createComposite(outerContainer);
        GridDataBuilder.newInstance().fill().grab().applyTo(innerContainer);

        wizardTree = createTree(innerContainer);
        wizardTree.getTree().setFocus();
        setControl(outerContainer);
    }

    /**
     * Create the wizard selection tree.
     *
     * @param innerContainer
     * @return
     */
    private TreeViewer createTree(final Composite innerContainer) {
        final FillLayout layout = new FillLayout();
        innerContainer.setLayout(layout);

        final TreeViewer viewer = new TreeViewer(innerContainer, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE);
        viewer.setUseHashlookup(true);
        viewer.setContentProvider(new ContentProvider());
        viewer.setLabelProvider(new ConfigLProvider());
        // viewer.addFilter(new Filter());

        viewer.setInput("Blah blah"); //$NON-NLS-1$
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                final CreateBuildConfigurationWizardNode wizardNode =
                    (CreateBuildConfigurationWizardNode) ((IStructuredSelection) event.getSelection()).getFirstElement();
                if (wizardNode != null) {
                    wizardNode.setParentWizardPage(CBCWizardSelectionPage.this);
                }
                setSelectedNode(wizardNode);
                setPageComplete(wizardNode != null);
            }
        });
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                advanceToNextPageOrFinish();
            }
        });
        return viewer;
    }

    /**
     * Sets whether the selected wizard advertises that it can finish early.
     *
     * @param newValue
     *        whether the selected wizard can finish early
     */
    public void setCanFinishEarly(final boolean newValue) {
        canFinishEarly = newValue;
    }

    /**
     * Answers whether the currently selected page, if any, advertises that it
     * may finish early.
     *
     * @return whether the page can finish early
     */
    public boolean canFinishEarly() {
        return canFinishEarly;
    }

    private class ContentProvider extends TreeContentProvider {

        @Override
        public Object[] getElements(final Object inputElement) {
            return getWizardDescriptors();
        }

        @Override
        public Object[] getChildren(final Object parentElement) {
            return null;
        }

        @Override
        public boolean hasChildren(final Object element) {
            return false;
        }

    }

    private class ConfigLProvider extends LabelProvider {
        private final TeamBuildImageHelper imageHelper = new TeamBuildImageHelper();

        @Override
        public Image getImage(final Object element) {
            return imageHelper.getImage(((CreateBuildConfigurationWizardNode) element).getIcon());
        }

        @Override
        public String getText(final Object element) {
            return ((CreateBuildConfigurationWizardNode) element).getName();
        }
    }

    public CreateBuildConfigurationWizardNode[] getWizardDescriptors() {
        return wizardDescriptors;
    }

}
