// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.vc.checkinpolicies;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.dialogs.vc.checkinpolicies.PolicyInstanceDialog;
import com.microsoft.tfs.client.common.ui.dialogs.vc.checkinpolicies.PolicyScopeDialog;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.validation.AbstractSelectionProviderValidator;
import com.microsoft.tfs.client.common.ui.framework.validation.ButtonValidatorBinding;
import com.microsoft.tfs.client.common.ui.framework.validation.NumericConstraint;
import com.microsoft.tfs.client.common.ui.framework.validation.SelectionProviderValidator;
import com.microsoft.tfs.core.checkinpolicies.PolicyContextKeys;
import com.microsoft.tfs.core.checkinpolicies.PolicyEditArgs;
import com.microsoft.tfs.core.checkinpolicies.PolicyInstance;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoader;
import com.microsoft.tfs.core.checkinpolicies.PolicyLoaderException;
import com.microsoft.tfs.core.clients.versioncontrol.TeamProject;
import com.microsoft.tfs.core.product.ProductInformation;
import com.microsoft.tfs.core.product.ProductName;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.valid.IValidity;
import com.microsoft.tfs.util.valid.Validity;

public class DefinePoliciesControl extends BaseControl {
    private final TeamProject teamProject;
    private final PolicyLoader policyLoader;
    private final List<PolicyConfiguration> policyConfigurations = new ArrayList<PolicyConfiguration>();
    private final PolicyConfigurationTable policyTable;

    public DefinePoliciesControl(
        final Composite parent,
        final int style,
        final TeamProject teamProject,
        final PolicyLoader policyLoader) {
        super(parent, style);

        Check.notNull(teamProject, "teamProject"); //$NON-NLS-1$
        Check.notNull(policyLoader, "policyLoader"); //$NON-NLS-1$
        this.teamProject = teamProject;
        this.policyLoader = policyLoader;

        final GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        setLayout(layout);

        policyTable = new PolicyConfigurationTable(this, SWT.MULTI | SWT.FULL_SELECTION);
        GridDataBuilder.newInstance().grab().fill().vSpan(4).applyTo(policyTable);
        policyTable.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                if (policyTable.getSelectedPolicyConfiguration().canEdit()) {
                    editClicked();
                }
            }
        });

        final Button addButton = SWTUtil.createButton(this, Messages.getString("DefinePoliciesControl.AddButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.TOP).hFill().applyTo(addButton);
        addButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                addClicked();
            }
        });

        final Button editButton =
            SWTUtil.createButton(this, Messages.getString("DefinePoliciesControl.EditButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.TOP).hFill().applyTo(editButton);
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                editClicked();
            }
        });

        final Button removeButton =
            SWTUtil.createButton(this, Messages.getString("DefinePoliciesControl.RemoveButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.TOP).hFill().applyTo(removeButton);
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                removeClicked();
            }
        });

        final Button scopeButton =
            SWTUtil.createButton(this, Messages.getString("DefinePoliciesControl.ScopeButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.TOP).hFill().applyTo(scopeButton);
        scopeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                scopeClicked();
            }
        });

        ButtonHelper.setButtonsToButtonBarSize(new Button[] {
            addButton,
            editButton,
            removeButton,
            scopeButton
        });

        new ButtonValidatorBinding(removeButton).bind(policyTable.getSelectionValidator());
        new ButtonValidatorBinding(editButton).bind(new EditabilityValidator(policyTable));
        new ButtonValidatorBinding(scopeButton).bind(
            new SelectionProviderValidator(policyTable, NumericConstraint.EXACTLY_ONE));
    }

    public void setPolicyConfigurations(final PolicyConfiguration[] policyConfigurationArray) {
        Check.notNull(policyConfigurationArray, "policyConfigurationArray"); //$NON-NLS-1$

        policyConfigurations.clear();
        policyConfigurations.addAll(Arrays.asList(policyConfigurationArray));
        refreshTable();
    }

    public PolicyConfiguration[] getPolicyConfigurations() {
        return policyConfigurations.toArray(new PolicyConfiguration[policyConfigurations.size()]);
    }

    private void removeClicked() {
        final PolicyConfiguration[] selectedPolicyConfigurations = policyTable.getSelectedPolicyConfigurations();
        policyConfigurations.removeAll(Arrays.asList(selectedPolicyConfigurations));
        refreshTable();
    }

    private void editClicked() {
        final PolicyConfiguration selectedPolicyConfiguration = policyTable.getSelectedPolicyConfiguration();
        edit(selectedPolicyConfiguration, false);
    }

    private void scopeClicked() {
        final PolicyConfiguration selectedPolicyConfiguration = policyTable.getSelectedPolicyConfiguration();
        final PolicyScopeDialog scopeDialog = new PolicyScopeDialog(
            getShell(),
            selectedPolicyConfiguration.getType().getName(),
            selectedPolicyConfiguration.getScopeExpressions());

        if (scopeDialog.open() != IDialogConstants.OK_ID) {
            return;
        }

        selectedPolicyConfiguration.setScopeExpressions(scopeDialog.getScopeExpressions());
    }

    private boolean edit(final PolicyConfiguration policyConfiguration, final boolean isNew) {
        if (policyConfiguration.canEdit() == false) {
            throw new UnsupportedOperationException();
        }

        final PolicyEditArgs policyEditArgs = new PolicyEditArgs(isNew, teamProject);

        policyEditArgs.getContext().addProperty(PolicyContextKeys.SWT_SHELL, getShell());
        policyEditArgs.getContext().addProperty(
            PolicyContextKeys.TFS_TEAM_PROJECT_COLLECTION,
            teamProject.getVersionControlClient().getConnection());

        final ProductName product = ProductInformation.getCurrent();
        if (product.equals(ProductName.PLUGIN)) {
            policyEditArgs.getContext().addProperty(PolicyContextKeys.RUNNING_PRODUCT_ECLIPSE_PLUGIN, new Object());
        }

        return policyConfiguration.edit(policyEditArgs);
    }

    private void addClicked() {
        String[] availablePolicyTypeIDs;
        try {
            availablePolicyTypeIDs = policyLoader.getAvailablePolicyTypeIDs();
        } catch (final PolicyLoaderException e) {
            final IStatus status = new Status(
                IStatus.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                0,
                Messages.getString("DefinePoliciesControl.UnableToGetPolicyIds"), //$NON-NLS-1$
                e);
            TFSCommonUIClientPlugin.getDefault().getLog().log(status);

            MessageBoxHelpers.errorMessageBox(
                getShell(),
                null,
                Messages.getString("DefinePoliciesControl.LoadAvailPoliciesFailed")); //$NON-NLS-1$
            return;
        }

        final List<PolicyInstance> instanceList = new ArrayList<PolicyInstance>();
        boolean loadError = false;
        for (int i = 0; i < availablePolicyTypeIDs.length; i++) {
            try {
                final PolicyInstance instance = policyLoader.load(availablePolicyTypeIDs[i]);
                if (instance != null) {
                    instanceList.add(instance);
                } else {
                    loadError = true;
                }
            } catch (final PolicyLoaderException e) {
                loadError = true;

                final String messageFormat = Messages.getString("DefinePoliciesControl.UnableToLoadPolicyFormat"); //$NON-NLS-1$
                final String message = MessageFormat.format(messageFormat, availablePolicyTypeIDs[i]);

                final IStatus status = new Status(IStatus.ERROR, TFSCommonUIClientPlugin.PLUGIN_ID, 0, message, e);
                TFSCommonUIClientPlugin.getDefault().getLog().log(status);
            }
        }

        final PolicyInstance[] instances = instanceList.toArray(new PolicyInstance[instanceList.size()]);

        if (loadError && instances.length == 0) {
            MessageBoxHelpers.errorMessageBox(
                getShell(),
                null,
                Messages.getString("DefinePoliciesControl.AllPolicyLoadFailed")); //$NON-NLS-1$
            return;
        }

        if (instances.length == 0) {
            MessageBoxHelpers.messageBox(
                getShell(),
                null,
                Messages.getString("DefinePoliciesControl.NoPoliciesAvailable")); //$NON-NLS-1$
            return;
        }

        if (loadError && instances.length > 0) {
            MessageBoxHelpers.warningMessageBox(
                getShell(),
                null,
                Messages.getString("DefinePoliciesControl.AtLeastOnePolicyLoadFailed")); //$NON-NLS-1$
        }

        final PolicyInstanceDialog dialog = new PolicyInstanceDialog(
            getShell(),
            Messages.getString("DefinePoliciesControl.AddCheckinPolicyDialogTitle"), //$NON-NLS-1$
            instances);

        if (IDialogConstants.OK_ID != dialog.open()) {
            return;
        }

        final PolicyInstance instance = dialog.getSelectedPolicyInstance();
        final PolicyConfiguration policyConfiguration =
            PolicyConfiguration.configurationFor(instance, true, 0, new String[0]);

        if (policyConfiguration.canEdit()) {
            final boolean editSuccess = edit(policyConfiguration, true);
            if (!editSuccess) {
                return;
            }
        }

        policyConfigurations.add(policyConfiguration);
        refreshTable();
    }

    private void refreshTable() {
        policyTable.setPolicyConfigurations(getPolicyConfigurations());
    }

    private static class EditabilityValidator extends AbstractSelectionProviderValidator {
        public EditabilityValidator(final ISelectionProvider selectionProvider) {
            super(selectionProvider);
            validate();
        }

        @Override
        protected IValidity computeValidity(final ISelection selection) {
            if (selection instanceof IStructuredSelection) {
                final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
                if (structuredSelection.size() == 1) {
                    final PolicyConfiguration policyConfiguration =
                        (PolicyConfiguration) structuredSelection.getFirstElement();
                    if (policyConfiguration.canEdit()) {
                        return Validity.VALID;
                    }
                }
            }

            return Validity.INVALID;
        }
    }
}
