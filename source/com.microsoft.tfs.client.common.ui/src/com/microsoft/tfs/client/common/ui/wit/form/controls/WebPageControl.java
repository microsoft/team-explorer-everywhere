// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.browser.BrowserFacade;
import com.microsoft.tfs.client.common.ui.controls.generic.CompatibleBrowser;
import com.microsoft.tfs.client.common.ui.framework.image.ImageHelper;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemStateListener;
import com.microsoft.tfs.core.clients.workitem.form.WIFormControl;
import com.microsoft.tfs.core.clients.workitem.form.WIFormWebPageControlOptions;
import com.microsoft.tfs.core.clients.workitem.internal.MacroTargetNotConfiguredException;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.Check;

/**
 * Control to support the WebPageControl WIT form control. The control consists
 * of a web browser control and an optional browser navigation toolbar.
 *
 *
 * @threadsafety unknown
 */
public class WebPageControl extends BaseWorkItemControl {
    private WorkItem workItem;
    private WIFormControl formControl;
    private WIFormWebPageControlOptions webPageControlOptions;
    private BrowserHistory browserHistory;
    private CompatibleBrowser browser;
    private boolean isBackOrForwardInProgress;
    private boolean isNavigateInProgress;
    private boolean hasNavigationToolbar;

    ToolItem backButton;
    ToolItem forwardButton;
    ToolItem stopButton;
    ToolItem refreshButton;
    ToolItem reloadButton;
    ToolItem openInBrowserButton;

    private static final String BACK_TOOLBAR_IMAGE = "images/webpage/navigate_back.gif"; //$NON-NLS-1$
    private static final String FORWARD_TOOLBAR_IMAGE = "images/webpage/navigate_forward.gif"; //$NON-NLS-1$
    private static final String REFRESH_PAGE_TOOLBAR_IMAGE = "images/webpage/refresh_page.gif"; //$NON-NLS-1$
    private static final String STOP_LOAD_TOOLBAR_IMAGE = "images/webpage/stop_load_page.gif"; //$NON-NLS-1$
    private static final String RELOAD_ORIGINAL_TOOLBAR_IMAGE = "images/webpage/reload_original_page.gif"; //$NON-NLS-1$
    private static final String OPEN_IN_BROWSER_TOOLBAR_IMAGE = "images/webpage/open_in_browser.gif"; //$NON-NLS-1$

    /**
     * {@inheritDoc}
     */
    @Override
    protected void hookInit() {
        workItem = getFormContext().getWorkItem();
        formControl = (WIFormControl) getFormElement();
        webPageControlOptions = formControl.getWebPageControlOptions();
        Check.notNull(webPageControlOptions, "webPageControlOptions"); //$NON-NLS-1$

        // Navigation is supported if the WIT type definition is a link.
        hasNavigationToolbar = webPageControlOptions.isLink();

        // Create a browser history if supporting navigation.
        if (hasNavigationToolbar) {
            browserHistory = new BrowserHistory();
        }

        // Listen for work item state changes if the WIT type definition
        // specifies a link with dynamic parameters.
        if (webPageControlOptions.getReloadOnParamChange()) {
            workItem.addWorkItemStateListener(new WorkItemStateListener() {
                @Override
                public void dirtyStateChanged(final boolean isDirty, final WorkItem workItem) {
                }

                @Override
                public void saved(final WorkItem workItem) {
                }

                @Override
                public void synchedToLatest(final WorkItem workItem) {
                }

                @Override
                public void validStateChanged(final boolean isValid, final WorkItem workItem) {
                    onFieldValueChanged();
                }
            });
        }
    }

    /**
     * Instantiate the browser control and optionally a navigation toolbar. A
     * navigation toolbar is created if the WIT type definition includes a link
     * rather than hard coded content. {@inheritDoc}
     */
    @Override
    public void addToComposite(final Composite parent) {
        final Integer height = formControl.getHeight();

        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();
        final int browserStyle = preferences.getInt(UIPreferenceConstants.EMBEDDED_WEB_BROWSER_TYPE);

        if (webPageControlOptions.isLink()) {
            // Create a container for the navigation toolbar and browser
            // control.
            final Composite composite = new Composite(parent, SWT.NONE);
            final GridLayout gridLayout = new GridLayout(1, true);
            gridLayout.marginHeight = 0;
            gridLayout.marginWidth = 0;
            gridLayout.horizontalSpacing = 0;
            gridLayout.verticalSpacing = 0;
            composite.setLayout(gridLayout);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            // Create a navigation toolbar at the top of the composite.
            createNavigationToolbar(composite);

            // Create the browser control beneath the navigation toolbar.
            browser = new CompatibleBrowser(composite, SWT.BORDER | browserStyle);
            browser.setLayoutData(new GridData(GridData.FILL_BOTH));

            if (height != null) {
                composite.setSize(composite.getSize().x, height.intValue());
            }
            composite.pack();
        } else {
            // Create only the browser control.
            browser = new CompatibleBrowser(parent, SWT.BORDER | browserStyle);
            browser.setLayoutData(new GridData(GridData.FILL_BOTH));

            if (height != null) {
                browser.setSize(browser.getSize().x, height.intValue());
            }
        }

        // Enable or disable javascript based on option from WIT type
        // definition.
        browser.setJavascriptEnabled(webPageControlOptions.getAllowScript());

        // Listen for navigation events.
        browser.addLocationListener(new LocationListener() {
            @Override
            public void changed(final LocationEvent event) {
                if (!event.top) {
                    return;
                }
                if (!isBackOrForwardInProgress) {
                    browserHistory.addURL(event.location);
                }

                isNavigateInProgress = false;
                isBackOrForwardInProgress = false;
                setToolbarButtonState();
            }

            @Override
            public void changing(final LocationEvent event) {
                isNavigateInProgress = true;
                setToolbarButtonState();
            }
        });

        // Populate the control.
        if (webPageControlOptions != null) {
            if (webPageControlOptions.isLink()) {
                // Content is a link. Navigate to the URL.
                try {
                    navigateToURL(webPageControlOptions.getLink().getURL(workItem));
                } catch (final MacroTargetNotConfiguredException ex) {
                    displayMacroTargetNotConfiguredError(ex);
                }
            } else if (webPageControlOptions.getContent() != null) {
                // Content is hard coded HTML.
                final StringBuffer sb = new StringBuffer();
                sb.append("<html><head></head><body>"); //$NON-NLS-1$
                sb.append(webPageControlOptions.getContent().getContent());
                sb.append("</body></html>"); //$NON-NLS-1$

                isBackOrForwardInProgress = true;
                browser.setText(sb.toString());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinimumRequiredColumnCount() {
        return 1;
    }

    /**
     * Navigate to the specified URL.
     *
     * @param url
     *        The URL to navigate to.
     */
    private void navigateToURL(final String url) {
        // Test to see if this is a valid URI. Don't navigate if
        // this is an invalid URI. Since a URI can be formed
        // dynamically by this control using user input, it is
        // possible for invalid URIs to occur.
        try {
            new URI(url);
        } catch (final URISyntaxException e) {
            return;
        }

        // Navigate to encoded URL.
        try {
            browser.setURL(url);
        } catch (final IOException e) {
            // TODO: this exception is only thrown by the
            // fallback control in CompatibleBrowser. How
            // do we want to handle this error? Should this
            // be handled within CompatibleBrowser?
        }
    }

    /**
     * Create the navigation toolbar.
     *
     * @param parent
     *        The composite container for this toolbar.
     */
    private void createNavigationToolbar(final Composite parent) {
        final ToolBar toolbar = new ToolBar(parent, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().hAlign(SWT.LEFT).applyTo(toolbar);
        final ImageHelper imageHelper = new ImageHelper(TFSCommonUIClientPlugin.PLUGIN_ID);

        backButton = new ToolItem(toolbar, 0);
        backButton.setImage(imageHelper.getImage(BACK_TOOLBAR_IMAGE));
        backButton.setToolTipText(Messages.getString("WebPageControl.BackButtonTooltipText")); //$NON-NLS-1$
        backButton.setEnabled(false);
        backButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                backButtonInvoked();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                backButtonInvoked();
            }
        });

        forwardButton = new ToolItem(toolbar, SWT.PUSH);
        forwardButton.setImage(imageHelper.getImage(FORWARD_TOOLBAR_IMAGE));
        forwardButton.setToolTipText(Messages.getString("WebPageControl.ForwardButtonTooltipText")); //$NON-NLS-1$
        forwardButton.setEnabled(false);
        forwardButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                forwardButtonInvoked();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                forwardButtonInvoked();
            }
        });

        stopButton = new ToolItem(toolbar, SWT.PUSH);
        stopButton.setImage(imageHelper.getImage(STOP_LOAD_TOOLBAR_IMAGE));
        stopButton.setToolTipText(Messages.getString("WebPageControl.StopButtonTooltipText")); //$NON-NLS-1$
        stopButton.setEnabled(false);
        stopButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                stopButtonInvoked();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                stopButtonInvoked();
            }
        });

        refreshButton = new ToolItem(toolbar, SWT.PUSH);
        refreshButton.setImage(imageHelper.getImage(REFRESH_PAGE_TOOLBAR_IMAGE));
        refreshButton.setToolTipText(Messages.getString("WebPageControl.RefreshButtonTooltipText")); //$NON-NLS-1$
        refreshButton.setEnabled(false);
        refreshButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                refreshButtonInvoked();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                refreshButtonInvoked();
            }
        });

        reloadButton = new ToolItem(toolbar, SWT.PUSH);
        reloadButton.setImage(imageHelper.getImage(RELOAD_ORIGINAL_TOOLBAR_IMAGE));
        reloadButton.setToolTipText(Messages.getString("WebPageControl.ReloadButtonTooltipText")); //$NON-NLS-1$
        reloadButton.setEnabled(false);
        reloadButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                reloadButtonInvoked();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                reloadButtonInvoked();
            }
        });

        openInBrowserButton = new ToolItem(toolbar, SWT.PUSH);
        openInBrowserButton.setImage(imageHelper.getImage(OPEN_IN_BROWSER_TOOLBAR_IMAGE));
        openInBrowserButton.setToolTipText(Messages.getString("WebPageControl.OpenInBrowserButtonTooltipText")); //$NON-NLS-1$
        openInBrowserButton.setEnabled(false);
        openInBrowserButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                openInBrowserButtonInvoked();
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                openInBrowserButtonInvoked();
            }
        });

        toolbar.pack();
    }

    /**
     * Handler for the work item event "Valid state changed". This work item
     * event is called each time a field is modified on the WIT form and not
     * only when the state becomes valid/invalid as the event name might imply.
     * When a field value changes we must recompute dynamic URLs to see if they
     * should be reloaded (supporting the ReloadOnParamChange option in the WIT
     * type definition). If the change results in a URL that differs from the
     * currently displayed URL, we navigate to the new URL.
     */
    private void onFieldValueChanged() {
        try {
            final String url = webPageControlOptions.getLink().getURL(workItem);

            if (!url.equalsIgnoreCase(browserHistory.getCurrentURL())) {
                navigateToURL(url);
            }
        } catch (final MacroTargetNotConfiguredException ex) {
            displayMacroTargetNotConfiguredError(ex);
        }
    }

    /**
     * Handler for the "Navigate backward" button. Navigate backward causes a
     * navigation to the previous URL listed in the history buffer.
     */
    private void backButtonInvoked() {
        final String backUrl = browserHistory.moveBack();
        setToolbarButtonState();

        isBackOrForwardInProgress = true;
        navigateToURL(backUrl);
    }

    /**
     * Handler for the "Navigate forward" button. Navigate forward causes a
     * navigation to the next URL listed in the history buffer.
     */
    private void forwardButtonInvoked() {
        final String forwardUrl = browserHistory.moveForward();
        setToolbarButtonState();

        isBackOrForwardInProgress = true;
        navigateToURL(forwardUrl);
    }

    /**
     * Handler for a click on the "Refresh page" button. Refresh page causes a
     * navigation to the current URL being displayed in this control.
     */
    private void refreshButtonInvoked() {
        final String currentUrl = browserHistory.getCurrentURL();

        isBackOrForwardInProgress = true;
        navigateToURL(currentUrl);
    }

    /**
     * Handler for a click on the "Reload original URL" button. Reload original
     * URL causes the current link to be re-evaluated and navigated to.
     */
    private void reloadButtonInvoked() {
        try {
            final String reloadUrl = webPageControlOptions.getLink().getURL(workItem);
            navigateToURL(reloadUrl);
        } catch (final MacroTargetNotConfiguredException ex) {
            displayMacroTargetNotConfiguredError(ex);
        }
    }

    /**
     * Handler for a click on the "Stop page load" button. Stop page load will
     * cause a cancel to be sent to the browser control. The stop button is only
     * enabled while a page is loading.
     */
    private void stopButtonInvoked() {
        browser.stop();
        isNavigateInProgress = false;
        setToolbarButtonState();
    }

    /**
     * Open the currently displayed URL in an internal or external browser,
     * depending on the user preference.
     */
    private void openInBrowserButtonInvoked() {
        try {
            final URI uri = URIUtils.newURI(browserHistory.getCurrentURL());
            BrowserFacade.launchURL(uri, uri.getPath());
        } catch (final IllegalArgumentException e) {
            final String messageFormat = Messages.getString("WebPageControl.ErrorDialogTextFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, e.getMessage());

            MessageDialog.openError(
                browser.getShell(),
                Messages.getString("WebPageControl.ErrorDialogTitle"), //$NON-NLS-1$
                message);
        }
    }

    /**
     * Display an error message in the browser control when a dynamic URL cannot
     * be allocated because of an unconfigured marcro target.
     *
     * @param ex
     *        The macro target not configured exception.
     */
    private void displayMacroTargetNotConfiguredError(final MacroTargetNotConfiguredException ex) {
        // Content is hard coded HTML.
        final StringBuffer sb = new StringBuffer();
        sb.append("<html><head></head><body>"); //$NON-NLS-1$
        sb.append("<p><b><u>" + ex.getMessageTitle() + "</u></b></p>"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("<p>" + ex.getMessageBody() + "</p>"); //$NON-NLS-1$ //$NON-NLS-2$
        sb.append("</body></html>"); //$NON-NLS-1$

        isBackOrForwardInProgress = true;
        browser.setText(sb.toString());
    }

    /**
     * Enable or disable each button on the navigation toolbar based on the
     * current state.
     */
    private void setToolbarButtonState() {
        if (hasNavigationToolbar) {
            backButton.setEnabled(browserHistory.canMoveBack());
            forwardButton.setEnabled(browserHistory.canMoveForward());
            refreshButton.setEnabled(browserHistory.hasCurrentURL());
            reloadButton.setEnabled(browserHistory.hasCurrentURL());
            openInBrowserButton.setEnabled(browserHistory.hasCurrentURL());
            stopButton.setEnabled(isNavigateInProgress);
        }
    }
}
