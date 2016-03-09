// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.sections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WebAccessHelper;

public class TeamExplorerSettingsCollectionSection extends TeamExplorerBaseSection {
    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        // Entire section is hidden if we don't have a security service.
        return context.isConnectedToCollection() && WebAccessHelper.hasSecurityService(context);
    }

    @Override
    public Composite getSectionContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);

        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        SWTUtil.gridLayout(composite, 1, true, 0, 5);
        Hyperlink link;

        // Security link.
        link = toolkit.createHyperlink(
            composite,
            Messages.getString("TeamExplorerSettingsCollectionSection.Security"), //$NON-NLS-1$
            SWT.WRAP);

        link.setUnderlined(false);
        link.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                WebAccessHelper.openCollectionSecurity(context);
            }
        });
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(link);

        // Group membership.
        link = toolkit.createHyperlink(
            composite,
            Messages.getString("TeamExplorerSettingsCollectionSection.GroupMembership"), //$NON-NLS-1$
            SWT.WRAP);

        link.setUnderlined(false);
        link.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                WebAccessHelper.openCollectionGroupMembership(context);
            }
        });
        GridDataBuilder.newInstance().hFill().hGrab().applyTo(link);

        return composite;
    }
}
