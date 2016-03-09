// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.wiqlparse;

import com.microsoft.tfs.core.Messages;

public class SyntaxError {
    public static final SyntaxError NO_ERROR = new SyntaxError(Messages.getString("SyntaxError.NoError")); //$NON-NLS-1$
    public static final SyntaxError DATE_CONSTANT_FORMAT =
        new SyntaxError(Messages.getString("SyntaxError.DateConstantFormat")); //$NON-NLS-1$
    public static final SyntaxError DUPLICATE_FROM = new SyntaxError(Messages.getString("SyntaxError.DuplicateFrom")); //$NON-NLS-1$
    public static final SyntaxError DUPLICATE_GROUP_BY =
        new SyntaxError(Messages.getString("SyntaxError.DuplicateGroupBy")); //$NON-NLS-1$
    public static final SyntaxError DUPLICATE_ORDER_BY =
        new SyntaxError(Messages.getString("SyntaxError.DuplicateOrderBy")); //$NON-NLS-1$
    public static final SyntaxError DUPLICATE_WHERE = new SyntaxError(Messages.getString("SyntaxError.DuplicateWhere")); //$NON-NLS-1$
    public static final SyntaxError DUPLICATE_AS_OF = new SyntaxError(Messages.getString("SyntaxError.DuplicateAsOf")); //$NON-NLS-1$
    public static final SyntaxError DUPLICATE_MODE = new SyntaxError(Messages.getString("SyntaxError.DuplicateMode")); //$NON-NLS-1$
    public static final SyntaxError EVER_NOT_EQUAL_OPERATOR =
        new SyntaxError(Messages.getString("SyntaxError.EverNotEqualOperator")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_BOOLEAN =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingBoolean")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_BY = new SyntaxError(Messages.getString("SyntaxError.ExpectingBy")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_CLOSING_QUOTE =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingClosingQuote")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_CLOSING_SQUARE_BRACKET =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingClosingSquareBracket")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_COMPARISON_OPERATOR =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingComparisonOperator")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_CONDITION =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingCondition")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_CONST = new SyntaxError(Messages.getString("SyntaxError.ExpectingConst")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_END_OF_STRING =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingEndOfString")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_FIELD_LIST =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingFieldList")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_FIELD_NAME =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingFieldName")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_LEFT_BRACKET =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingLeftBracket")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_RIGHT_BRACKET =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingRightBracket")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_SELECT =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingSelect")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_TABLE_NAME =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingTableName")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_VALUE = new SyntaxError(Messages.getString("SyntaxError.ExpectingValue")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_DATE = new SyntaxError(Messages.getString("SyntaxError.ExpectingDate")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_MODE = new SyntaxError(Messages.getString("SyntaxError.ExpectingMode")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_EXPRESSION =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingExpression")); //$NON-NLS-1$
    public static final SyntaxError EXPECTING_FIELD_OR_EXPRESSION =
        new SyntaxError(Messages.getString("SyntaxError.ExpectingFieldOrExpression")); //$NON-NLS-1$
    public static final SyntaxError EMPTY_NAME = new SyntaxError(Messages.getString("SyntaxError.EmptyName")); //$NON-NLS-1$
    public static final SyntaxError FIELD_DOES_NOT_EXIST_IN_THE_TABLE =
        new SyntaxError(Messages.getString("SyntaxError.FieldDoesNotExistInTheTable")); //$NON-NLS-1$
    public static final SyntaxError GROUP_BY_IS_NOT_SUPPORTED =
        new SyntaxError(Messages.getString("SyntaxError.GroupByIsNotSupported")); //$NON-NLS-1$
    public static final SyntaxError IN_OPERATOR_WITH_TREE_PATH_FIELD =
        new SyntaxError(Messages.getString("SyntaxError.InOperatorWithTreePathField")); //$NON-NLS-1$
    public static final SyntaxError INCOMPATIBLE_RIGHT_CONST =
        new SyntaxError(Messages.getString("SyntaxError.IncompatibleRightConst")); //$NON-NLS-1$
    public static final SyntaxError INVALID_CONDITIONAL_OPERATOR =
        new SyntaxError(Messages.getString("SyntaxError.InvalidConditionalOperator")); //$NON-NLS-1$
    public static final SyntaxError INVALID_NODE_TYPE =
        new SyntaxError(Messages.getString("SyntaxError.InvalidNodeType")); //$NON-NLS-1$
    public static final SyntaxError CONTAINS_WORKS_FOR_STRINGS_ONLY =
        new SyntaxError(Messages.getString("SyntaxError.ContainsWorksForStringsOnly")); //$NON-NLS-1$
    public static final SyntaxError NON_SORTABLE_FIELD =
        new SyntaxError(Messages.getString("SyntaxError.NonSortableField")); //$NON-NLS-1$
    public static final SyntaxError PATH_MUST_BE_A_STRING_NOT_STARTING_WITH_BACKSLASH =
        new SyntaxError(Messages.getString("SyntaxError.PathMustBeAStringNotStartingWithBackslash")); //$NON-NLS-1$
    public static final SyntaxError STRING_IS_NOT_A_NUMBER =
        new SyntaxError(Messages.getString("SyntaxError.StringIsNotANumber")); //$NON-NLS-1$
    public static final SyntaxError TABLE_DOES_NOT_EXIST =
        new SyntaxError(Messages.getString("SyntaxError.TableDoesNotExist")); //$NON-NLS-1$
    public static final SyntaxError TOO_COMPLEX_EVER_OPERATOR =
        new SyntaxError(Messages.getString("SyntaxError.TooComplexEverOperator")); //$NON-NLS-1$
    public static final SyntaxError TREE_PATH_IS_NOT_FOUND_IN_HIERARCHY =
        new SyntaxError(Messages.getString("SyntaxError.TreePathIsNotFoundInHierarchy")); //$NON-NLS-1$
    public static final SyntaxError UNDER_CAN_BE_USED_FOR_TREE_PATH_FIELD_ONLY =
        new SyntaxError(Messages.getString("SyntaxError.UnderCanBeUsedForTreePathFieldOnly")); //$NON-NLS-1$
    public static final SyntaxError UNKNOWN_FIELD_TYPE =
        new SyntaxError(Messages.getString("SyntaxError.UnknownFieldType")); //$NON-NLS-1$
    public static final SyntaxError UNKNOWN_OR_INCOMPATIBLE_TYPES_IN_THE_LIST =
        new SyntaxError(Messages.getString("SyntaxError.UnknownOrIncompatibleTypesInTheList")); //$NON-NLS-1$
    public static final SyntaxError FROM_IS_NOT_SPECIFIED =
        new SyntaxError(Messages.getString("SyntaxError.FromIsNotSpecified")); //$NON-NLS-1$
    public static final SyntaxError INVALID_CONDITION_FOR_LONG_TEXT_FIELD =
        new SyntaxError(Messages.getString("SyntaxError.InvalidConditionForLongTextField")); //$NON-NLS-1$
    public static final SyntaxError INVALID_CONDITION_FOR_TREE_FIELD =
        new SyntaxError(Messages.getString("SyntaxError.InvalidConditionForTreeField")); //$NON-NLS-1$
    public static final SyntaxError NON_QUERYABLE_FIELD =
        new SyntaxError(Messages.getString("SyntaxError.NonQueryableField")); //$NON-NLS-1$
    public static final SyntaxError VARIABLE_DOES_NOT_EXIST =
        new SyntaxError(Messages.getString("SyntaxError.VariableDoesNotExist")); //$NON-NLS-1$
    public static final SyntaxError UNKNOWN_VARIABLE_TYPE =
        new SyntaxError(Messages.getString("SyntaxError.UnknownVariableType")); //$NON-NLS-1$
    public static final SyntaxError INVALID_CONDITION_FOR_NODE_FIELD =
        new SyntaxError(Messages.getString("SyntaxError.InvalidConditionForNodeField")); //$NON-NLS-1$
    public static final SyntaxError INVALID_PROJECT_NAME =
        new SyntaxError(Messages.getString("SyntaxError.InvalidProjectName")); //$NON-NLS-1$
    public static final SyntaxError PROJECT_NOT_FOUND =
        new SyntaxError(Messages.getString("SyntaxError.ProjectNotFound")); //$NON-NLS-1$
    public static final SyntaxError WRONG_TYPE_FOR_ARITHMETIC =
        new SyntaxError(Messages.getString("SyntaxError.WrongTypeForArithmetic")); //$NON-NLS-1$
    public static final SyntaxError INVALID_LONG_TEXT_SEARCH_FOR_WHITESPACE =
        new SyntaxError(Messages.getString("SyntaxError.InvalidLongTextSearchForWhitespace")); //$NON-NLS-1$
    public static final SyntaxError WRONG_TYPE_FOR_ARITHMETIC_RIGHT_OPERAND =
        new SyntaxError(Messages.getString("SyntaxError.WrongTypeForArithmeticRightOperand")); //$NON-NLS-1$
    public static final SyntaxError NON_ZERO_TIME = new SyntaxError(Messages.getString("SyntaxError.NonZeroTime")); //$NON-NLS-1$
    public static final SyntaxError INVALID_CONDITION_FOR_EMPTY_STRING =
        new SyntaxError(Messages.getString("SyntaxError.InvalidConditionForEmptyString")); //$NON-NLS-1$
    public static final SyntaxError EVER_WITH_DATE_PRECISION =
        new SyntaxError(Messages.getString("SyntaxError.EverWithDatePrecision")); //$NON-NLS-1$
    public static final SyntaxError MIXED_PREFIXES = new SyntaxError(Messages.getString("SyntaxError.MixedPrefixes")); //$NON-NLS-1$
    public static final SyntaxError EVER_WITH_LINK_QUERY =
        new SyntaxError(Messages.getString("SyntaxError.EverWithLinkQuery")); //$NON-NLS-1$
    public static final SyntaxError INVALID_CONDITION_FOR_LINK_TYPE =
        new SyntaxError(Messages.getString("SyntaxError.InvalidConditionForLinkType")); //$NON-NLS-1$
    public static final SyntaxError INVALID_LINK_TYPE_NAME =
        new SyntaxError(Messages.getString("SyntaxError.InvalidLinkTypeName")); //$NON-NLS-1$
    public static final SyntaxError NOT_SUPPORTED_TREE_QUERY =
        new SyntaxError(Messages.getString("SyntaxError.NotSupportedTreeQuery")); //$NON-NLS-1$
    public static final SyntaxError INCORRECT_QUERY_METHOD =
        new SyntaxError(Messages.getString("SyntaxError.IncorrectQueryMethod")); //$NON-NLS-1$
    public static final SyntaxError TREE_QUERY_NEEDS_ONE_LINK_TYPE =
        new SyntaxError(Messages.getString("SyntaxError.TreeQueryNeedsOneLinkType")); //$NON-NLS-1$
    public static final SyntaxError RECURSIVE_ON_WORK_ITEMS =
        new SyntaxError(Messages.getString("SyntaxError.RecursiveOnWorkitems")); //$NON-NLS-1$
    public static final SyntaxError UNKNOWN_MODE = new SyntaxError(Messages.getString("SyntaxError.UnknownMode")); //$NON-NLS-1$
    public static final SyntaxError MODE_ON_WORK_ITEMS =
        new SyntaxError(Messages.getString("SyntaxError.ModeOnWorkitems")); //$NON-NLS-1$
    public static final SyntaxError ORDER_BY_LINK_FIELD =
        new SyntaxError(Messages.getString("SyntaxError.OrderByLinkField")); //$NON-NLS-1$
    public static final SyntaxError INVALID_LINK_TYPE_NAME_RECURSIVE =
        new SyntaxError(Messages.getString("SyntaxError.InvalidLinkTypeNameRecursive")); //$NON-NLS-1$
    public static final SyntaxError DUPLICATE_ORDER_BY_FIELD =
        new SyntaxError(Messages.getString("SyntaxError.DuplicateOrderByField")); //$NON-NLS-1$
    public static final SyntaxError INCOMPATIBLE_CONDITION_PARTS_TYPE =
        new SyntaxError(Messages.getString("SyntaxError.IncompatibleConditionPartsType")); //$NON-NLS-1$
    public static final SyntaxError INVALID_RIGHT_EXPRESSION_IN_CONDITION =
        new SyntaxError(Messages.getString("SyntaxError.InvalidRightExpressionInCondition")); //$NON-NLS-1$
    public static final SyntaxError FIELD_CONDITIONS_IN_LINK_QUERIES =
        new SyntaxError(Messages.getString("SyntaxError.FieldConditionsInLinkQueries")); //$NON-NLS-1$
    public static final SyntaxError INVALID_FIELD_TYPE_FOR_CONDITION =
        new SyntaxError(Messages.getString("SyntaxError.InvalidFieldTypeForCondition")); //$NON-NLS-1$

    private final String message;

    private SyntaxError(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
