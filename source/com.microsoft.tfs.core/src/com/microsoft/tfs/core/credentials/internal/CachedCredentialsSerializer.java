// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.credentials.internal;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.credentials.CachedCredentials;
import com.microsoft.tfs.core.internal.persistence.DOMObjectSerializer;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMUtils;

public class CachedCredentialsSerializer extends DOMObjectSerializer {
    private static final Log log = LogFactory.getLog(CachedCredentialsSerializer.class);

    private static final int SCHEMA_VERSION = 1;

    private static final String CREDENTIALS_CACHE_ELEMENT_NAME = "credentials"; //$NON-NLS-1$
    private static final String VERSION_ATTRIBUTE_NAME = "version"; //$NON-NLS-1$

    private static final String CREDENTIALS_ELEMENT_NAME = "credentials"; //$NON-NLS-1$
    private static final String URI_ATTRIBUTE_NAME = "uri"; //$NON-NLS-1$
    private static final String USERNAME_ELEMENT_NAME = "username"; //$NON-NLS-1$
    private static final String PASSWORD_ELEMENT_NAME = "password"; //$NON-NLS-1$

    public CachedCredentialsSerializer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Document createDocumentFromComponent(final Object object) {
        @SuppressWarnings("unchecked")
        final Map<URI, CachedCredentials> credentialsMap = (Map<URI, CachedCredentials>) object;

        final Document document = DOMCreateUtils.newDocument(CREDENTIALS_CACHE_ELEMENT_NAME);
        final Element rootElement = document.getDocumentElement();
        rootElement.setAttribute(VERSION_ATTRIBUTE_NAME, String.valueOf(SCHEMA_VERSION));

        for (final CachedCredentials credentials : credentialsMap.values()) {
            serializeCredentials(credentials, document, rootElement);
        }

        return document;
    }

    private void serializeCredentials(
        final CachedCredentials credentials,
        final Document document,
        final Element parentElement) {
        final Element credentialsElement = DOMUtils.appendChild(parentElement, CREDENTIALS_ELEMENT_NAME);
        credentialsElement.setAttribute(
            URI_ATTRIBUTE_NAME,
            URIUtils.removeTrailingSlash(credentials.getURI()).toString());

        if (credentials.getUsername() != null) {
            DOMUtils.appendChildWithText(
                credentialsElement,
                USERNAME_ELEMENT_NAME,
                encodeCredentialField(credentials.getUsername()));
        }

        if (credentials.getPassword() != null) {
            DOMUtils.appendChildWithText(
                credentialsElement,
                PASSWORD_ELEMENT_NAME,
                encodeCredentialField(credentials.getPassword()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createComponentFromDocument(final Document document) {
        final Element root = document.getDocumentElement();

        if (!CREDENTIALS_CACHE_ELEMENT_NAME.equals(root.getNodeName())) {
            throw new RuntimeException(MessageFormat.format("Unexpected root element {0}", root.getNodeName())); //$NON-NLS-1$
        }

        int schemaVersion = -1;
        final String sSchemaVersion = root.getAttribute(VERSION_ATTRIBUTE_NAME);
        if (sSchemaVersion != null && sSchemaVersion.length() > 0) {
            try {
                schemaVersion = Integer.parseInt(sSchemaVersion);
            } catch (final NumberFormatException ex) {
                // ignore
            }
        }

        final Map<URI, CachedCredentials> credentialsMap = PersistenceStoreCredentialsManager.newMap();

        final Element[] credentialsElements = DOMUtils.getChildElements(root, CREDENTIALS_ELEMENT_NAME);
        for (int i = 0; i < credentialsElements.length; i++) {
            final CachedCredentials credentials = deserializeCredentials(credentialsElements[i], schemaVersion);

            if (credentials == null) {
                continue;
            }

            credentialsMap.put(credentials.getURI(), credentials);
        }

        return credentialsMap;
    }

    private CachedCredentials deserializeCredentials(final Element workspaceElement, final int schemaVersion) {
        final String uriSerialized = workspaceElement.getAttribute(URI_ATTRIBUTE_NAME);

        if (uriSerialized == null || uriSerialized.length() == 0) {
            return null;
        }

        URI uri;
        String username = null;
        String password = null;

        try {
            uri = new URI(uriSerialized);
        } catch (final Exception e) {
            log.warn("Could not deserialize credentials data", e); //$NON-NLS-1$
            return null;
        }

        final Element usernameElement = DOMUtils.getFirstChildElement(workspaceElement, USERNAME_ELEMENT_NAME);
        if (usernameElement != null) {
            username = DOMUtils.getText(usernameElement);

            if (username != null) {
                username = decodeCredentialField(username).trim();
                if (username.length() == 0) {
                    username = null;
                }
            }
        }

        final Element passwordElement = DOMUtils.getFirstChildElement(workspaceElement, PASSWORD_ELEMENT_NAME);
        if (passwordElement != null) {
            password = DOMUtils.getText(passwordElement);

            if (password != null) {
                password = decodeCredentialField(password);
                if (password.length() == 0) {
                    password = null;
                }
            }
        }

        return new CachedCredentials(uri, username, password);
    }

    private String encodeCredentialField(final String field) {
        try {
            return URLEncoder.encode(field, "UTF-8"); //$NON-NLS-1$
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String decodeCredentialField(final String field) {
        try {
            return URLDecoder.decode(field, "UTF-8"); //$NON-NLS-1$
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
