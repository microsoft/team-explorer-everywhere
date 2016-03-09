// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.dialogs.connect;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.config.UIClientConnectionAdvisor;
import com.microsoft.tfs.client.common.ui.controls.generic.FullFeaturedBrowser;
import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.helpers.HTMLIncludeHelper;
import com.microsoft.tfs.client.common.ui.helpers.HTMLIncludeHelper.HTMLIncludeResourceProvider;
import com.microsoft.tfs.client.common.ui.prefs.UIPreferenceConstants;
import com.microsoft.tfs.core.config.ConnectionInstanceData;
import com.microsoft.tfs.core.httpclient.Cookie;
import com.microsoft.tfs.core.httpclient.CookieCredentials;
import com.microsoft.tfs.core.httpclient.Header;
import com.microsoft.tfs.core.httpclient.HttpClient;
import com.microsoft.tfs.core.httpclient.HttpStatus;
import com.microsoft.tfs.core.httpclient.NameValuePair;
import com.microsoft.tfs.core.httpclient.cookie.CookiePolicy;
import com.microsoft.tfs.core.httpclient.cookie.CookieSpec;
import com.microsoft.tfs.core.httpclient.methods.PostMethod;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.ws.runtime.exceptions.FederatedAuthException;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.Platform;
import com.microsoft.tfs.util.TypesafeEnum;
import com.microsoft.tfs.util.listeners.SingleListenerFacade;

/**
 * Provide a dialog to handle ACS authentication.
 *
 * @threadsafety unknown
 */
public class ACSCredentialsDialogD11 extends CredentialsCompleteDialog {
    private static final Log log = LogFactory.getLog(ACSCredentialsDialogD11.class);

    /*
     * Minimum version of SWT required to use the HTML evaluate method. 3.5 and
     * later are supported.
     */
    private static final int MINIMUM_SWT_VERSION = 3500;

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

    private static final String ACS_SCHEME = "https"; //$NON-NLS-1$
    private static final String ACS_DOMAIN = ".accesscontrol.windows.net"; //$NON-NLS-1$
    private static final String ACS_PATH = "/v2/wsfederation"; //$NON-NLS-1$
    private static final String ACS_QUERY = "wa=wsignin1.0"; //$NON-NLS-1$

    private final List<Cookie> cookies = new ArrayList<Cookie>();

    /*
     * Whether we should start processing on the authentication provider page
     * (to work around a webkit bug) instead of the normal processing beginning
     * at the ACS page.
     */
    private Boolean useAuthenticationProviderPage = null;

    private Text locationText;
    private FullFeaturedBrowser browser;

    private HttpClient httpClient;

    private final SingleListenerFacade credentialsCompleteListeners =
        new SingleListenerFacade(CredentialsCompleteListener.class);

    public ACSCredentialsDialogD11(
        final Shell parentShell,
        final URI serverURI,
        final URI serverSigninURL,
        final FederatedAuthException exception) {
        super(parentShell);

        Check.notNull(serverURI, "serverURI"); //$NON-NLS-1$
        Check.notNull(serverSigninURL, "serverSigninURL"); //$NON-NLS-1$
        Check.notNull(exception, "exception"); //$NON-NLS-1$

        this.serverURI = serverURI;
        this.serverSigninURL = serverSigninURL;
        this.exception = exception;

        setOptionIncludeDefaultButtons(false);
        setOptionEnforceMinimumSize(false);
    }

    /**
     * Gets whether {@link ACSCredentialsDialog} is available for the running
     * platform (browser requirements are met). This status is advisory - you
     * may still open a {@link ACSCredentialsDialog}, however it will likely not
     * provide authentication services to the client (likely due to lack of
     * Javascript.)
     *
     * @return <code>true</code> if {@link ACSCredentialsDialog} is supported on
     *         the running platform, <code>false</code> if minimum browser
     *         requirements are not met (see the log for details)
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

        final int width = Math.min((int) (parentBounds.width * 0.75), 976);
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
                            browser.setUrl(serverSigninURL.toString());
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

        browser = new FullFeaturedBrowser(dialogArea, SWT.NONE, getBrowserStyle());

        GridDataBuilder.newInstance().grab().fill().wHint((int) (getMinimumMessageAreaWidth() * 1.5)).hHint(
            getMinimumMessageAreaWidth()).applyTo(browser);

        browser.setText(loadInterstitial());

        browser.addLocationListener(new LocationListener() {
            @Override
            public void changing(final LocationEvent event) {
                locationText.setText(event.location);

                /*
                 * The browser is trying to navigate to the signin completion
                 * page, that means the current page is the page that posts back
                 * to TFS (and thus gets cookies set.) Intercept here so that we
                 * can get the cookies and configure the profile appropriately.
                 *
                 * On error, show a dialog but still close the page.
                 */

                URI locationURI;

                try {
                    locationURI = URIUtils.newURI(event.location);
                } catch (final IllegalArgumentException e) {
                    log.warn(MessageFormat.format("Server redirected to an unparseable URL: {0}", event.location), e); //$NON-NLS-1$
                    return;
                }

                ACSConfigurationResult configurationResult = ACSConfigurationResult.CONTINUE;

                try {
                    configurationResult = configureAuthenticationFromCurrentPage(locationURI);
                } catch (final Exception e) {
                    log.error("Could not process ACS authentication", e); //$NON-NLS-1$
                    configurationResult = ACSConfigurationResult.FAILURE;
                }

                /* Return to continue processing. */
                if (ACSConfigurationResult.CONTINUE.equals(configurationResult)) {
                    return;
                }

                /* Stop the browser, close this dialog and stop processing. */
                setReturnCode(
                    ACSConfigurationResult.SUCCESS.equals(configurationResult) ? IDialogConstants.OK_ID
                        : IDialogConstants.CANCEL_ID);
                browser.stop();
                close();

                if (ACSConfigurationResult.FAILURE.equals(configurationResult)) {
                    MessageDialog.openError(
                        getShell(),
                        Messages.getString("ACSCredentialsDialog.ACSFailedErrorTitle"), //$NON-NLS-1$
                        Messages.getString("ACSCredentialsDialog.ACSFailedErrorMessage")); //$NON-NLS-1$
                }

                event.doit = false;
                return;
            }

            @Override
            public void changed(final LocationEvent event) {
                final String message;

                log.debug("The location changed to the URL: " + event.location); //$NON-NLS-1$

                if (event.location.equalsIgnoreCase(serverSigninURL.toString())) {
                    if (exception.getCredentials() != null
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

                locationText.setText(message);
            }
        });

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

    @Override
    protected Control createButtonBar(final Composite parent) {
        return null;
    }

    @Override
    protected void hookDialogAboutToClose() {
        ((CredentialsCompleteListener) credentialsCompleteListeners.getListener()).credentialsComplete();
    }

    @Override
    public void addCredentialsCompleteListener(final CredentialsCompleteListener listener) {
        credentialsCompleteListeners.addListener(listener);
    }

    /*
     * On Mac OS we need to start our hijacking at the authentication provider's
     * page, when running under WebKit 534.56.5 (included with Mac OS 10.7.4.)
     */
    private boolean useAuthenticationProviderPage() {
        if (useAuthenticationProviderPage == null) {
            useAuthenticationProviderPage = Boolean.FALSE;

            if (Platform.isCurrentPlatform(Platform.MAC_OS_X)) {
                try {
                    final String userAgent = (String) browser.evaluate("return navigator.userAgent;"); //$NON-NLS-1$

                    if (userAgent.contains("AppleWebKit/534.56.5 ")) //$NON-NLS-1$
                    {
                        useAuthenticationProviderPage = Boolean.TRUE;
                    }
                } catch (final Exception e) {
                    log.warn("Could not determine Webkit browser version", e); //$NON-NLS-1$
                }
            }
        }

        return useAuthenticationProviderPage.booleanValue();
    }

    private ACSConfigurationResult configureAuthenticationFromCurrentPage(final URI locationURI) throws Exception {
        /*
         * Mac OS 10.7.4's Webkit has a bug where it caches post data
         * incorrectly, causing it to resubmit an old form back to ACS. Thus, we
         * need to hijack the form bound for ACS and submit it ourselves,
         * wherein we can process the result and send that to TFS service.
         */
        if (useAuthenticationProviderPage()
            && ACS_SCHEME.equalsIgnoreCase(locationURI.getScheme())
            && locationURI.getHost() != null
            && locationURI.getHost().toLowerCase().endsWith(ACS_DOMAIN)
            && ACS_PATH.equalsIgnoreCase(locationURI.getPath())
            && ACS_QUERY.equalsIgnoreCase(locationURI.getQuery())) {
            return configureAuthenticationFromAuthProviderPage();
        }

        /*
         * General case: hijack the post results going to TFS service and we'll
         * submit them ourselves to capture the cookie.
         */
        if (locationURI.getHost().equalsIgnoreCase(serverSigninURL.getHost())
            && locationURI.getPath().equalsIgnoreCase(serverSigninURL.getPath())) {
            final String[] queryPairs = locationURI.getQuery().split("&"); //$NON-NLS-1$

            for (final String pair : queryPairs) {
                final String[] keyValue = pair.split("=", 2); //$NON-NLS-1$
                final String key;
                final String value;

                try {
                    key = URLDecoder.decode(keyValue[0], "UTF-8"); //$NON-NLS-1$
                    value = URLDecoder.decode(keyValue[1], "UTF-8"); //$NON-NLS-1$
                } catch (final UnsupportedEncodingException e) {
                    log.warn("Could not decode location URI query parameters as UTF-8", e); //$NON-NLS-1$
                    break;
                }

                /*
                 * The "complete=1" param means that auth has completed and we
                 * can harvest the cookies from the ACS page.
                 */
                if ("complete".equalsIgnoreCase(key) && "1".equals(value)) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    return configureAuthenticationFromACSPage();
                }
            }
        }

        /* This page is not helpful, continue processing. */
        return ACSConfigurationResult.CONTINUE;
    }

    /*
     * Configure authentication by submitting some authentication provider's
     * data to ACS, then taking the ACS result and submitting that back to TFS.
     * This is not the preferred way to handle this, but may be necessary if
     * there's some sort of browser bug in authentication handling w/ ACS.
     */
    private ACSConfigurationResult configureAuthenticationFromAuthProviderPage() throws Exception {
        final Object hasSwtForm = browser.evaluate(
            "if(document != null && document.forms != null && document.forms.length == 1) { return true; } return false;"); //$NON-NLS-1$

        if (!Boolean.TRUE.equals(hasSwtForm)) {
            throw new Exception("Document does not contain ACS form data"); //$NON-NLS-1$
        }

        /* Determine the location of the hidden ACS form */
        final String postLocation = (String) browser.evaluate("return document.forms[0].action;"); //$NON-NLS-1$

        /* Sanity check that we're posting back to our original server. */
        if (postLocation == null) {
            throw new Exception("ACS form does not have an action"); //$NON-NLS-1$
        }

        /* Determine the number of elements in the ACS form */
        int elementCount = 0;
        final Object elementCountObj = browser.evaluate("return document.forms[0].elements.length;"); //$NON-NLS-1$

        if (elementCountObj instanceof Integer) {
            elementCount = ((Integer) elementCountObj).intValue();
        } else if (elementCountObj instanceof Double) {
            elementCount = (int) (((Double) elementCountObj).doubleValue());
        } else {
            throw new Exception(
                MessageFormat.format("Could not deserialize ACS form element length ({0})", elementCountObj)); //$NON-NLS-1$
        }

        if (elementCount == 0) {
            throw new Exception("No ACS form elements"); //$NON-NLS-1$
        }

        final NameValuePair[] postParameters = new NameValuePair[elementCount];
        for (int i = 0; i < elementCount; i++) {
            postParameters[i] = new NameValuePair(
                (String) browser.evaluate(
                    MessageFormat.format("return document.forms[0].elements[{0}].name;", Integer.toString(i))), //$NON-NLS-1$
                (String) browser.evaluate(
                    MessageFormat.format("return document.forms[0].elements[{0}].value;", Integer.toString(i)))); //$NON-NLS-1$
        }

        return configureAuthenticationFromAuthProvider(postLocation, postParameters);
    }

    private ACSConfigurationResult configureAuthenticationFromAuthProvider(
        final String target,
        final NameValuePair[] formInput) throws Exception {
        final URI targetURI;

        try {
            targetURI = new URI(target);
        } catch (final Exception e) {
            log.warn(MessageFormat.format("ACS form action is not parseable: {0}", target), e); //$NON-NLS-1$
            throw new Exception("ACS form action is not parseable"); //$NON-NLS-1$
        }

        /* Make sure we're posting back to ACS */
        if (!ACS_SCHEME.equalsIgnoreCase(targetURI.getScheme())
            || targetURI.getHost() == null
            || !targetURI.getHost().toLowerCase().endsWith(ACS_DOMAIN)
            || !ACS_PATH.equalsIgnoreCase(targetURI.getPath())
            || !ACS_QUERY.equalsIgnoreCase(targetURI.getQuery())) {
            throw new Exception(MessageFormat.format("ACS form location is not in the domain: {0}", ACS_DOMAIN)); //$NON-NLS-1$
        }

        /*
         * Build a connection to the server to submit the form.
         */
        final HttpClient httpClient = getHttpClient();
        final PostMethod postMethod = new PostMethod(target);

        /*
         * Ignore cookies, do not follow redirects, do not do authentication. We
         * expect the resultant page (and only the resultant page) to populate
         * our cookies.
         */
        postMethod.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        postMethod.setFollowRedirects(false);
        postMethod.setDoAuthentication(false);
        postMethod.getParams().setContentCharset("UTF-8"); //$NON-NLS-1$

        /* Add the ACS form elements */
        for (int i = 0; i < formInput.length; i++) {
            postMethod.addParameter(formInput[i]);
        }

        final int status = httpClient.executeMethod(postMethod);

        if (status != HttpStatus.SC_OK) {
            final String message =
                MessageFormat.format("ACS authentication did not return success: {0}", Integer.toString(status)); //$NON-NLS-1$
            throw new Exception(message);
        }

        final SAXParser acsResultParser = SAXParserFactory.newInstance().newSAXParser();
        final ACSResultHandler acsResultHandler = new ACSResultHandler();
        acsResultParser.parse(postMethod.getResponseBodyAsStream(), acsResultHandler);

        final String finalTarget = acsResultHandler.getFormAction();
        final NameValuePair[] finalParameters = acsResultHandler.getFormInputs();

        return configureAuthenticationFromACS(finalTarget, finalParameters);
    }

    /*
     * Configure authentication by submitting the ACS result form to TFS, based
     * on the contents of the current browser's document. The current browser
     * document should have the form to post to ACS, which we will do. This is
     * generally the "best" way to handle authentication to TFS service as it
     * allows the browser to handle as much as possible.
     */
    private ACSConfigurationResult configureAuthenticationFromACSPage() throws Exception {
        final Object hasSwtForm =
            browser.evaluate("if(document != null && document.hiddenform != null) { return true; } return false;"); //$NON-NLS-1$

        if (!Boolean.TRUE.equals(hasSwtForm)) {
            throw new Exception("Document does not contain ACS form data"); //$NON-NLS-1$
        }

        /* Determine the location of the hidden ACS form */
        final String postLocation = (String) browser.evaluate("return document.hiddenform.action;"); //$NON-NLS-1$

        /* Sanity check that we're posting back to our original server. */
        if (postLocation == null) {
            throw new Exception("ACS form does not have an action"); //$NON-NLS-1$
        }

        /* Determine the number of elements in the ACS form */
        int elementCount = 0;
        final Object elementCountObj = browser.evaluate("return document.hiddenform.elements.length;"); //$NON-NLS-1$

        if (elementCountObj instanceof Integer) {
            elementCount = ((Integer) elementCountObj).intValue();
        } else if (elementCountObj instanceof Double) {
            elementCount = (int) (((Double) elementCountObj).doubleValue());
        } else {
            throw new Exception(
                MessageFormat.format("Could not deserialize ACS form element length ({0})", elementCountObj)); //$NON-NLS-1$
        }

        if (elementCount == 0) {
            throw new Exception("No ACS form elements"); //$NON-NLS-1$
        }

        final NameValuePair[] postParameters = new NameValuePair[elementCount];
        for (int i = 0; i < elementCount; i++) {
            postParameters[i] = new NameValuePair(
                (String) browser.evaluate(
                    MessageFormat.format("return document.hiddenform.elements[{0}].name;", Integer.toString(i))), //$NON-NLS-1$
                (String) browser.evaluate(
                    MessageFormat.format("return document.hiddenform.elements[{0}].value;", Integer.toString(i)))); //$NON-NLS-1$
        }

        return configureAuthenticationFromACS(postLocation, postParameters);
    }

    private ACSConfigurationResult configureAuthenticationFromACS(final String target, final NameValuePair[] formInput)
        throws Exception {
        final URI targetURI;

        try {
            targetURI = new URI(target);
        } catch (final Exception e) {
            log.warn(MessageFormat.format("ACS form action is not parseable: {0}", target), e); //$NON-NLS-1$
            throw new Exception("ACS form action is not parseable"); //$NON-NLS-1$
        }

        /* Make sure we're posting back to the original host */
        if (!targetURI.getHost().equalsIgnoreCase(serverSigninURL.getHost())) {
            throw new Exception("ACS form location does not match initial target"); //$NON-NLS-1$
        }

        /*
         * Build a connection to the server, just to get the cookies. We'll
         * throw this away.
         */
        final HttpClient httpClient = getHttpClient();
        final PostMethod postMethod = new PostMethod(target);

        /*
         * Ignore cookies, do not follow redirects, do not do authentication. We
         * expect the resultant page (and only the resultant page) to populate
         * our cookies.
         */
        postMethod.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        postMethod.setFollowRedirects(false);
        postMethod.setDoAuthentication(false);

        /*
         * Set the content charset to UTF-8: this is the charset that the post
         * data will be encoded with. The default (according to RFC 2616) is
         * that the data is delivered as ISO-8859-1. IIS, however, appears to
         * want to decode this as UTF-8. The name of the Windows Live provider
         * is currently "Windows Live(tm) ID", which means that we need to
         * ensure that we encode the (tm) correctly.
         */
        postMethod.getParams().setContentCharset("UTF-8"); //$NON-NLS-1$

        /* Add the ACS form elements */
        for (int i = 0; i < formInput.length; i++) {
            postMethod.addParameter(formInput[i]);
        }

        final int status = httpClient.executeMethod(postMethod);

        final ArrayList<Cookie> fedAuthCookies = new ArrayList<Cookie>();

        /* Expect a 302 to the LocationService */
        if (status == HttpStatus.SC_MOVED_TEMPORARILY) {
            final Header[] cookieHeaders = postMethod.getResponseHeaders("Set-Cookie"); //$NON-NLS-1$

            if (cookieHeaders.length == 0) {
                throw new Exception("Team Foundation Server did not return FedAuth tokens"); //$NON-NLS-1$
            }

            /* Parse cookies according to RFC2109 */
            final CookieSpec cookieParser = CookiePolicy.getCookieSpec(CookiePolicy.RFC_2109);

            for (final Header cookieHeader : cookieHeaders) {
                /*
                 * Parse the cookie headers, store the serialized cookies in the
                 * profile.
                 */
                try {
                    final Cookie[] cookies = cookieParser.parse(targetURI, cookieHeader.getValue());

                    for (final Cookie cookie : cookies) {
                        if (cookie.getName().startsWith("FedAuth")) //$NON-NLS-1$
                        {
                            fedAuthCookies.add(cookie);
                        }
                    }

                } catch (final Exception e) {
                    log.warn(
                        MessageFormat.format("Could not parse authentication cookie {0}", cookieHeader.getValue()), //$NON-NLS-1$
                        e);
                }
            }
        } else {
            String logMessage = "ACS authentication to TFS failed."; //$NON-NLS-1$
            final String exceptionMessage =
                MessageFormat.format(
                    "Received unexpected HTTP status code during authentication: {0}.", //$NON-NLS-1$
                    Integer.toString(status));

            if (status == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
                try {
                    final Header[] headers = postMethod.getResponseHeaders("X-TFS-ServiceError"); //$NON-NLS-1$

                    for (final Header header : headers) {
                        logMessage +=
                            MessageFormat.format("  TFS error was: {0}", URLDecoder.decode(header.getValue(), "UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                } catch (final Exception e) {
                    // suppress - must not have had service error headers.
                }
            }

            log.warn(logMessage);
            throw new Exception(exceptionMessage);
        }

        cookies.clear();
        cookies.addAll(fedAuthCookies);

        return ACSConfigurationResult.SUCCESS;
    }

    private HttpClient getHttpClient() {
        if (httpClient == null) {
            final ConnectionInstanceData instanceData = new ConnectionInstanceData(serverURI, GUID.newGUID());

            final UIClientConnectionAdvisor connectionAdvisor = new UIClientConnectionAdvisor();
            httpClient = connectionAdvisor.getHTTPClientFactory(instanceData).newHTTPClient();
        }

        return httpClient;
    }

    public List<Cookie> getFederatedAuthenticationCookies() {
        return cookies;
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

                    return ACSCredentialsDialogD11.this.getClass().getResourceAsStream(resourcePath);
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

    private static class ACSResultHandler extends DefaultHandler {
        private final List<String> elementHierarchy = new ArrayList<String>();

        private boolean inForm = false;
        private String formAction;
        private final List<NameValuePair> formInputs = new ArrayList<NameValuePair>();

        @Override
        public void startElement(
            final String namespaceURI,
            final String localName,
            final String qualifiedName,
            final Attributes attributes) throws SAXException {
            elementHierarchy.add(qualifiedName);

            if (isElement("html", "body", "form")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            {
                if ("hiddenform".equals(attributes.getValue("name"))) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    if (attributes.getValue("action") == null) //$NON-NLS-1$
                    {
                        throw new SAXException("ACS form lacks action"); //$NON-NLS-1$
                    }

                    inForm = true;
                    formAction = attributes.getValue("action"); //$NON-NLS-1$
                }
            } else if (inForm && isElement("html", "body", "form", "input")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
            {
                if (!"hidden".equalsIgnoreCase(attributes.getValue("type"))) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    throw new SAXException(
                        MessageFormat.format(
                            "Expected hidden input type in ACS result form, got: {0}", //$NON-NLS-1$
                            attributes.getValue("type"))); //$NON-NLS-1$
                }

                if (attributes.getValue("name") == null || attributes.getValue("value") == null) //$NON-NLS-1$ //$NON-NLS-2$
                {
                    throw new SAXException("Malformed input element - missing name / value pair"); //$NON-NLS-1$
                }

                formInputs.add(new NameValuePair(attributes.getValue("name"), attributes.getValue("value"))); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        @Override
        public void endElement(final String namespaceURI, final String localName, final String qualifiedName)
            throws SAXException {
            if (elementHierarchy.size() == 0) {
                throw new SAXException(MessageFormat.format("Malformed closing element: {0}", qualifiedName)); //$NON-NLS-1$
            }

            if (!qualifiedName.equalsIgnoreCase(elementHierarchy.get(elementHierarchy.size() - 1))) {
                throw new SAXException(MessageFormat.format("Received unexpected closing element: {0}", qualifiedName)); //$NON-NLS-1$
            }

            if (inForm && isElement("html", "body", "form")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            {
                inForm = false;
            }

            elementHierarchy.remove(elementHierarchy.size() - 1);
        }

        @Override
        public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException {
            log.warn(MessageFormat.format("Refused to resolve entity {0} / {1}", publicId, systemId)); //$NON-NLS-1$

            return null;
        }

        private boolean isElement(final String... elements) {
            Check.notNull(elements, "elements"); //$NON-NLS-1$

            if (elementHierarchy.size() != elements.length) {
                return false;
            }

            for (int i = 0; i < elements.length; i++) {
                if (!elements[i].equalsIgnoreCase(elementHierarchy.get(i))) {
                    return false;
                }
            }

            return true;
        }

        public String getFormAction() {
            return formAction;
        }

        public NameValuePair[] getFormInputs() {
            return formInputs.toArray(new NameValuePair[formInputs.size()]);
        }
    }
}
