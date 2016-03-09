// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards.v1;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;

public class LocationWizardPage extends WizardPage {

    private Text buildAgentText;
    private Text buildDirectoryText;
    private Text dropLocationText;

    public LocationWizardPage() {
        super("v1LocationPage", Messages.getString("LocationWizardPage.PageTitle"), null); //$NON-NLS-1$ //$NON-NLS-2$
        setDescription(Messages.getString("LocationWizardPage.PageDescription")); //$NON-NLS-1$
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);
        SWTUtil.gridLayout(composite, 1);

        SWTUtil.createLabel(composite, Messages.getString("LocationWizardPage.BuildMachineLabelText")); //$NON-NLS-1$
        buildAgentText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(buildAgentText);

        SWTUtil.createLabel(composite, ""); //$NON-NLS-1$

        SWTUtil.createLabel(composite, Messages.getString("LocationWizardPage.BuildDirectoryLabelText")); //$NON-NLS-1$
        buildDirectoryText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(buildDirectoryText);
        final Label dirExampleLabel =
            SWTUtil.createLabel(composite, Messages.getString("LocationWizardPage.ExampleDirLabelText")); //$NON-NLS-1$
        dirExampleLabel.setEnabled(false);

        SWTUtil.createLabel(composite, ""); //$NON-NLS-1$

        SWTUtil.createLabel(composite, Messages.getString("LocationWizardPage.DropLocationLabelText")); //$NON-NLS-1$
        dropLocationText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(dropLocationText);
        final Label dropExampleLabel =
            SWTUtil.createLabel(composite, Messages.getString("LocationWizardPage.ExampleDropLabelText")); //$NON-NLS-1$
        dropExampleLabel.setEnabled(false);

        final ModifyListener validationListener = new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                updatePageComplete();
            }
        };

        buildAgentText.addModifyListener(validationListener);
        buildDirectoryText.addModifyListener(validationListener);
        dropLocationText.addModifyListener(validationListener);

        updatePageComplete();
        setControl(composite);
    }

    private void updatePageComplete() {
        if (buildAgentText == null || buildDirectoryText == null || dropLocationText == null) {
            return;
        }

        final String buildAgent = buildAgentText.getText().trim();
        final String buildDirectory = buildDirectoryText.getText().trim();
        final String dropLocation = dropLocationText.getText().trim();

        if (dropLocation.length() > 0 && !(dropLocation.startsWith("\\\\") //$NON-NLS-1$
            && dropLocation.lastIndexOf('\\') >= 3
            && dropLocation.length() > dropLocation.lastIndexOf('\\') + 1)) {
            setErrorMessage(Messages.getString("LocationWizardPage.DropLocationMustBeWindowsServerPath")); //$NON-NLS-1$
            setPageComplete(false);
            return;
        }

        setPageComplete((buildAgent.length() > 0) && (buildDirectory.length() > 0) && (dropLocation.length() > 0));
        setErrorMessage(null);
        setMessage(null);
    }

    public String getBuildAgent() {
        return buildAgentText.getText().trim();
    }

    public String getBuildDirectory() {
        return buildDirectoryText.getText().trim();
    }

    public String getDropLocation() {
        return dropLocationText.getText().trim();
    }
}
