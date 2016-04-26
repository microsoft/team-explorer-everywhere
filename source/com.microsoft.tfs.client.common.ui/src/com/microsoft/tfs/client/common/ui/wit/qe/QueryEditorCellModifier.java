// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.qe;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Item;

import com.microsoft.tfs.client.common.ui.Messages;
import com.microsoft.tfs.client.common.ui.helpers.ComboHelper;
import com.microsoft.tfs.core.clients.workitem.CoreFieldReferenceNames;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.fields.AllowedValuesCollection;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldType;
import com.microsoft.tfs.core.clients.workitem.fields.FieldUsages;
import com.microsoft.tfs.core.clients.workitem.internal.wiqlparse.WIQLAdapter;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryRow;
import com.microsoft.tfs.core.clients.workitem.query.qe.QEQueryRowCollection;
import com.microsoft.tfs.core.clients.workitem.query.qe.WIQLOperators;

public class QueryEditorCellModifier implements ICellModifier {
    private static final int MAX_VISIBLE_ITEM_COUNT = 20;

    /*
     * Pseudo field names used to associate a name with the allowed values for a
     * clause of "IN GROUP <users>" or "IN GROUP <category>". These are used as
     * keys in the cache which stores the list of allowed values for fields.
     */
    private static final String GROUPVALUES_USERGROUPS = "GROUPVALUES_USERGRUOPS"; //$NON-NLS-1$
    private static final String GROUPVALUES_WITCATEGORIES = "GROUPVALUES_WITCATEGORIES"; //$NON-NLS-1$

    /*
     * Pseudo field names used to associate a name with the allowed value for a
     * clause of "<compare> [Field]". These are used as keys in the cache which
     * stores the list of allowed values for fields. The allowed values will be
     * the list of field names that are the same type as the field referenced on
     * the left side of the condition.
     */
    private static final String FIELDVALUES_FOR_STRING = "FIELDVALUES_FOR_STRING"; //$NON-NLS-1$
    private static final String FIELDVALUES_FOR_DATE = "FIELDVALUES_FOR_DATE"; //$NON-NLS-1$
    private static final String FIELDVALUES_FOR_INTEGER = "FIELDVALUES_FOR_INTEGER"; //$NON-NLS-1$
    private static final String FIELDVALUES_FOR_DOUBLE = "FIELDVALUES_FOR_DOUBLE"; //$NON-NLS-1$
    private static final String FIELDVALUES_FOR_BOOLEAN = "FIELDVALUES_FOR_BOOLEAN"; //$NON-NLS-1$
    private static final String FIELDVALUES_FOR_GUID = "FIELDVALUES_FOR_GUID"; //$NON-NLS-1$

    private final QEQueryRowCollection rowCollection;
    private final Project project;
    private final WorkItemClient client;
    private final TableViewer tableViewer;

    /*
     * Used when hooking the ComboBoxCellEditors so we can capture typed text.
     *
     * Maps from String (tableviewer column property name) to String (text typed
     * in the ComboBoxCellEditor for that column).
     */
    private final Map<String, String> propertyNameToComboText = new HashMap<String, String>();

    /*
     * Caches calculated combo items for the value column, which are different
     * for each row and depend on the selected field.
     *
     * Maps from String (field name) to String[] (combo items).
     */
    private final Map<String, String[]> valueItemsForField = new HashMap<String, String[]>();

    /*
     * Caches calculated combo items for the operator column, which are
     * different for each row and depend on the selected field.
     *
     * Maps from String (field name) to String[] (combo items).
     */
    private final Map<String, String[]> operatorItemsForField = new HashMap<String, String[]>();

    private CellEditor normalValueCellEditor;

    public QueryEditorCellModifier(
        final QEQueryRowCollection rowCollection,
        final Project project,
        final TableViewer tableViewer) {
        this.rowCollection = rowCollection;
        this.project = project;
        client = project.getWorkItemClient();
        this.tableViewer = tableViewer;
    }

    public void hookCellEditors() {
        /*
         * Hook all column combo boxes so we can later retrieve the typed text.
         */
        hookComboBoxCellEditor(QueryEditorControl.LOGICAL_OPERATOR_COLUMN);
        hookComboBoxCellEditor(QueryEditorControl.FIELD_NAME_COLUMN);
        hookComboBoxCellEditor(QueryEditorControl.OPERATOR_COLUMN);
        hookComboBoxCellEditor(QueryEditorControl.VALUE_COLUMN);

        /*
         * Set the items and visible item count for the logical operator column
         * - these items are the same for every row.
         */
        ComboBoxCellEditor comboBoxCellEditor =
            (ComboBoxCellEditor) getCellEditorForProperty(QueryEditorControl.LOGICAL_OPERATOR_COLUMN);
        comboBoxCellEditor.setItems(getItemsForLogicalOperatorCombo());
        setVisibleItemCount(QueryEditorControl.LOGICAL_OPERATOR_COLUMN);

        /*
         * Set the items and visible item count for the field column - these
         * items are the same for every row.
         */
        comboBoxCellEditor = (ComboBoxCellEditor) getCellEditorForProperty(QueryEditorControl.FIELD_NAME_COLUMN);
        comboBoxCellEditor.setItems(getItemsForFieldCombo());
        setVisibleItemCount(QueryEditorControl.FIELD_NAME_COLUMN);

        normalValueCellEditor = getCellEditorForProperty(QueryEditorControl.VALUE_COLUMN);
    }

    private void hookComboBoxCellEditor(final String property) {
        final ComboBoxCellEditor comboBoxCellEditor = (ComboBoxCellEditor) getCellEditorForProperty(property);

        ((CCombo) comboBoxCellEditor.getControl()).addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                final String text = ((CCombo) e.widget).getText();
                propertyNameToComboText.put(property, text);
            }
        });
    }

    @Override
    public boolean canModify(final Object element, final String property) {
        final QEQueryRow row = (QEQueryRow) element;

        if (QueryEditorControl.LOGICAL_OPERATOR_COLUMN.equals(property)) {
            return rowCollection.indexOf(row) != 0;
        } else if (QueryEditorControl.FIELD_NAME_COLUMN.equals(property)) {
            return true;
        } else if (QueryEditorControl.OPERATOR_COLUMN.equals(property)) {
            return row.getFieldName() != null;
        } else if (QueryEditorControl.VALUE_COLUMN.equals(property)) {
            final String fieldName = row.getFieldName();
            if (fieldName == null) {
                return false;
            }

            setCellEditorForProperty(QueryEditorControl.VALUE_COLUMN, normalValueCellEditor);
            return true;
        }

        return false;
    }

    @Override
    public Object getValue(final Object element, final String property) {
        final QEQueryRow row = (QEQueryRow) element;

        if (QueryEditorControl.LOGICAL_OPERATOR_COLUMN.equals(property)) {
            return getValueForComboBoxCellEditor(row.getLogicalOperator(), property);
        } else if (QueryEditorControl.FIELD_NAME_COLUMN.equals(property)) {
            return getValueForComboBoxCellEditor(row.getFieldName(), property);
        } else if (QueryEditorControl.OPERATOR_COLUMN.equals(property)) {
            setComboBoxCellEditorItems(property, getItemsForOperatorCombo(row.getFieldName()));
            setVisibleItemCount(QueryEditorControl.OPERATOR_COLUMN);
            return getValueForComboBoxCellEditor(row.getOperator(), property);
        } else if (QueryEditorControl.VALUE_COLUMN.equals(property)) {
            final FieldDefinition fieldDefinition = client.getFieldDefinitions().get(row.getFieldName());
            final String op = WIQLOperators.getInvariantOperator(row.getOperator());
            String fieldKey = fieldDefinition.getName();

            // Allowed values are cached by fieldName unless this is an
            // "IN GROUP" clause in which
            // case the allowed values are stored under a special case key.
            if (WIQLOperators.isGroupOperator(op)) {
                if (fieldDefinition.getReferenceName().equalsIgnoreCase(CoreFieldReferenceNames.WORK_ITEM_TYPE)) {
                    fieldKey = GROUPVALUES_WITCATEGORIES;
                } else {
                    fieldKey = GROUPVALUES_USERGROUPS;
                }
            } else if (WIQLOperators.isFieldNameOperator(op)) {
                final FieldType type = fieldDefinition.getFieldType();
                if (type == FieldType.DATETIME) {
                    fieldKey = FIELDVALUES_FOR_DATE;
                } else if (type == FieldType.DOUBLE) {
                    fieldKey = FIELDVALUES_FOR_DOUBLE;
                } else if (type == FieldType.INTEGER) {
                    fieldKey = FIELDVALUES_FOR_INTEGER;
                } else if (type == FieldType.STRING) {
                    fieldKey = FIELDVALUES_FOR_STRING;
                } else if (type == FieldType.BOOLEAN) {
                    fieldKey = FIELDVALUES_FOR_BOOLEAN;
                } else if (type == FieldType.GUID) {
                    fieldKey = FIELDVALUES_FOR_GUID;
                }
            }

            setComboBoxCellEditorItems(property, getItemsForValueCombo(fieldKey));
            setVisibleItemCount(QueryEditorControl.VALUE_COLUMN);

            final Integer comboBoxValue = getValueForComboBoxCellEditor(row.getValue(), property);

            final String value = row.getValue();
            if (comboBoxValue.intValue() == -1 && value != null && value.trim().length() > 0) {
                final ComboBoxCellEditor comboBoxCellEditor =
                    (ComboBoxCellEditor) getCellEditorForProperty(QueryEditorControl.VALUE_COLUMN);
                ((CCombo) comboBoxCellEditor.getControl()).addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(final FocusEvent e) {
                        final CCombo combo = (CCombo) e.widget;
                        combo.setText(value);
                        combo.setSelection(new Point(0, value.length()));
                        combo.removeFocusListener(this);
                    }
                });
            }

            return comboBoxValue;
        }

        return null;
    }

    @Override
    public void modify(Object element, final String property, final Object value) {
        final List<String> propertiesToUpdate = new ArrayList<String>();

        if (element instanceof Item) {
            element = ((Item) element).getData();
        }

        final QEQueryRow row = (QEQueryRow) element;

        if (QueryEditorControl.LOGICAL_OPERATOR_COLUMN.equals(property)) {
            final String s = getStringFromModifyComboBoxCellEditor(value, property, true);
            if (s != null) {
                final boolean changed = (!s.equals(row.getLogicalOperator()));
                if (changed) {
                    row.setLogicalOperator(s);
                    propertiesToUpdate.add(property);
                }
            }
        } else if (QueryEditorControl.FIELD_NAME_COLUMN.equals(property)) {
            final String oldFieldName = row.getFieldName();
            final String newFieldName = getStringFromModifyComboBoxCellEditor(value, property, true);
            final boolean changed = newFieldName != null ? !newFieldName.equals(oldFieldName) : oldFieldName != null;

            if (changed) {
                row.setFieldName(newFieldName);
                propertiesToUpdate.add(property);

                if (fieldTypeChanged(oldFieldName, newFieldName)) {
                    row.setValue(null);
                    propertiesToUpdate.add(QueryEditorControl.VALUE_COLUMN);

                    String newOperator;
                    if (newFieldName == null) {
                        newOperator = WIQLOperators.getLocalizedOperator(WIQLOperators.EQUAL_TO);
                    } else {
                        final FieldDefinition newFieldDefinition = client.getFieldDefinitions().get(newFieldName);
                        if (CoreFieldReferenceNames.TITLE.equals(newFieldDefinition.getReferenceName())) {
                            newOperator = WIQLOperators.getLocalizedOperator(WIQLOperators.CONTAINS);
                        } else {
                            final String[] availableOperators = getItemsForOperatorCombo(newFieldName);
                            newOperator = availableOperators[0];
                        }
                    }

                    final boolean operatorChanged = !newOperator.equals(row.getOperator());
                    if (operatorChanged) {
                        row.setOperator(newOperator);
                        propertiesToUpdate.add(QueryEditorControl.OPERATOR_COLUMN);
                    }
                }
            }
        } else if (QueryEditorControl.OPERATOR_COLUMN.equals(property)) {
            final String s = getStringFromModifyComboBoxCellEditor(value, property, true);
            if (s != null) {
                row.setOperator(s);
                propertiesToUpdate.add(property);
            }
        } else if (QueryEditorControl.VALUE_COLUMN.equals(property)) {
            final String s = getStringFromModifyComboBoxCellEditor(value, property, false);
            if (s != null) {
                row.setValue(s);
                propertiesToUpdate.add(property);
            }
        }

        if (propertiesToUpdate.size() > 0) {
            final String[] properties = propertiesToUpdate.toArray(new String[propertiesToUpdate.size()]);
            tableViewer.update(row, properties);
        }
    }

    /**
     * Called to translated from a value supplied by a ComboBoxCellEditor into a
     * value suitable for setting into the model. This method makes use of the
     * modify listener that we hook onto the CCombo to retrieve typed text.
     */
    private String getStringFromModifyComboBoxCellEditor(
        final Object value,
        final String property,
        final boolean mustBeItemInList) {
        final ComboBoxCellEditor comboBoxCellEditor = (ComboBoxCellEditor) getCellEditorForProperty(property);
        final String[] items = comboBoxCellEditor.getItems();

        final int index = ((Integer) value).intValue();

        /*
         * When you tab-away from a field, modify gets called with the index of
         * the currently selected item. If the user has entered text into the
         * cell then we should use the user-entered value instead of the value
         * at the specified index. Otherwise, pressing 'tab' would clear what
         * the user just typed
         */

        if (index == -1 || propertyNameToComboText.containsKey(property)) {
            if (propertyNameToComboText.containsKey(property)) {
                final String text = propertyNameToComboText.get(property);

                if (!mustBeItemInList) {
                    return text;
                }

                for (int i = 0; i < items.length; i++) {
                    if (items[i].equalsIgnoreCase(text)) {
                        return items[i];
                    }
                }
            }
        } else {
            return items[index];
        }

        return null;
    }

    /**
     * Sets the given array of items on the ComboBoxCellEditor indicated by the
     * given column property.
     */
    private void setComboBoxCellEditorItems(final String property, final String[] items) {
        final ComboBoxCellEditor comboBoxCellEditor = (ComboBoxCellEditor) getCellEditorForProperty(property);
        comboBoxCellEditor.setItems(items);
    }

    /**
     * Computes an Integer value to give to a ComboBoxCellEditor. The value
     * corresponds to an index into the cell editor's array of items. The item
     * at that index is equal to the current value held in the model.
     */
    private Integer getValueForComboBoxCellEditor(final String objectModelValue, final String property) {
        propertyNameToComboText.remove(property);

        if (objectModelValue == null) {
            return new Integer(-1);
        }

        final ComboBoxCellEditor comboBoxCellEditor = (ComboBoxCellEditor) getCellEditorForProperty(property);
        final String[] items = comboBoxCellEditor.getItems();

        for (int i = 0; i < items.length; i++) {
            if (objectModelValue.equalsIgnoreCase(items[i])) {
                return new Integer(i);
            }
        }

        return new Integer(-1);
    }

    /**
     * Given a column property name, retrieves the cell editor for that column.
     */
    private CellEditor getCellEditorForProperty(final String property) {
        final Object[] columnProperties = tableViewer.getColumnProperties();

        for (int i = 0; i < columnProperties.length; i++) {
            if (property.equals(columnProperties[i])) {
                return tableViewer.getCellEditors()[i];
            }
        }

        final String messageFormat = Messages.getString("QueryEditorCellModifier.NoSuchPropertyFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, property);
        throw new IllegalStateException(message);
    }

    private void setCellEditorForProperty(final String property, final CellEditor cellEditor) {
        final Object[] columnProperties = tableViewer.getColumnProperties();

        for (int i = 0; i < columnProperties.length; i++) {
            if (property.equals(columnProperties[i])) {
                tableViewer.getCellEditors()[i] = cellEditor;
                return;
            }
        }

        final String messageFormat = Messages.getString("QueryEditorCellModifier.NoSuchPropertyFormat"); //$NON-NLS-1$
        final String message = MessageFormat.format(messageFormat, property);
        throw new IllegalStateException(message);
    }

    /**
     * Sets the visible item count for the combo box indicated by the column
     * property name.
     */
    private void setVisibleItemCount(final String property) {
        final ComboBoxCellEditor comboBoxCellEditor = (ComboBoxCellEditor) getCellEditorForProperty(property);
        final CCombo combo = (CCombo) comboBoxCellEditor.getControl();
        ComboHelper.setVisibleItemCount(combo, combo.getItemCount(), MAX_VISIBLE_ITEM_COUNT);
    }

    /**
     * Builds an array of items for the value combo box for a given row.
     */
    private String[] getItemsForValueCombo(final String fieldName) {
        if (valueItemsForField.containsKey(fieldName)) {
            return valueItemsForField.get(fieldName);
        }

        final List<String> list = new ArrayList<String>();
        if (GROUPVALUES_USERGROUPS.equals(fieldName)) {
            final String[] groupNames = client.getGroupDataProvider(project.getName()).getGroups();
            for (int i = 0; i < groupNames.length; i++) {
                list.add(groupNames[i]);
            }
        } else if (GROUPVALUES_WITCATEGORIES.equals(fieldName)) {
            final String[] categoryNames = client.getGroupDataProvider(project.getName()).getWorkItemCategories();
            for (int i = 0; i < categoryNames.length; i++) {
                list.add(categoryNames[i]);
            }
        } else if (FIELDVALUES_FOR_STRING.equals(fieldName)) {
            list.addAll(getFieldNamesOfType(FieldType.STRING));
        } else if (FIELDVALUES_FOR_INTEGER.equals(fieldName)) {
            list.addAll(getFieldNamesOfType(FieldType.INTEGER));
        } else if (FIELDVALUES_FOR_DOUBLE.equals(fieldName)) {
            list.addAll(getFieldNamesOfType(FieldType.DOUBLE));
        } else if (FIELDVALUES_FOR_DATE.equals(fieldName)) {
            list.addAll(getFieldNamesOfType(FieldType.DATETIME));
        } else if (FIELDVALUES_FOR_BOOLEAN.equals(fieldName)) {
            list.addAll(getFieldNamesOfType(FieldType.BOOLEAN));
        } else if (FIELDVALUES_FOR_GUID.equals(fieldName)) {
            list.addAll(getFieldNamesOfType(FieldType.GUID));
        } else {
            final FieldDefinition fieldDefinition = client.getFieldDefinitions().get(fieldName);

            if (WIQLAdapter.fieldSupportsAnySyntax(fieldDefinition.getID())) {
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.SPECIAL_ANY));
            }

            final AllowedValuesCollection allowedValues = fieldDefinition.getAllowedValues();
            list.addAll(Arrays.asList(allowedValues.getValues()));

            if (CoreFieldReferenceNames.TEAM_PROJECT.equals(fieldDefinition.getReferenceName())) {
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.MACRO_PROJECT));
            } else if (CoreFieldReferenceNames.AREA_PATH.equals(fieldDefinition.getReferenceName())) {
                list.add(project.getName());
                computeClassificationValues(list, project.getAreaRootNodes().getNodes());
            } else if (CoreFieldReferenceNames.ITERATION_PATH.equals(fieldDefinition.getReferenceName())) {
                if (client.supportsWIQLEvaluationOnServer()) {
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.MACRO_CURRENT_ITERATION));
                }

                list.add(project.getName());
                computeClassificationValues(list, project.getIterationRootNodes().getNodes());
            } else {
                final FieldType type = fieldDefinition.getFieldType();

                if (FieldType.DATETIME == type) {
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.MACRO_TODAY));
                    list.add(WIQLOperators.getLocalizedTodayMinusMacro(1));
                    list.add(WIQLOperators.getLocalizedTodayMinusMacro(7));
                    list.add(WIQLOperators.getLocalizedTodayMinusMacro(30));
                } else if (FieldType.STRING == type) {
                    final String refName = fieldDefinition.getReferenceName();
                    if (CoreFieldReferenceNames.ASSIGNED_TO.equals(refName)
                        || CoreFieldReferenceNames.CREATED_BY.equals(refName)
                        || CoreFieldReferenceNames.CHANGED_BY.equals(refName)
                        || CoreFieldReferenceNames.AUTHORIZED_AS.equals(refName)) {
                        list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.MACRO_ME));
                    }
                } else if (FieldType.BOOLEAN == type) {
                    if (list.size() == 0) {
                        list.add(Messages.getString("QueryEditorCellModifier.FalseValue")); //$NON-NLS-1$
                        list.add(Messages.getString("QueryEditorCellModifier.TrueValue")); //$NON-NLS-1$
                    }
                }
            }
        }

        final String[] items = list.toArray(new String[list.size()]);

        valueItemsForField.put(fieldName, items);
        return items;
    }

    private void computeClassificationValues(final List<String> paths, final Node[] nodes) {
        if (nodes == null || nodes.length == 0) {
            return;
        }

        for (int i = 0; i < nodes.length; i++) {
            paths.add(nodes[i].getPath());
            final Node[] children = nodes[i].getChildNodes().getNodes();

            if (children != null && children.length > 0) {
                computeClassificationValues(paths, children);
            }
        }
    }

    /**
     * Builds an array of items for the operator combo box for a given row.
     */
    private String[] getItemsForOperatorCombo(final String fieldName) {
        if (operatorItemsForField.containsKey(fieldName)) {
            return operatorItemsForField.get(fieldName);
        }

        final List<String> list = new ArrayList<String>();
        final FieldDefinition fieldDefinition = client.getFieldDefinitions().get(fieldName);

        if (CoreFieldReferenceNames.TEAM_PROJECT.equals(fieldDefinition.getReferenceName())) {
            list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.EQUAL_TO));
            list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_EQUAL_TO));
            list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.IN));
        } else {
            final FieldType type = fieldDefinition.getFieldType();
            final boolean computed = fieldDefinition.isComputed();

            if (FieldType.STRING == type) {
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.EQUAL_TO));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_EQUAL_TO));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.GREATER_THAN));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.LESS_THAN));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.GREATER_THAN_OR_EQUAL_TO));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.LESS_THAN_OR_EQUAL_TO));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.CONTAINS));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_CONTAINS));
                if (fieldDefinition.supportsTextQuery()) {
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.CONTAINS_WORDS));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_CONTAINS_WORDS));
                }
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.IN));
                if (client.supportsWIQLFieldAndGroupOperators()) {
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.IN_GROUP));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_IN_GROUP));
                }
                if (!computed) {
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.EVER));
                }
                if (client.supportsWIQLFieldAndGroupOperators()) {
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.EQUAL_TO_FIELD));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_EQUAL_TO_FIELD));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.GREATER_THAN_FIELD));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.LESS_THAN_FIELD));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.GREATER_THAN_OR_EQUAL_TO_FIELD));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.LESS_THAN_OR_EQUAL_TO_FIELD));
                }
            } else if (FieldType.INTEGER == type || FieldType.DOUBLE == type || FieldType.DATETIME == type) {
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.EQUAL_TO));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_EQUAL_TO));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.GREATER_THAN));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.LESS_THAN));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.GREATER_THAN_OR_EQUAL_TO));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.LESS_THAN_OR_EQUAL_TO));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.IN));
                if (!computed) {
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.EVER));
                }
                if (client.supportsWIQLFieldAndGroupOperators()) {
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.EQUAL_TO_FIELD));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_EQUAL_TO_FIELD));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.GREATER_THAN_FIELD));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.LESS_THAN_FIELD));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.GREATER_THAN_OR_EQUAL_TO_FIELD));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.LESS_THAN_OR_EQUAL_TO_FIELD));
                }
            } else if (FieldType.PLAINTEXT == type || FieldType.HTML == type || FieldType.HISTORY == type) {
                if (fieldDefinition.supportsTextQuery()) {
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.CONTAINS_WORDS));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_CONTAINS_WORDS));
                } else {
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.CONTAINS));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_CONTAINS));
                }
            } else if (FieldType.TREEPATH == type) {
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.UNDER));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_UNDER));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.EQUAL_TO));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_EQUAL_TO));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.IN));
            } else if (FieldType.BOOLEAN == type || FieldType.GUID == type) {
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.EQUAL_TO));
                list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_EQUAL_TO));
                if (!computed) {
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.EVER));
                }
                if (client.supportsWIQLFieldAndGroupOperators()) {
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.EQUAL_TO_FIELD));
                    list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.NOT_EQUAL_TO_FIELD));
                }
            }
        }

        final String[] items = list.toArray(new String[list.size()]);

        operatorItemsForField.put(fieldName, items);
        return items;
    }

    private String[] getItemsForLogicalOperatorCombo() {
        final List<String> list = new ArrayList<String>();
        list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.AND));
        list.add(WIQLOperators.getLocalizedOperator(WIQLOperators.OR));
        return list.toArray(new String[list.size()]);
    }

    private String[] getItemsForFieldCombo() {
        final List<String> list = new ArrayList<String>();

        for (final FieldDefinition fieldDefinition : client.getFieldDefinitions()) {
            if (fieldDefinition.isQueryable()) {
                list.add(fieldDefinition.getName());
            }
        }

        return list.toArray(new String[list.size()]);
    }

    private List<String> getFieldNamesOfType(final FieldType fieldType) {
        final List<String> list = new ArrayList<String>();

        for (final FieldDefinition fieldDefinition : client.getFieldDefinitions()) {
            if (fieldDefinition.isQueryable()
                && fieldDefinition.getUsage() == FieldUsages.WORK_ITEM
                && fieldDefinition.getFieldType().equals(fieldType)) {
                list.add(fieldDefinition.getName());
            }
        }

        return list;
    }

    private boolean fieldTypeChanged(final String oldFieldName, final String newFieldName) {
        FieldType oldFieldType;
        try {
            oldFieldType = client.getFieldDefinitions().get(oldFieldName).getFieldType();
        } catch (final Exception e) {
            oldFieldType = null;
        }

        FieldType newFieldType;
        try {
            newFieldType = client.getFieldDefinitions().get(newFieldName).getFieldType();
        } catch (final Exception e) {
            newFieldType = null;
        }

        return newFieldType == null ? oldFieldType != null : !newFieldType.equals(oldFieldType);
    }
}
