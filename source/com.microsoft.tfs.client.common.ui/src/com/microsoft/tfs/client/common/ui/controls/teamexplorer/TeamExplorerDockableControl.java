// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.teamexplorer;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerPageConfig;

/**
 * This is core class to create the dockable control UI for different pageID.
 */
public class TeamExplorerDockableControl extends TeamExplorerBaseControl {
    private static final Log log = LogFactory.getLog(TeamExplorerDockableControl.class);
    // reuse the page ID here to create equivalent UI of TE page
    private final String viewID;
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    public TeamExplorerDockableControl(
        final String viewID,
        final TeamExplorerConfig config,
        final TeamExplorerContext context,
        final Composite parent,
        final int style) {
        super(config, context, parent, style);
        this.viewID = viewID;
        createView();
    }

    private void createView() {
        if (pageComposite != null && !pageComposite.isDisposed()) {
            return;
        }

        pageComposite = toolkit.createComposite(subForm.getBody());
        GridDataBuilder.newInstance().fill().grab().applyTo(pageComposite);
        SWTUtil.gridLayout(pageComposite);

        final TeamExplorerPageConfig page = configuration.getPage(viewID);
        createPageContent(pageComposite, page, configuration.getPageSections(page.getID()));
        form.setText(page.getTitle());
        form.setMessage(getProjectAndTeamText(), IMessageProvider.NONE);
    }

    private void disposeView() {
        if (pageComposite == null || pageComposite.isDisposed()) {
            return;
        }

        final Control[] controls = pageComposite.getChildren();

        for (final Control control : controls) {
            control.dispose();
        }

        pageComposite.dispose();
        pageComposite = null;
    }

    @Override
    public void refreshView() {
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                if (!refreshing.getAndSet(true)) {
                    disposeView();
                    createView();

                    refreshing.set(false);
                } else {
                    log.info("refreshView: " + viewID + " is already refreshing"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        });
    }
}
