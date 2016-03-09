// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.vc.checkinpolicies;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.dialog.BaseDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ContentProviderAdapter;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.framework.validation.NumericConstraint;
import com.microsoft.tfs.client.common.ui.framework.validation.SelectionProviderValidator;
import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.Validator;

public class PolicyInstanceDialog extends BaseDialog {
    private final String title;
    private final PolicyInstance[] instances;

    private Composite dialogComposite;
    private ListViewer listViewer;
    private Label longDescriptionLabel;

    private PolicyInstance selectedPolicyInstance;

    public PolicyInstanceDialog(final Shell parentShell, final String title, final PolicyInstance[] instances) {
        super(parentShell);

        Check.notNull(title, "title"); //$NON-NLS-1$
        Check.notNull(instances, "instances"); //$NON-NLS-1$

        this.title = title;
        this.instances = instances;
    }

    public PolicyInstance getSelectedPolicyInstance() {
        return selectedPolicyInstance;
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        createControls(dialogArea);
    }

    @Override
    protected void hookAfterButtonsCreated() {
        final Button okButton = getButton(IDialogConstants.OK_ID);

        final Validator validator = new SelectionProviderValidator(listViewer, NumericConstraint.EXACTLY_ONE, null);
        new ButtonValidatorBinding(okButton).bind(validator);
    }

    @Override
    protected String provideDialogTitle() {
        return title;
    }

    private void createControls(final Composite composite) {
        dialogComposite = composite;

        final GridLayout layout = new GridLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        composite.setLayout(layout);

        SWTUtil.createLabel(composite, Messages.getString("PolicyInstanceDialog.CheckinPolicyLabelText")); //$NON-NLS-1$

        listViewer = new ListViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        listViewer.setLabelProvider(new LabelProvider());
        listViewer.setContentProvider(new ContentProvider());
        listViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                PolicyInstanceDialog.this.selectionChanged(event);
            }
        });
        listViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                okPressed();
            }
        });
        GridDataBuilder.newInstance().grab().fill().applyTo(listViewer.getControl());
        ControlSize.setCharHeightHint(listViewer.getControl(), 10);
        ControlSize.setCharWidthHint(listViewer.getControl(), 60);

        final Group descriptionGroup =
            SWTUtil.createGroup(composite, Messages.getString("PolicyInstanceDialog.DescriptionGroupText")); //$NON-NLS-1$

        GridDataBuilder.newInstance().hFill().hGrab().wHint(getMinimumMessageAreaWidth()).applyTo(descriptionGroup);

        SWTUtil.fillLayout(descriptionGroup, SWT.HORIZONTAL, getHorizontalMargin(), getVerticalMargin(), getSpacing());

        longDescriptionLabel = new Label(descriptionGroup, SWT.WRAP);

        listViewer.setInput(instances);
    }

    private void selectionChanged(final SelectionChangedEvent event) {
        final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        selectedPolicyInstance = (PolicyInstance) selection.getFirstElement();
        if (selectedPolicyInstance != null) {
            longDescriptionLabel.setText(selectedPolicyInstance.getPolicyType().getLongDescription());
        } else {
            longDescriptionLabel.setText(""); //$NON-NLS-1$
        }

        dialogComposite.layout();
    }

    private static class ContentProvider extends ContentProviderAdapter {
        @Override
        public Object[] getElements(final Object inputElement) {
            return (Object[]) inputElement;
        }
    }

    private static class LabelProvider extends org.eclipse.jface.viewers.LabelProvider {
        @Override
        public String getText(final Object element) {
            final PolicyInstance policyInstance = (PolicyInstance) element;

            return policyInstance.getPolicyType().getName();
        }
    }
}
