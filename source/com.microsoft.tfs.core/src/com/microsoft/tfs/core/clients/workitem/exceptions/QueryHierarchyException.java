// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.exceptions;

/**
 * Class for an error that occurs during a query of the hierarchy of the work
 * item store.
 *
 * @since TEE-SDK-10.1
 */
public class QueryHierarchyException extends RuntimeException {
    private static final long serialVersionUID = -6847740945116559234L;

    private final Type type;

    public QueryHierarchyException(final String message, final Throwable cause, final Type type) {
        super(message, cause);

        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public static final class Type {
        public static final Type OTHER = new Type("Other", 0); //$NON-NLS-1$
        public static final Type DENIED_OR_NOT_EXIST = new Type("DeniedOrNotExist", 1); //$NON-NLS-1$
        public static final Type PARENT_DOES_NOT_EXIST = new Type("ParentDoesNotExist", 2); //$NON-NLS-1$
        public static final Type PARENT_IS_NOT_A_FOLDER = new Type("ParentIsNotAFolder", 3); //$NON-NLS-1$
        public static final Type NAME_CONFLICTS_WITH_EXISTING_ITEM = new Type("NameConflictsWithExistingItem", 4); //$NON-NLS-1$
        public static final Type CIRCULAR_REFERENCE = new Type("CircularReference", 5); //$NON-NLS-1$
        public static final Type TYPE_MISMATCH = new Type("TypeMismatch", 6); //$NON-NLS-1$
        public static final Type ITEM_ALREADY_EXISTS = new Type("ItemAlreadyExists", 7); //$NON-NLS-1$
        public static final Type CANNOT_MOVE_ROOT_FOLDER = new Type("CannotMoveRootFolder", 8); //$NON-NLS-1$
        public static final Type CANNOT_DELETE_ROOT_FOLDER = new Type("CannotDeleteRootFolder", 9); //$NON-NLS-1$
        public static final Type ACCESS_EXCEPTION = new Type("AccessException", 10); //$NON-NLS-1$
        public static final Type CANNOT_DENY_ADMIN = new Type("CannotDenyAdmin", 11); //$NON-NLS-1$
        public static final Type INVALID_PERMISSION_COMBINATION = new Type("InvalidPermissionCombination", 12); //$NON-NLS-1$

        private final String name;
        private final int value;

        private Type(final String name, final int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}