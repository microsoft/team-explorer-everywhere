// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;

/**
 * Read-only implementation of the AssociatedAutomationControl introduced in TFS
 * 2010
 */
public class AssociatedAutomationControl extends LabelableControl {
    ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    @Override
    protected void createControl(final Composite parent, final int columnsToTake) {
        final String testType = getFieldDataAsString("Microsoft.VSTS.TCM.AutomatedTestType"); //$NON-NLS-1$
        final String testName = getFieldDataAsString("Microsoft.VSTS.TCM.AutomatedTestName"); //$NON-NLS-1$
        final String testStorage = getFieldDataAsString("Microsoft.VSTS.TCM.AutomatedTestStorage"); //$NON-NLS-1$

        final Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, columnsToTake, 1));

        final GridLayout layout = new GridLayout(3, false);
        composite.setLayout(layout);

        getDebuggingContext().debug(composite, getFormElement());
        getDebuggingContext().setupGridLayout(layout);

        final Label testNameLabel = SWTUtil.createLabel(
            composite,
            Messages.getString("AssociatedAutomationControl.AutomatedTestnameLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(testNameLabel);

        SWTUtil.createLabel(composite, getTestImage(testStorage, testType));
        final Text testNameText = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
        GridDataBuilder.newInstance().fill().hGrab().applyTo(testNameText);
        testNameText.setText(testName);

        final Button testNameButton =
            SWTUtil.createButton(composite, Messages.getString("AssociatedAutomationControl.TestNameButtonText")); //$NON-NLS-1$
        testNameButton.setEnabled(false);
        GridDataBuilder.newInstance().fill().applyTo(testNameButton);

        final Label testStorageLabel =
            SWTUtil.createLabel(composite, Messages.getString("AssociatedAutomationControl.TestStorageLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(testStorageLabel);

        final Text testStorageText = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(testStorageText);
        testStorageText.setText(testStorage);

        final Label testTypeLabel =
            SWTUtil.createLabel(composite, Messages.getString("AssociatedAutomationControl.TestTypeLabelText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(testTypeLabel);

        final Text testTypeText = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().applyTo(testTypeText);
        testTypeText.setText(testType);

        final Button removeButton =
            SWTUtil.createButton(composite, Messages.getString("AssociatedAutomationControl.RemoveButtonText")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).hAlign(SWT.RIGHT).applyTo(removeButton);
        removeButton.setEnabled(false);

    }

    private Image getTestImage(final String storage, final String typeName) {
        if (storage != null && storage.length() > 0) {
            if (storage.toLowerCase(Locale.ENGLISH).endsWith(".webtest") || typeName.indexOf("Web Test") >= 0) //$NON-NLS-1$ //$NON-NLS-2$
            {
                return imageHelper.getImage("images/test/test_web.gif"); //$NON-NLS-1$
            }
            if (typeName.indexOf("Unit Test") >= 0) //$NON-NLS-1$
            {
                return imageHelper.getImage("images/test/test_unit.gif"); //$NON-NLS-1$
            }
        }
        return imageHelper.getImage("images/test/test.gif"); //$NON-NLS-1$
    }

    @Override
    protected int getControlColumns() {
        return 1;
    }

}
