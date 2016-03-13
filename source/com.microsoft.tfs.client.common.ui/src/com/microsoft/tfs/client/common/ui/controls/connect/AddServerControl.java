// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.controls.connect;

import java.net.URI;
import java.text.MessageFormat;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.controls.generic.BaseControl;
import com.microsoft.tfs.client.common.ui.framework.helper.SWTUtil;
import com.microsoft.tfs.client.common.ui.framework.layout.GridDataBuilder;
import com.microsoft.tfs.client.common.ui.framework.sizing.ControlSize;
import com.microsoft.tfs.client.common.ui.helpers.AutomationIDHelper;
import com.microsoft.tfs.core.util.ServerURIUtils;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.valid.Validatable;
import com.microsoft.tfs.util.valid.Validator;

/**
 *
 *
 * @threadsafety unknown
 */
public class AddServerControl extends BaseControl implements Validatable {
    private static final Log log = LogFactory.getLog(AddServerControl.class);

    private static final String DEFAULT_PATH = "tfs"; //$NON-NLS-1$

    /* These are the ports we use as defaults - ie, TFS is on port 8080. */
    private static final String DEFAULT_PORT_HTTP = "8080"; //$NON-NLS-1$
    private static final String DEFAULT_PORT_HTTPS = "443"; //$NON-NLS-1$

    /*
     * These are the ports the rest of the world uses as defaults, ie well-known
     * ports defined by IETF.
     */
    private static final String WELL_KNOWN_PORT_HTTP = "80"; //$NON-NLS-1$
    private static final String WELL_KNOWN_PORT_HTTPS = "443"; //$NON-NLS-1$

    public static final String SERVER_TEXT_ID = "AddServerControl.serverText"; //$NON-NLS-1$
    public static final String PATH_TEXT_ID = "AddServerControl.pathText"; //$NON-NLS-1$
    public static final String PORT_TEXT_ID = "AddServerControl.portText"; //$NON-NLS-1$
    public static final String PROTOCOL_HTTP_BUTTON_ID = "AddServerControl.protocolHttpButton"; //$NON-NLS-1$
    public static final String PROTOCOL_HTTPS_BUTTON_ID = "AddServerControl.protocolHttpsButton"; //$NON-NLS-1$

    private final Text serverText;
    private final Group connectionDetailsGroup;
    private final Text pathText;
    private final Text portText;
    private final Button httpButton, httpsButton;
    private final Text previewText;
    private final TextControlURLOrHostnameValidator validator;

    private URI serverURI;

    public AddServerControl(final Composite parent, final int style) {
        super(parent, style);

        final GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = getHorizontalSpacing();
        layout.verticalSpacing = getVerticalSpacing();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        setLayout(layout);

        final Label serverPromptLabel = new Label(this, SWT.NONE);
        serverPromptLabel.setText(Messages.getString("AddServerControl.ServerPrompt")); //$NON-NLS-1$
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).applyTo(serverPromptLabel);

        serverText = new Text(this, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).wHint(getMinimumMessageAreaWidth()).applyTo(serverText);
        AutomationIDHelper.setWidgetID(serverText, SERVER_TEXT_ID);

        // Create a validator for the server textbox. Make sure to hook up the
        // text box modifier listener after the validator so that the validator
        // gets run before the validation of all fields.
        validator =
            new TextControlURLOrHostnameValidator(
                serverText,
                Messages.getString("AddServerControl.NameOrUrlFieldName"), //$NON-NLS-1$
                true);

        serverText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                refresh();
            }
        });

        connectionDetailsGroup = new Group(this, SWT.NONE);
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).applyTo(connectionDetailsGroup);

        final GridLayout connectionDetailsGroupLayout = new GridLayout(3, false);
        connectionDetailsGroupLayout.horizontalSpacing = getHorizontalSpacing();
        connectionDetailsGroupLayout.verticalSpacing = getVerticalSpacing();
        connectionDetailsGroupLayout.marginWidth = getHorizontalMargin();
        connectionDetailsGroupLayout.marginHeight = getVerticalMargin();
        connectionDetailsGroup.setLayout(connectionDetailsGroupLayout);
        connectionDetailsGroup.setText(Messages.getString("AddServerControl.ConnectionDetailsGroupText")); //$NON-NLS-1$

        final Label pathPromptLabel = new Label(connectionDetailsGroup, SWT.NONE);
        pathPromptLabel.setText(Messages.getString("AddServerControl.PathPrompt")); //$NON-NLS-1$

        pathText = new Text(connectionDetailsGroup, SWT.BORDER);
        GridDataBuilder.newInstance().hGrab().hFill().hSpan(2).applyTo(pathText);
        pathText.setText(DEFAULT_PATH);
        pathText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                refresh();
            }
        });

        final Label portPromptLabel = new Label(connectionDetailsGroup, SWT.NONE);
        portPromptLabel.setText(Messages.getString("AddServerControl.PortPrompt")); //$NON-NLS-1$

        portText = new Text(connectionDetailsGroup, SWT.BORDER);
        GridDataBuilder.newInstance().hSpan(2).applyTo(portText);
        ControlSize.setCharWidthHint(portText, 8);
        portText.setText(DEFAULT_PORT_HTTP);
        portText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                refresh();
            }
        });

        final Label protocolLabel = new Label(connectionDetailsGroup, SWT.NONE);
        protocolLabel.setText(Messages.getString("AddServerControl.ProtocolPrompt")); //$NON-NLS-1$

        final Composite buttonComposite = new Composite(connectionDetailsGroup, SWT.NONE);
        final GridLayout buttonCompositeLayout = new GridLayout(2, false);
        buttonCompositeLayout.horizontalSpacing = getHorizontalSpacing();
        buttonCompositeLayout.verticalSpacing = getVerticalSpacing();
        buttonCompositeLayout.marginWidth = 0;
        buttonCompositeLayout.marginHeight = 0;
        buttonComposite.setLayout(buttonCompositeLayout);

        httpButton = new Button(buttonComposite, SWT.RADIO);
        httpButton.setText(Messages.getString("AddServerControl.ProtocolHTTPButton")); //$NON-NLS-1$
        httpButton.setSelection(true);
        httpButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                protocolButtonSelected();
            }
        });

        httpsButton = new Button(buttonComposite, SWT.RADIO);
        httpsButton.setText(Messages.getString("AddServerControl.ProtocolHTTPSButton")); //$NON-NLS-1$
        httpsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                protocolButtonSelected();
            }
        });

        final Label previewPromptLabel = new Label(this, SWT.NONE);
        previewPromptLabel.setText(Messages.getString("AddServerControl.PreviewPrompt")); //$NON-NLS-1$

        previewText = new Text(this, SWT.READ_ONLY | SWT.BORDER);
        previewText.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        GridDataBuilder.newInstance().hGrab().hFill().applyTo(previewText);

        /* Exclude the preview text control from the tab list */
        this.setTabList(new Control[] {
            serverText,
            connectionDetailsGroup
        });

        refresh();
    }

    private void protocolButtonSelected() {
        /*
         * If the HTTP button was selected and the port is the default HTTPS
         * port, update it to be the default HTTP port.
         */
        if (httpButton.getSelection() && DEFAULT_PORT_HTTPS.equals(portText.getText())) {
            portText.setText(DEFAULT_PORT_HTTP);
        }
        /*
         * If the HTTP button was selected and the port is the default HTTPS
         * port, update it to be the default HTTP port.
         */
        else if (httpsButton.getSelection() && DEFAULT_PORT_HTTP.equals(portText.getText())) {
            portText.setText(DEFAULT_PORT_HTTPS);
        }
        /* Otherwise, just do a refresh to update the preview. */
        else {
            refresh();
        }
    }

    private void refresh() {
        // Bail if the server name validator found an error.
        if (validator.getValidity().isValid() == false) {
            SWTUtil.setCompositeEnabled(connectionDetailsGroup, true);
            serverURI = null;
            previewText.setText(validator.getValidity().getFirstMessage().getMessage());
            return;
        }

        final String serverName = serverText.getText().trim();

        /*
         * Look for a colon in the server name - this would be an invalid
         * hostname (unless it begins with a '[' and is an ipv6 address) and we
         * thus treat it as a URI. (We're more opportunistic about what we
         * determine to be a URI.)
         */
        if (serverName.contains(":") && !serverName.startsWith("[")) //$NON-NLS-1$ //$NON-NLS-2$
        {
            try {
                serverURI = ServerURIUtils.normalizeURI(URIUtils.newURI(serverName), true);

                /* Make sure this is a complete URI. */
                if (serverURI.getHost() != null) {
                    SWTUtil.setCompositeEnabled(connectionDetailsGroup, false);

                    if (ServerURIUtils.isHosted(serverURI)) {
                        serverURI = URIUtils.newURI(
                            ServerURIUtils.HOSTED_SERVICE_DEFAULT_SCHEME,
                            serverURI.getAuthority(),
                            null,
                            null,
                            null);
                    }

                    previewText.setText(serverURI.toString());
                    return;
                }
            } catch (final Exception e) {
                log.error("Error processing server URL: ", e); //$NON-NLS-1$
            }

            serverURI = null;
            previewText.setText(Messages.getString("AddServerControl.ErrorServerNameEmpty")); //$NON-NLS-1$
            return;
        }

        /* The user is entering a hostname, not a URI. */

        final String enteredPath = pathText.getText().trim();
        final String path = enteredPath.startsWith("/") ? enteredPath : "/" + enteredPath; //$NON-NLS-1$ //$NON-NLS-2$
        final String port = portText.getText().trim();
        final boolean https = httpsButton.getSelection();
        final String scheme = https ? "https" : "http"; //$NON-NLS-1$ //$NON-NLS-2$

        /*
         * If the host name matches our hosted service, choose the correct
         * scheme for the user. Split off any path part they may have typed so
         * we can rejoin it when we build the URI (VS does this).
         */
        final String[] serverNameAndPath = serverName.split(Pattern.quote("/"), 2); //$NON-NLS-1$
        if (serverNameAndPath.length > 0) {
            final String uriServerName = serverNameAndPath[0];

            // Check for match with a hosted suffix
            final boolean isHosted = ServerURIUtils.isHosted(uriServerName);

            // DNS names may end with a final ".", so check for both.
            if (isHosted) {
                SWTUtil.setCompositeEnabled(connectionDetailsGroup, false);

                try {
                    serverURI = ServerURIUtils.normalizeURI(
                        URIUtils.newURI(ServerURIUtils.HOSTED_SERVICE_DEFAULT_SCHEME, uriServerName, null, null, null),
                        true);
                } catch (final IllegalArgumentException e) {
                    log.error("Error processing server URL: ", e); //$NON-NLS-1$
                    serverURI = null;
                    previewText.setText(Messages.getString("AddServerControl.ErrorServerNameEmpty")); //$NON-NLS-1$
                    return;
                }

                previewText.setText(serverURI.toString());
                return;
            }
        }

        /*
         * Reenable connection details group if this is the first time through
         * the refresh after user clears a URI.
         */
        SWTUtil.setCompositeEnabled(connectionDetailsGroup, true);

        /* Server name is required. */
        if (serverName.length() == 0) {
            serverURI = null;
            previewText.setText(Messages.getString("AddServerControl.ErrorServerNameEmpty")); //$NON-NLS-1$
            return;
        }

        /* Port is required. */
        if (StringUtil.isNullOrEmpty(port)) {
            serverURI = null;
            previewText.setText(Messages.getString("AddServerControl.ErrorPortEmpty")); //$NON-NLS-1$
            return;
        }

        /* Check port for validity. */
        try {
            Integer.parseInt(port);
        } catch (final NumberFormatException e) {
            serverURI = null;
            previewText.setText(Messages.getString("AddServerControl.ErrorPortEmpty")); //$NON-NLS-1$
            return;
        }

        /*
         * Determine the authority - the host:port pair. Only specify the port
         * if it is not the well-known port for the scheme (ie, port 80 for
         * http.)
         */
        final String authority;

        if ((!https && WELL_KNOWN_PORT_HTTP.equals(port)) || (https && WELL_KNOWN_PORT_HTTPS.equals(port))) {
            authority = serverName;
        } else {
            authority = MessageFormat.format("{0}:{1}", serverName, port); //$NON-NLS-1$
        }

        try {
            serverURI = ServerURIUtils.normalizeURI(URIUtils.newURI(scheme, authority, path, null, null), true);
        } catch (final IllegalArgumentException e) {
            log.error("Error processing server URL: ", e); //$NON-NLS-1$
            serverURI = null;
            previewText.setText(Messages.getString("AddServerControl.ErrorServerNameEmpty")); //$NON-NLS-1$
            return;
        }

        previewText.setText(serverURI.toString());
    }

    public URI getServerURI() {
        return serverURI;
    }

    @Override
    public Validator getValidator() {
        return validator;
    }
}
