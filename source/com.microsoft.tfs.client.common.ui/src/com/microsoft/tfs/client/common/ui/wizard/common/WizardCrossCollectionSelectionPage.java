// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wizard.common;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.tfs.client.common.catalog.TeamProjectCollectionInfo;
import com.microsoft.tfs.client.common.commands.configuration.QueryProjectCollectionsCommand;
import com.microsoft.tfs.client.common.framework.command.ICommandExecutor;
import com.microsoft.tfs.client.common.framework.command.ThreadedCancellableCommand;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade.LaunchMode;
import com.microsoft.tfs.client.common.ui.controls.connect.ConnectionErrorControl;
import com.microsoft.tfs.client.common.ui.controls.generic.SizeConstrainedComposite;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkControl;
import com.microsoft.tfs.client.common.ui.controls.generic.compatibility.link.CompatibilityLinkFactory;
import com.microsoft.tfs.client.common.ui.framework.command.UICommandFinishedCallbackFactory;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.wizard.ExtendedWizardPage;
import com.microsoft.tfs.client.common.ui.wizard.connectwizard.ConnectWizard;
import com.microsoft.tfs.core.TFSConfigurationServer;
import com.microsoft.tfs.core.TFSConnection;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.commonstructure.ProjectInfo;
import com.microsoft.tfs.core.clients.versioncontrol.SourceControlCapabilityFlags;
import com.microsoft.tfs.core.util.ServerURIUtils;

public abstract class WizardCrossCollectionSelectionPage extends ExtendedWizardPage {
    private static final Log logger = LogFactory.getLog(WizardCrossCollectionSelectionPage.class);

    public static final String PROJECT_COLLECTION_ERROR_MESSAGE =
        "WizardCrossCollectionSelectionPage.teamProjectErrorMessage"; //$NON-NLS-1$

    private ConnectionErrorControl errorControl;

    private ImageHelper imageHelper;
    private static final String VSTS_IMAGE_LOC = "/images/common/vso-account.png"; //$NON-NLS-1$
    private static final String TFS_IMAGE_LOC = "/images/common/windows-account.png"; //$NON-NLS-1$

    private Label imageLabel;
    private Label emailLabel;
    private CompatibilityLinkControl serverLink;

    public WizardCrossCollectionSelectionPage(final String pageName, final String title, final String description) {
        super(pageName, title, description);
    }

    @Override
    protected void doCreateControl(final Composite parent, final IDialogSettings dialogSettings) {
        final Composite container = new Composite(parent, SWT.NULL);
        setControl(container);

        final GridLayout layout = new GridLayout();
        layout.marginWidth = getHorizontalMargin();
        layout.marginHeight = getVerticalMargin();
        layout.marginBottom = 0;
        layout.marginTop = 0;
        layout.verticalSpacing = 0;
        container.setLayout(layout);

        if (getExtendedWizard().hasPageData(PROJECT_COLLECTION_ERROR_MESSAGE)) {
            final SizeConstrainedComposite errorComposite = new SizeConstrainedComposite(container, SWT.NONE);
            errorComposite.setDefaultSize(parent.getSize().x, SWT.DEFAULT);
            errorComposite.setLayout(new FillLayout());
            GridDataBuilder.newInstance().hGrab().hFill().applyTo(errorComposite);

            errorControl = new ConnectionErrorControl(errorComposite, SWT.NONE);
            errorControl.setMessage((String) getExtendedWizard().getPageData(PROJECT_COLLECTION_ERROR_MESSAGE));

            final Label spacerLabel = new Label(container, SWT.NONE);
            spacerLabel.setText(""); //$NON-NLS-1$
        }

        final ICommandExecutor noErrorDialogCommandExecutor = getCommandExecutor();
        noErrorDialogCommandExecutor.setCommandFinishedCallback(
            UICommandFinishedCallbackFactory.getDefaultNoErrorDialogCallback());

        final SourceControlCapabilityFlags sourceControlCapabilityFlags =
            ((ConnectWizard) getExtendedWizard()).getSourceControlCapabilityFlags();

        createControls(container, sourceControlCapabilityFlags);

        final Composite connectionLabeComposite = buildConnectionLabel(container);
        GridDataBuilder.newInstance().hSpan(layout).hFill().hGrab().vIndent(getVerticalSpacing() * 2).applyTo(
            connectionLabeComposite);

        final Label changeServerLabel =
            SWTUtil.createLabel(container, Messages.getString("WizardCrossCollectionSelectionPage.ChangeConnection")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hSpan(layout).wHint(100).hFill().hGrab().vIndent(getVerticalSpacing()).applyTo(
            changeServerLabel);

        updateConnectionLabel();

        setPageComplete(false);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (imageHelper != null) {
            imageHelper.dispose();
            imageHelper = null;
        }
    }

    private Composite buildConnectionLabel(final Composite parent) {
        imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

        final Composite composite = new Composite(parent, SWT.NULL);

        final GridLayout connectionLabelLayout = new GridLayout(2, false);
        connectionLabelLayout.marginBottom = 0;
        connectionLabelLayout.marginTop = 0;
        connectionLabelLayout.verticalSpacing = 0;
        composite.setLayout(connectionLabelLayout);

        imageLabel = SWTUtil.createLabel(composite);
        imageLabel.setImage(imageHelper.getImage(VSTS_IMAGE_LOC));
        GridDataBuilder.newInstance().hSpan(1).vSpan(2).vAlign(SWT.TOP).applyTo(imageLabel);

        serverLink = CompatibilityLinkFactory.createLink(composite, SWT.NONE);
        serverLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                // Use external dialog since this is being launched from a
                // dialog, and therefore would be hidden by this dialog if
                // launched internally

                final URI uri = getServerUri();
                if (uri != null) {
                    BrowserFacade.launchURL(uri, null, null, null, LaunchMode.EXTERNAL);
                }
            }
        });
        GridDataBuilder.newInstance().hSpan(1).hFill().applyTo(serverLink.getControl());

        emailLabel = SWTUtil.createLabel(composite, SWT.NONE);
        GridDataBuilder.newInstance().hSpan(1).hFill().applyTo(emailLabel);

        return composite;
    }

    private void updateConnectionLabel() {
        if (serverLink != null) {
            // Adding whitespace to work around Link control bug
            // (https://bugs.eclipse.org/bugs/show_bug.cgi?id=151322)
            serverLink.setText("<a>" + getServerForDisplay() + "</a>      "); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (emailLabel != null) {
            emailLabel.setText(getUserNameForDisplay());
        }
        if (imageLabel != null && imageHelper != null) {
            boolean isVSTS = true;
            final URI uri = getServerUri();
            if (uri != null) {
                isVSTS = ServerURIUtils.isHosted(uri);
            }

            final String imageLocation = isVSTS ? VSTS_IMAGE_LOC : TFS_IMAGE_LOC;
            imageLabel.setImage(imageHelper.getImage(imageLocation));
        }
    }

    /**
     * Subclasses should override this method to add their own controls to the
     * middle of the page
     * 
     * @param container
     *        - main container control
     * @param sourceControlCapabilityFlags
     *        - sccFlags
     */
    protected abstract void createControls(
        final Composite container,
        final SourceControlCapabilityFlags sourceControlCapabilityFlags);

    protected void setWizardData(final TFSTeamProjectCollection collection, final ProjectInfo projectInfo) {
        removeWizardData(false);
        if (collection != null) {
            getExtendedWizard().setPageData(URI.class, collection.getConfigurationServer().getBaseURI());
            getExtendedWizard().setPageData(TFSTeamProjectCollection.class, collection);
        }
        if (projectInfo != null) {
            getExtendedWizard().setPageData(ConnectWizard.SELECTED_TEAM_PROJECTS, new ProjectInfo[] {
                projectInfo
            });
        }
    }

    protected void removeWizardData(final boolean removeAll) {
        if (removeAll) {
            getExtendedWizard().removePageData(URI.class);
        }
        getExtendedWizard().removePageData(TFSTeamProjectCollection.class);
        getExtendedWizard().removePageData(ConnectWizard.SELECTED_TEAM_PROJECTS);
    }

    private URI getServerUri() {
        final URI serverURI =
            getExtendedWizard().hasPageData(URI.class) ? (URI) getExtendedWizard().getPageData(URI.class) : null;
        return serverURI;
    }

    private String getServerForDisplay() {
        final URI serverURI = getServerUri();

        if (serverURI != null) {
            return serverURI.toString();
        } else {
            return Messages.getString("WizardCrossCollectionSelectionPage.Unknown"); //$NON-NLS-1$
        }
    }

    private String getUserNameForDisplay() {
        final TFSConnection[] connections = getExtendedWizard().hasPageData(TFSConnection[].class)
            ? (TFSConnection[]) getExtendedWizard().getPageData(TFSConnection[].class) : null;

        if (connections != null && connections.length > 0) {
            return connections[0].getAuthorizedAccountName();
        } else {
            return Messages.getString("WizardCrossCollectionSelectionPage.Unknown"); //$NON-NLS-1$
        }
    }

    @Override
    protected void refresh() {
        removeWizardData(false);
        updateConnectionLabel();
        clearList();
        refreshUI();

        final TFSConnection[] connections = getExtendedWizard().hasPageData(TFSConnection[].class)
            ? (TFSConnection[]) getExtendedWizard().getPageData(TFSConnection[].class) : null;

        if (connections == null || connections.length == 0) {
            return;
        }

        // TODO see if this can be pulled off the UI thread entirely
        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                // TODO see about running each connection concurrently
                for (final TFSConnection connection : connections) {
                    TFSConfigurationServer configurationServer = null;
                    if (connection instanceof TFSConfigurationServer) {
                        configurationServer = (TFSConfigurationServer) connection;
                    } else if (connection instanceof TFSTeamProjectCollection) {
                        configurationServer = ((TFSTeamProjectCollection) connection).getConfigurationServer();
                    }

                    if (configurationServer == null) {
                        logger.error(
                            new IllegalArgumentException(
                                "Unexpected connection type: " + connection.getClass().getName())); //$NON-NLS-1$
                        continue;
                    }

                    final List<TFSTeamProjectCollection> collections = new ArrayList<TFSTeamProjectCollection>(5);
                    final QueryProjectCollectionsCommand queryCommand =
                        new QueryProjectCollectionsCommand(configurationServer);

                    final IStatus status = getCommandExecutor().execute(new ThreadedCancellableCommand(queryCommand));
                    if (!status.isOK()) {
                        return;
                    }

                    final TeamProjectCollectionInfo[] projectCollections = queryCommand.getProjectCollections();
                    for (final TeamProjectCollectionInfo collectionInfo : projectCollections) {
                        try {
                            collections.add(
                                configurationServer.getTeamProjectCollection(collectionInfo.getIdentifier()));
                        } catch (final Exception e) {
                            logger.warn("Failed to get Team Project Collection: " + collectionInfo.getDisplayName()); //$NON-NLS-1$
                            logger.warn(e);
                        }
                    }

                    // For each collection get the list of projects
                    for (final TFSTeamProjectCollection collection : collections) {
                        appendCollectionInformation(collection);
                    }

                    updateConnectionLabel();
                    refreshUI();
                }
            }
        });
    }

    /**
     * Subclasses should use this method to prepare for a refresh.
     */
    protected abstract void clearList();

    /**
     * Subclasses should use this method to append information to their lists
     * that pertain to the given collection.
     */
    protected abstract void appendCollectionInformation(TFSTeamProjectCollection collection);

    /**
     * Subclasses should use this method to refresh the UI with the newly
     * updated list.
     */
    protected abstract void refreshUI();
}
