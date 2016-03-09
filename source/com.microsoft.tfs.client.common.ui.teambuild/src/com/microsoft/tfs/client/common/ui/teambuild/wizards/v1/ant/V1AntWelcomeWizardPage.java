// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards.v1.ant;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

public class V1AntWelcomeWizardPage extends WizardPage {

    private Text nameText;
    private Text descriptionText;

    /**
     * @param pageName
     */
    public V1AntWelcomeWizardPage(final IBuildDefinition buildDefinition) {
        super("v1WelcomePage", Messages.getString("V1AntWelcomeWizardPage.PageTitle"), null); //$NON-NLS-1$ //$NON-NLS-2$
        setDescription(Messages.getString("V1AntWelcomeWizardPage.PageDescription")); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.
     * widgets .Composite)
     */
    @Override
    public void createControl(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);
        SWTUtil.gridLayout(composite, 1);

        SWTUtil.createLabel(composite, Messages.getString("V1AntWelcomeWizardPage.BuildDefinitionNameLabelText")); //$NON-NLS-1$
        nameText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(nameText);
        nameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                updatePageComplete();
            }
        });

        SWTUtil.createLabel(composite, Messages.getString("V1AntWelcomeWizardPage.DescriptionLabelText")); //$NON-NLS-1$
        descriptionText = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        descriptionText.addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(final TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    e.doit = true;
                }
            }
        });

        GridDataBuilder.newInstance().fill().grab().applyTo(descriptionText);

        setPageComplete(false);
        setControl(composite);
    }

    private void updatePageComplete() {

        if (nameText != null) {
            final boolean hasName = nameText.getText().trim().length() > 0;
            setPageComplete(hasName);
            if (!hasName) {
                setErrorMessage(Messages.getString("V1AntWelcomeWizardPage.PleaseEnterNameForBuildDefinition")); //$NON-NLS-1$
                return;
            }
        }

        setPageComplete(true);
        setErrorMessage(null);
        setMessage(null);
    }

    public String getBuildDefinitionName() {
        return nameText.getText().trim();
    }

    public String getBuildDefinitionDescription() {
        return descriptionText.getText().trim();
    }
}
