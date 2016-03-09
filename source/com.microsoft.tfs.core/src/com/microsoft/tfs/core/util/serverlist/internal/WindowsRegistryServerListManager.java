// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.util.serverlist.internal;

import java.net.URI;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.config.RegistryUtils;
import com.microsoft.tfs.core.util.URIUtils;
import com.microsoft.tfs.core.util.serverlist.ServerList;
import com.microsoft.tfs.core.util.serverlist.ServerListCollectionEntry;
import com.microsoft.tfs.core.util.serverlist.ServerListConfigurationEntry;
import com.microsoft.tfs.core.util.serverlist.ServerListEntryType;
import com.microsoft.tfs.core.util.serverlist.ServerListManager;
import com.microsoft.tfs.jni.RegistryKey;
import com.microsoft.tfs.jni.RegistryValue;
import com.microsoft.tfs.jni.ValueType;

public class WindowsRegistryServerListManager implements ServerListManager {
    private static final Log log = LogFactory.getLog(WindowsRegistryServerListManager.class);

    private static final String REGISTRY_PATH = "TeamFoundation\\Instances"; //$NON-NLS-1$

    private static final String COLLECTIONS_KEY_NAME = "Collections"; //$NON-NLS-1$

    private static final String OFFLINE_ATTRIBUTE_VALUE = "Offline"; //$NON-NLS-1$
    private static final String TYPE_ATTRIBUTE_VALUE = "Type"; //$NON-NLS-1$
    private static final String URI_ATTRIBUTE_VALUE = "Uri"; //$NON-NLS-1$

    @Override
    public ServerList getServerList() {
        final ServerList serverList = new ServerList();
        final RegistryKey userKey = RegistryUtils.openOrCreateRootUserRegistryKey();

        if (userKey != null) {
            final RegistryKey registryKey = userKey.getSubkey(REGISTRY_PATH);

            if (registryKey != null) {
                for (final RegistryKey server : registryKey.subkeys()) {
                    final String serverName = server.getName();
                    final ServerListEntryType serverType = getType(server);
                    final URI serverURI = getURI(server);

                    if (serverName == null || serverType == null || serverURI == null) {
                        continue;
                    }

                    final ServerListConfigurationEntry serverListEntry =
                        new ServerListConfigurationEntry(serverName, serverType, serverURI);

                    final RegistryKey collectionsKey = server.getSubkey(COLLECTIONS_KEY_NAME);

                    if (collectionsKey != null) {
                        for (final RegistryKey collection : collectionsKey.subkeys()) {
                            final String collectionName = collection.getName();
                            final Boolean collectionOffline = getOffline(collection);
                            final ServerListEntryType collectionType = getType(collection);
                            final URI collectionURI = getURI(collection);

                            if (collectionName == null || collectionType == null || collectionURI == null) {
                                continue;
                            }

                            serverListEntry.getCollections().add(
                                new ServerListCollectionEntry(
                                    collectionName,
                                    collectionType,
                                    collectionURI,
                                    collectionOffline));
                        }
                    }

                    serverList.add(serverListEntry);
                }
            }
        }

        return serverList;
    }

    private static ServerListEntryType getType(final RegistryKey key) {
        final RegistryValue typeValue = key.getValue(TYPE_ATTRIBUTE_VALUE);

        if (typeValue == null || typeValue.getType() != ValueType.REG_DWORD) {
            log.info(MessageFormat.format("Expected DWORD for server instance type for server {0}", key.getName())); //$NON-NLS-1$
            return null;
        }

        final ServerListEntryType type = ServerListEntryType.fromValue(typeValue.getIntegerValue());

        if (type == null) {
            log.info(MessageFormat.format(
                "Unknown server instance type {0} for server {1}", //$NON-NLS-1$
                typeValue.getIntegerValue(),
                key.getName()));
        }

        return type;
    }

    private static URI getURI(final RegistryKey key) {
        final RegistryValue uriValue = key.getValue(URI_ATTRIBUTE_VALUE);

        if (uriValue == null
            || uriValue.getType() != ValueType.REG_SZ
            || uriValue.getStringValue() == null
            || uriValue.getStringValue().length() == 0) {
            log.info(MessageFormat.format(
                "Expected non-empty string for server instance uri for server {0}", //$NON-NLS-1$
                key.getName()));
            return null;
        }

        URI uri;

        try {
            uri = URIUtils.newURI(uriValue.getStringValue());
        } catch (final Exception e) {
            log.info(MessageFormat.format("Invalid URI for server {0}", key.getName()), e); //$NON-NLS-1$
            return null;
        }

        return uri;
    }

    private static Boolean getOffline(final RegistryKey key) {
        final RegistryValue offlineValue = key.getValue(OFFLINE_ATTRIBUTE_VALUE);

        if (offlineValue == null || offlineValue.getType() != ValueType.REG_DWORD) {
            return null;
        }

        return offlineValue.getIntegerValue() == 0 ? Boolean.FALSE : Boolean.TRUE;
    }

    @Override
    public boolean setServerList(final ServerList serverList) {
        final RegistryKey userKey = RegistryUtils.openOrCreateRootUserRegistryKey();

        if (userKey == null) {
            log.warn("Could not create registry key while saving server list"); //$NON-NLS-1$
            return false;
        }

        RegistryKey registryKey = userKey.getSubkey(REGISTRY_PATH);
        final Set<String> removeServers = getSubkeyNames(registryKey);

        if (registryKey == null) {
            registryKey = userKey.createSubkey(REGISTRY_PATH);
        }

        for (final ServerListConfigurationEntry server : serverList.getServers()) {
            RegistryKey serverKey = registryKey.getSubkey(server.getName());

            if (serverKey == null) {
                serverKey = registryKey.createSubkey(server.getName());
            }

            serverKey.setValue(new RegistryValue(TYPE_ATTRIBUTE_VALUE, server.getType().getValue()));
            serverKey.setValue(new RegistryValue(URI_ATTRIBUTE_VALUE, server.getURI().toString()));

            RegistryKey collectionsKey = serverKey.getSubkey(COLLECTIONS_KEY_NAME);
            final Set<String> removeCollections = getSubkeyNames(collectionsKey);

            if (server.getCollections().size() > 0) {
                if (collectionsKey == null) {
                    collectionsKey = serverKey.createSubkey(COLLECTIONS_KEY_NAME);
                }

                for (final ServerListCollectionEntry collection : server.getCollections()) {
                    RegistryKey collectionKey = collectionsKey.getSubkey(collection.getName());

                    if (collectionKey == null) {
                        collectionKey = collectionsKey.createSubkey(collection.getName());
                    }

                    if (collection.getOffline() != null) {
                        collectionKey.setValue(
                            new RegistryValue(OFFLINE_ATTRIBUTE_VALUE, collection.getOffline().booleanValue() ? 1 : 0));
                    } else if (collectionKey.hasValue(OFFLINE_ATTRIBUTE_VALUE)) {
                        collectionKey.deleteValue(OFFLINE_ATTRIBUTE_VALUE);
                    }

                    collectionKey.setValue(new RegistryValue(TYPE_ATTRIBUTE_VALUE, collection.getType().getValue()));
                    collectionKey.setValue(new RegistryValue(URI_ATTRIBUTE_VALUE, collection.getURI().toString()));

                    removeCollections.remove(collection.getName());
                }
            }

            for (final String removeCollection : removeCollections) {
                collectionsKey.deleteSubkeyRecursive(removeCollection);
            }

            removeServers.remove(server.getName());
        }

        /* Remove deleted servers */
        for (final String removeServer : removeServers) {
            registryKey.deleteSubkeyRecursive(removeServer);
        }

        return true;
    }

    private static Set<String> getSubkeyNames(final RegistryKey key) {
        final Set<String> subkeySet = new HashSet<String>();

        if (key == null) {
            return subkeySet;
        }

        for (final RegistryKey subkey : key.subkeys()) {
            subkeySet.add(subkey.getName());
        }

        return subkeySet;
    }
}
