// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.exceptions;

/**
 * Class for an error that occurs during validation of a work item link.
 *
 * @since TEE-SDK-10.1
 */
public class WorkItemLinkValidationException extends WorkItemException {
    private static final long serialVersionUID = -4442222895050498396L;

    /**
     * Defines the {@link WorkItemLinkValidationException} types.
     *
     * @threadsafety immutable
     * @since TEE-SDK-10.1
     */
    public static class Type {
        public static final Type LINK_TYPE_NOT_FOUND = new Type("LinkTypeNotFound", 0); //$NON-NLS-1$
        public static final Type LINK_NOT_FOUND = new Type("LinkNotFound", 1); //$NON-NLS-1$
        public static final Type ADD_LINK_CANNOT_LINK_TO_SELF = new Type("AddLinkCannotLinkToSelf", 2); //$NON-NLS-1$
        public static final Type ADD_LINK_DISABLED_TYPE = new Type("AddLinkDisabledType", 3); //$NON-NLS-1$
        public static final Type ADD_LINK_CIRCULARITY = new Type("AddLinkCircularity", 4); //$NON-NLS-1$
        public static final Type ADD_LINK_EXTRA_PARENT = new Type("AddLinkExtraParent", 5); //$NON-NLS-1$
        public static final Type ADD_LINK_CHILD_IS_ANCESTOR = new Type("AddLinkChildIsAncestor", 6); //$NON-NLS-1$
        public static final Type ADD_LINK_ALREADY_EXISTS = new Type("AddLinkAlreadyExists", 7); //$NON-NLS-1$
        public static final Type ADD_LINK_MAX_DEPTH_EXCEEDED = new Type("AddLinkMaxDepthExceeded", 8); //$NON-NLS-1$

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

    private Type type;

    public WorkItemLinkValidationException(final String message) {
        super(message);
    }

    public WorkItemLinkValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public WorkItemLinkValidationException(
        final String message,
        final Throwable cause,
        final int errorId,
        final Type type) {
        super(message, cause, errorId);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
