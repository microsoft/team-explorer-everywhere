// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.build.IBuildServer;

public class RetentionPolicyTabPage extends BuildDefinitionTabPage {

    private RetentionPolicyControl control;

    /**
     * @param buildDefinition
     */
    public RetentionPolicyTabPage(final IBuildDefinition buildDefinition) {
        super(buildDefinition);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.teambuild.controls.ToolStripTabPage
     * #createControl(org .eclipse.swt.widgets.Composite)
     */
    @Override
    public Control createControl(final Composite parent) {
        control = new RetentionPolicyControl(parent, SWT.NONE, getBuildDefinition().getBuildServer());
        populate();
        return control;
    }

    private void populate() {
        control.getTable().setRetentionPolicies(getBuildDefinition().getRetentionPolicies());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.teambuild.controls.ToolStripTabPage
     * #getName()
     */
    @Override
    public String getName() {
        return Messages.getString("RetentionPolicyTabPage.TabLabelText"); //$NON-NLS-1$
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.microsoft.tfs.client.common.ui.teambuild.controls.ToolStripTabPage
     * #isValid()
     */
    @Override
    public boolean isValid() {
        return true;
    }

    public class RetentionPolicyControl extends BaseControl {
        private final IBuildServer buildServer;
        private RetentionPolicyTableControl table;

        public RetentionPolicyControl(final Composite parent, final int style, final IBuildServer buildServer) {
            super(parent, style);
            this.buildServer = buildServer;
            createControls(this);
        }

        private void createControls(final Composite composite) {
            final GridLayout layout = SWTUtil.gridLayout(composite, 1);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.horizontalSpacing = getHorizontalSpacing();
            layout.verticalSpacing = getVerticalSpacing();

            SWTUtil.createLabel(composite, Messages.getString("RetentionPolicyTabPage.SummaryLabelText")); //$NON-NLS-1$

            table = new RetentionPolicyTableControl(composite, SWT.SINGLE | SWT.FULL_SELECTION, buildServer);
            GridDataBuilder.newInstance().fill().grab().applyTo(table);

            final Label noteLabel =
                SWTUtil.createLabel(composite, SWT.WRAP, Messages.getString("RetentionPolicyTabPage.NoteLabelText")); //$NON-NLS-1$

            GridDataBuilder.newInstance().fill().hGrab().vIndent(getVerticalSpacing()).applyTo(noteLabel);
            ControlSize.setCharWidthHint(noteLabel, 42);
        }

        public RetentionPolicyTableControl getTable() {
            return table;
        }
    }

}
