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
 *         This class contains the build dependency wizard page UI for maven
 */
public class MavenBuildDependencyWizardPage extends WizardPage {
    private final IBuildDefinition buildDefinition;

    private BuildToolPicker javaPicker;
    private BuildToolPicker mavenPicker;

    private final static String[] JAVA_HELPERS = {
        Messages.getString("AntBuildDependencyWizardPage.JavaDefaultDescription"), //$NON-NLS-1$
        Messages.getString("AntBuildDependencyWizardPage.JavaLocalDescription"), //$NON-NLS-1$
        Messages.getString("AntBuildDependencyWizardPage.JavaZipDescription"), //$NON-NLS-1$
    };

    private final static String[] MAVEN_HELPERS = {
        Messages.getString("MavenBuildDependencyWizardPage.MavenDefaultDescription"), //$NON-NLS-1$
        Messages.getString("MavenBuildDependencyWizardPage.MavenLocalDescription"), //$NON-NLS-1$
        Messages.getString("MavenBuildDependencyWizardPage.MavenZipDescription"), //$NON-NLS-1$
    };

    public MavenBuildDependencyWizardPage(final IBuildDefinition buildDefinition) {
        super(
            "MavenBuildDependencyWizardPage", //$NON-NLS-1$
            Messages.getString("MavenBuildDependencyWizardPage.PageTitle"), //$NON-NLS-1$
            null);
        setDescription(Messages.getString("MavenBuildDependencyWizardPage.PageDescription")); //$NON-NLS-1$
        this.buildDefinition = buildDefinition;
    }

    @Override
    public void createControl(final Composite parent) {
        final Composite composite = SWTUtil.createComposite(parent);
        SWTUtil.gridLayout(composite, 1, false);
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(composite);
        setControl(composite);

        final String javaLabel = Messages.getString("AntBuildDependencyWizardPage.JavaLabel"); //$NON-NLS-1$
        final String mavenLabel = Messages.getString("MavenBuildDependencyWizardPage.MavenLabel"); //$NON-NLS-1$

        final int labelWidth = Math.max(javaLabel.length(), mavenLabel.length());
        javaPicker =
            new BuildToolPicker(composite, SWT.NONE, javaLabel, labelWidth, "Java", JAVA_HELPERS, buildDefinition); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(javaPicker);
        addListeners(javaPicker);

        mavenPicker = new BuildToolPicker(composite, SWT.NONE, mavenLabel, 0, "Maven", MAVEN_HELPERS, buildDefinition); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(mavenPicker);
        addListeners(mavenPicker);
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
        if (javaPicker.validate() && mavenPicker.validate()) {
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

    public String getMavenLocalPath() {
        return mavenPicker.getLocalPath();
    }

    public String getMavenServerPath() {
        return mavenPicker.getServerPath();
    }
}
