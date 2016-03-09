// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.checkinpolicies.build.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.checkinpolicies.build.Messages;
import com.microsoft.tfs.checkinpolicies.build.settings.Area;
import com.microsoft.tfs.checkinpolicies.build.settings.BuildPolicyConfiguration;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.util.Check;

public class GeneralTabControl extends BaseControl {
    private final BuildPolicyConfiguration configuration;

    private final Button fileButton;
    private final Button projectButton;
    private final Button workspaceButton;

    private static class ClickHandler extends SelectionAdapter {
        private final GeneralTabControl control;

        private ClickHandler(final GeneralTabControl control) {
            this.control = control;
        }

        @Override
        public void widgetSelected(final SelectionEvent e) {
            control.configuration.setArea((Area) ((Button) e.getSource()).getData());
        }
    }

    public GeneralTabControl(final Composite parent, final int style, final BuildPolicyConfiguration configuration) {
        super(parent, style);

        Check.notNull(configuration, "configuration"); //$NON-NLS-1$
        this.configuration = configuration;

        SWTUtil.gridLayout(this);

        final Label label =
            SWTUtil.createLabel(this, SWT.WRAP, Messages.getString("GeneralTabControl.SummaryLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hFill().hGrab().wHint(getMinimumMessageAreaWidth()).applyTo(label);

        final Composite groupComposite = new Composite(this, SWT.NONE);
        SWTUtil.gridLayout(groupComposite);

        GridDataBuilder.newInstance().hFill().hGrab().applyTo(groupComposite);

        final ClickHandler clickHandler = new ClickHandler(this);

        fileButton =
            SWTUtil.createButton(groupComposite, SWT.RADIO, Messages.getString("GeneralTabControl.FileButtonText")); //$NON-NLS-1$
        fileButton.setData(Area.FILE);
        fileButton.addSelectionListener(clickHandler);

        projectButton =
            SWTUtil.createButton(groupComposite, SWT.RADIO, Messages.getString("GeneralTabControl.ProjectButtonText")); //$NON-NLS-1$
        projectButton.setData(Area.PROJECT);
        projectButton.addSelectionListener(clickHandler);

        workspaceButton = SWTUtil.createButton(
            groupComposite,
            SWT.RADIO,
            Messages.getString("GeneralTabControl.WorkspaceButtonText")); //$NON-NLS-1$
        workspaceButton.setData(Area.WORKSPACE);
        workspaceButton.addSelectionListener(clickHandler);

        refreshControls();
    }

    /**
     * Refreshes controls from data in this class's model (private fields).
     */
    private void refreshControls() {
        final Area selectedArea = configuration.getArea();

        final Button[] buttons = new Button[] {
            fileButton,
            projectButton,
            workspaceButton
        };

        for (int i = 0; i < buttons.length; i++) {
            if (((Area) buttons[i].getData()) == selectedArea) {
                buttons[i].setSelection(true);
            } else {
                buttons[i].setSelection(false);
            }
        }
    }
}
