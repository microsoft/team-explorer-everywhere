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
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.AreasAndIterationsHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.CheckinPolicyHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WebAccessHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;

public class TeamExplorerSettingsProjectSection extends TeamExplorerBaseSection {
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

        if (WebAccessHelper.hasSecurityService(context)) {
            // Security link.
            link =
                toolkit.createHyperlink(
                    composite,
                    Messages.getString("TeamExplorerSettingsCollectionSection.Security"), //$NON-NLS-1$
                    SWT.WRAP);

            link.setUnderlined(false);
            link.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                public void linkActivated(final HyperlinkEvent e) {
                    WebAccessHelper.openProjectSecurity(context);
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
                    WebAccessHelper.openProjectGroupMembership(context);
                }
            });
            GridDataBuilder.newInstance().hFill().hGrab().applyTo(link);
        }

        // TFVC Check-in Policies.
        if (TeamExplorerHelpers.supportsTfvc(context)) {
            link = toolkit.createHyperlink(
                composite,
                Messages.getString("TeamExplorerSettingsCollectionSection.SourceControl"), //$NON-NLS-1$
                SWT.WRAP);

            link.setUnderlined(false);
            link.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                public void linkActivated(final HyperlinkEvent e) {
                    CheckinPolicyHelper.showCheckinPolicyDialog(composite.getShell(), context);
                }
            });
            GridDataBuilder.newInstance().hFill().hGrab().applyTo(link);
        }

        if (WebAccessHelper.hasWorkItemAreasService(context)) {
            // Work item areas.
            link = toolkit.createHyperlink(
                composite,
                Messages.getString("TeamExplorerSettingsProjectSection.WorkItemAreas"), //$NON-NLS-1$
                SWT.WRAP);

            link.setUnderlined(false);
            link.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                public void linkActivated(final HyperlinkEvent e) {
                    WebAccessHelper.openProjectWorkItemAreas(context);
                }
            });
            GridDataBuilder.newInstance().hFill().hGrab().applyTo(link);

            // Work item iterations.
            link = toolkit.createHyperlink(
                composite,
                Messages.getString("TeamExplorerSettingsProjectSection.WorkItemIterations"), //$NON-NLS-1$
                SWT.WRAP);

            link.setUnderlined(false);
            link.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                public void linkActivated(final HyperlinkEvent e) {
                    WebAccessHelper.openProjectWorkItemIterations(context);
                }
            });
            GridDataBuilder.newInstance().hFill().hGrab().applyTo(link);
        } else {
            // Show legacy dialog for work item areas and iterations.
            link = toolkit.createHyperlink(
                composite,
                Messages.getString("TeamExplorerSettingsProjectSection.AreasAndIterationsLinkText"), //$NON-NLS-1$
                SWT.WRAP);

            link.setUnderlined(false);
            link.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                public void linkActivated(final HyperlinkEvent e) {
                    AreasAndIterationsHelper.showAreasAndIterationsDialog(composite.getShell(), context);
                }
            });
            GridDataBuilder.newInstance().hFill().hGrab().applyTo(link);
        }

        if (WebAccessHelper.hasProjectAlertsService(context)) {
            // Project alerts.
            link = toolkit.createHyperlink(
                composite,
                Messages.getString("TeamExplorerSettingsProjectSection.ProjectAlerts"), //$NON-NLS-1$
                SWT.WRAP);

            link.setUnderlined(false);
            link.addHyperlinkListener(new HyperlinkAdapter() {
                @Override
                public void linkActivated(final HyperlinkEvent e) {
                    WebAccessHelper.openProjectAlerts(context);
                }
            });
            GridDataBuilder.newInstance().hFill().hGrab().applyTo(link);
        }

        return composite;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVisible(final TeamExplorerContext context) {
        return context.isConnected();
    }

}
