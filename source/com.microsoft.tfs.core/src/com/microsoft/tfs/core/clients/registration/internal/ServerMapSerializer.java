// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.registration.internal;

import java.text.MessageFormat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.clients.registration.RegistrationData;
import com.microsoft.tfs.core.clients.registration.ServerMap;
import com.microsoft.tfs.core.internal.persistence.DOMObjectSerializer;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMUtils;

/**
 * Serializes the {@link ServerMap} from/to a file in the default settings
 * location. Subdirectories are created/read for the {@link RegistrationData}
 * values found in the {@link ServerMap}.
 *
 * @threadsafety thread-safe
 */
public class ServerMapSerializer extends DOMObjectSerializer {
    private static final int SCHEMA_VERSION = 1;

    private static final String SERVERS_ELEMENT_NAME = "servers"; //$NON-NLS-1$
    private static final String SERVER_ELEMENT_NAME = "server"; //$NON-NLS-1$
    private static final String URI_ELEMENT_NAME = "uri"; //$NON-NLS-1$
    private static final String ID_ELEMENT_NAME = "id"; //$NON-NLS-1$
    private static final String VERSION_ATTRIBUTE_NAME = "version"; //$NON-NLS-1$

    /**
     * {@inheritDoc}
     */
    @Override
    protected Document createDocumentFromComponent(final Object object) {
        final ServerMap serverMap = (ServerMap) object;

        final Document document = DOMCreateUtils.newDocument(SERVERS_ELEMENT_NAME);
        final Element rootElement = document.getDocumentElement();
        rootElement.setAttribute(VERSION_ATTRIBUTE_NAME, String.valueOf(SCHEMA_VERSION));

        final String[] uris = serverMap.getURIs();
        for (int i = 0; i < uris.length; i++) {
            final String id = serverMap.getServerID(uris[i]);
            serializeServerData(uris[i], id, document, rootElement);
        }

        return document;
    }

    private void serializeServerData(
        final String uri,
        final String id,
        final Document document,
        final Element parentElement) {
        final Element serverElement = DOMUtils.appendChild(parentElement, SERVER_ELEMENT_NAME);

        DOMUtils.appendChildWithText(serverElement, URI_ELEMENT_NAME, uri);
        DOMUtils.appendChildWithText(serverElement, ID_ELEMENT_NAME, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createComponentFromDocument(final Document document) {
        final Element root = document.getDocumentElement();

        if (!SERVERS_ELEMENT_NAME.equals(root.getNodeName())) {
            throw new RuntimeException(MessageFormat.format("unexpected root element: {0}", root.getNodeName())); //$NON-NLS-1$
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

        final ServerMap serverMap = new ServerMap();

        final Element[] serverElements = DOMUtils.getChildElements(root, SERVER_ELEMENT_NAME);
        for (int i = 0; i < serverElements.length; i++) {
            deserializeServerData(serverElements[i], serverMap, schemaVersion);
        }

        return serverMap;
    }

    private void deserializeServerData(
        final Element serverElement,
        final ServerMap serverMap,
        final int schemaVersion) {
        final Element uriElement = DOMUtils.getFirstChildElement(serverElement, URI_ELEMENT_NAME);

        if (uriElement == null) {
            return;
        }

        String uri = DOMUtils.getText(uriElement);
        if (uri == null || uri.trim().length() == 0) {
            return;
        }
        uri = uri.trim();

        final Element idElement = DOMUtils.getFirstChildElement(serverElement, ID_ELEMENT_NAME);

        if (idElement == null) {
            return;
        }

        final String id = DOMUtils.getText(idElement);
        if (id == null || id.trim().length() == 0) {
            return;
        }

        serverMap.addServerID(uri, id);
    }
}
