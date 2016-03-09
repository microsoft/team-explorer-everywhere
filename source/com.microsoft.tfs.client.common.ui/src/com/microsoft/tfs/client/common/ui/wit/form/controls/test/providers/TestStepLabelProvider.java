// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls.test.providers;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.microsoft.tfs.client.common.ui.wit.form.controls.test.SharedSteps;
import com.microsoft.tfs.client.common.ui.wit.form.controls.test.TestStep;
import com.microsoft.tfs.client.common.ui.wit.form.controls.test.TestStepUtil;
import com.microsoft.tfs.client.common.ui.wit.form.controls.test.TestStepsControl;

public class TestStepLabelProvider extends LabelProvider implements ITableLabelProvider {

    @Override
    public Image getColumnImage(final Object element, final int columnIndex) {
        if (columnIndex == 1) {
            if (TestStepUtil.isTestStep(element)) {
                final String type = ((TestStep) element).type;
                if (TestStepsControl.VALIDATE_STEP.equals(type)) {
                    return TestStepUtil.imageHelper.getImage("images/test/TestStepCheck.gif"); //$NON-NLS-1$
                }
                if (TestStepsControl.ACTION_STEP.equals(type)) {
                    return TestStepUtil.imageHelper.getImage("images/test/TestStep.gif"); //$NON-NLS-1$
                }
            }
            if (TestStepUtil.isSharedSteps(element)) {
                return TestStepUtil.imageHelper.getImage("images/test/TestSharedSteps.gif"); //$NON-NLS-1$
            }
        }
        return null;
    }

    @Override
    public String getColumnText(final Object element, final int columnIndex) {
        if (TestStepUtil.isTestStep(element)) {
            final TestStep step = (TestStep) element;
            switch (columnIndex) {
                case 0:
                    if (step.noOfAttachments > 0) {
                        return step.noOfAttachments + ""; //$NON-NLS-1$
                    } else {
                        return ""; //$NON-NLS-1$
                    }
                case 1:
                    if (step.parameterizedStrings.length > 0) {
                        return step.parameterizedStrings[0];
                    }
                case 2:
                    if (TestStepsControl.VALIDATE_STEP.equals(step.type) && step.parameterizedStrings.length > 1) {
                        return step.parameterizedStrings[1];
                    }
                    break;
                default:
                    break;
            }
        }

        if (TestStepUtil.isSharedSteps(element)) {
            if (columnIndex == 1) {
                return ((SharedSteps) element).title;
            }
        }
        return null;
    }

}