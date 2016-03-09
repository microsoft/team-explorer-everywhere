// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.metadata;

import java.text.MessageFormat;

import com.microsoft.tfs.core.clients.workitem.internal.rules.Rule;

public class SpecialConstantIDs {
    public static final int SPECIAL_CONSTANT_IDS_START_RANGE = -20000;
    public static final int SPECIAL_CONSTANT_IDS_END_RANGE = -10000;

    public static final int CONST_VALID_USER = -2;
    public static final int CONST_EMPTY_VALUE = -10000;
    public static final int CONST_SAME_AS_OLD_VALUE = -10001;
    public static final int CONST_CURRENT_USER = -10002;
    public static final int CONST_OLD_VALUE_PLUS_ONE = -10003;
    public static final int CONST_WAS_EMPTY_VALUE = -10006;
    public static final int CONST_OLD_VALUE_IN_OTHER_FIELD = -10012;
    public static final int CONST_SERVER_DATE_TIME = -10013;
    public static final int CONST_BECAME_NON_EMPTY_VALUE = -10014;
    public static final int CONST_REMAINED_NON_EMPTY_VALUE = -10015;
    public static final int CONST_DELETED_TREE_LOCATION = -10016;
    public static final int CONST_ADMIN_ONLY_TREE_LOCATION = -10017;
    public static final int CONST_AFTER_SERVER_TIME = -10019;
    public static final int CONST_WAS_EMPTY_OR_SAME_AS_OLD_VALUE = -10022;
    public static final int CONST_VALUE_IN_OTHER_FIELD = -10025;
    public static final int CONST_SERVER_CURRENT_USER = -10026;
    public static final int CONST_LOCAL_DATE_TIME = -10027;
    public static final int CONST_UTC_DATE_TIME = -10028;
    public static final int CONST_IS_EMPTY_OR_SAME_AS_OLD_VALUE = -10029;
    public static final int CONST_GREATER_THAN_OLD_VALUE = -10030;
    public static final int CONST_SERVER_RANDOM_GUID = -10031;
    public static final int CONST_NOT_GREATER_THAN_SERVER_TIME = -10032;

    public static boolean isSpecialConstantID(final int id) {
        return (id >= SPECIAL_CONSTANT_IDS_START_RANGE) && (id <= SPECIAL_CONSTANT_IDS_END_RANGE);
    }

    public static final String makeErrorMessage(final int constantId, final Rule rule, final String subMessage) {
        return MessageFormat.format(
            "unhandled special constant [{0}] ({1}) for \"{2}\" (rule {3})", //$NON-NLS-1$
            Integer.toString(constantId),
            getSpecialConstantString(constantId),
            subMessage,
            Integer.toString(rule.getRuleID()));
    }

    public static final String getSpecialConstantString(final int id) {
        switch (id) {
            case CONST_VALID_USER:
                return "ConstValidUser"; //$NON-NLS-1$

            case CONST_EMPTY_VALUE:
                return "ConstEmptyValue"; //$NON-NLS-1$

            case CONST_SAME_AS_OLD_VALUE:
                return "ConstSameAsOldValue"; //$NON-NLS-1$

            case CONST_CURRENT_USER:
                return "ConstCurrentUser"; //$NON-NLS-1$

            case CONST_WAS_EMPTY_VALUE:
                return "ConstWasEmptyValue"; //$NON-NLS-1$

            case CONST_OLD_VALUE_IN_OTHER_FIELD:
                return "ConstOldValueInOtherField"; //$NON-NLS-1$

            case CONST_SERVER_DATE_TIME:
                return "ConstServerDateTime"; //$NON-NLS-1$

            case CONST_UTC_DATE_TIME:
                return "ConstUtcDateTime"; //$NON-NLS-1$

            case CONST_SERVER_CURRENT_USER:
                return "ConstServerCurrentUser"; //$NON-NLS-1$

            case CONST_LOCAL_DATE_TIME:
                return "ConstLocalDateTime"; //$NON-NLS-1$

            case CONST_REMAINED_NON_EMPTY_VALUE:
                return "ConstRemainedNonEmptyValue"; //$NON-NLS-1$

            case CONST_AFTER_SERVER_TIME:
                return "ConstAfterServerTime"; //$NON-NLS-1$

            case CONST_VALUE_IN_OTHER_FIELD:
                return "ConstValueInOtherField"; //$NON-NLS-1$

            case CONST_IS_EMPTY_OR_SAME_AS_OLD_VALUE:
                return "ConstIsEmptyOrSameAsOldValue"; //$NON-NLS-1$

            case CONST_WAS_EMPTY_OR_SAME_AS_OLD_VALUE:
                return "ConstWasEmptyOrSameAsOldValue"; //$NON-NLS-1$

            case CONST_BECAME_NON_EMPTY_VALUE:
                return "ConstBecameNonEmptyValue"; //$NON-NLS-1$

            case CONST_OLD_VALUE_PLUS_ONE:
                return "ConstOldValuePlusOne"; //$NON-NLS-1$

            case CONST_GREATER_THAN_OLD_VALUE:
                return "ConstGreaterThanOldValue"; //$NON-NLS-1$

            case CONST_DELETED_TREE_LOCATION:
                return "ConstDeletedTreeLocation"; //$NON-NLS-1$

            case CONST_ADMIN_ONLY_TREE_LOCATION:
                return "ConstAdminOnlyTreeLocation"; //$NON-NLS-1$

            case CONST_NOT_GREATER_THAN_SERVER_TIME:
                return "ConstNotGreaterThanServerTime"; //$NON-NLS-1$

            default:
                return "Unknown Special Constant"; //$NON-NLS-1$
        }
    }
}
