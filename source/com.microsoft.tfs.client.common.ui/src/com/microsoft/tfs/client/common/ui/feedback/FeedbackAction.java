// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.feedback;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;

import com.microsoft.tfs.client.common.ui.framework.action.ExtendedAction;

public class FeedbackAction extends ExtendedAction {

    private static final String CMD_SEND_SMILE = "sendSmile";//$NON-NLS-1$
    private static final String CMD_SEND_FROWN = "sendFrown";//$NON-NLS-1$

    private final String feedbackContext;
    private final boolean smile;

    public FeedbackAction(final String context, final boolean smile) {
        super();
        this.feedbackContext = context;
        this.smile = smile;
    }

    @Override
    public void doRun(final IAction action) {
        final FeedbackDialog dialog = new FeedbackDialog(getShell(), smile);
    }

}