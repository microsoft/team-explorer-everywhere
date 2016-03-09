// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.type;

import com.microsoft.tfs.core.clients.workitem.exceptions.WorkItemException;
import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;

/**
 * <p>
 * An Exception thrown by the type converters when an input value cannot be
 * converted into a field value of the proper type.
 * </p>
 * <p>
 * In addition to indicating a type conversion error, an instance of
 * WITypeConverterException also contains information that may be useful to
 * client code. A WITypeConverterException holds a FieldStatus instance that
 * indicates what type of field invalidity was a result of the type conversion
 * error. The normal value for this FieldStatus instance is INVALID_TYPE, but in
 * some edge cases it may be a different value. Optionally, a
 * WITypeConverterException may hold the Object that is considered invalid. This
 * is not neccessarily the same object that was an input to a WITypeConverter.
 * The main reason for this behavior is to support the fact that Fields hold
 * invalid values which may not neccessarily be the same value that was set on
 * them.
 * </p>
 */
public class WITypeConverterException extends WorkItemException {
    private static final long serialVersionUID = -6644351979123496998L;

    private final boolean containsInvalidValue;
    private final FieldStatus invalidStatus;
    private final Object invalidValue;

    public WITypeConverterException(final String message) {
        this(message, null, FieldStatus.INVALID_TYPE, false, null);
    }

    public WITypeConverterException(final String message, final Throwable cause) {
        this(message, cause, FieldStatus.INVALID_TYPE, false, null);
    }

    public WITypeConverterException(final String message, final FieldStatus invalidStatus, final Object invalidValue) {
        this(message, null, invalidStatus, true, invalidValue);
    }

    private WITypeConverterException(
        final String message,
        final Throwable cause,
        final FieldStatus invalidStatus,
        final boolean containsInvalidValue,
        final Object invalidValue) {
        super(message, cause);
        this.invalidStatus = invalidStatus;
        this.containsInvalidValue = containsInvalidValue;
        this.invalidValue = invalidValue;
    }

    public FieldStatus getInvalidStatus() {
        return invalidStatus;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }

    public boolean containsInvalidValue() {
        return containsInvalidValue;
    }
}
