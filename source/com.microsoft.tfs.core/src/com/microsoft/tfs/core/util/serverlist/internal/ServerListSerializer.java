// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.serverlist.internal;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.microsoft.tfs.core.internal.persistence.DOMObjectSerializer;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.util.serverlist.ServerList;
import com.microsoft.tfs.core.util.serverlist.ServerListCollectionEntry;
import com.microsoft.tfs.core.util.serverlist.ServerListConfigurationEntry;
import com.microsoft.tfs.core.util.serverlist.ServerListEntryType;
import com.microsoft.tfs.util.xml.DOMCreateUtils;
import com.microsoft.tfs.util.xml.DOMUtils;

public class ServerListSerializer extends DOMObjectSerializer {
    private static final Log log = LogFactory.getLog(ServerListSerializer.class);

    private static final int SCHEMA_VERSION = 1;

    private static final String SERVERS_ELEMENT_NAME = "servers"; //$NON-NLS-1$
    private static final String SERVER_ELEMENT_NAME = "server"; //$NON-NLS-1$

    private static final String COLLECTIONS_ELEMENT_NAME = "collections"; //$NON-NLS-1$
    private static final String COLLECTION_ELEMENT_NAME = "collection"; //$NON-NLS-1$

    private static final String VERSION_ATTRIBUTE_NAME = "version"; //$NON-NLS-1$

    private static final String NAME_ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
    private static final String OFFLINE_ATTRIBUTE_NAME = "offline"; //$NON-NLS-1$
    private static final String TYPE_ATTRIBUTE_NAME = "type"; //$NON-NLS-1$
    private static final String URI_ATTRIBUTE_NAME = "uri"; //$NON-NLS-1$

    public ServerListSerializer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Document createDocumentFromComponent(final Object object) {
        final ServerList serverList = (ServerList) object;

        final Document document = DOMCreateUtils.newDocument(SERVERS_ELEMENT_NAME);
        final Element rootElement = document.getDocumentElement();
        rootElement.setAttribute(VERSION_ATTRIBUTE_NAME, String.valueOf(SCHEMA_VERSION));

        for (final ServerListConfigurationEntry server : serverList.getServers()) {
            final Element serverElement = DOMUtils.appendChild(rootElement, SERVER_ELEMENT_NAME);

            serverElement.setAttribute(NAME_ATTRIBUTE_NAME, server.getName());
            serverElement.setAttribute(TYPE_ATTRIBUTE_NAME, Integer.toString(server.getType().getValue()));
            serverElement.setAttribute(URI_ATTRIBUTE_NAME, URIUtils.removeTrailingSlash(server.getURI()).toString());

            final Set<ServerListCollectionEntry> collections = server.getCollections();

            if (collections != null && collections.size() > 0) {
                final Element collectionsElement = DOMUtils.appendChild(serverElement, COLLECTIONS_ELEMENT_NAME);

                for (final ServerListCollectionEntry collection : collections) {
                    final Element collectionElement = DOMUtils.appendChild(collectionsElement, COLLECTION_ELEMENT_NAME);

                    collectionElement.setAttribute(NAME_ATTRIBUTE_NAME, collection.getName());

                    if (collection.getOffline() != null) {
                        collectionElement.setAttribute(
                            OFFLINE_ATTRIBUTE_NAME,
                            Boolean.toString(collection.getOffline()));
                    }

                    collectionElement.setAttribute(
                        TYPE_ATTRIBUTE_NAME,
                        Integer.toString(collection.getType().getValue()));
                    collectionElement.setAttribute(
                        URI_ATTRIBUTE_NAME,
                        URIUtils.removeTrailingSlash(collection.getURI()).toString());
                }
            }
        }

        return document;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createComponentFromDocument(final Document document) {
        final Element root = document.getDocumentElement();

        if (!SERVERS_ELEMENT_NAME.equals(root.getNodeName())) {
            throw new RuntimeException(MessageFormat.format("Unexpected root element {0}", root.getNodeName())); //$NON-NLS-1$
        }

        final String sSchemaVersion = root.getAttribute(VERSION_ATTRIBUTE_NAME);
        if (sSchemaVersion != null && sSchemaVersion.length() > 0) {
            try {
                Integer.parseInt(sSchemaVersion);
            } catch (final NumberFormatException ex) {
                // ignore
            }
        }

        final ServerList serverList = new ServerList();

        final Element[] serverElements = DOMUtils.getChildElements(root, SERVER_ELEMENT_NAME);
        for (int i = 0; i < serverElements.length; i++) {
            final String serverName = getName(serverElements[i].getAttribute(NAME_ATTRIBUTE_NAME));
            final ServerListEntryType serverType = getType(serverElements[i].getAttribute(TYPE_ATTRIBUTE_NAME));
            final URI serverUri = getURI(serverElements[i].getAttribute(URI_ATTRIBUTE_NAME));

            if (serverName == null || serverType == null || serverUri == null) {
                continue;
            }

            final ServerListConfigurationEntry server =
                new ServerListConfigurationEntry(serverName, serverType, serverUri);

            final Element[] collectionsElements =
                DOMUtils.getChildElements(serverElements[i], COLLECTIONS_ELEMENT_NAME);

            for (int j = 0; j < collectionsElements.length; j++) {
                final Element[] collectionElements =
                    DOMUtils.getChildElements(serverElements[i], COLLECTION_ELEMENT_NAME);

                for (int k = 0; k < collectionElements.length; k++) {
                    final String collectionName = getName(collectionElements[k].getAttribute(NAME_ATTRIBUTE_NAME));
                    final Boolean collectionOffline =
                        getOffline(collectionElements[k].getAttribute(OFFLINE_ATTRIBUTE_NAME));
                    final ServerListEntryType collectionType =
                        getType(collectionElements[k].getAttribute(TYPE_ATTRIBUTE_NAME));
                    final URI collectionUri = getURI(collectionElements[k].getAttribute(URI_ATTRIBUTE_NAME));

                    if (collectionName == null || collectionType == null || collectionUri == null) {
                        continue;
                    }

                    final ServerListCollectionEntry collection =
                        new ServerListCollectionEntry(collectionName, collectionType, collectionUri, collectionOffline);

                    server.getCollections().add(collection);
                }
            }

            serverList.add(server);
        }

        return serverList;
    }

    private static String getName(final String nameAttribute) {
        if (nameAttribute == null || nameAttribute.length() == 0) {
            return null;
        }

        return nameAttribute;
    }

    private static Boolean getOffline(final String offlineAttribute) {
        if (offlineAttribute == null || offlineAttribute.length() == 0) {
            return null;
        }

        return Boolean.valueOf(offlineAttribute);
    }

    private static ServerListEntryType getType(final String typeAttribute) {
        if (typeAttribute == null || typeAttribute.length() == 0) {
            return null;
        }

        try {
            return ServerListEntryType.fromValue(Integer.parseInt(typeAttribute));
        } catch (final Exception e) {
            log.warn("Could not deserialize type", e); //$NON-NLS-1$
            return null;
        }
    }

    private static URI getURI(final String uriAttribute) {
        try {
            return URIUtils.newURI(uriAttribute);
        } catch (final Exception e) {
            log.warn("Could not deserialize uri", e); //$NON-NLS-1$
            return null;
        }
    }
}
