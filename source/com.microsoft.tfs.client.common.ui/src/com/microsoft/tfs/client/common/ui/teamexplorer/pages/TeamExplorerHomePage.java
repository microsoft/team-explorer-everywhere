// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.pages;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.teamexplorer.TeamExplorerTileControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ConnectHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationLinkConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.items.ITeamExplorerNavigationItem;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;

public class TeamExplorerHomePage extends TeamExplorerBasePage {
    private final TeamExplorerConfig configuration;
    private final TeamExplorerNavigator navigator;
    private static final Log log = LogFactory.getLog(TeamExplorerHomePage.class);

    public TeamExplorerHomePage(final TeamExplorerConfig configuration, final TeamExplorerNavigator navigator) {
        this.configuration = configuration;
        this.navigator = navigator;
    }

    @Override
    public Composite getPageContent(
        final FormToolkit toolkit,
        final Composite parent,
        final int style,
        final TeamExplorerContext context) {
        final Composite composite = toolkit.createComposite(parent);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(composite);
        // Form-style border painting not enabled (0 pixel margins OK) because
        // no applicable controls in this composite
        final GridLayout layout = SWTUtil.gridLayout(composite, 1, false, 5, 0);
        layout.verticalSpacing = 0;

        if (!context.isConnectedToCollection()) {
            log.debug("Disconnected context"); //$NON-NLS-1$
            createDisconnectedUI(toolkit, composite);
        } else {
            log.debug("Connected context"); //$NON-NLS-1$
            log.debug("Source control:" + //$NON-NLS-1$
                (context.getSourceControlCapability().contains(SourceControlCapabilityFlags.GIT) ? " GIT" : "") //$NON-NLS-1$ //$NON-NLS-2$
                + (context.getSourceControlCapability().contains(SourceControlCapabilityFlags.TFS) ? " TFS" : "")); //$NON-NLS-1$ //$NON-NLS-2$

            createConnectedUI(toolkit, composite, context);
        }

        final boolean canOpenInWeb = TeamExplorerHelpers.isVersion2010OrGreater(context);
        final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

        // Create links to the top level navigation items.
        for (final TeamExplorerNavigationItemConfig item : configuration.getNavigationItems()) {
            final ITeamExplorerNavigationItem navItem = item.createInstance();
            if (navItem.isVisible(context)) {
                log.debug("Item " + navItem.getClass().getName() + " is visible."); //$NON-NLS-1$ //$NON-NLS-2$

                final TeamExplorerTileControl metroControl = new TeamExplorerTileControl(toolkit, composite, SWT.NONE);
                metroControl.setIcon(item.getIcon());
                metroControl.setTitle(item.getTitle());
                metroControl.setColorBar(item.getID());
                GridDataBuilder.newInstance().hGrab().hFill().vIndent(10).applyTo(metroControl);
                toolkit.paintBordersFor(metroControl);

                metroControl.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseDown(final MouseEvent e) {
                        if (e.button != TeamExplorerHelpers.MOUSE_RIGHT_BUTTON) {
                            onClick(context, item, navItem);
                        }
                    }
                });

                final Action openAction = new Action() {
                    @Override
                    public void run() {
                        onClick(context, item, navItem);
                    }
                };

                openAction.setText(Messages.getString("TeamExplorerHomePage.OpenActionName")); //$NON-NLS-1$
                openAction.setImageDescriptor(imageHelper.getImageDescriptor("images/teamexplorer/Open.png")); //$NON-NLS-1$

                Action openInWebAction = null;
                if (canOpenInWeb && navItem.canOpenInWeb()) {
                    openInWebAction = new Action() {
                        @Override
                        public void run() {
                            navItem.openInWeb(context);
                        }
                    };
                    openInWebAction.setText(Messages.getString("TeamExplorerHomePage.OpenInWebActionName")); //$NON-NLS-1$
                    openInWebAction.setImageDescriptor(
                        imageHelper.getImageDescriptor("images/common/internal_browser.gif")); //$NON-NLS-1$
                }

                final TeamExplorerNavigationLinkConfig[] navLinks = configuration.getNavigationLinks(item.getID());
                metroControl.addMenuItems(openAction, openInWebAction, navLinks, context, navigator, item);
            } else {
                log.debug("Item " + navItem.getClass().getName() + " is not visible."); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        return composite;
    }

    private void createDisconnectedUI(final FormToolkit toolkit, final Composite parent) {
        final Hyperlink connectLink =
            toolkit.createHyperlink(parent, Messages.getString("TeamExplorerHomePage.ConnectToTFSLinkText"), SWT.WRAP); //$NON-NLS-1$

        connectLink.setUnderlined(false);
        connectLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                ConnectHelpers.connectToServer(parent.getShell());
            }
        });

        GridDataBuilder.newInstance().hAlignFill().hGrab().vIndent(5).applyTo(connectLink);

        final Label labelOr = toolkit.createLabel(parent, Messages.getString("TeamExplorerHomePage.LabelOr"), SWT.WRAP); //$NON-NLS-1$
        GridDataBuilder.newInstance().hAlignFill().hGrab().vIndent(5).applyTo(labelOr);

        final Hyperlink signupLink =
            toolkit.createHyperlink(parent, Messages.getString("TeamExplorerHomePage.SignUpForTFSLinkText"), SWT.WRAP); //$NON-NLS-1$

        signupLink.setUnderlined(false);
        signupLink.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                ConnectHelpers.signupForTeamFoundationService(parent.getShell());
            }
        });

        GridDataBuilder.newInstance().hAlignFill().hGrab().vIndent(5).applyTo(signupLink);

        final Label labelHosted =
            toolkit.createLabel(
                parent,
                Messages.getString("TeamExplorerHomePage.HostedDescriptionLabelText"), //$NON-NLS-1$
                SWT.WRAP);
        GridDataBuilder.newInstance().hAlignFill().hGrab().vIndent(5).wHint(200).applyTo(labelHosted);
    }

    private void createConnectedUI(
        final FormToolkit toolkit,
        final Composite parent,
        final TeamExplorerContext context) {
    }

    private void onClick(
        final TeamExplorerContext context,
        final TeamExplorerNavigationItemConfig navItem,
        final ITeamExplorerNavigationItem instance) {
        final String viewID = navItem.getViewID();

        // do specific if targetPageID is null
        if (navItem.getTargetPageID() == null) {
            instance.clicked(context);
        }
        // viewID not null -> check undocked views
        else if (viewID != null && TeamExplorerHelpers.isViewUndocked(viewID)) {
            TeamExplorerHelpers.showView(viewID);
        }
        // other cases -> navigate in Team Explorer view
        else {
            navigator.navigateToItem(navItem);
        }
    }
}
