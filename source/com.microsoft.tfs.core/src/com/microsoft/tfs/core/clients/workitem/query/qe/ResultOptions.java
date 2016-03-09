// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.query.qe;

import java.text.MessageFormat;
import java.util.Iterator;

import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.fields.FieldType;
import com.microsoft.tfs.core.clients.workitem.fields.FieldUsages;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.query.QueryDocument;
import com.microsoft.tfs.core.memento.Memento;
import com.microsoft.tfs.util.Check;

public class ResultOptions {
    public static final int CHANGE_TYPE_NONE = 0;
    public static final int CHANGE_TYPE_WIDTHS = 1;
    public static final int CHANGE_TYPE_COLUMNS = 2;
    public static final int CHANGE_TYPE_SORT = 4;

    private static final String COLUMN_MEMENTO_NAME = "column"; //$NON-NLS-1$
    private static final String COLUMN_NAME_NAME = "name"; //$NON-NLS-1$
    private static final String WIDTH_NAME = "width"; //$NON-NLS-1$

    private static final String DESC = " desc"; //$NON-NLS-1$
    private static final String ORDER_BY = "ORDER BY"; //$NON-NLS-1$
    private static final String SELECT = "SELECT"; //$NON-NLS-1$

    public static boolean checkChangeFlag(final int flags, final int flagToCheck) {
        return ((flags & flagToCheck) == flagToCheck);
    }

    public static int determineChange(final ResultOptions orig, final ResultOptions current) {
        int changeType = CHANGE_TYPE_NONE;

        if (orig.getDisplayFields().getCount() != current.getDisplayFields().getCount()) {
            changeType |= CHANGE_TYPE_WIDTHS;
            changeType |= CHANGE_TYPE_COLUMNS;
        } else {
            for (int i = 0; i < orig.getDisplayFields().getCount(); i++) {
                if (!orig.getDisplayFields().get(i).getFieldName().equals(
                    current.getDisplayFields().get(i).getFieldName())) {
                    changeType |= CHANGE_TYPE_WIDTHS;
                    changeType |= CHANGE_TYPE_COLUMNS;
                    break;
                }
            }
        }

        if (!checkChangeFlag(changeType, CHANGE_TYPE_COLUMNS)) {
            for (int i = 0; i < orig.getDisplayFields().getCount(); i++) {
                if (orig.getDisplayFields().get(i).getWidth() != current.getDisplayFields().get(i).getWidth()) {
                    changeType |= CHANGE_TYPE_WIDTHS;
                    break;
                }
            }
        }

        if (orig.getSortFields().getCount() != current.getSortFields().getCount()) {
            return changeType | CHANGE_TYPE_SORT;
        }

        for (int i = 0; i < orig.getSortFields().getCount(); i++) {
            if (!orig.getSortFields().get(i).getFieldName().equals(current.getSortFields().get(i).getFieldName())
                || orig.getSortFields().get(i).isAscending() != current.getSortFields().get(i).isAscending()) {
                return changeType | CHANGE_TYPE_SORT;
            }
        }

        return changeType;
    }

    public static int getDefaultColumnWidth(final String fieldName, final FieldDefinitionCollection fieldDefinitions) {
        if (fieldDefinitions != null && fieldDefinitions.contains(fieldName)) {
            final FieldDefinition fieldDefinition = fieldDefinitions.get(fieldName);
            return getDefaultColumnWidth(fieldDefinition);
        }
        return 75;
    }

    public static int getDefaultColumnWidth(final FieldDefinition fieldDefinition) {
        if (fieldDefinition.getID() == WorkItemFieldIDs.TITLE) {
            return 450;
        }

        final FieldType type = fieldDefinition.getFieldType();

        if (type == FieldType.INTEGER) {
            return 50;
        } else if (type == FieldType.DATETIME) {
            return 120;
        } else if (type == FieldType.TREEPATH) {
            return 300;
        }

        return 75;
    }

    private DisplayFieldCollection displayFields;
    private SortFieldCollection sortFields;
    private final QueryDocument queryDocument;

    public ResultOptions(final QueryDocument queryDocument) {
        this.queryDocument = queryDocument;
        createCollections();
    }

    public ResultOptions(
        final DisplayFieldCollection displayFields,
        final SortFieldCollection sortFields,
        final QueryDocument queryDocument) {
        this.displayFields = displayFields;
        this.sortFields = sortFields;
        this.queryDocument = queryDocument;
        createCollections();
    }

    public ResultOptions(
        final ResultOptions existingOptions,
        final boolean makeDeepCopy,
        final QueryDocument queryDocument) {
        this.queryDocument = queryDocument;
        if (makeDeepCopy) {
            displayFields = new DisplayFieldCollection(existingOptions.getDisplayFields());
            sortFields = new SortFieldCollection(existingOptions.getSortFields());
        } else {
            displayFields = existingOptions.displayFields;
            sortFields = existingOptions.sortFields;
        }
        createCollections();
    }

    public boolean isLinkQuery() {
        return queryDocument.isLinkQuery() || queryDocument.isTreeQuery();
    }

    public void onQueryTypeChanged(final FieldDefinitionCollection fieldDefinitions) {
        FieldDefinition linkTypeDefinition = null;
        if (fieldDefinitions.contains(CoreFieldReferenceNames.LINK_TYPE)) {
            linkTypeDefinition = fieldDefinitions.get(CoreFieldReferenceNames.LINK_TYPE);
        }

        if (linkTypeDefinition == null) {
            return;
        }

        final String linkTypeName = linkTypeDefinition.getName();
        if (queryDocument.isLinkQuery() && !queryDocument.isTreeQuery()) {
            boolean hasLinkType = false;
            for (int i = 0; i < displayFields.getCount(); i++) {
                // Add LinkType to display fields if not already there.
                final DisplayField field = displayFields.get(i);
                if (field.getFieldName().equals(linkTypeName)) {
                    hasLinkType = true;
                    break;
                }
            }

            if (!hasLinkType) {
                displayFields.add(new DisplayField(linkTypeName, getDefaultColumnWidth(linkTypeDefinition)));
            }
        } else {
            // Remove LinkType field from display and sort.
            for (int i = 0; i < displayFields.getCount(); i++) {
                final DisplayField field = displayFields.get(i);
                if (field.getFieldName().equals(linkTypeName)) {
                    displayFields.removeAt(i);
                }
            }

            for (int i = 0; i < sortFields.getCount(); i++) {
                final DisplayField field = sortFields.get(i);
                if (field.getFieldName().equals(linkTypeName)) {
                    sortFields.removeAt(i);
                }
            }
        }
    }

    /**
     * Saves the viewable state of the {@link ResultOptions} (column widths) to
     * the given empty {@link Memento}.
     *
     * @param memento
     *        the {@link Memento} to save state to (must not be
     *        <code>null</code>)
     */
    public void saveToMemento(final Memento memento) {
        Check.notNull(memento, "memento"); //$NON-NLS-1$

        /*
         * Write all the columns.
         */
        for (final Iterator<DisplayField> it = displayFields.iterator(); it.hasNext();) {
            final DisplayField displayField = it.next();

            final Memento child = memento.createChild(COLUMN_MEMENTO_NAME);

            child.putString(COLUMN_NAME_NAME, displayField.getFieldName());
            child.putInteger(WIDTH_NAME, displayField.getWidth());
        }
    }

    /**
     * Restores the viewable state of the {@link ResultOptions} (column widths)
     * from the given {@link Memento}.
     *
     * @param memento
     *        the {@link Memento} to read state from (may be null)
     */
    public void loadFromMemento(final Memento memento) {
        if (memento == null) {
            return;
        }

        /*
         * Read all the columns.
         */
        final Memento[] columns = memento.getChildren(COLUMN_MEMENTO_NAME);

        for (int i = 0; i < columns.length; i++) {
            final String name = columns[i].getString(COLUMN_NAME_NAME);
            final int width = columns[i].getInteger(WIDTH_NAME).intValue();

            setColumnWidth(name, width);
        }
    }

    private void createCollections() {
        if (displayFields == null) {
            displayFields = new DisplayFieldCollection();
        }
        if (sortFields == null) {
            sortFields = new SortFieldCollection();
        }
    }

    public String getOrderByClause(final FieldDefinitionCollection fieldDefinitions) {
        final int count = sortFields.getCount();

        if (count == 0) {
            return ""; //$NON-NLS-1$
        }

        final StringBuffer buffer = new StringBuffer();
        buffer.append(ORDER_BY);

        for (int i = 0; i < count; i++) {
            String name = DisplayField.getInvariantFieldName(sortFields.get(i).getFieldName(), fieldDefinitions);
            final FieldDefinition fieldDefinition = fieldDefinitions.get(name);
            if (fieldDefinition.getUsage() == FieldUsages.WORK_ITEM_LINK && !isLinkQuery()) {
                continue;
            }

            name = fieldDefinition.getReferenceName();
            if (i == 0) {
                buffer.append(MessageFormat.format(" [{0}]", name)); //$NON-NLS-1$
            } else {
                buffer.append(MessageFormat.format(", [{0}]", name)); //$NON-NLS-1$
            }
            if (!sortFields.get(i).isAscending()) {
                buffer.append(DESC);
            }
        }

        return buffer.toString();
    }

    public String getSelectClause(final FieldDefinitionCollection fieldDefinitions) {
        final int count = displayFields.getCount();

        if (count == 0) {
            return ""; //$NON-NLS-1$
        }

        final StringBuffer buffer = new StringBuffer();
        buffer.append(SELECT);

        for (int i = 0; i < count; i++) {
            String name = DisplayField.getInvariantFieldName(displayFields.get(i).getFieldName(), fieldDefinitions);
            final FieldDefinition fieldDefinition = fieldDefinitions.get(name);
            if (fieldDefinition.getUsage() == FieldUsages.WORK_ITEM_LINK && !isLinkQuery()) {
                continue;
            }

            name = fieldDefinition.getReferenceName();
            if (i == 0) {
                buffer.append(MessageFormat.format(" [{0}]", name)); //$NON-NLS-1$
            } else {
                buffer.append(MessageFormat.format(", [{0}]", name)); //$NON-NLS-1$
            }
        }

        return buffer.toString();
    }

    private void setColumnWidth(final String name, final int width) {
        for (final Iterator<DisplayField> it = displayFields.iterator(); it.hasNext();) {
            final DisplayField displayField = it.next();
            if (displayField.getFieldName().equals(name)) {
                displayField.setWidth(width);
            }
        }
    }

    public DisplayFieldCollection getDisplayFields() {
        return displayFields;
    }

    public SortFieldCollection getSortFields() {
        return sortFields;
    }
}
