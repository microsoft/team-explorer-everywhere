// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.versioncontrol.conflicts;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.clients.versioncontrol.OperationStatus;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ChangeType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Conflict;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.ConflictType;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.TypesafeEnum;

/**
 * A {@link ConflictCategory} is a typed enumeration of known types of
 * conflicts. (The class name "ConflictType" was already taken.)
 *
 * @since TEE-SDK-10.1
 */
public class ConflictCategory extends TypesafeEnum {
    public static final ConflictCategory UNKNOWN = new ConflictCategory(0);
    public static final ConflictCategory VERSION = new ConflictCategory(1);
    public static final ConflictCategory MERGE = new ConflictCategory(2);
    public static final ConflictCategory SERVER_DELETED = new ConflictCategory(3);
    public static final ConflictCategory MERGE_TARGET_DELETED = new ConflictCategory(4);
    public static final ConflictCategory LOCALLY_DELETED = new ConflictCategory(5);
    public static final ConflictCategory MERGE_SOURCE_DELETED = new ConflictCategory(6);
    public static final ConflictCategory BOTH_DELETED = new ConflictCategory(7);
    public static final ConflictCategory MERGE_BOTH_DELETED = new ConflictCategory(8);
    public static final ConflictCategory FILENAME = new ConflictCategory(9);
    public static final ConflictCategory WRITABLE = new ConflictCategory(10);
    public static final ConflictCategory SHELVESET = new ConflictCategory(11);
    public static final ConflictCategory ROLLBACK_LOCAL = new ConflictCategory(12);

    private final static Log log = LogFactory.getLog(ConflictCategory.class);

    protected ConflictCategory(final int value) {
        super(value);
    }

    public static ConflictCategory[] getAllConflictCategories() {
        return new ConflictCategory[] {
            UNKNOWN,
            VERSION,
            MERGE,
            SERVER_DELETED,
            MERGE_TARGET_DELETED,
            LOCALLY_DELETED,
            BOTH_DELETED,
            MERGE_BOTH_DELETED,
            FILENAME,
            WRITABLE,
            SHELVESET,
            ROLLBACK_LOCAL
        };
    }

    /**
     * Given an AConflict, determines the type of conflict and returns the
     * appropriate ConflictCategory which describes this conflict.
     *
     * @param conflict
     *        the AConflict to determine the type of
     * @return the appropriate ConflictCategory or null if the conflict type is
     *         unknown
     */
    public static ConflictCategory getConflictCategory(final Conflict conflict) {
        Check.notNull(conflict, "conflict"); //$NON-NLS-1$

        /*
         * Handle version conflicts first (conflict.getReason() == Conflict)
         */
        if (conflict.getReason() == OperationStatus.CONFLICT.getValue()) {
            /* Shelveset conflicts. */
            if (conflict.getTheirShelvesetName() != null && conflict.getTheirShelvesetOwnerName() != null) {
                return ConflictCategory.SHELVESET;
            }

            /*
             * A file was deleted locally and on the server. Note in dev10:
             * could include renames.
             */
            if (conflict.getTheirDeletionID() > 0 && conflict.getYourChangeType().contains(ChangeType.DELETE)) {
                /* Deleted in source and target of the merge */
                if (conflict.getType() == ConflictType.MERGE) {
                    return ConflictCategory.MERGE_BOTH_DELETED;
                } else {
                    return ConflictCategory.BOTH_DELETED;
                }
            }

            /* A file was deleted locally and edited on the server */
            if (conflict.getYourChangeType().contains(ChangeType.DELETE)) {
                /* Deleted in the target of the merge */
                if (conflict.getType() == ConflictType.MERGE) {
                    return ConflictCategory.MERGE_TARGET_DELETED;
                } else {
                    return ConflictCategory.LOCALLY_DELETED;
                }
            }

            /* A file was deleted on the server and edited locally */
            if (conflict.getTheirDeletionID() > 0) {
                /* Deleted in the source of the merge */
                if (conflict.getType() == ConflictType.MERGE) {
                    return ConflictCategory.MERGE_SOURCE_DELETED;
                } else {
                    return ConflictCategory.SERVER_DELETED;
                }
            }

            /*
             * Trying to add a file whose name already exists (first case) or a
             * more generic namespace conflict from the server (second case)
             */
            else if (conflict.getType() != ConflictType.MERGE
                && (conflict.getBaseServerItem() == null || conflict.isNamespaceConflict())) {
                return ConflictCategory.FILENAME;
            }
            /*
             * Occurs when you pend a rollback change but already have a pending
             * change.
             *
             * Has absolutely nothing to do with merging.
             */
            else if (conflict.getType() == ConflictType.MERGE
                && conflict.getBaseChangeType().contains(ChangeType.ROLLBACK)) {
                return ConflictCategory.ROLLBACK_LOCAL;
            } else if (conflict.getType() == ConflictType.MERGE) {
                return ConflictCategory.MERGE;
            } else {
                return ConflictCategory.VERSION;
            }
        }

        /* Local conflicts */
        else {
            /* Writable conflicts */
            if (conflict.getReason() == OperationStatus.SOURCE_WRITABLE.getValue()
                || conflict.getReason() == OperationStatus.TARGET_WRITABLE.getValue()) {
                return ConflictCategory.WRITABLE;
            } else if (conflict.isNamespaceConflict()) {
                return ConflictCategory.FILENAME;
            }
        }

        log.error(
            MessageFormat.format("Could not determine conflict category for conflict: {0}", conflict.getReason())); //$NON-NLS-1$
        return ConflictCategory.UNKNOWN;
    }
}
