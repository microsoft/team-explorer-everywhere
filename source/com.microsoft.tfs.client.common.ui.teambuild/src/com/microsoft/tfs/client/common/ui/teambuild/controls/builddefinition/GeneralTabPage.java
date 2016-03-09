// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;

public class GeneralTabPage extends BuildDefinitionTabPage {

    private GeneralPropertiesControl control = null;

    public GeneralTabPage(final IBuildDefinition buildDefinition) {
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
        if (control == null) {
            control = new GeneralPropertiesControl(parent, SWT.NONE);

            populateControl();
        }
        return control;
    }

    public GeneralPropertiesControl getControl() {
        return control;
    }

    private void populateControl() {
        if (getBuildDefinition().getName() != null) {
            getControl().getNameText().setText(getBuildDefinition().getName());
        }
        if (getBuildDefinition().getDescription() != null) {
            getControl().getDescText().setText(getBuildDefinition().getDescription());
        }

        getControl().getDisableButton().setSelection(!getBuildDefinition().isEnabled());
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
        return Messages.getString("GeneralTabPage.TabLabelText"); //$NON-NLS-1$
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
        return control.isValid();
    }

    public class GeneralPropertiesControl extends BaseControl {
        private Text nameText;
        private Text descText;
        private Button disableButton;

        public GeneralPropertiesControl(final Composite parent, final int style) {
            super(parent, style);
            createControls(this);
        }

        private void createControls(final Composite composite) {
            final GridLayout layout = SWTUtil.gridLayout(composite, 1);
            layout.marginWidth = 0;
            layout.marginHeight = 0;
            layout.horizontalSpacing = getHorizontalSpacing();
            layout.verticalSpacing = getVerticalSpacing();

            SWTUtil.createLabel(composite, Messages.getString("GeneralTabPage.BuildNameLabelText")); //$NON-NLS-1$

            nameText = new Text(composite, SWT.BORDER);
            GridDataBuilder.newInstance().fill().applyTo(nameText);

            SWTUtil.createLabel(composite, Messages.getString("GeneralTabPage.DescriptionLabelText")); //$NON-NLS-1$

            descText = new Text(composite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
            descText.addTraverseListener(new TraverseListener() {
                @Override
                public void keyTraversed(final TraverseEvent e) {
                    if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                        e.doit = true;
                    }
                }
            });
            GridDataBuilder.newInstance().fill().grab().applyTo(descText);

            disableButton =
                SWTUtil.createButton(composite, SWT.CHECK, Messages.getString("GeneralTabPage.DisableButtonText")); //$NON-NLS-1$

        }

        public boolean isValid() {
            if (nameText == null || descText == null || disableButton == null) {
                throw new IllegalStateException(
                    "Attempt to perform validation on General page before control was created"); //$NON-NLS-1$
            }

            return nameText.getText().trim().length() > 0;
        }

        /**
         * @return
         */
        public Text getNameText() {
            return nameText;
        }

        /**
         * @return
         */
        public Text getDescText() {
            return descText;
        }

        /**
         * @return
         */
        public Button getDisableButton() {
            return disableButton;
        }
    }

}
