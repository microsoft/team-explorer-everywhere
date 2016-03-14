// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.commands.search;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.tfs.client.common.commands.TFSCommand;
import com.microsoft.tfs.client.common.repository.TFSRepository;
import com.microsoft.tfs.client.common.server.TFSServer;
import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.TFSCommonUIClientPlugin;
import com.microsoft.tfs.client.common.ui.framework.helper.UIHelpers;
import com.microsoft.tfs.client.common.ui.helpers.WorkItemEditorHelper;
import com.microsoft.tfs.client.common.ui.wit.results.QueryResultsEditor;
import com.microsoft.tfs.core.clients.webservices.PropertyValidation;
import com.microsoft.tfs.core.clients.workitem.CoreCategoryReferenceNames;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.NonCoreFieldsReferenceNames;
import com.microsoft.tfs.core.clients.workitem.SupportedFeatures;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldType;
import com.microsoft.tfs.core.clients.workitem.internal.fields.FieldDefinitionImpl;
import com.microsoft.tfs.core.clients.workitem.internal.type.WITypeConverter;
import com.microsoft.tfs.core.clients.workitem.internal.type.WITypeConverterException;
import com.microsoft.tfs.core.clients.workitem.internal.type.WIValueSource;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.clients.workitem.query.QueryScope;
import com.microsoft.tfs.core.clients.workitem.query.qe.WIQLHelpers;
import com.microsoft.tfs.core.clients.workitem.query.qe.WIQLOperators;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.search.IVSSearchFilterToken;
import com.microsoft.tfs.core.search.IVSSearchQuery;
import com.microsoft.tfs.core.search.IVSSearchQueryParser;
import com.microsoft.tfs.core.search.IVSSearchToken;
import com.microsoft.tfs.core.search.VSSearchFilterTokenType;
import com.microsoft.tfs.core.search.VSSearchUtils;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.GUID;
import com.microsoft.tfs.util.LocaleInvariantStringHelpers;
import com.microsoft.tfs.util.LocaleUtil;
import com.microsoft.tfs.util.StringUtil;
import com.microsoft.tfs.util.datetime.LenientDateTimeParser;

@SuppressWarnings("restriction")
public class WorkItemSearchCommand extends TFSCommand {
    private static final Log log = LogFactory.getLog(WorkItemSearchCommand.class);

    private static final int SEARCH_TERM_LIMIT = 50;
    private static final int SEARCH_CHAR_LIMIT = 2000;

    private final TFSServer server;
    private final TFSRepository repository;
    private final Project project;
    private final String searchString;

    private WorkItemClient workItemClient;
    private final WorkItemSearchCriteria criteria;

    private final LenientDateTimeParser lenientDateTimeParser = new LenientDateTimeParser();
    private final DateFormat wiqlDateFormatter = DateFormat.getDateInstance(DateFormat.SHORT);
    private final DateFormat rangeErrorDateFormatter =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

    public WorkItemSearchCommand(
        final TFSServer server,
        final TFSRepository repository,
        final Project project,
        final String searchString) {
        super();

        Check.notNull(server, "server"); //$NON-NLS-1$
        Check.notNull(repository, "repository"); //$NON-NLS-1$
        Check.notNull(project, "project"); //$NON-NLS-1$
        Check.notNull(searchString, "searchString"); //$NON-NLS-1$

        this.server = server;
        this.repository = repository;
        this.project = project;
        this.searchString = searchString;

        criteria = new WorkItemSearchCriteria();

        setCancellable(true);
    }

    @Override
    public String getName() {
        return Messages.getString("WorkItemSearchCommand.CommandText"); //$NON-NLS-1$
    }

    @Override
    public String getErrorDescription() {
        return Messages.getString("WorkItemSearchCommand.CommandErrorText"); //$NON-NLS-1$
    }

    @Override
    public String getLoggingDescription() {
        return Messages.getString("WorkItemSearchCommand.CommandText", LocaleUtil.ROOT); //$NON-NLS-1$
    }

    @Override
    protected IStatus doRun(final IProgressMonitor progressMonitor) throws Exception {
        this.workItemClient = repository.getVersionControlClient().getConnection().getWorkItemClient();

        if (searchString.length() > SEARCH_CHAR_LIMIT) {
            return new Status(
                Status.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                MessageFormat.format(
                    Messages.getString("WorkItemSearchCommand.SearchPageExceededCharacterLimitFormat"), //$NON-NLS-1$
                    SEARCH_CHAR_LIMIT));
        }

        // It might just be a simple ID
        if (trySearchAsWorkItemID(searchString)) {
            return Status.OK_STATUS;
        }

        final IVSSearchQueryParser parser = VSSearchUtils.createSearchQueryParser();
        final IVSSearchQuery query = parser.parse(searchString);

        if (query.getSearchString() == null || query.getSearchString().length() == 0) {
            return new Status(
                Status.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                "Cannot start a search with a query for a string that is null, empty or consisting only of white-space characters."); //$NON-NLS-1$
        }

        /*
         * TODO Maybe check parsedQuery.getParseSerror() at this point,
         * returning an error status in some cases? VS doesn't appear to do
         * this, instead just checking each token later.
         */

        int numTokens = query.getTokens(0, null);
        final IVSSearchToken[] tokens = new IVSSearchToken[numTokens];
        numTokens = query.getTokens(numTokens, tokens);

        if (numTokens > SEARCH_TERM_LIMIT) {
            return new Status(
                Status.ERROR,
                TFSCommonUIClientPlugin.PLUGIN_ID,
                MessageFormat.format(
                    Messages.getString("WorkItemSearchCommand.SearchPageExceededTermLimitFormat"), //$NON-NLS-1$
                    SEARCH_TERM_LIMIT));
        }

        final String wiql = buildWIQL(tokens, true);

        if (progressMonitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        final QueryDocument queryDocument =
            server.getQueryDocumentService().createNewQueryDocument(wiql, project.getName(), QueryScope.PRIVATE);

        if (progressMonitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }

        UIHelpers.runOnUIThread(true, new Runnable() {
            @Override
            public void run() {
                QueryResultsEditor.openEditor(server, queryDocument);
            }
        });

        return Status.OK_STATUS;
    }

    private boolean trySearchAsWorkItemID(final String text) {
        try {
            final int id = Integer.parseInt(text);
            if (id > 0) {
                UIHelpers.runOnUIThread(true, new Runnable() {
                    @Override
                    public void run() {
                        WorkItemEditorHelper.openEditor(server, id);
                    }
                });
                return true;
            }
        } catch (final NumberFormatException e) {
        }

        return false;
    }

    private String buildWIQL(final IVSSearchToken[] tokens, final boolean includePreviewFields) {
        final StringBuilder wiql = new StringBuilder();

        wiql.append(buildSelect(includePreviewFields));
        wiql.append(buildWhere(tokens));
        wiql.append(buildOrder());

        return wiql.toString();
    }

    private String buildSelect(final boolean includePreviewFields) {
        final StringBuilder wiql = new StringBuilder();

        // Basic fields
        wiql.append("SELECT System.Id, "); //$NON-NLS-1$
        if (includePreviewFields) {
            // Hover preview fields
            wiql.append(getRequiredFieldsWiqlFragment());
        } else {
            wiql.append(StringUtil.join(new String[] {
                CoreFieldReferenceNames.WORK_ITEM_TYPE,
                CoreFieldReferenceNames.TITLE,
                CoreFieldReferenceNames.STATE,
                CoreFieldReferenceNames.ASSIGNED_TO
            }, ",")); //$NON-NLS-1$
        }

        wiql.append(" FROM workitems"); //$NON-NLS-1$

        return wiql.toString();
    }

    private String buildWhere(final IVSSearchToken[] tokens) {
        final StringBuilder wiql = new StringBuilder();

        wiql.append(" WHERE System.TeamProject = @project"); //$NON-NLS-1$
        for (final IVSSearchToken token : tokens) {
            wiql.append(" " + WIQLOperators.AND + " "); //$NON-NLS-1$//$NON-NLS-2$

            if (token instanceof IVSSearchFilterToken) {
                appendFilterCondition((IVSSearchFilterToken) token, wiql);
            } else {
                appendBasicCondition(token, wiql);
            }
        }

        final Project p = workItemClient.getProjects().get(project.getName());
        if (p != null
            && p.getWITContext().getServerInfo().isSupported(SupportedFeatures.WORK_ITEM_TYPE_CATEGORY_MEMBERS)
            && p.getCategories().contains(CoreCategoryReferenceNames.CODE_REVIEW_RESPONSE)) {
            // Exclude the code review response category
            wiql.append(" "); //$NON-NLS-1$
            wiql.append(StringUtil.join(new String[] {
                WIQLOperators.AND,
                WIQLHelpers.getEnclosedField(CoreFieldReferenceNames.WORK_ITEM_TYPE),
                WIQLOperators.NOT_IN_GROUP,
                WIQLHelpers.getSingleQuotedValue(CoreCategoryReferenceNames.CODE_REVIEW_RESPONSE)
            }, " ")); //$NON-NLS-1$
            wiql.append(" "); //$NON-NLS-1$
        }

        return wiql.toString();
    }

    private String buildOrder() {
        final StringBuilder wiql = new StringBuilder();

        // ORDER BY
        wiql.append(" "); //$NON-NLS-1$
        wiql.append(getOrderByDescendingCreatedDate());

        return wiql.toString();
    }

    private void appendFilterCondition(final IVSSearchFilterToken filterToken, final StringBuilder wiql) {
        final FieldDefinition field =
            workItemClient.getFieldDefinitions().get(getFieldReferenceName(filterToken.getFilterField()));
        wiql.append(field.getReferenceName());

        wiql.append(" "); //$NON-NLS-1$

        final String operation = getOperator(field, filterToken.getFilterTokenType());
        wiql.append(operation);

        wiql.append(" "); //$NON-NLS-1$

        wiql.append(getInvariantFieldValue(field, filterToken.getFilterValue()));

        // need to do separate filtering for criteria builder.
        String fieldValue = filterToken.getFilterValue();
        final String macroCandidate = getInvariantMacro(field, filterToken.getFilterValue());
        if (macroCandidate != null && macroCandidate.length() > 0) {
            fieldValue = evaluateInvariantMacro(field, macroCandidate);
        }

        // add the criterion to the store of raw criteria information.
        criteria.addCriterion(field.getName(), operation, fieldValue);
    }

    /**
     * Return a WIQL operator based on the search operator and field.
     *
     * @param field
     *        The field the operator is used with.
     * @param filterOp
     *        VS search operator flags
     * @return WIQL operator
     */
    private String getOperator(final FieldDefinition field, final int filterOp) {
        // Determine which search token flags have been set.
        final boolean exactMatch = (filterOp & VSSearchFilterTokenType.EXACT_MATCH) != 0;
        final boolean exclude = (filterOp & VSSearchFilterTokenType.EXCLUDE) != 0;

        // Determine the correct WIQL operator.
        final FieldType fieldType = field.getFieldType();

        if (fieldType == FieldType.BOOLEAN
            || fieldType == FieldType.DOUBLE
            || fieldType == FieldType.GUID
            || fieldType == FieldType.INTEGER
            || fieldType == FieldType.INTERNAL
            || fieldType == FieldType.DATETIME) {
            // FIELDS SUPPORTING (NOT)EQUALS ONLY
            // (For these fields, both ":" and "=" are interpreted as "=")
            if (exclude) {
                return WIQLOperators.NOT_EQUAL_TO;
            }
            return WIQLOperators.EQUAL_TO;

        } else if (fieldType == FieldType.HISTORY || fieldType == FieldType.HTML || fieldType == FieldType.PLAINTEXT) {
            // FIELDS SUPPORTING (NOT)CONTAINS ONLY
            // (For these fields, both ":" and "=" are interpreted as ":")
            if (exclude) {
                return WIQLOperators.NOT_CONTAINS;
            }
            return WIQLOperators.CONTAINS;
        } else if (fieldType == FieldType.STRING) {
            // FIELDS SUPPORTING BOTH (NOT)EQUALS AND (NOT)CONTAINS
            if (exactMatch) {
                if (exclude) {
                    return WIQLOperators.NOT_EQUAL_TO;
                }
                return WIQLOperators.EQUAL_TO;
            } else {
                if (exclude) {
                    return WIQLOperators.NOT_CONTAINS;
                }
                return WIQLOperators.CONTAINS;
            }
        } else if (fieldType == FieldType.TREEPATH) {
            // SPECIAL CASE: TREE PATH
            if (exactMatch) {
                if (exclude) {
                    return WIQLOperators.NOT_EQUAL_TO;
                }
                return WIQLOperators.EQUAL_TO;
            } else {
                if (exclude) {
                    return WIQLOperators.NOT_UNDER;
                }
                return WIQLOperators.UNDER;
            }
        } else {
            // DEFAULT
            log.warn(
                "Defaulting to equals / not equals operator; this situation should have been covered by one of the above cases."); //$NON-NLS-1$
            if (exclude) {
                return WIQLOperators.NOT_EQUAL_TO;
            }
            return WIQLOperators.EQUAL_TO;
        }
    }

    private String getInvariantMacro(final FieldDefinition field, final String localizedValue) {
        // is it a macro, replace with invariant macro
        // TODO: This implementation differs a fair bit from similar method in
        // FilterGridAdapter...
        // If subset behavior is intentional, this should be called out,
        // otherwise fixed.

        // today is special in that it can have (@today - x)
        if (LocaleInvariantStringHelpers.caseInsensitiveStartsWith(
            localizedValue,
            WIQLOperators.getLocalizedOperator(WIQLOperators.MACRO_TODAY))) {
            return WIQLOperators.getInvariantTodayMinusMacro(localizedValue);
        } else if (LocaleInvariantStringHelpers.caseInsensitiveEquals(
            localizedValue,
            WIQLOperators.getLocalizedOperator(WIQLOperators.MACRO_ME))) {
            return WIQLOperators.MACRO_ME;
        } else if (LocaleInvariantStringHelpers.caseInsensitiveEquals(
            localizedValue,
            WIQLOperators.getLocalizedOperator(WIQLOperators.MACRO_PROJECT))) {
            return WIQLOperators.MACRO_PROJECT;
        } else if (LocaleInvariantStringHelpers.caseInsensitiveEquals(
            localizedValue,
            WIQLOperators.getLocalizedOperator(WIQLOperators.MACRO_CURRENT_ITERATION))) {
            return WIQLOperators.MACRO_CURRENT_ITERATION;
        }
        return null;
    }

    /**
     * Note: This culture conversion is modelled on
     * GetInvariantFieldValueFromString() method in Query builder
     * implementation.
     *
     * \vset\SCM\workitemtracking\Controls\WinForms\QueryBuilder\
     * FilterGridAdapter.cs
     *
     * The two have similar functional requirements, but their current contracts
     * differ a fair bit, making code sharing impractical in current form.
     */
    private String getInvariantFieldValue(final FieldDefinition field, final String localValue) {
        String invariantResult = localValue;
        try {
            final String macroCandidate = getInvariantMacro(field, localValue);

            final boolean isEmpty = localValue.length() == 0;

            if (macroCandidate != null) {
                // Convert macros as you would operators
                invariantResult = macroCandidate;
            } else if (field.getSystemType().equals(Date.class) && !isEmpty) {
                // Convert date time values to invariant format using
                // LenientDateTimeParser, which is much more flexible than
                // WITypeConverter.
                final Date d = lenientDateTimeParser.parse(localValue, true, true).getTime();
                throwIfDateOutOfRange(d);
                invariantResult = wiqlDateFormatter.format(d);
                invariantResult = WIQLHelpers.getEscapedSingleQuotedValue(invariantResult);
            } else if (!isEmpty && field.getSystemType().equals(Integer.class)
                || field.getSystemType().equals(Double.class)
                || field.getSystemType().equals(Boolean.class)) {
                // Generic object conversion.
                final WITypeConverter typeConverter = ((FieldDefinitionImpl) field).getTypeConverter();
                final Object ob = typeConverter.translate(localValue, WIValueSource.LOCAL);
                invariantResult = ob.toString();
            } else {
                final boolean isString = field.getSystemType().equals(String.class);
                final boolean isGuid = field.getSystemType().equals(GUID.class);
                if (isGuid || isString || isEmpty) {
                    invariantResult = WIQLHelpers.getEscapedSingleQuotedValue(localValue);
                }
            }
        } catch (final Exception e) {
            log.warn(MessageFormat.format("Error parsing field value {0}", localValue), e); //$NON-NLS-1$

            throw new RuntimeException(
                MessageFormat.format(
                    Messages.getString("WorkItemSearchCommand.SearchPageFormatExceptionFormat"), //$NON-NLS-1$
                    localValue,
                    field.getName(),
                    field.getFieldType().getDisplayName()));
        }

        return invariantResult;
    }

    private String getFieldReferenceName(final String name) {
        if (name.equalsIgnoreCase("A")) //$NON-NLS-1$
        {
            return CoreFieldReferenceNames.ASSIGNED_TO;
        } else if (name.equalsIgnoreCase("C")) //$NON-NLS-1$
        {
            return CoreFieldReferenceNames.CREATED_BY;
        } else if (name.equalsIgnoreCase("S")) //$NON-NLS-1$
        {
            return CoreFieldReferenceNames.STATE;
        } else if (name.equalsIgnoreCase("T")) //$NON-NLS-1$
        {
            return CoreFieldReferenceNames.WORK_ITEM_TYPE;
        } else {
            if (workItemClient.getFieldDefinitions().contains(name)) {
                return workItemClient.getFieldDefinitions().get(name).getReferenceName();
            }

            final String format = Messages.getString("WorkItemSearchCommand.ErrorSearchInvalidFieldNameFormat"); //$NON-NLS-1$
            throw new TECoreException(MessageFormat.format(format, name));
        }
    }

    private void appendBasicCondition(final IVSSearchToken token, final StringBuilder wiql) {
        final FieldDefinition titleField = workItemClient.getFieldDefinitions().get(CoreFieldReferenceNames.TITLE);
        final FieldDefinition descriptionField =
            workItemClient.getFieldDefinitions().get(CoreFieldReferenceNames.DESCRIPTION);
        final FieldDefinition reproStepsField =
            workItemClient.getFieldDefinitions().contains(NonCoreFieldsReferenceNames.REPRO_STEPS)
                ? workItemClient.getFieldDefinitions().get(NonCoreFieldsReferenceNames.REPRO_STEPS) : null;

        boolean exclude = false;
        String parsedValue = token.getParsedTokenText();

        // Only 'exclude' if the very first character is a '-' from a
        // non-literal (quoted) token.
        if (parsedValue.length() > 1 && parsedValue.charAt(0) == '-' && !isQuoted(token.getOriginalTokenText())) {
            exclude = true;
            parsedValue = parsedValue.substring(1);
        }

        final String clauseOperator = exclude ? WIQLOperators.AND : WIQLOperators.OR;

        final String escapedValue = WIQLHelpers.getEscapedSingleQuotedValue(parsedValue);

        // Use text search for all fields if available, for more efficient
        // search. For simplicity, we'll show "contains words" in the
        // description if Title supports text search, since Description and
        // ReproSteps are text fields and will as well then.
        final boolean useTextSearchIfAvailable = true;
        final boolean showContainsWords = useTextSearchIfAvailable && titleField.supportsTextQuery();

        wiql.append("("); //$NON-NLS-1$
        wiql.append(getContainsClause(titleField, escapedValue, exclude, useTextSearchIfAvailable));

        wiql.append(" " + clauseOperator + " "); //$NON-NLS-1$ //$NON-NLS-2$
        wiql.append(getContainsClause(descriptionField, escapedValue, exclude, useTextSearchIfAvailable));
        if (reproStepsField != null) {
            wiql.append(" " + clauseOperator + " "); //$NON-NLS-1$ //$NON-NLS-2$
            wiql.append(getContainsClause(reproStepsField, escapedValue, exclude, useTextSearchIfAvailable));
        }
        wiql.append(")"); //$NON-NLS-1$

        criteria.addCriterion(
            getKeywordSearchHelpText(exclude, titleField, descriptionField, reproStepsField),
            (exclude
                ? (showContainsWords ? WorkItemSearchCriteriaHelper.INVARIANT_KEYWORD_NOT_CONTAINS_WORDS
                    : WorkItemSearchCriteriaHelper.INVARIANT_KEYWORD_NOT_CONTAINS)
                : (showContainsWords ? WorkItemSearchCriteriaHelper.INVARIANT_KEYWORD_CONTAINS_WORDS
                    : WorkItemSearchCriteriaHelper.INVARIANT_KEYWORD_CONTAINS)),
            parsedValue);
    }

    /*
     * TODO Move this method to some other place if/when we do work item
     * previews in Team Explorer.
     */
    /**
     * Fields (defined in WIQL format) used by the work item preview control.
     * These fields must be directly retrieved by the query to avoid paging work
     * item field data on the UI thread when opening the Work Item Preview.
     * <p>
     * This string is in the format
     * "System.FieldX, System.FieldY, System.FieldZ" (no ending comma) and can
     * be be treated as a self-contained fragment in the SELECT clause when
     * inserting into WIQL.
     * <p>
     * For example:
     * <code>String.Format("SELECT X, {0}, Y, Z FROM ...", WorkItemPreview.GetRequiredFieldsWiqlFragment(store))</code>
     *
     * @param store
     * @return
     */
    private String getRequiredFieldsWiqlFragment() {
        String retval =
            "System.Title, System.CreatedBy, System.CreatedDate, System.WorkItemType, System.State, System.AssignedTo, System.ChangedDate, System.AreaPath, System.IterationPath, System.Description"; //$NON-NLS-1$
        final boolean containsRepro =
            workItemClient.getFieldDefinitions().contains(NonCoreFieldsReferenceNames.REPRO_STEPS);

        if (containsRepro) {
            retval += ", " + NonCoreFieldsReferenceNames.REPRO_STEPS; //$NON-NLS-1$
        }

        return retval;
    }

    /**
     * Returns a WIQL 'ORDER BY' clause (including 'ORDER BY') that sorts by
     * created date in descending order.
     */
    public static String getOrderByDescendingCreatedDate() {
        final StringBuilder wiql = new StringBuilder();
        wiql.append("ORDER BY ["); //$NON-NLS-1$
        wiql.append(CoreFieldReferenceNames.CREATED_DATE);
        wiql.append("] desc"); //$NON-NLS-1$
        return wiql.toString();
    }

    /**
     * Returns <code>true</code> if the specified string is contained in double
     * quotes, <code>false</code> otherwise
     */
    private boolean isQuoted(String text) {
        text = text.trim();
        return text.length() > 1 && text.charAt(0) == '"' && text.charAt(text.length() - 1) == '"';
    }

    /**
     * If the user is searching a bare term, we want to tell them what fields
     * are being searched (title, description and/or repro steps).
     *
     *
     * @param exclude
     *        Whether the search term is negated (used to determine whether the
     *        last separator is an "and" or "or").
     * @param titleField
     *        The title field.
     * @param descriptionField
     *        The description field.
     * @param reproStepsField
     *        The repro steps field, null if not present in this team project.
     * @return A user-friendly representation of the keyword fields being
     *         searched, in localized form - for example,
     *         "Title, Description, and[/or] Repro Steps" or
     *         "Title and[/or] Description".
     */
    private static String getKeywordSearchHelpText(
        final boolean exclude,
        final FieldDefinition titleField,
        final FieldDefinition descriptionField,
        final FieldDefinition reproStepsField) {
        String keywordSearchHelpText = ""; //$NON-NLS-1$
        if (reproStepsField != null) {
            final String format =
                exclude ? Messages.getString("WorkItemSearchCommand.SearchPageKeywordNotHelpTextWithReproFormat") : //$NON-NLS-1$
                    Messages.getString("WorkItemSearchCommand.SearchPageKeywordHelpTextWithReproFormat"); //$NON-NLS-1$

            keywordSearchHelpText = MessageFormat.format(
                format,
                titleField.getName(),
                descriptionField.getName(),
                reproStepsField.getName());
        } else {
            final String format =
                exclude ? Messages.getString("WorkItemSearchCommand.SearchPageKeywordNotHelpTextWithoutReproFormat") //$NON-NLS-1$
                    : Messages.getString("WorkItemSearchCommand.SearchPageKeywordHelpTextWithoutReproFormat"); //$NON-NLS-1$

            keywordSearchHelpText = MessageFormat.format(format, titleField.getName(), descriptionField.getName());
        }
        return keywordSearchHelpText;
    }

    private String getContainsClause(
        final FieldDefinition field,
        final String value,
        final boolean exclude,
        final boolean useTextSearchIfAvailable) {
        // Use "ContainsWords" on String fields if available. "Contains" already
        // does text search for Text fields if available, and works with old
        // clients.
        final boolean useContainsWords = useTextSearchIfAvailable && !field.isLongText() && field.supportsTextQuery();
        final String op = exclude ? (useContainsWords ? WIQLOperators.NOT_CONTAINS_WORDS : WIQLOperators.NOT_CONTAINS)
            : (useContainsWords ? WIQLOperators.CONTAINS_WORDS : WIQLOperators.CONTAINS);

        return field.getReferenceName() + " " + op + " " + value; //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String evaluateInvariantMacro(final FieldDefinition field, final String invariantMacro) {
        // is it a macro, replace with actual macro value
        if (field.getFieldType() == FieldType.DATETIME
            && LocaleInvariantStringHelpers.caseInsensitiveStartsWith(
                invariantMacro.trim(),
                WIQLOperators.MACRO_TODAY)) {
            // if it is only @today with no @today - n, then just return the
            // date
            if (invariantMacro.trim().equalsIgnoreCase(WIQLOperators.MACRO_TODAY)) {
                return wiqlDateFormatter.format(Calendar.getInstance().getTime());
            }

            // if the macro contains a '-' sign that isn't at the end of
            // the string
            boolean subtract = true;
            String[] macroTokens = invariantMacro.split(Pattern.quote("-")); //$NON-NLS-1$

            if (macroTokens.length != 2) {
                // try for the '+' sign
                macroTokens = invariantMacro.split(Pattern.quote("+")); //$NON-NLS-1$
                subtract = false;
            }

            // if there aren't two tokens, one on either side of the minus or
            // plus sign, the macro is ill-formed and we will not parse it.
            if (macroTokens.length != 2) {
                return invariantMacro;
            }

            int n;
            try {
                n = Integer.parseInt(macroTokens[1].trim());
            } catch (final NumberFormatException e) {
                return invariantMacro;
            }

            if (subtract) {
                n *= -1;
            }

            // return the date corresponding to today + (-n)
            final Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, n);
            throwIfDateOutOfRange(calendar.getTime());
            return wiqlDateFormatter.format(calendar.getTime());
        } else if (invariantMacro.equalsIgnoreCase(WIQLOperators.MACRO_ME)) {
            return workItemClient.getUserDisplayName();
        } else if (invariantMacro.equalsIgnoreCase(WIQLOperators.MACRO_PROJECT)) {
            return project.getName();
        }

        return invariantMacro;
    }

    /**
     * Checks that the {@link Date} is within the supported range of dates for
     * work item queries. This method is present because the messages from the
     * client-side WIQL parser and query builder are hard to understand in
     * overflow or underflow cases.
     *
     * @param date
     *        the date to check
     * @throws WITypeConverterException
     *         for consistency with core validation methods
     */
    private void throwIfDateOutOfRange(final Date date) throws WITypeConverterException {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        if (cal.before(PropertyValidation.MIN_ALLOWED_DATE_TIME)
            || cal.after(PropertyValidation.MAX_ALLOWED_DATE_TIME)) {
            throw new WITypeConverterException(
                MessageFormat.format(
                    Messages.getString("WorkItemSearchCommand.DateOutOfRangeFormat"), //$NON-NLS-1$
                    rangeErrorDateFormatter.format(date),
                    rangeErrorDateFormatter.format(PropertyValidation.MIN_ALLOWED_DATE_TIME.getTime()),
                    rangeErrorDateFormatter.format(PropertyValidation.MAX_ALLOWED_DATE_TIME.getTime())));
        }
    }
}
