// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.console;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;

public class TFSMessageConsolePageParticipant implements IConsolePageParticipant {
    private final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

    private ShowConsoleMenuAction menuAction;

    private ShowConsoleAction messageAction;
    private ShowConsoleAction warningAction;
    private ShowConsoleAction errorAction;

    @Override
    public void init(final IPageBookViewPage page, final IConsole console) {
        menuAction = new ShowConsoleMenuAction(
            Messages.getString("TFSMessageConsolePageParticipant.ShowConsoleMenu"), //$NON-NLS-1$
            imageHelper.getImageDescriptor("/images/common/console_showmessages.gif")); //$NON-NLS-1$

        messageAction =
            new ShowConsoleAction(
                Messages.getString("TFSMessageConsolePageParticipant.ShowConsoleForNewMessage"), //$NON-NLS-1$
                UIPreferenceConstants.CONSOLE_SHOW_ON_NEW_MESSAGE,
                null);

        warningAction =
            new ShowConsoleAction(
                Messages.getString("TFSMessageConsolePageParticipant.ShowConsoleForNewWarning"), //$NON-NLS-1$
                UIPreferenceConstants.CONSOLE_SHOW_ON_NEW_WARNING,
                null);

        errorAction =
            new ShowConsoleAction(
                Messages.getString("TFSMessageConsolePageParticipant.ShowConsoleForNewError"), //$NON-NLS-1$
                UIPreferenceConstants.CONSOLE_SHOW_ON_NEW_ERROR,
                null);

        // contribute to menubar
        menuAction.addAction(messageAction);
        menuAction.addAction(warningAction);
        menuAction.addAction(errorAction);

        // contribute to toolbar
        final IActionBars actionBars = page.getSite().getActionBars();

        actionBars.getToolBarManager().appendToGroup(IConsoleConstants.OUTPUT_GROUP, menuAction);
    }

    @Override
    public void activated() {
    }

    @Override
    public void deactivated() {
    }

    @Override
    public Object getAdapter(final Class adapter) {
        return null;
    }

    @Override
    public void dispose() {
    }
}
