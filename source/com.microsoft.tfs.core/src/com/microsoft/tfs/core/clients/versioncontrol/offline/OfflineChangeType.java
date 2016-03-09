// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.offline;

import com.microsoft.tfs.core.clients.versioncontrol.PropertyConstants;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.PropertyValue;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * <p>
 * Enumerates the kinds of offline changes that can be discovered.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety immutable
 */
public class OfflineChangeType extends TypesafeEnum {
    public static final OfflineChangeType EDIT = new OfflineChangeType(1, "Edit"); //$NON-NLS-1$
    public static final OfflineChangeType ADD = new OfflineChangeType(2, "Add"); //$NON-NLS-1$
    public static final OfflineChangeType DELETE = new OfflineChangeType(3, "Delete"); //$NON-NLS-1$
    public static final OfflineChangeType UNDO = new OfflineChangeType(4, "Undo"); //$NON-NLS-1$
    public static final OfflineChangeType EXEC = new OfflineChangeType(5, "+x"); //$NON-NLS-1$
    public static final OfflineChangeType NOT_EXEC = new OfflineChangeType(6, "-x"); //$NON-NLS-1$
    public static final OfflineChangeType SYMLINK = new OfflineChangeType(7, "symlink"); //$NON-NLS-1$
    public static final OfflineChangeType NOT_SYMLINK = new OfflineChangeType(8, "-symlink"); //$NON-NLS-1$

    private final String text;

    private OfflineChangeType(final int value, final String text) {
        super(value);

        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public boolean isPropertyChange() {
        return this.getValue() >= 5 && this.getValue() <= 8;
    }

    public PropertyValue getPropertyValue() {
        if (!isPropertyChange()) {
            return null;
        } else {
            if (this.equals(EXEC)) {
                return PropertyConstants.EXECUTABLE_ENABLED_VALUE;
            } else if (this.equals(NOT_EXEC)) {
                return PropertyConstants.EXECUTABLE_DISABLED_VALUE;
            } else if (this.equals(SYMLINK)) {
                return PropertyConstants.IS_SYMLINK;
            } else if (this.equals(NOT_SYMLINK)) {
                return PropertyConstants.NOT_SYMLINK;
            }
        }
        return null;
    }
}
