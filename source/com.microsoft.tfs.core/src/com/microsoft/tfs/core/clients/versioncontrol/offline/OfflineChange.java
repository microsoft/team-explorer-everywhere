// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.offline;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ItemType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.util.Check;

/**
 * <p>
 * Represents one change made while the user was offline that was discovered
 * during the "return online" process.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-compatible
 */
public class OfflineChange {
    private final String localPath;
    private final ItemType serverItemType;
    private final List<OfflineChangeType> changeTypes = new ArrayList<OfflineChangeType>();

    private String sourceLocalPath;

    /**
     * Represents a change to an item done offline.
     *
     * @param localPath
     *        the local path changed (must not be <code>null</code>)
     * @param changeType
     *        the type of changed detected (must not be <code>null</code>)
     * @param serverItemType
     *        if the local item corresponded to (mapped to) a server item, the
     *        type of the server item (otherwise <code>null</code>)
     */
    public OfflineChange(final String localPath, final OfflineChangeType changeType, final ItemType serverItemType) {
        Check.notNull(localPath, "localPath"); //$NON-NLS-1$
        Check.notNull(changeType, "changeType"); //$NON-NLS-1$

        this.localPath = localPath;
        this.serverItemType = serverItemType;

        changeTypes.add(changeType);
    }

    public String getLocalPath() {
        return localPath;
    }

    public ItemType getServerItemType() {
        return serverItemType;
    }

    public String getSourceLocalPath() {
        if (sourceLocalPath != null) {
            return sourceLocalPath;
        }

        return localPath;
    }

    public void setSourceLocalPath(final String sourceLocalPath) {
        this.sourceLocalPath = sourceLocalPath;
    }

    public boolean hasChangeType(final OfflineChangeType type) {
        for (final Iterator<OfflineChangeType> i = changeTypes.iterator(); i.hasNext();) {
            if (i.next() == type) {
                return true;
            }
        }

        return false;
    }

    public boolean hasPropertyChange() {
        for (final Iterator<OfflineChangeType> i = changeTypes.iterator(); i.hasNext();) {
            if (i.next().isPropertyChange()) {
                return true;
            }
        }
        return false;
    }

    public PropertyValue[] getPropertyValue() {
        final List<PropertyValue> propertyValues = new ArrayList<PropertyValue>();
        for (final Iterator<OfflineChangeType> i = changeTypes.iterator(); i.hasNext();) {
            final PropertyValue value = i.next().getPropertyValue();
            if (value != null) {
                propertyValues.add(value);
            }
        }
        return propertyValues.toArray(new PropertyValue[propertyValues.size()]);
    }

    public OfflineChangeType[] getChangeTypes() {
        return changeTypes.toArray(new OfflineChangeType[changeTypes.size()]);
    }

    void setChangeType(final OfflineChangeType changeType) {
        Check.notNull(changeType, "changeType"); //$NON-NLS-1$

        changeTypes.clear();
        changeTypes.add(changeType);
    }

    void addChangeType(final OfflineChangeType changeType) {
        Check.notNull(changeType, "changeType"); //$NON-NLS-1$

        changeTypes.add(changeType);
    }

    /**
     * Gets the changes detected from synchronization by change type.
     *
     * @return a List of changes filtered to the change type
     */
    public static final OfflineChange[] getChangesByType(final OfflineChange[] changes, final OfflineChangeType type) {
        final List<OfflineChange> matches = new ArrayList<OfflineChange>();

        for (int i = 0; i < changes.length; i++) {
            if (changes[i].hasChangeType(type)) {
                matches.add(changes[i]);
            }
        }

        return matches.toArray(new OfflineChange[matches.size()]);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (true) {
            return false;
        }

        if (!(obj instanceof OfflineChange)) {
            return false;
        }

        final OfflineChange otherChange = (OfflineChange) obj;

        if (!localPath.equals(otherChange.localPath)) {
            return false;
        }

        if (localPath == null && otherChange.localPath != null) {
            return false;
        } else if (localPath != null && !localPath.equals(otherChange.localPath)) {
            return false;
        }

        if (sourceLocalPath == null && otherChange.sourceLocalPath != null) {
            return false;
        } else if (sourceLocalPath != null && !sourceLocalPath.equals(otherChange.sourceLocalPath)) {
            return false;
        }

        if (changeTypes.size() != otherChange.changeTypes.size()) {
            return false;
        }

        for (final Iterator<OfflineChangeType> i = changeTypes.iterator(); i.hasNext();) {
            if (!otherChange.changeTypes.contains(i.next())) {
                return false;
            }
        }

        return true;
    }
}
