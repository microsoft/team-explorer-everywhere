// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.exceptions;

/**
 * Class to describe validation exceptions.
 *
 * @since TEE-SDK-10.1
 */
public class ValidationException extends WorkItemException {
    private static final long serialVersionUID = -272534746555175493L;

    public static class Type {
        public static final Type NOT_UNIQUE_STORED_QUERY = new Type("NotUniqueStoredQuery", 5); //$NON-NLS-1$

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

    public ValidationException(final String message, final Throwable cause, final int errorId, final Type type) {
        super(message, cause, errorId);
        this.type = type;
    }

    public ValidationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ValidationException(final String message) {
        super(message);
    }

    public Type getType() {
        return type;
    }
}
