// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.fields;

import java.text.MessageFormat;

import com.microsoft.tfs.core.Messages;

/**
 * Enumerates the status of a {@link Field}. The status depends upon the
 * validity of the field's value.
 *
 * @since TEE-SDK-10.1
 */
public class FieldStatus {
    public static final FieldStatus VALID = new FieldStatus("VALID"); //$NON-NLS-1$
    public static final FieldStatus INVALID_TYPE = new FieldStatus("INVALID_TYPE"); //$NON-NLS-1$
    public static final FieldStatus INVALID_LIST_VALUE = new FieldStatus("INVALID_LIST_VALUE"); //$NON-NLS-1$
    public static final FieldStatus INVALID_EMPTY = new FieldStatus("INVALID_EMPTY"); //$NON-NLS-1$
    public static final FieldStatus INVALID_NOT_EMPTY = new FieldStatus("INVALID_NOT_EMPTY"); //$NON-NLS-1$
    public static final FieldStatus INVALID_PATH = new FieldStatus("INVALID_PATH"); //$NON-NLS-1$
    public static final FieldStatus INVALID_FORMAT = new FieldStatus("INVALID_FORMAT"); //$NON-NLS-1$
    public static final FieldStatus INVALID_UNKNOWN = new FieldStatus("INVALID_UNKNOWN"); //$NON-NLS-1$
    public static final FieldStatus INVALID_NOT_EMPTY_OR_OLD_VALUE = new FieldStatus("INVALID_NOT_EMPTY_OR_OLD_VALUE"); //$NON-NLS-1$
    public static final FieldStatus INVALID_EMPTY_OR_OLD_VALUE = new FieldStatus("INVALID_EMPTY_OR_OLD_VALUE"); //$NON-NLS-1$
    public static final FieldStatus INVALID_VALUE_IN_OTHER_FIELD = new FieldStatus("INVALID_VALUE_IN_OTHER_FIELD"); //$NON-NLS-1$
    public static final FieldStatus INVALID_VALUE_NOT_IN_OTHER_FIELD =
        new FieldStatus("INVALID_VALUE_NOT_IN_OTHER_FIELD"); //$NON-NLS-1$
    public static final FieldStatus INVALID_DATE = new FieldStatus("INVALID_DATE"); //$NON-NLS-1$
    public static final FieldStatus INVALID_CHARACTERS = new FieldStatus("INVALID_CHARACTERS"); //$NON-NLS-1$

    private final String value;

    private FieldStatus(final String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public String getInvalidMessage(final Field field) {
        String message = null;

        if (this == FieldStatus.INVALID_TYPE) {
            message = Messages.getString("FieldStatus.InvalidTypeFormat"); //$NON-NLS-1$
        } else if (this == FieldStatus.INVALID_LIST_VALUE) {
            message = Messages.getString("FieldStatus.InvalidListValueFormat"); //$NON-NLS-1$
        } else if (this == FieldStatus.INVALID_EMPTY) {
            message = Messages.getString("FieldStatus.InvalidEmptyFormat"); //$NON-NLS-1$
        } else if (this == FieldStatus.INVALID_NOT_EMPTY) {
            message = Messages.getString("FieldStatus.InvalidNotEmptyFormat"); //$NON-NLS-1$
        } else if (this == FieldStatus.INVALID_PATH) {
            // TODO this is not Visual Studio's message
            message = Messages.getString("FieldStatus.InvalidPathFormat"); //$NON-NLS-1$
        } else if (this == FieldStatus.INVALID_FORMAT) {
            message = Messages.getString("FieldStatus.InvalidFormatFormat"); //$NON-NLS-1$
        } else if (this == FieldStatus.INVALID_NOT_EMPTY_OR_OLD_VALUE) {
            message = Messages.getString("FieldStatus.InvalidNotEmptyOrOldFormat"); //$NON-NLS-1$
        } else if (this == FieldStatus.INVALID_EMPTY_OR_OLD_VALUE) {
            message = Messages.getString("FieldStatus.InvalidEmptyOrOldFormat"); //$NON-NLS-1$
        } else if (this == FieldStatus.INVALID_VALUE_IN_OTHER_FIELD) {
            message = Messages.getString("FieldStatus.InvalidValueInOtherFieldFormat"); //$NON-NLS-1$
        } else if (this == FieldStatus.INVALID_VALUE_NOT_IN_OTHER_FIELD) {
            message = Messages.getString("FieldStatus.InvalidValueNotInOtherFieldFormat"); //$NON-NLS-1$
        } else if (this == FieldStatus.INVALID_DATE) {
            message = Messages.getString("FieldStatus.InvalidDateFormat"); //$NON-NLS-1$
        } else if (this == FieldStatus.INVALID_CHARACTERS) {
            message = Messages.getString("FieldStatus.InvalidCharactersFormat"); //$NON-NLS-1$
        } else {
            // FieldStatus.INVALID_UNKNOWN or an unhandled status
            message = Messages.getString("FieldStatus.UnknownErrorFormat"); //$NON-NLS-1$
        }

        return MessageFormat.format(message, field.getName());
    }
}
