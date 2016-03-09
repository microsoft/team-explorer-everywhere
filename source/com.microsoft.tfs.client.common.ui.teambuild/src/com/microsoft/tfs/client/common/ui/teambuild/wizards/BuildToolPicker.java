// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.wizards;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.MessageBoxHelpers;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.vc.serveritem.ServerItemType;
import com.microsoft.tfs.core.clients.build.IBuildDefinition;
import com.microsoft.tfs.core.clients.versioncontrol.path.ServerPath;
import com.microsoft.tfs.util.Check;

/**
 *         The new UI composite for build dependency setting
 */
public class BuildToolPicker extends BaseControl {

    private Label label;
    private Combo pathTypeCombo;
    private Label helpLabel;
    private Text pathText;

    private Button selectButton;

    private final String buildToolName;
    private String localPath;
    private String serverPath;
    private int selectedIndex = -1;

    private final static String[] PATH_TYPES = {
        Messages.getString("BuildToolPicker.Default"), //$NON-NLS-1$
        Messages.getString("BuildToolPicker.Local"), //$NON-NLS-1$
        Messages.getString("BuildToolPicker.SourceControl"), //$NON-NLS-1$
    };

    private final String labelName;
    private final String[] helpers;
    private final IBuildDefinition buildDefinition;
    private final int labelWidth;
    private final boolean localDisabled;

    /**
     *
     * @param parent
     * @param style
     */
    public BuildToolPicker(
        final Composite parent,
        final int style,
        final String labelName,
        final int labelWidth,
        final String buildToolName,
        final String[] hints,
        final IBuildDefinition buildDefinition) {
        this(parent, style, labelName, labelWidth, buildToolName, hints, buildDefinition, false);
    }

    public BuildToolPicker(
        final Composite parent,
        final int style,
        final String labelName,
        final int labelWidth,
        final String buildToolName,
        final String[] hints,
        final IBuildDefinition buildDefinition,
        final boolean disableLocal) {
        super(parent, style);

        Check.notNull(labelName, "labelName"); //$NON-NLS-1$
        Check.notNull(hints, "hints");//$NON-NLS-1$
        Check.notNull(buildDefinition, "buildDefinition"); //$NON-NLS-1$
        Check.isTrue(hints.length == PATH_TYPES.length, "hints.length == PATH_TYPES.length"); //$NON-NLS-1$

        this.labelName = labelName;
        this.labelWidth = labelWidth;
        this.buildToolName = buildToolName;
        this.helpers = hints;
        this.buildDefinition = buildDefinition;
        this.localDisabled = disableLocal;
        createUI();
    }

    protected void createUI() {
        SWTUtil.gridLayout(this, 4, false);

        label = new Label(this, SWT.NONE);
        label.setText(labelName);
        if (labelWidth > 0) {
            GridDataBuilder.newInstance().vAlign(SWT.CENTER).wCHint(label, labelWidth).applyTo(label);
        } else {
            GridDataBuilder.newInstance().vAlign(SWT.CENTER).applyTo(label);
        }

        pathTypeCombo = new Combo(this, SWT.READ_ONLY);
        GridDataBuilder.newInstance().hFill().vAlign(SWT.CENTER).applyTo(pathTypeCombo);
        selectedIndex = 0;
        updateCombo(pathTypeCombo);

        pathText = new Text(this, SWT.BORDER);
        GridDataBuilder.newInstance().hAlign(SWT.FILL).hGrab().vAlign(SWT.CENTER).applyTo(pathText);

        selectButton = new Button(this, SWT.NONE);
        selectButton.setText(Messages.getString("BuildToolPicker.SelectButtonLabel")); //$NON-NLS-1$
        GridDataBuilder.newInstance().vAlign(SWT.CENTER).applyTo(selectButton);

        selectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                browseOnServer(((Button) e.widget).getShell());
            }
        });

        SWTUtil.createHorizontalGridLayoutSpacer(this, 1);

        helpLabel = new Label(this, SWT.WRAP);
        GridDataBuilder.newInstance().hSpan(2).hFill().hGrab().wHint(
            IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH).applyTo(helpLabel);

        pathTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectedIndex = pathTypeCombo.getSelectionIndex();
                processSelectedLinkType();
            }
        });

        processSelectedLinkType();
    }

    protected void processSelectedLinkType() {
        helpLabel.setText(helpers[selectedIndex]);

        if (selectedIndex == 0 || selectedIndex == 1) {
            selectButton.setVisible(false);
            ((GridData) pathText.getLayoutData()).horizontalSpan = 2;
            ((GridData) pathText.getLayoutData()).grabExcessHorizontalSpace = true;
            ((GridData) selectButton.getLayoutData()).exclude = true;
            pathText.setEnabled(!localDisabled && selectedIndex == 1);
        } else {
            selectButton.setVisible(true);
            ((GridData) pathText.getLayoutData()).horizontalSpan = 1;
            ((GridData) pathText.getLayoutData()).grabExcessHorizontalSpace = true;
            ((GridData) selectButton.getLayoutData()).exclude = false;
            pathText.setEnabled(true);
        }

        layout();
    }

    public void addSelectionListener(final SelectionAdapter adapter) {
        pathTypeCombo.addSelectionListener(adapter);
        selectButton.addSelectionListener(adapter);
    }

    public void addTextModifyListener(final ModifyListener listener) {
        pathText.addModifyListener(listener);
    }

    protected void browseOnServer(final Shell shell) {
        final String startingItem = ServerPath.isServerPath(pathText.getText()) ? pathText.getText()
            : ServerPath.combine(ServerPath.ROOT, buildDefinition.getTeamProject());

        final SelectArchiveOnServerDialog selectArchiveDialog = new SelectArchiveOnServerDialog(
            shell,
            MessageFormat.format(Messages.getString("BuildToolPicker.SelectDialogTitleFormat"), buildToolName), //$NON-NLS-1$
            buildToolName,
            startingItem,
            ServerItemType.ALL,
            buildDefinition);

        if (selectArchiveDialog.open() == IDialogConstants.OK_ID) {
            if (!ServerItemType.isFile(selectArchiveDialog.getSelectedItem().getType())) {
                MessageBoxHelpers.warningMessageBox(
                    getShell(),
                    Messages.getString("BuildToolPicker.WarningMessage"), //$NON-NLS-1$
                    Messages.getString("BuildToolPicker.WarningMessageText")); //$NON-NLS-1$
            } else {
                serverPath = selectArchiveDialog.getServerPath();
                pathText.setText(serverPath);
            }
        }
    }

    public String getLocalPath() {
        if (selectedIndex == 1) {
            localPath = pathText.getText().trim();
            return localPath;
        } else {
            return null;
        }
    }

    public String getServerPath() {
        if (selectedIndex == 2) {
            serverPath = pathText.getText().trim();
            return serverPath;
        } else {
            return null;
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    private void updateCombo(final Combo combo) {
        for (final String pathType : PATH_TYPES) {
            combo.add(pathType);
        }
        combo.select(0);
    }

    public boolean validate() {
        if (selectedIndex == 0) {
            return true;
        } else {
            return pathText.getText() != null && pathText.getText().trim().length() > 0;
        }
    }
}
