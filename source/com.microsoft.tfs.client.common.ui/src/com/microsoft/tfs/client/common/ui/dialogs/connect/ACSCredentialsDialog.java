// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.connect;

import java.io.InputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.tfs.client.common.credentials.EclipseCredentialsManagerFactory;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.controls.generic.FullFeaturedBrowser;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.HTMLIncludeHelper;
import com.microsoft.tfs.client.common.ui.helpers.HTMLIncludeHelper.HTMLIncludeResourceProvider;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.config.persistence.DefaultPersistenceStoreProvider;
import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.credentials.CredentialsManager;
import com.microsoft.tfs.core.httpclient.Cookie;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.TypesafeEnum;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * Provide a dialog to handle ACS authentication.
 *
 * @threadsafety unknown
 */
public class ACSCredentialsDialog extends CredentialsCompleteDialog {
    private static final Log log = LogFactory.getLog(ACSCredentialsDialog.class);

    private static final String COOKIE_PREFIX = "FedAuth"; //$NON-NLS-1$

    /*
     * Minimum version of SWT required to use the HTML evaluate method. 3.5 and
     * later are supported.
     */
    private static final int MINIMUM_SWT_VERSION = 3600;

    /*
     * A property to define whether the browser-based login should be allowed.
     * Default is true.
     */
    private static final String ENABLE_BROWSER_AUTH_PROPERTY_NAME =
        "com.microsoft.tfs.client.common.ui.dialogs.connect.enablebrowserauth"; //$NON-NLS-1$

    /* Availability checks */
    private static Boolean browserAvailable = null;
    private static int browserAvailableForStyle;

    /* HTML to decorate our internal loading transient page */
    private static final String HTML_RESOURCE_PATH = "resources"; //$NON-NLS-1$
    private static final String LOADING_HTML_RESOURCE_NAME = "acscredentialsdialog.html"; //$NON-NLS-1$

    private final URI serverURI;
    private final URI serverSigninURL;
    private final FederatedAuthException exception;

    private final List<Cookie> cookies = new ArrayList<Cookie>();

    private Text locationText;
    private FullFeaturedBrowser browser;
    private BrowserFunction notifyToken;

    private ACSConfigurationResult configurationResult = ACSConfigurationResult.CONTINUE;

    private final SingleListenerFacade credentialsCompleteListeners =
        new SingleListenerFacade(CredentialsCompleteListener.class);

    public ACSCredentialsDialog(
        final Shell parentShell,
        final URI serverURI,
        final URI serverSigninURL,
        final FederatedAuthException exception) {
        super(parentShell);

        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(serverSigninURL, "serverSigninURL"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.serverSigninURL = getSignInURI(serverSigninURL);
        this.exception = exception;

        log.trace("serverURI = " + serverURI); //$NON-NLS-1$
        log.trace("serverSigninURL = " + serverSigninURL); //$NON-NLS-1$
        if (exception != null) {
            log.trace("exception: " + exception.getMessage()); //$NON-NLS-1$
            log.trace("    FedAuthIssuer = " + exception.getFedAuthIssuer()); //$NON-NLS-1$
            log.trace("    FedAuthRealm = " + exception.getFedAuthRealm()); //$NON-NLS-1$
            log.trace("    ServerURI = " + exception.getServerURI()); //$NON-NLS-1$
            log.trace(
                "    Credentials = " + (exception.getCredentials() != null ? exception.getCredentials() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
            log.trace("    Mechanisms = " + StringUtil.join(exception.getMechanisms(), ", ")); //$NON-NLS-1$ //$NON-NLS-2$
        }

        setOptionIncludeDefaultButtons(false);
        setOptionEnforceMinimumSize(false);
    }

    protected URI getSignInURI(final URI serverSigninURL) {
        String query = serverSigninURL.getQuery();

        if (query.indexOf("protocol=") < 0) //$NON-NLS-1$
        {
            query += "&protocol=javascriptnotify"; //$NON-NLS-1$
        }

        if (query.indexOf("force=") < 0) //$NON-NLS-1$
        {
            query += "&force=1"; //$NON-NLS-1$
        }

        if (query.indexOf("compact=") < 0) //$NON-NLS-1$
        {
            query += "&compact=1"; //$NON-NLS-1$
        }

        return URIUtils.newURI(
            serverSigninURL.getScheme(),
            serverSigninURL.getAuthority(),
            serverSigninURL.getPath(),
            query,
            serverSigninURL.getFragment());
    }

    /**
     * Gets whether {@link ACSCredentialsDialogBase} is available for the
     * running platform (browser requirements are met). This status is advisory
     * - you may still open a {@link ACSCredentialsDialogBase}, however it will
     * likely not provide authentication services to the client (likely due to
     * lack of Javascript.)
     *
     * @return <code>true</code> if {@link ACSCredentialsDialogBase} is
     *         supported on the running platform, <code>false</code> if minimum
     *         browser requirements are not met (see the log for details)
     */
    public static boolean isAvailable() {
        /*
         * No synchronization here because this method is always called on the
         * UI thread.
         */

        // If never checked or was checked for a different style, re-check
        final int browserStyle = getBrowserStyle();
        if (browserAvailable == null || browserStyle != browserAvailableForStyle) {
            browserAvailable = Boolean.valueOf(isAvailableInternal());
            browserAvailableForStyle = browserStyle;
        }

        return browserAvailable.booleanValue();
    }

    private static boolean isAvailableInternal() {
        final String enabledPropValue = System.getProperty(ENABLE_BROWSER_AUTH_PROPERTY_NAME);

        if (enabledPropValue != null && enabledPropValue.equalsIgnoreCase("false")) //$NON-NLS-1$
        {
            log.info("ACSCredentialsDialog asked not to start (via system property)"); //$NON-NLS-1$
            return false;
        }

        /*
         * OS X's SWT native browser is bad on Carbon, avoid it.
         * https://bugs.eclipse.org/bugs/show_bug.cgi?id=230035
         */
        if (WindowSystem.isCurrentWindowSystem(WindowSystem.CARBON)) {
            log.warn("ACSCredentialsDialog does not support SWT Browser on Mac OS Carbon"); //$NON-NLS-1$
            return false;
        }

        /*
         * FreeBSD's xulrunner version does not appear to handle the Javascript
         * used by the Live ID login page.
         */
        if (Platform.isCurrentPlatform(Platform.FREEBSD)) {
            log.warn("ACSCredentialsDialog does not support SWT Browser on FreeBSD"); //$NON-NLS-1$
            return false;
        }

        if (SWT.getVersion() < MINIMUM_SWT_VERSION) {
            log.warn(
                MessageFormat.format(
                    "SWT version {0} not new enough ({1} or newer required) to use ACSCredentialsDialog", //$NON-NLS-1$
                    Integer.toString(SWT.getVersion()),
                    Integer.toString(MINIMUM_SWT_VERSION)));
            return false;
        }

        Shell shell = null;
        FullFeaturedBrowser browser = null;

        try {
            shell = new Shell();
            browser = new FullFeaturedBrowser(shell, SWT.NONE, getBrowserStyle());

            browser.setJavascriptEnabled(true);
            if (browser.getJavascriptEnabled() == false) {
                log.warn("Could not enable Javascript in SWT Browser for ACSCredentialsDialog"); //$NON-NLS-1$
                return false;
            }

            /*
             * On Windows, only IE 7 and newer will run our Javascript
             * correctly. We don't use XMLHttpRequest, but it's a good test for
             * IE 6, which doesn't have it (IE 7+ does).
             */
            if (Platform.isCurrentPlatform(Platform.WINDOWS) && browser.getBrowserType().equalsIgnoreCase("ie")) //$NON-NLS-1$
            {
                // Our Javascript needs a document to run with.
                browser.setText("<html></html>"); //$NON-NLS-1$

                final Object hasXMLHttpRequest = browser.evaluate("return ('XMLHttpRequest' in window);"); //$NON-NLS-1$

                if (hasXMLHttpRequest != null
                    && hasXMLHttpRequest instanceof Boolean
                    && ((Boolean) hasXMLHttpRequest).booleanValue() == false) {
                    log.warn("IE major version 6 detected; this version not supported by ACSCredentialsDialog"); //$NON-NLS-1$
                    return false;
                }
            }

            // Success!
            log.info("SWT Browser successfully loaded for ACSCredentialsDialog"); //$NON-NLS-1$
            return true;
        } catch (final Throwable t) {
            log.warn("SWT Browser failed to load for ACSCredentialsDialog", t); //$NON-NLS-1$
            return false;
        } finally {
            if (browser != null) {
                browser.dispose();
            }

            if (shell != null) {
                shell.dispose();
            }
        }
    }

    private static int getBrowserStyle() {
        final Preferences preferences = TFSCommonUIClientPlugin.getDefault().getPluginPreferences();
        return preferences.getInt(UIPreferenceConstants.EMBEDDED_WEB_BROWSER_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String provideDialogTitle() {
        return MessageFormat.format(
            Messages.getString("ACSCredentialsDialog.DialogTitleFormat"), //$NON-NLS-1$
            this.serverURI.getHost());
    }

    @Override
    protected Point defaultComputeInitialSize() {
        // Set the default size of the ACS Dialog to be one that the standard
        // log-in page is set up for
        // But if running on a small screen, set to 75% of screen real estate.
        final Rectangle parentBounds = getParentShell().getMonitor().getClientArea();

        final int width = Math.min((int) (parentBounds.width * 0.75), 488);
        final int height = Math.min((int) (parentBounds.height * 0.75), 616);
        return new Point(width, height);
    }

    @Override
    protected void hookDialogIsOpen() {
        /*
         * Take the user to the ACS authentication URL once the dialog is open.
         * This allows our setText call, below, to decorate the loading page.
         * Without this, the user will simply see a blank white page while
         * loading the first page.
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                UIHelpers.runOnUIThread(true, new Runnable() {
                    @Override
                    public void run() {
                        if (!browser.isDisposed()) {
                            log.debug("Go to the ACS authentication URL: " + serverSigninURL.toString()); //$NON-NLS-1$
                            browser.setUrl(serverSigninURL.toString());

                            /*
                             * Expect that a page may contain JavaScript
                             * notification providing FedAuth token. Define a
                             * handler for the JavaScript call of the
                             * window.notifyToken(String) method. The String
                             * parameter will contain a JSon object:
                             * {"securityToken":
                             * ["fedAuthCookie","fedAuthCookie1"
                             * ,...,"fedAuthCookieN"]}
                             */
                            notifyToken = new NotifyTokenBrowserFunction(browser.getBrowser(), "notifyToken"); //$NON-NLS-1$
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    protected void hookAddToDialogArea(final Composite dialogArea) {
        final GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        dialogArea.setLayout(layout);

        log.trace("get browser instance"); //$NON-NLS-1$
        browser = new FullFeaturedBrowser(dialogArea, SWT.NONE, getBrowserStyle());

        GridDataBuilder.newInstance().grab().fill().wHint((int) (getMinimumMessageAreaWidth() * 1.5)).hHint(
            getMinimumMessageAreaWidth()).applyTo(browser);

        final String interstitialText = loadInterstitial();
        log.trace("set browser text to: " + interstitialText); //$NON-NLS-1$
        browser.setText(loadInterstitial());

        log.trace("add location listener"); //$NON-NLS-1$
        final LocationListener locationListener = getLocationListener();
        if (locationListener != null) {
            browser.addLocationListener(locationListener);
        }

        final Composite spacerComposite = new Composite(dialogArea, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(spacerComposite);

        final GridLayout spacerLayout = new GridLayout(1, false);
        spacerLayout.marginHeight = 0;
        spacerLayout.marginTop = 0;
        spacerLayout.marginBottom = getVerticalMargin() / 2;
        spacerLayout.marginWidth = getHorizontalMargin() / 2;
        spacerComposite.setLayout(spacerLayout);

        locationText = new Text(spacerComposite, SWT.READ_ONLY);
        locationText.setText(serverSigninURL.toString());
        locationText.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(locationText);
    }

    protected LocationListener getLocationListener() {
        return new LocationListener() {
            @Override
            public void changing(final LocationEvent event) {
                log.debug("The browser redirects to the URL: " + event.location); //$NON-NLS-1$
                locationText.setText(event.location);
            }

            @Override
            public void changed(final LocationEvent event) {
                final String message;

                log.debug("The location changed to the URL: " + event.location); //$NON-NLS-1$

                if (event.location.equalsIgnoreCase(serverSigninURL.toString())) {
                    log.trace("location matches to serverSigninURL:" + serverSigninURL); //$NON-NLS-1$
                    if (exception != null
                        && exception.getCredentials() != null
                        && (exception.getCredentials() instanceof CookieCredentials)) {
                        message = MessageFormat.format(
                            Messages.getString("ACSCredentialsDialog.UnauthorizedErrorMessageFormat"), //$NON-NLS-1$
                            serverURI.toString());
                    } else {
                        message = Messages.getString("ACSCredentialsDialog.SignInMessage"); //$NON-NLS-1$
                    }

                    locationText.setText(message);
                } else {
                    message = Messages.getString("ACSCredentialsDialog.DoneMessageText"); //$NON-NLS-1$
                }

                log.trace("set browser text to: " + message); //$NON-NLS-1$
                locationText.setText(message);
            }
        };
    }

    private void checkCredentials() {
        setReturnCode(
            ACSConfigurationResult.SUCCESS.equals(configurationResult) ? IDialogConstants.OK_ID
                : IDialogConstants.CANCEL_ID);

        log.debug("Close the browser: " //$NON-NLS-1$
            + (getReturnCode() == IDialogConstants.OK_ID ? "FedAuth* cookies found" //$NON-NLS-1$
                : "the user has cancelled the browser dialod")); //$NON-NLS-1$

        if (ACSConfigurationResult.FAILURE.equals(configurationResult)) {
            MessageDialog.openError(
                getShell(),
                Messages.getString("ACSCredentialsDialog.ACSFailedErrorTitle"), //$NON-NLS-1$
                Messages.getString("ACSCredentialsDialog.ACSFailedErrorMessage")); //$NON-NLS-1$
        }

        browser.stop();
        close();
    }

    @Override
    protected Control createButtonBar(final Composite parent) {
        return null;
    }

    @Override
    protected void hookDialogAboutToClose() {
        if (notifyToken != null && !notifyToken.isDisposed()) {
            notifyToken.dispose();
            notifyToken = null;
        }

        if (cookies.size() > 0) {
            final Credentials newCredentials = new CookieCredentials(cookies.toArray(new Cookie[cookies.size()]));

            log.debug("New Cookie Credentials created"); //$NON-NLS-1$

            final CredentialsManager credentialsManager =
                EclipseCredentialsManagerFactory.getCredentialsManager(DefaultPersistenceStoreProvider.INSTANCE);

            log.debug("Save the new Cookie Credentials in the Eclipse secure storage for future sessions."); //$NON-NLS-1$
            credentialsManager.setCredentials(new CachedCredentials(serverURI, newCredentials));
            log.debug("The new Cookie Credentials are saved to the Eclipse secure storage."); //$NON-NLS-1$
        }

        ((CredentialsCompleteListener) credentialsCompleteListeners.getListener()).credentialsComplete();
    }

    @Override
    public void addCredentialsCompleteListener(final CredentialsCompleteListener listener) {
        credentialsCompleteListeners.addListener(listener);
    }

    private void onNotifyToken(final List<String> fedAuthCookies) {
        Check.isTrue(fedAuthCookies.size() > 0, "fedAuthCookies.size > 0"); //$NON-NLS-1$

        log.debug("Generating FedAuth* cookies..."); //$NON-NLS-1$

        final String domain = serverURI.getHost();
        int port = serverURI.getPort();
        final boolean secure = "https".equalsIgnoreCase(serverURI.getScheme()); //$NON-NLS-1$
        if (port < 0) {
            if (secure) {
                port = 443;
            } else {
                port = 80;
            }
        }

        log.debug("   domain = " + domain); //$NON-NLS-1$
        log.debug("   port   = " + port); //$NON-NLS-1$

        cookies.clear();

        for (int k = 0; k < fedAuthCookies.size(); k++) {
            final Cookie cookie =
                new Cookie(domain, COOKIE_PREFIX + (k == 0 ? "" : String.valueOf(k)), fedAuthCookies.get(k)); //$NON-NLS-1$
            cookie.setPath("/"); //$NON-NLS-1$
            cookie.setPathAttributeSpecified(true);
            cookie.setSecure(secure);

            log.debug(cookie.getName() + " = " + cookie.toExternalForm()); //$NON-NLS-1$

            cookies.add(cookie);
        }

        configurationResult = ACSConfigurationResult.SUCCESS;
    }

    @Override
    public CookieCredentials getCredentials() {
        return new CookieCredentials(cookies.toArray(new Cookie[cookies.size()]));
    }

    private String loadInterstitial() {
        try {
            final HTMLIncludeHelper includeHelper = new HTMLIncludeHelper(new HTMLIncludeResourceProvider() {
                @Override
                public InputStream getInputStream(final String filename) {
                    Check.notNull(filename, "filename"); //$NON-NLS-1$

                    /*
                     * Note: class resources always use '/' as a separator, not
                     * local path characters.
                     */
                    final String resourcePath = HTML_RESOURCE_PATH + "/" + filename; //$NON-NLS-1$

                    return ACSCredentialsDialog.this.getClass().getResourceAsStream(resourcePath);
                }

                @Override
                public String getMessage(final String key) {
                    return Messages.getString(key);
                }
            });

            return includeHelper.readResource(LOADING_HTML_RESOURCE_NAME);
        } catch (final Exception e) {
            log.warn("Could not load interstitial ACS resource", e); //$NON-NLS-1$
            return Messages.getString("ACSCredentialsDialog.LoadingFallbackMessage"); //$NON-NLS-1$
        }
    }

    private static class ACSConfigurationResult extends TypesafeEnum {
        /**
         * Authentication is not complete, continue running the web-based
         * authentication.
         */
        public static final ACSConfigurationResult CONTINUE = new ACSConfigurationResult(0);

        /** Authentication completed, but failed. Stop processing. */
        public static final ACSConfigurationResult FAILURE = new ACSConfigurationResult(1);

        /** Authentication completed successfully. Stop processing. */
        public static final ACSConfigurationResult SUCCESS = new ACSConfigurationResult(2);

        private ACSConfigurationResult(final int value) {
            super(value);
        }
    }

    private class NotifyTokenBrowserFunction extends BrowserFunction {
        public NotifyTokenBrowserFunction(final Browser browser, final String name) {
            super(browser, name);
        }

        @Override
        public Object function(final Object[] arguments) {
            log.debug("window.notifyToken(...) invoked"); //$NON-NLS-1$

            Check.notNull(arguments, "arguments"); //$NON-NLS-1$
            Check.isTrue(arguments.length == 1, "arguments length"); //$NON-NLS-1$
            Check.isTrue(arguments[0] instanceof String, "argument[0]"); //$NON-NLS-1$

            final String arg = (String) arguments[0];
            log.debug("   argument = " + arg); //$NON-NLS-1$

            try {
                @SuppressWarnings("unchecked")
                final Map<String, List<String>> tokenData = getObjectMapper().readValue(arg, Map.class);
                onNotifyToken(tokenData.get("securityToken")); //$NON-NLS-1$
            } catch (final Exception e) {
            }

            checkCredentials();

            return null;
        }

        private ObjectMapper getObjectMapper() {
            final ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

            return objectMapper;
        }
    }

    public static class VSTSCredentialsDialog extends ACSCredentialsDialog {

        public VSTSCredentialsDialog(final Shell parentShell) {
            super(parentShell, URIUtils.VSTS_ROOT_URL, URIUtils.VSTS_ROOT_URL, null);
        }

        @Override
        protected LocationListener getLocationListener() {
            return null;
        }

        @Override
        protected URI getSignInURI(final URI serverSigninURL) {
            final Map<String, String> queryParameters = new HashMap<String, String>();
            queryParameters.put("realm", URIUtils.TFS_REALM_URL_STRING); //$NON-NLS-1$
            queryParameters.put("protocol", "javascriptnotify"); //$NON-NLS-1$ //$NON-NLS-2$
            queryParameters.put("force", "1"); //$NON-NLS-1$ //$NON-NLS-2$
            queryParameters.put("compact", "1"); //$NON-NLS-1$ //$NON-NLS-2$

            return URIUtils.addQueryParameters(URIUtils.newURI(URIUtils.VSTS_ROOT_SIGNIN_URL_STRING), queryParameters);
        }
    }
}
