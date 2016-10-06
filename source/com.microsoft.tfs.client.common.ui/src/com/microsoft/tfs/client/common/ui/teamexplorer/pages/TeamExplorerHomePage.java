// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.teamexplorer.pages;

import java.net.URI;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.teamexplorer.TeamExplorerTileControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.protocolhandler.ProtocolHandler;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerContext;
import com.microsoft.tfs.client.common.ui.teamexplorer.TeamExplorerNavigator;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.ConnectHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.helpers.WebAccessHelper;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerHelpers;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationItemConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.internal.TeamExplorerNavigationLinkConfig;
import com.microsoft.tfs.client.common.ui.teamexplorer.items.ITeamExplorerNavigationItem;
import com.microsoft.tfs.client.common.ui.teamexplorer.link.ITeamExplorerNavigationLink;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;

public class TeamExplorerHomePage extends TeamExplorerBasePage {

    private final static String HOME_PAGE_ID =
        "com.microsoft.tfs.client.common.ui.teamexplorer.pages.TeamExplorerHomePage"; //$NON-NLS-1$

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
            log.debug("Source control:" //$NON-NLS-1$
                + (context.getSourceControlCapability().contains(SourceControlCapabilityFlags.GIT) ? " GIT" : "") //$NON-NLS-1$ //$NON-NLS-2$
                + (context.getSourceControlCapability().contains(SourceControlCapabilityFlags.TFS) ? " TFS" : "")); //$NON-NLS-1$ //$NON-NLS-2$

            createProtocolHandlerUI(toolkit, composite, context);
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

    private void createProtocolHandlerUI(
        final FormToolkit toolkit,
        final Composite parent,
        final TeamExplorerContext context) {

        if (!ProtocolHandler.getInstance().hasProtocolHandlerRequest() || !context.isConnectedToCollection()) {
            return;
        }

        final Composite composite = toolkit.createComposite(parent);
        composite.setBackground(TeamExplorerHelpers.getDropCompositeBackground(parent));
        SWTUtil.gridLayout(composite, 1, false, 3, 3);
        GridDataBuilder.newInstance().hAlignFill().hGrab().vIndent(5).applyTo(composite);

        final Composite innerComposite = toolkit.createComposite(composite);
        innerComposite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        innerComposite.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
        SWTUtil.gridLayout(innerComposite, 2, false, 5, 5);
        GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(innerComposite);

        final TeamExplorerNavigationLinkConfig[] navHomPageLinks = configuration.getNavigationLinks(HOME_PAGE_ID);
        for (final TeamExplorerNavigationLinkConfig item : navHomPageLinks) {
            final ITeamExplorerNavigationLink navLink = item.createInstance();
            if (navLink.isVisible(context)) {
                final Composite itemComposite = toolkit.createComposite(innerComposite);
                itemComposite.setBackground(innerComposite.getBackground());
                SWTUtil.gridLayout(itemComposite, 1, false, 5, 5);
                GridDataBuilder.newInstance().hAlignFill().hGrab().applyTo(itemComposite);

                final String branchText = MessageFormat.format(
                    "<span color=\"linkcolor\">{0}</span>", //$NON-NLS-1$
                    ProtocolHandler.getInstance().getProtocolHandlerBranchForHtml());

                final String repositoryLink = MessageFormat.format(
                    "<a href=\"{0}\">{1}</a>", //$NON-NLS-1$
                    WebAccessHelper.getGitRepoURL(
                        context,
                        ProtocolHandler.getInstance().getProtocolHandlerProject(),
                        ProtocolHandler.getInstance().getProtocolHandlerRepository(),
                        ProtocolHandler.getInstance().getProtocolHandlerBranch()),
                    ProtocolHandler.getInstance().getProtocolHandlerRepositoryForHtml());

                final String localizedMessageText =
                    MessageFormat.format(
                        Messages.getString("TeamExplorerHomePage.ImportRepoMessageFormat"), //$NON-NLS-1$
                        branchText,
                        repositoryLink);

                final String messageText = MessageFormat.format("<form><p>{0}</p></form>", localizedMessageText); //$NON-NLS-1$

                final FormText formText = toolkit.createFormText(itemComposite, false);
                formText.setBackground(innerComposite.getBackground());
                formText.setForeground(innerComposite.getForeground());
                formText.setText(messageText, true, false);
                formText.setColor("linkcolor", formText.getHyperlinkSettings().getForeground()); //$NON-NLS-1$
                formText.addHyperlinkListener(new HyperlinkAdapter() {
                    @Override
                    public void linkActivated(HyperlinkEvent e) {
                        try {
                            final URI repoUri = new URI((String) e.getHref());
                            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(repoUri.toURL());
                        } catch (final Exception ex) {
                            log.error("Error opening browser:", ex); //$NON-NLS-1$
                        }
                    }
                });
                GridDataBuilder.newInstance().hAlignFill().hGrab().wHint(200).applyTo(formText);

                final String cloneText = Messages.getString("TeamExplorerHomePage.CloneButtonText"); //$NON-NLS-1$

                final Button cloneButton = toolkit.createButton(itemComposite, cloneText, SWT.PUSH);
                cloneButton.setBackground(TeamExplorerHelpers.getDropCompositeBackground(parent));
                cloneButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(final SelectionEvent e) {
                        navLink.clicked(parent.getShell(), context, navigator, null);
                        TeamExplorerHelpers.toggleCompositeVisibility(composite);
                        TeamExplorerHelpers.relayoutContainingScrolledComposite(parent);
                        ProtocolHandler.getInstance().removeProtocolHandlerArguments();
                    }

                    @Override
                    public void widgetDefaultSelected(final SelectionEvent e) {
                        navLink.clicked(parent.getShell(), context, navigator, null);
                        TeamExplorerHelpers.toggleCompositeVisibility(composite);
                        TeamExplorerHelpers.relayoutContainingScrolledComposite(parent);
                        ProtocolHandler.getInstance().removeProtocolHandlerArguments();
                    }
                });
                GridDataBuilder.newInstance().hAlignLeft().hGrab().applyTo(cloneButton);
            }
        }

        final ImageHyperlink closeButton = toolkit.createImageHyperlink(innerComposite, SWT.PUSH);

        final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);
        final Image image = imageHelper.getImage("/images/common/close_button.png"); //$NON-NLS-1$

        closeButton.setImage(image);
        closeButton.setText(""); //$NON-NLS-1$
        closeButton.setBackground(innerComposite.getBackground());
        GridDataBuilder.newInstance().vAlignTop().hAlignRight().applyTo(closeButton);

        closeButton.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            public void linkActivated(final HyperlinkEvent e) {
                TeamExplorerHelpers.toggleCompositeVisibility(composite);
                TeamExplorerHelpers.relayoutContainingScrolledComposite(parent);
                ProtocolHandler.getInstance().removeProtocolHandlerArguments();
            }
        });

        composite.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(final DisposeEvent e) {
                imageHelper.dispose();
                image.dispose();
            }
        });
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
