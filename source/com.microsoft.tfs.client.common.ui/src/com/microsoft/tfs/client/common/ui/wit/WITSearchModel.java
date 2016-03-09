// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogSettings;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldType;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.project.Project;

public class WITSearchModel {
    public static final int SEARCH_TYPE_ALL_TERMS = 1;
    public static final int SEARCH_TYPE_ANY_TERM = 2;

    public static final String SEARCH_FIELD_SELECT = Messages.getString("WITSearchModel.SelectFieldPrompt"); //$NON-NLS-1$

    private final Map<String, FieldDefinition> nameToFieldDefinition = new HashMap<String, FieldDefinition>();
    private final Map<String, FieldDefinition> referenceNameToFieldDefinition = new HashMap<String, FieldDefinition>();
    private final DateFormat wiqlDateFormatter = DateFormat.getDateInstance(DateFormat.SHORT);
    private final Map<String, Project> nameToProject = new HashMap<String, Project>();
    private final Set<String> uniqueWorkItemTypeNames;

    public WITSearchModel(
        final FieldDefinition[] queryableFields,
        final Project[] projects,
        final String[] workItemTypeNames) {
        for (int i = 0; i < queryableFields.length; i++) {
            nameToFieldDefinition.put(queryableFields[i].getName(), queryableFields[i]);
            referenceNameToFieldDefinition.put(queryableFields[i].getReferenceName(), queryableFields[i]);
        }
        for (int i = 0; i < projects.length; i++) {
            nameToProject.put(projects[i].getName(), projects[i]);
        }
        uniqueWorkItemTypeNames = new HashSet<String>(Arrays.asList(workItemTypeNames));
    }

    private String searchTerms;
    private int searchType = SEARCH_TYPE_ALL_TERMS;
    private boolean includeTitle = true;
    private boolean includeDescription = true;
    private boolean includeHistory = true;
    private boolean includeCustomField1;
    private boolean includeCustomField2;
    private boolean includeCustomField3;
    private String customField1;
    private String customField2;
    private String customField3;

    private String projectName;
    private String workItemTypeName;
    private String state;
    private String assignedTo;

    private String areaPath;
    private String iterationPath;

    private Date createdDateOnAfter;
    private Date createdDateOnBefore;
    private Date changedDateOnAfter;
    private Date changedDateOnBefore;

    private final Set<WITSearchModelChangedListener> changeListeners = new HashSet<WITSearchModelChangedListener>();

    private static final String SEARCH_TERMS_KEY = "search-terms"; //$NON-NLS-1$
    private static final String SEARCH_TYPE_KEY = "search-type"; //$NON-NLS-1$
    private static final String INCLUDE_TITLE_KEY = "include-title"; //$NON-NLS-1$
    private static final String INCLUDE_DESCRIPTION_KEY = "include-description"; //$NON-NLS-1$
    private static final String INCLUDE_HISTORY_KEY = "include-history"; //$NON-NLS-1$
    private static final String INCLUDE_CUSTOM_FIELD1_KEY = "include-custom-field1"; //$NON-NLS-1$
    private static final String INCLUDE_CUSTOM_FIELD2_KEY = "include-custom-field2"; //$NON-NLS-1$
    private static final String INCLUDE_CUSTOM_FIELD3_KEY = "include-custom-field3"; //$NON-NLS-1$
    private static final String CUSTOM_FIELD1_KEY = "custom-field1"; //$NON-NLS-1$
    private static final String CUSTOM_FIELD2_KEY = "custom-field2"; //$NON-NLS-1$
    private static final String CUSTOM_FIELD3_KEY = "custom-field3"; //$NON-NLS-1$
    private static final String PROJECT_NAME_KEY = "project-name"; //$NON-NLS-1$
    private static final String WORK_ITEM_TYPE_NAME_KEY = "work-item-type-name"; //$NON-NLS-1$
    private static final String STATE_KEY = "state"; //$NON-NLS-1$
    private static final String ASSIGNED_TO_KEY = "assigned-to"; //$NON-NLS-1$
    private static final String AREA_PATH_KEY = "area-path"; //$NON-NLS-1$
    private static final String ITERATION_PATH_KEY = "iteration-path"; //$NON-NLS-1$
    private static final String CREATED_DATE_ON_AFTER_KEY = "created-date-on-after"; //$NON-NLS-1$
    private static final String CREATED_DATE_ON_BEFORE_KEY = "created-date-on-before"; //$NON-NLS-1$
    private static final String CHANGED_DATE_ON_AFTER_KEY = "changed-date-on-after"; //$NON-NLS-1$
    private static final String CHANGED_DATE_ON_BEFORE_KEY = "changed-date-on-before"; //$NON-NLS-1$

    public void saveToSettings(final IDialogSettings settings) {
        settings.put(SEARCH_TERMS_KEY, searchTerms);
        settings.put(SEARCH_TYPE_KEY, searchType);
        settings.put(INCLUDE_TITLE_KEY, includeTitle);
        settings.put(INCLUDE_DESCRIPTION_KEY, includeDescription);
        settings.put(INCLUDE_HISTORY_KEY, includeHistory);
        settings.put(INCLUDE_CUSTOM_FIELD1_KEY, includeCustomField1);
        settings.put(INCLUDE_CUSTOM_FIELD2_KEY, includeCustomField2);
        settings.put(INCLUDE_CUSTOM_FIELD3_KEY, includeCustomField3);
        settings.put(CUSTOM_FIELD1_KEY, customField1);
        settings.put(CUSTOM_FIELD2_KEY, customField2);
        settings.put(CUSTOM_FIELD3_KEY, customField3);
        settings.put(PROJECT_NAME_KEY, projectName);
        settings.put(WORK_ITEM_TYPE_NAME_KEY, workItemTypeName);
        settings.put(STATE_KEY, state);
        settings.put(ASSIGNED_TO_KEY, assignedTo);
        settings.put(AREA_PATH_KEY, areaPath);
        settings.put(ITERATION_PATH_KEY, iterationPath);
        settings.put(
            CREATED_DATE_ON_AFTER_KEY,
            (createdDateOnAfter != null ? String.valueOf(createdDateOnAfter.getTime()) : null));
        settings.put(
            CREATED_DATE_ON_BEFORE_KEY,
            (createdDateOnBefore != null ? String.valueOf(createdDateOnBefore.getTime()) : null));
        settings.put(
            CHANGED_DATE_ON_AFTER_KEY,
            (changedDateOnAfter != null ? String.valueOf(changedDateOnAfter.getTime()) : null));
        settings.put(
            CHANGED_DATE_ON_BEFORE_KEY,
            (changedDateOnBefore != null ? String.valueOf(changedDateOnBefore.getTime()) : null));
    }

    public void restoreFromSettings(final IDialogSettings settings) {
        searchTerms = settings.get(SEARCH_TERMS_KEY);
        try {
            searchType = settings.getInt(SEARCH_TYPE_KEY);
            if (searchType != SEARCH_TYPE_ALL_TERMS && searchType != SEARCH_TYPE_ANY_TERM) {
                searchType = SEARCH_TYPE_ALL_TERMS;
            }
        } catch (final NumberFormatException ex) {
        }
        if (settings.get(INCLUDE_TITLE_KEY) != null) {
            includeTitle = settings.getBoolean(INCLUDE_TITLE_KEY);
        }
        if (settings.get(INCLUDE_DESCRIPTION_KEY) != null) {
            includeDescription = settings.getBoolean(INCLUDE_DESCRIPTION_KEY);
        }
        if (settings.get(INCLUDE_HISTORY_KEY) != null) {
            includeHistory = settings.getBoolean(INCLUDE_HISTORY_KEY);
        }
        if (settings.get(INCLUDE_CUSTOM_FIELD1_KEY) != null) {
            includeCustomField1 = settings.getBoolean(INCLUDE_CUSTOM_FIELD1_KEY);
        }
        if (settings.get(INCLUDE_CUSTOM_FIELD2_KEY) != null) {
            includeCustomField2 = settings.getBoolean(INCLUDE_CUSTOM_FIELD2_KEY);
        }
        if (settings.get(INCLUDE_CUSTOM_FIELD3_KEY) != null) {
            includeCustomField3 = settings.getBoolean(INCLUDE_CUSTOM_FIELD3_KEY);
        }

        customField1 = settings.get(CUSTOM_FIELD1_KEY);
        if (customField1 != null && !nameToFieldDefinition.containsKey(customField1)) {
            customField1 = null;
        }

        customField2 = settings.get(CUSTOM_FIELD2_KEY);
        if (customField2 != null && !nameToFieldDefinition.containsKey(customField2)) {
            customField2 = null;
        }

        customField3 = settings.get(CUSTOM_FIELD3_KEY);
        if (customField3 != null && !nameToFieldDefinition.containsKey(customField3)) {
            customField3 = null;
        }

        projectName = settings.get(PROJECT_NAME_KEY);
        Project project = null;
        if (projectName != null) {
            project = nameToProject.get(projectName);
            if (project == null) {
                projectName = null;
            }
        }

        workItemTypeName = settings.get(WORK_ITEM_TYPE_NAME_KEY);
        if (workItemTypeName != null) {
            if (project != null) {
                if (project.getWorkItemTypes().get(workItemTypeName) == null) {
                    workItemTypeName = null;
                }
            } else {
                if (!uniqueWorkItemTypeNames.contains(workItemTypeName)) {
                    workItemTypeName = null;
                }
            }
        }

        state = settings.get(STATE_KEY);
        if (state != null && state.trim().length() == 0) {
            state = null;
        }

        assignedTo = settings.get(ASSIGNED_TO_KEY);
        if (assignedTo != null && assignedTo.trim().length() == 0) {
            assignedTo = null;
        }

        areaPath = settings.get(AREA_PATH_KEY);
        if (areaPath != null) {
            if (project != null) {
                if (project.resolvePath(areaPath, Node.TreeType.AREA) == null) {
                    areaPath = null;
                }
            } else {
                areaPath = null;
            }
        }

        iterationPath = settings.get(ITERATION_PATH_KEY);
        if (iterationPath != null) {
            if (project != null) {
                if (project.resolvePath(iterationPath, Node.TreeType.ITERATION) == null) {
                    iterationPath = null;
                }
            } else {
                iterationPath = null;
            }
        }

        if (settings.get(CREATED_DATE_ON_AFTER_KEY) != null) {
            try {
                createdDateOnAfter = new Date(Long.parseLong(settings.get(CREATED_DATE_ON_AFTER_KEY)));
            } catch (final NumberFormatException ex) {
            }
        }

        if (settings.get(CREATED_DATE_ON_BEFORE_KEY) != null) {
            try {
                createdDateOnBefore = new Date(Long.parseLong(settings.get(CREATED_DATE_ON_BEFORE_KEY)));
            } catch (final NumberFormatException ex) {
            }
        }

        if (settings.get(CHANGED_DATE_ON_AFTER_KEY) != null) {
            try {
                changedDateOnAfter = new Date(Long.parseLong(settings.get(CHANGED_DATE_ON_AFTER_KEY)));
            } catch (final NumberFormatException ex) {
            }
        }

        if (settings.get(CHANGED_DATE_ON_BEFORE_KEY) != null) {
            try {
                changedDateOnBefore = new Date(Long.parseLong(settings.get(CHANGED_DATE_ON_BEFORE_KEY)));
            } catch (final NumberFormatException ex) {
            }
        }
    }

    public static interface WITSearchModelChangedListener {
        public void modelChanged();
    }

    public void addModelChangeListener(final WITSearchModelChangedListener listener) {
        changeListeners.add(listener);
    }

    private void fireChangeListeners() {
        for (final WITSearchModelChangedListener listener : changeListeners) {
            listener.modelChanged();
        }
    }

    private FieldDefinition getFieldDefinitionByName(final String fieldName) {
        final FieldDefinition def = nameToFieldDefinition.get(fieldName);
        if (def == null) {
            final String messageFormat = Messages.getString("WITSearchModel.FieldNameDoesNotExistFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, fieldName);
            throw new RuntimeException(message);
        }
        return def;
    }

    private FieldDefinition getFieldDefinitionByReferenceName(final String referenceName) {
        final FieldDefinition def = referenceNameToFieldDefinition.get(referenceName);
        if (def == null) {
            final String messageFormat = Messages.getString("WITSearchModel.ReferenceNameNotFoundFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, referenceName);
            throw new RuntimeException(message);
        }
        return def;
    }

    public void setSearchTerms(final String searchTerms) {
        this.searchTerms = searchTerms;
        fireChangeListeners();
    }

    public String getSearchTerms() {
        return searchTerms == null ? "" : searchTerms; //$NON-NLS-1$
    }

    public void setSearchType(final int searchType) {
        if (searchType == SEARCH_TYPE_ALL_TERMS || searchType == SEARCH_TYPE_ANY_TERM) {
            this.searchType = searchType;
            fireChangeListeners();
        }
    }

    public int getSearchType() {
        return searchType;
    }

    public void setIncludeTitle(final boolean includeTitle) {
        this.includeTitle = includeTitle;
        fireChangeListeners();
    }

    public boolean isIncludeTitle() {
        return includeTitle;
    }

    public void setIncludeDescription(final boolean includeDescription) {
        this.includeDescription = includeDescription;
        fireChangeListeners();
    }

    public boolean isIncludeDescription() {
        return includeDescription;
    }

    public void setIncludeHistory(final boolean includeHistory) {
        this.includeHistory = includeHistory;
        fireChangeListeners();
    }

    public boolean isIncludeHistory() {
        return includeHistory;
    }

    public void setIncludeCustomField1(final boolean includeCustomField1) {
        this.includeCustomField1 = includeCustomField1;
        fireChangeListeners();
    }

    public boolean isIncludeCustomField1() {
        return includeCustomField1;
    }

    public void setIncludeCustomField2(final boolean includeCustomField2) {
        this.includeCustomField2 = includeCustomField2;
        fireChangeListeners();
    }

    public boolean isIncludeCustomField2() {
        return includeCustomField2;
    }

    public void setIncludeCustomField3(final boolean includeCustomField3) {
        this.includeCustomField3 = includeCustomField3;
        fireChangeListeners();
    }

    public boolean isIncludeCustomField3() {
        return includeCustomField3;
    }

    public void setCustomField1(final String customField1Input) {
        if (customField1Input != null
            && (customField1Input.trim().length() == 0 || SEARCH_FIELD_SELECT.equals(customField1Input.trim()))) {
            customField1 = null;
        } else {
            customField1 = customField1Input;
        }
        fireChangeListeners();
    }

    public String getCustomField1() {
        return customField1 == null ? SEARCH_FIELD_SELECT : customField1;
    }

    public void setCustomField2(final String customField2Input) {
        if (customField2Input != null
            && (customField2Input.trim().length() == 0 || SEARCH_FIELD_SELECT.equals(customField2Input.trim())))

        {
            customField2 = null;
        } else {
            customField2 = customField2Input;
        }
        fireChangeListeners();
    }

    public String getCustomField2() {
        return customField2 == null ? SEARCH_FIELD_SELECT : customField2;
    }

    public void setCustomField3(final String customField3Input) {
        if (customField3Input != null
            && (customField3Input.trim().length() == 0 || SEARCH_FIELD_SELECT.equals(customField3Input.trim())))

        {
            customField3 = null;
        } else {
            customField3 = customField3Input;
        }
        fireChangeListeners();
    }

    public String getCustomField3() {
        return customField3 == null ? SEARCH_FIELD_SELECT : customField3;
    }

    public void setAreaPath(final String areaPath) {
        this.areaPath = areaPath;
        fireChangeListeners();
    }

    public String getAreaPath() {
        return areaPath == null ? "" : areaPath; //$NON-NLS-1$
    }

    public void setIterationPath(final String iterationPath) {
        this.iterationPath = iterationPath;
        fireChangeListeners();
    }

    public String getIterationPath() {
        return iterationPath == null ? "" : iterationPath; //$NON-NLS-1$
    }

    public void setProjectName(final String projectName) {
        if (projectName == null) {
            this.projectName = null;
            fireChangeListeners();
        } else {
            if (nameToProject.containsKey(projectName)) {
                this.projectName = projectName;
                fireChangeListeners();
            }
        }
    }

    public String getProjectName() {
        return projectName == null ? "" : projectName; //$NON-NLS-1$
    }

    public void setWorkItemTypeName(final String workItemTypeNameInput) {
        if (workItemTypeNameInput != null && workItemTypeNameInput.trim().length() == 0) {
            workItemTypeName = null;
        } else {
            workItemTypeName = workItemTypeNameInput;
        }
        fireChangeListeners();
    }

    public String getWorkItemTypeName() {
        return workItemTypeName == null ? "" : workItemTypeName; //$NON-NLS-1$
    }

    public void setState(final String stateInput) {
        if (stateInput != null && stateInput.trim().length() == 0) {
            state = null;
        } else {
            state = stateInput;
        }
        fireChangeListeners();
    }

    public String getState() {
        return state == null ? "" : state; //$NON-NLS-1$
    }

    public void setAssignedTo(final String assignedToInput) {
        if (assignedToInput != null && assignedToInput.trim().length() == 0) {
            assignedTo = null;
        } else {
            assignedTo = assignedToInput;
        }
        fireChangeListeners();
    }

    public String getAssignedTo() {
        return assignedTo == null ? "" : assignedTo; //$NON-NLS-1$
    }

    public Date getDate(final String fieldName, final boolean after) {
        if (CoreFieldReferenceNames.CREATED_DATE.equals(fieldName)) {
            if (after) {
                return createdDateOnAfter;
            } else {
                return createdDateOnBefore;
            }
        } else if (CoreFieldReferenceNames.CHANGED_DATE.equals(fieldName)) {
            if (after) {
                return changedDateOnAfter;
            } else {
                return changedDateOnBefore;
            }
        }

        throw new IllegalArgumentException(fieldName);
    }

    public void setDate(final Date dateInput, final String fieldName, final boolean after) {
        if (CoreFieldReferenceNames.CREATED_DATE.equals(fieldName)) {
            if (after) {
                createdDateOnAfter = dateInput;
            } else {
                createdDateOnBefore = dateInput;
            }
        } else if (CoreFieldReferenceNames.CHANGED_DATE.equals(fieldName)) {
            if (after) {
                changedDateOnAfter = dateInput;
            } else {
                changedDateOnBefore = dateInput;
            }
        }

        fireChangeListeners();
    }

    public boolean isValid() {
        final String whereClause = buildWIQLWhereClause();
        return whereClause != null;
    }

    public String buildWIQL() {
        final String whereClause = buildWIQLWhereClause();

        if (whereClause == null) {
            return null;
        }

        return "select [System.Id], [System.Title], [System.WorkItemType], [System.State], [System.AssignedTo], [System.AreaPath], [System.IterationPath] from workitems " //$NON-NLS-1$
            + whereClause
            + " " //$NON-NLS-1$
            + "order by [System.AssignedTo], [System.WorkItemType], [System.State]"; //$NON-NLS-1$
    }

    public String buildWIQLWhereClause() {
        final List<String> clauses = new ArrayList<String>();

        final String searchTermClause = makeSearchTermClause();
        if (searchTermClause != null) {
            clauses.add(searchTermClause);
        }

        if (projectName != null) {
            clauses.add(makeClause(CoreFieldReferenceNames.TEAM_PROJECT, "=", projectName)); //$NON-NLS-1$
        }
        if (areaPath != null) {
            clauses.add(makeClause(CoreFieldReferenceNames.AREA_PATH, "UNDER", areaPath)); //$NON-NLS-1$
        }
        if (iterationPath != null) {
            clauses.add(makeClause(CoreFieldReferenceNames.ITERATION_PATH, "UNDER", iterationPath)); //$NON-NLS-1$
        }
        if (workItemTypeName != null) {
            clauses.add(makeClause(CoreFieldReferenceNames.WORK_ITEM_TYPE, "=", workItemTypeName)); //$NON-NLS-1$
        }
        if (state != null) {
            clauses.add(makeClause(CoreFieldReferenceNames.STATE, "=", state)); //$NON-NLS-1$
        }
        if (assignedTo != null) {
            clauses.add(makeClause(CoreFieldReferenceNames.ASSIGNED_TO, "=", assignedTo)); //$NON-NLS-1$
        }
        if (createdDateOnAfter != null) {
            clauses.add(makeClause(
                CoreFieldReferenceNames.CREATED_DATE,
                ">=", //$NON-NLS-1$
                wiqlDateFormatter.format(createdDateOnAfter)));
        }
        if (createdDateOnBefore != null) {
            clauses.add(makeClause(
                CoreFieldReferenceNames.CREATED_DATE,
                "<=", //$NON-NLS-1$
                wiqlDateFormatter.format(createdDateOnBefore)));
        }
        if (changedDateOnAfter != null) {
            clauses.add(makeChangedDateClause(">=", changedDateOnAfter)); //$NON-NLS-1$
        }
        if (changedDateOnBefore != null) {
            clauses.add(makeChangedDateClause("<=", changedDateOnBefore)); //$NON-NLS-1$
        }

        if (clauses.size() == 0) {
            return null;
        }

        return "WHERE " + makeJoinedClauses(clauses, "AND"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String makeClause(final String fieldName, final String operator, final String value) {
        return "[" + fieldName + "]" + " " + operator + " \"" + value + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    }

    private String makeChangedDateClause(final String operator, final Date date) {
        final String fieldName;
        if (referenceNameToFieldDefinition.containsKey(CoreFieldReferenceNames.AUTHORIZED_DATE)) {
            fieldName = CoreFieldReferenceNames.AUTHORIZED_DATE;
        } else {
            fieldName = CoreFieldReferenceNames.CHANGED_DATE;
        }

        return makeClause(fieldName, operator, wiqlDateFormatter.format(date));
    }

    private String makeSearchTermClause() {
        if (searchTerms == null) {
            return null;
        }

        final List<String> searchTermClauses = new ArrayList<String>();
        final String[] terms = tokenize();

        if (terms.length == 0) {
            return null;
        }

        final List<FieldDefinition> fields = new ArrayList<FieldDefinition>();

        if (includeTitle) {
            fields.add(getFieldDefinitionByReferenceName(CoreFieldReferenceNames.TITLE));
        }
        if (includeDescription) {
            fields.add(getFieldDefinitionByReferenceName(CoreFieldReferenceNames.DESCRIPTION));
        }
        if (includeHistory) {
            fields.add(getFieldDefinitionByReferenceName(CoreFieldReferenceNames.HISTORY));
        }
        if (includeCustomField1 && customField1 != null) {
            fields.add(getFieldDefinitionByName(customField1));
        }
        if (includeCustomField2 && customField2 != null) {
            fields.add(getFieldDefinitionByName(customField2));
        }
        if (includeCustomField3 && customField3 != null) {
            fields.add(getFieldDefinitionByName(customField3));
        }

        if (fields.size() == 0) {
            return null;
        }

        for (final FieldDefinition field : fields) {
            final List<String> clausesForField = new ArrayList<String>();

            for (int i = 0; i < terms.length; i++) {
                clausesForField.add(makeClauseForFieldAndTerm(field, terms[i]));
            }

            final String clauseForField =
                makeJoinedClauses(clausesForField, (searchType == SEARCH_TYPE_ALL_TERMS ? "AND" //$NON-NLS-1$
                    : "OR")); //$NON-NLS-1$
            searchTermClauses.add(clauseForField);
        }

        return makeJoinedClauses(searchTermClauses, "OR"); //$NON-NLS-1$
    }

    private String makeJoinedClauses(final Collection<String> individualClauses, final String joiner) {
        if (individualClauses.size() == 0) {
            return ""; //$NON-NLS-1$
        }

        final StringBuffer sb = new StringBuffer();

        if (individualClauses.size() > 1) {
            sb.append("("); //$NON-NLS-1$
        }

        for (final Iterator<String> it = individualClauses.iterator(); it.hasNext();) {
            final String clause = it.next();
            sb.append(clause);
            if (it.hasNext()) {
                sb.append(" " + joiner + " "); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }

        if (individualClauses.size() > 1) {
            sb.append(")"); //$NON-NLS-1$
        }

        return sb.toString();
    }

    private String makeClauseForFieldAndTerm(final FieldDefinition field, final String term) {
        if (field.getFieldType() == FieldType.STRING
            || field.getFieldType() == FieldType.PLAINTEXT
            || field.getFieldType() == FieldType.HISTORY
            || field.getFieldType() == FieldType.HTML) {
            return "[" + field.getReferenceName() + "] CONTAINS \"" + term + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } else if (field.getFieldType() == FieldType.DATETIME
            || field.getFieldType() == FieldType.INTEGER
            || field.getFieldType() == FieldType.DOUBLE) {
            return "[" + field.getReferenceName() + "] = \"" + term + "\""; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        } else {
            final String messageFormat = Messages.getString("WITSearchModel.BadFieldTypeFormat"); //$NON-NLS-1$
            final String message = MessageFormat.format(messageFormat, field.getFieldType().getDisplayName());
            throw new RuntimeException(message);
        }
    }

    private static final int STATE_OUTSIDE_TOKEN = 1;
    private static final int STATE_READING_TOKEN = 2;
    private static final int STATE_READING_QUOTED_TOKEN = 3;

    private String[] tokenize() {
        final List<String> tokens = new ArrayList<String>();
        StringBuffer currentToken = null;

        int currentState = STATE_OUTSIDE_TOKEN;

        for (int i = 0; i < searchTerms.length(); i++) {
            final char c = searchTerms.charAt(i);

            switch (currentState) {
                case STATE_OUTSIDE_TOKEN:
                    if (!Character.isWhitespace(c)) {
                        currentToken = new StringBuffer();

                        if (isQuoteCharacter(c)) {
                            currentState = STATE_READING_QUOTED_TOKEN;
                        } else {
                            currentToken.append(c);
                            currentState = STATE_READING_TOKEN;
                        }
                    }
                    break;

                case STATE_READING_TOKEN:
                    if (!Character.isWhitespace(c) && !isQuoteCharacter(c)) {
                        currentToken.append(c);
                    } else {
                        if (currentToken.length() > 0) {
                            tokens.add(currentToken.toString());
                        }
                        if (isQuoteCharacter(c)) {
                            currentToken = new StringBuffer();
                            currentState = STATE_READING_QUOTED_TOKEN;
                        } else {
                            currentState = STATE_OUTSIDE_TOKEN;
                        }
                    }
                    break;

                case STATE_READING_QUOTED_TOKEN:
                    if (isQuoteCharacter(c)) {
                        if (currentToken.length() > 0) {
                            tokens.add(currentToken.toString());
                        }
                        currentState = STATE_OUTSIDE_TOKEN;
                    } else {
                        currentToken.append(c);
                    }
                    break;

                default:
                    final String messageFormat = Messages.getString("WITSearchModel.UnhandledStateFormat"); //$NON-NLS-1$
                    final String message = MessageFormat.format(messageFormat, currentState);
                    throw new RuntimeException(message);
            }
        }

        if (currentState == STATE_READING_TOKEN || currentState == STATE_READING_QUOTED_TOKEN) {
            if (currentToken.length() > 0) {
                tokens.add(currentToken.toString());
            }
        }

        return tokens.toArray(new String[] {});
    }

    private boolean isQuoteCharacter(final char c) {
        return (c == '\"');
    }
}
