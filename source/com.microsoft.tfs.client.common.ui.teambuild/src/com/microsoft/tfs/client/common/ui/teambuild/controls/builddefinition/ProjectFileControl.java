// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teambuild.controls.builddefinition;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.ButtonHelper;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teambuild.Messages;
import com.microsoft.tfs.client.common.ui.teambuild.TFSTeamBuildPlugin;
import com.microsoft.tfs.core.product.ProductInformation;

public abstract class ProjectFileControl extends BaseControl {
    protected final String BUILD_FILE_EXISTS = Messages.getString("ProjectFileTabPage.BuildFileExists"); //$NON-NLS-1$
    protected final String WF_BUILD = Messages.getString("ProjectFileTabPage.WindowWorkFlowBasedTemplete"); //$NON-NLS-1$

    protected Text configFolderText;
    protected Label buildFileLocationInfoImage;
    protected Label buildFileLocationInfo;
    protected final ImageHelper imageHelper = new ImageHelper(TFSTeamBuildPlugin.PLUGIN_ID);
    protected final Image warningImage;
    protected final Image infoImage;
    protected Button createButton;
    protected Button browseButton;
    protected boolean fileExists;
    protected boolean isValidServerPath;

    public ProjectFileControl(final Composite parent, final int style) {
        super(parent, style);
        warningImage = imageHelper.getImage("icons/warning.gif"); //$NON-NLS-1$
        infoImage = imageHelper.getImage("icons/info.gif"); //$NON-NLS-1$
        createControls(this);
    }

    protected void createControls(final Composite composite) {
        final GridLayout layout = SWTUtil.gridLayout(composite, 3, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();

        final String messageFormat = getSummaryLabelTextFormat();
        final String message =
            MessageFormat.format(messageFormat, ProductInformation.getCurrent().getFamilyShortName());

        final Label summary = SWTUtil.createLabel(composite, SWT.WRAP, message);
        GridDataBuilder.newInstance().hGrab().fill().hSpan(layout).hHint(40).applyTo(summary);

        final Label configFolderLabel = SWTUtil.createLabel(composite, getConfigFolderLabelText());
        GridDataBuilder.newInstance().hGrab().fill().hSpan(layout).vIndent(getVerticalSpacing()).applyTo(
            configFolderLabel);

        configFolderText = new Text(composite, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).applyTo(configFolderText);

        browseButton = SWTUtil.createButton(composite, Messages.getString("ProjectFileTabPage.BrowseButtonText")); //$NON-NLS-1$
        ButtonHelper.setButtonToButtonBarSize(browseButton);
        GridDataBuilder.newInstance().fill().applyTo(browseButton);

        buildFileLocationInfoImage = SWTUtil.createLabel(composite, warningImage);
        GridDataBuilder.newInstance().vAlign(SWT.TOP).applyTo(buildFileLocationInfoImage);

        buildFileLocationInfo = new Label(composite, SWT.WRAP);
        GridDataBuilder.newInstance().hFill().vGrab().vAlign(SWT.TOP).wHint(50).applyTo(buildFileLocationInfo);

        createButton = SWTUtil.createButton(composite, Messages.getString("ProjectFileTabPage.CreateButtonText")); //$NON-NLS-1$
        ButtonHelper.setButtonToButtonBarSize(createButton);
        GridDataBuilder.newInstance().hFill().vAlign(SWT.TOP).applyTo(createButton);

        configFolderText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                validate();
            }
        });
    }

    /**
     * @return
     */
    public Text getConfigFolderText() {
        return configFolderText;
    }

    /**
     * @return
     */
    public Label getBuildFileLocationInfoImage() {
        return buildFileLocationInfoImage;
    }

    /**
     * @return
     */
    public Label getBuildFileLocationInfo() {
        return buildFileLocationInfo;
    }

    public boolean getProjectFileExists() {
        return fileExists;
    }

    public void clearProjectFileStatus() {
        fileExists = false;
    }

    public void setProjectFileExists(final boolean exists) {
        fileExists = exists;

        if (exists) {
            buildFileLocationInfoImage.setImage(infoImage);
            buildFileLocationInfo.setText(getConfigFolderText().isEnabled() ? BUILD_FILE_EXISTS : WF_BUILD);
        } else {
            buildFileLocationInfoImage.setImage(warningImage);
            buildFileLocationInfo.setText(getBuildFileWarningMessage());
        }

        createButton.setEnabled(canCreatProject());
        layout();
    }

    protected boolean canCreatProject() {
        return !fileExists;
    }

    public Button getCreateButton() {
        return createButton;
    }

    public Button getBrowseButton() {
        return browseButton;
    }

    /**
     * used for GitProjectFileControl only
     *
     *
     */
    public void setLocalPath(final String path) {
    }

    protected void validate() {
        createButton.setEnabled(isValidServerPath);
        buildFileLocationInfo.setText(getBuildFileWarningMessage());
        layout();
    }

    public boolean isServerPathValid() {
        return isValidServerPath;
    }

    protected abstract String getBuildFileWarningMessage();

    protected abstract String getSummaryLabelTextFormat();

    protected abstract String getConfigFolderLabelText();
}