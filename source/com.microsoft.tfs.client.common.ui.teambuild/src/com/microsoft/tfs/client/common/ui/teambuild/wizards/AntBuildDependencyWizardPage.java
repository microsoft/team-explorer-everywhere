// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

/**
 *         This class contains the build dependency wizard page UI
 */
public class AntBuildDependencyWizardPage extends WizardPage {
    private final IBuildDefinition buildDefinition;

    private BuildToolPicker javaPicker;
    private BuildToolPicker antPicker;

    private final static String[] JAVA_HELPERS = {
        Messages.getString("AntBuildDependencyWizardPage.JavaDefaultDescription"), //$NON-NLS-1$
        Messages.getString("AntBuildDependencyWizardPage.JavaLocalDescription"), //$NON-NLS-1$
        Messages.getString("AntBuildDependencyWizardPage.JavaZipDescription"), //$NON-NLS-1$
    };

    private final static String[] ANT_HELPERS = {
        Messages.getString("AntBuildDependencyWizardPage.AntDefaultDescription"), //$NON-NLS-1$
        Messages.getString("AntBuildDependencyWizardPage.AntLocalDescription"), //$NON-NLS-1$
        Messages.getString("AntBuildDependencyWizardPage.AntZipDescription"), //$NON-NLS-1$
    };

    public AntBuildDependencyWizardPage(final IBuildDefinition buildDefinition) {
        super(
            "AntBuildDependencyWizardPage", //$NON-NLS-1$
            Messages.getString("AntBuildDependencyWizardPage.PageTitle"), //$NON-NLS-1$
            null);
        setDescription(Messages.getString("AntBuildDependencyWizardPage.PageDescription")); //$NON-NLS-1$
        this.buildDefinition = buildDefinition;
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);
        SWTUtil.gridLayout(composite, 1, false);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(composite);
        setControl(composite);

        final String javaLabel = Messages.getString("AntBuildDependencyWizardPage.JavaLabel"); //$NON-NLS-1$
        final String antLabel = Messages.getString("AntBuildDependencyWizardPage.AntLabel"); //$NON-NLS-1$

        final int labelWidth = Math.max(javaLabel.length(), antLabel.length());
        javaPicker =
            new BuildToolPicker(composite, SWT.NONE, javaLabel, labelWidth, "Java", JAVA_HELPERS, buildDefinition); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(javaPicker);
        addListeners(javaPicker);

        antPicker = new BuildToolPicker(composite, SWT.NONE, antLabel, labelWidth, "Ant", ANT_HELPERS, buildDefinition); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(antPicker);
        addListeners(antPicker);
    }

    private void addListeners(final BuildToolPicker picker) {
        picker.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                toggleFinishButton();
            }
        });

        picker.addTextModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                toggleFinishButton();
            }
        });
    }

    protected void toggleFinishButton() {
        if (javaPicker.validate() && antPicker.validate()) {
            setPageComplete(true);
        } else {
            setPageComplete(false);
        }
    }

    public String getJavaLocalPath() {
        return javaPicker.getLocalPath();
    }

    public String getJavaServerPath() {
        return javaPicker.getServerPath();
    }

    public String getAntLocalPath() {
        return antPicker.getLocalPath();
    }

    public String getAntServerPath() {
        return antPicker.getServerPath();
    }
}
