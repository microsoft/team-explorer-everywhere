// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.viewstate;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;

public class ViewState {
    private final Map typeToObjectSerializer = new HashMap();
    private final IEclipsePreferences eclipsePreferences;
    private final Preferences currentNode;

    public ViewState(final String rootNodeName) {
        this(new DefaultScope(rootNodeName));
    }

    public ViewState(final ViewStateScope scope) {
        eclipsePreferences = scope.getEclipsePreferencesScope().getNode(
            TFSCommonUIClientPlugin.getDefault().getBundle().getSymbolicName());
        currentNode = scope.getNestedPreferences(eclipsePreferences);
    }

    public void addObjectSerializer(final Class type, final ObjectSerializer objectSerializer) {
        typeToObjectSerializer.put(type, objectSerializer);
    }

    public boolean persistString(final String key, final String value) {
        if (currentNode.get(key, null) != null) {
            currentNode.remove(key);
        }

        currentNode.put(key, value);

        return true;
    }

    public void persistBoolean(final String key, final boolean value) {
        if (currentNode.get(key, null) != null) {
            currentNode.remove(key);
        }

        currentNode.putBoolean(key, value);
    }

    public boolean persistList(final List list, final String key, final Class itemType) {
        try {
            if (currentNode.nodeExists(key)) {
                currentNode.node(key).removeNode();
            }

            final Preferences listNode = currentNode.node(key);
            final ObjectSerializer serializer = getObjectSerializer(itemType);

            for (int i = 0; i < list.size(); i++) {
                final String objString = serializer.toString(list.get(i));
                listNode.put(String.valueOf(i), objString);
            }

            return true;
        } catch (final BackingStoreException ex) {
            return false;
        }
    }

    private ObjectSerializer getObjectSerializer(final Class itemType) {
        if (!typeToObjectSerializer.containsKey(itemType)) {
            final String messageFormat = "the type [{0}] does not have a registered ObjectSerialzier"; //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, itemType);
            throw new IllegalArgumentException(message);
        }
        return (ObjectSerializer) typeToObjectSerializer.get(itemType);
    }

    public String restoreString(final String key) {
        return currentNode.get(key, null);
    }

    public boolean restoreBoolean(final String key, final boolean def) {
        return currentNode.getBoolean(key, def);
    }

    public List restoreList(final String key, final Class itemType) {
        try {
            if (!currentNode.nodeExists(key)) {
                return null;
            }

            final List results = new ArrayList();
            final Preferences listNode = currentNode.node(key);
            final String[] keys = listNode.keys();
            Arrays.sort(keys);
            final ObjectSerializer serializer = getObjectSerializer(itemType);

            for (int i = 0; i < keys.length; i++) {
                final String currentKey = listNode.get(keys[i], null);
                final Object object = serializer.fromString(currentKey);
                if (object != null) {
                    results.add(object);
                }
            }

            return results;
        } catch (final BackingStoreException ex) {
            return null;
        }
    }

    public void commit() {
        try {
            eclipsePreferences.flush();
        } catch (final BackingStoreException e) {
            // ignore, non-critical
        }
    }
}
