// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.microsoft.tfs.core.Messages;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.fields.AllowedValuesCollection;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldChangeListener;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldStatus;
import com.microsoft.tfs.core.clients.workitem.fields.ValuesCollection;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemImpl;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.LookupFailedException;
import com.microsoft.tfs.core.clients.workitem.internal.node.NodeImpl;
import com.microsoft.tfs.core.clients.workitem.internal.rules.FieldPickListSupport;
import com.microsoft.tfs.core.clients.workitem.internal.rules.IFieldPickListSupport;
import com.microsoft.tfs.core.clients.workitem.internal.rules.IRuleTargetField;
import com.microsoft.tfs.core.clients.workitem.internal.rules.RuleEngine;
import com.microsoft.tfs.core.clients.workitem.internal.type.WITypeConverterException;
import com.microsoft.tfs.core.clients.workitem.internal.type.WIValueSource;
import com.microsoft.tfs.core.clients.workitem.link.ExternalLink;
import com.microsoft.tfs.core.clients.workitem.link.Hyperlink;
import com.microsoft.tfs.core.clients.workitem.link.RelatedLink;
import com.microsoft.tfs.core.clients.workitem.node.Node;
import com.microsoft.tfs.core.clients.workitem.project.Project;

public class FieldImpl implements Field, IRuleTargetField {
    private static final Log log = LogFactory.getLog(FieldImpl.class);

    /*
     * The field definition that defines this field.
     */
    private final FieldDefinitionImpl fieldDefinition;

    /*
     * The field collection that contains this field and all it's siblings
     */
    private final FieldCollectionImpl fieldCollection;

    /*
     * The work item this field belongs to.
     */
    private final WorkItemImpl workItem;

    /*
     * The WIT context.
     */
    private final WITContext witContext;

    /*
     * These store the state of this field's value.
     */
    private Object originalValue = null;
    private boolean originalValueSet = false;
    private Object newValue;
    private boolean newValueSet = false;
    private FieldModificationType fieldModificationType;
    private ServerComputedFieldType serverComputedValueType = null;

    /*
     * This field's status.
     */
    private FieldStatus fieldStatus = FieldStatus.VALID; // valid by default

    /*
     * This field's help text.
     */
    private String helpText;

    /*
     * The FieldChangeListenerSupport instance manages our change listeners for
     * us.
     */
    private final FieldChangeListenerSupport changeListeners = new FieldChangeListenerSupport(this);

    /*
     * The FieldPickListSupport manages the state of the pick list for this
     * field.
     */
    private final FieldPickListSupport pickListSupport;

    /*
     * Attribute used to track the readonly status of this field from the rule
     * engine's perspective.
     */
    private boolean ruleReadOnly = false; // not readonly by default

    public FieldImpl(
        final FieldDefinitionImpl fieldDefinition,
        final FieldCollectionImpl fieldCollection,
        final WorkItemImpl workItem,
        final WITContext witContext) {
        this.fieldDefinition = fieldDefinition;
        this.fieldCollection = fieldCollection;
        this.workItem = workItem;
        this.witContext = witContext;

        pickListSupport =
            new FieldPickListSupport(fieldDefinition.getPSType(), "field [" + fieldDefinition.getID() + "]"); //$NON-NLS-1$ //$NON-NLS-2$

        /*
         * System.History gets treated differently than all other fields
         */
        if (fieldDefinition.getID() == WorkItemFieldIDs.HISTORY) {
            newValue = null;
            newValueSet = true;
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format(
            "FieldImpl({0},{1})@{2}", //$NON-NLS-1$
            Integer.toString(fieldDefinition.getID()),
            fieldDefinition.getReferenceName(),
            Integer.toHexString(System.identityHashCode(this)));
    }

    /*
     * START of implementation of Field interface
     */

    @Override
    public void addFieldChangeListener(final FieldChangeListener listener) {
        changeListeners.addFieldChangeListener(listener);
    }

    @Override
    public void removeFieldChangeListener(final FieldChangeListener listener) {
        changeListeners.removeFieldChangeListener(listener);
    }

    @Override
    public int getID() {
        return fieldDefinition.getID();
    }

    @Override
    public String getName() {
        return fieldDefinition.getName();
    }

    @Override
    public String getReferenceName() {
        return fieldDefinition.getReferenceName();
    }

    @Override
    public AllowedValuesCollection getAllowedValues() {
        return pickListSupport.getAllowedValues();
    }

    @Override
    public ValuesCollection getProhibitedValues() {
        return pickListSupport.getProhibitedValues();
    }

    @Override
    public Object getOriginalValue() {
        /*
         * if the original value has been explicitly set, always use that this
         * can be the case even for computed fields when dealing with query
         * results
         *
         * NOTE just to avoid confusion. As of now the only computed fields that
         * come back from query results should be the count fields. Other
         * computed fields are transparently converted from their non-computed
         * counterparts, to be more in-line with Visual Studio's implementation.
         */
        if (originalValueSet) {
            return originalValue;
        }

        final int id = fieldDefinition.getID();

        if (id == WorkItemFieldIDs.AREA_PATH
            || id == WorkItemFieldIDs.ITERATION_PATH
            || id == WorkItemFieldIDs.NODE_NAME
            || id == WorkItemFieldIDs.TEAM_PROJECT) {
            final int idField =
                (id == WorkItemFieldIDs.ITERATION_PATH ? WorkItemFieldIDs.ITERATION_ID : WorkItemFieldIDs.AREA_ID);

            final Integer originalId = (Integer) fieldCollection.getFieldInternal(idField).getOriginalValue();

            if (originalId == null) {
                return null;
            }
            NodeImpl node = witContext.getRootNode().findNodeDownwards(originalId.intValue());

            if (node == null) {
                return null;
            }

            if (id == WorkItemFieldIDs.TEAM_PROJECT) {
                node = node.getProjectNodeParent();
            }

            if (id == WorkItemFieldIDs.NODE_NAME || id == WorkItemFieldIDs.TEAM_PROJECT) {
                return node.getName();
            } else {
                return node.getPath();
            }
        }

        /*
         * authorized as is computed from the person id field
         */
        if (id == WorkItemFieldIDs.AUTHORIZED_AS) {
            final Integer personId =
                (Integer) fieldCollection.getFieldInternal(WorkItemFieldIDs.PERSON_ID).getOriginalValue();
            if (personId == null) {
                return null;
            }
            try {
                return witContext.getMetadata().getConstantsTable().getConstantByID(personId.intValue());
            } catch (final LookupFailedException ex) {
                /*
                 * This happened to a customer (on 8-15-2006 running Teamprise
                 * 2.0 preview 1). Apparently the value of System.PersonId does
                 * not always reference a valid constant.
                 *
                 * Until more information is known, ignore.
                 */
                return null;
            }
        }

        /*
         * attached file count field uses the field attachment collection to
         * compute the count
         */
        if (id == WorkItemFieldIDs.ATTACHED_FILE_COUNT) {
            if (workItem.getFieldsInternal().getFieldInternal(WorkItemFieldIDs.ID).isDirty()) {
                return null;
            }
            return getAttachmentCount(true);
        }

        /*
         * link count fields always use the link collection to compute the count
         */
        if (isLinkCountField()) {
            /*
             * if the work item is unsaved, we return null as the original value
             * for a link count field (as per Visual Studio's object model)
             */
            if (workItem.getFieldsInternal().getFieldInternal(WorkItemFieldIDs.ID).isDirty()) {
                return null;
            }
            return getLinkCount(true);
        }

        return originalValue;
    }

    @Override
    public Object getValue() {
        final int id = fieldDefinition.getID();

        /*
         * the link count fields always use the link collection to compute a
         * value
         */
        if (isLinkCountField()) {
            return getLinkCount(false);
        }

        /*
         * attached file count always uses the file attachment collection to
         * compute a value
         */
        if (id == WorkItemFieldIDs.ATTACHED_FILE_COUNT) {
            return getAttachmentCount(false);
        }

        /*
         * the history field always returns newValue if the work item is open
         */
        if (id == WorkItemFieldIDs.HISTORY && workItem.isOpen()) {
            return newValue;
        }

        if (isDirty()) {
            /*
             * the following fields are special-cased computed fields
             */

            if (id == WorkItemFieldIDs.AREA_PATH
                || id == WorkItemFieldIDs.ITERATION_PATH
                || id == WorkItemFieldIDs.NODE_NAME
                || id == WorkItemFieldIDs.TEAM_PROJECT) {
                final int idField =
                    (id == WorkItemFieldIDs.ITERATION_PATH ? WorkItemFieldIDs.ITERATION_ID : WorkItemFieldIDs.AREA_ID);

                final Integer currentId = (Integer) fieldCollection.getFieldInternal(idField).getValue();

                if (currentId == null) {
                    return null;
                }
                NodeImpl node = witContext.getRootNode().findNodeDownwards(currentId.intValue());

                if (id == WorkItemFieldIDs.TEAM_PROJECT) {
                    node = node.getProjectNodeParent();
                }

                if (id == WorkItemFieldIDs.NODE_NAME || id == WorkItemFieldIDs.TEAM_PROJECT) {
                    return node.getName();
                } else {
                    return node.getPath();
                }
            }

            if (id == WorkItemFieldIDs.AUTHORIZED_AS) {
                final Integer personId =
                    (Integer) fieldCollection.getFieldInternal(WorkItemFieldIDs.PERSON_ID).getValue();
                if (personId == null) {
                    return null;
                }
                try {
                    return witContext.getMetadata().getConstantsTable().getConstantByID(personId.intValue());
                } catch (final LookupFailedException ex) {
                    /*
                     * This happened to a customer (on 8-15-2006 running
                     * Teamprise 2.0 preview 1). Apparently the value of
                     * System.PersonId does not always reference a valid
                     * constant.
                     *
                     * Until more information is known, ignore.
                     */
                    return null;
                }
            }

            /*
             * if we get to here, the field is dirty and is not a special-cased
             * computed field return the new value
             */
            return newValue;
        }

        /*
         * non-dirty fields return the original value
         */
        return getOriginalValue();
    }

    @Override
    public void setValue(final Object value) {
        setValue(null, value);
    }

    @Override
    public void setValue(final Object source, final Object data) {
        final int id = fieldDefinition.getID();

        if (id == WorkItemFieldIDs.AREA_PATH || id == WorkItemFieldIDs.ITERATION_PATH) {
            /*
             * convert the input into a String (the data type for this field)
             */
            String inputPath = null;
            try {
                inputPath = (String) fieldDefinition.getTypeConverter().translate(data, WIValueSource.LOCAL);
            } catch (final WITypeConverterException e) {
                /*
                 * TODO need to handle this better - currently not a big deal
                 * since the only current client of the OM is the GUI which will
                 * always pass Strings for area/iteration path. Need to check
                 * and see what Visual Studio's OM does - probably need to set
                 * field status to INVALID_TYPE.
                 */
                throw new RuntimeException(e);
            }

            final Node.TreeType treeType =
                (id == WorkItemFieldIDs.AREA_PATH ? Node.TreeType.AREA : Node.TreeType.ITERATION);

            final Object resolvedObject =
                workItem.getTypeInternal().getProjectInternal().resolvePath(inputPath, treeType);

            if (resolvedObject != null) {
                int resolvedId;
                if (resolvedObject instanceof Project) {
                    resolvedId = ((Project) resolvedObject).getID();
                } else {
                    resolvedId = ((Node) resolvedObject).getID();
                }
                final FieldImpl targetField = fieldCollection.getFieldInternal(
                    (id == WorkItemFieldIDs.AREA_PATH ? WorkItemFieldIDs.AREA_ID : WorkItemFieldIDs.ITERATION_ID));
                targetField.setValue(source, new Integer(resolvedId));

                if (fieldStatus == FieldStatus.INVALID_PATH) {
                    fieldStatus = FieldStatus.VALID;
                    fireFieldChangeListeners(source);
                }
            } else {
                if (fieldStatus != FieldStatus.INVALID_PATH) {
                    log.trace(MessageFormat.format(
                        "(non-rule) setting field [{0}] invalid: invalid path", //$NON-NLS-1$
                        Integer.toString(fieldDefinition.getID())));
                    setStatus(FieldStatus.INVALID_PATH);
                    fireFieldChangeListeners(source);
                }
            }
        } else {
            setValue(source, data, FieldModificationType.USER);
        }

        workItem.fireStateListenersIfNeeded();
    }

    @Override
    public FieldStatus getStatus() {
        /*
         * NOTE: important change as of 12/01/06: We used to report the status
         * as VALID if the field was invalid but the modification type was RULE.
         * This was incorrect and has been removed.
         */

        return fieldStatus;
    }

    @Override
    public WorkItem getWorkItem() {
        return workItem;
    }

    @Override
    public boolean isDirty() {
        final int id = fieldDefinition.getID();

        /*
         * System.History has a special definition of dirty
         */
        if (id == WorkItemFieldIDs.HISTORY) {
            return newValue != null;
        }

        if (!originalValueSet) {
            /*
             * Calculated fields area path, node name, team project, and
             * iteration path delegate dirty computation to the corresponding
             * underlying node id field.
             */
            if (id == WorkItemFieldIDs.AREA_PATH
                || id == WorkItemFieldIDs.NODE_NAME
                || id == WorkItemFieldIDs.TEAM_PROJECT) {
                return fieldCollection.getFieldInternal(WorkItemFieldIDs.AREA_ID).isDirty();
            }
            if (id == WorkItemFieldIDs.ITERATION_PATH) {
                return fieldCollection.getFieldInternal(WorkItemFieldIDs.ITERATION_ID).isDirty();
            }

            /*
             * authorized as delegates dirty compuation to underlying person id
             * field
             */
            if (id == WorkItemFieldIDs.AUTHORIZED_AS) {
                return fieldCollection.getFieldInternal(WorkItemFieldIDs.PERSON_ID).isDirty();
            }
        }

        /*
         * NOTE: important change as of 12/01/06: It used to be that if a
         * field's modification type was RULE and the field was dirty, we would
         * return false from this method, reporting the field to be non-dirty.
         * This was incorrect and broke the rule engine in some cases.
         */

        /*
         * a field is dirty if a new value has been set
         *
         * NOTE: important change as of 10/04/06: The "ruleReadOnly" field used
         * to be consulted for isDirty(). If ruleReadOnly was true, isDirty
         * always returned false. However, sometimes we want to have the rule
         * engine affect editability by setting ruleReadOnly to true but NOT
         * affect dirty state. The change was that isDirty no longer uses
         * ruleReadOnly. In the case where the rule engine wants to affect both
         * editability and dirty state, it will both set ruleReadOnly and call
         * unsetNewValue().
         */
        return newValueSet;
    }

    @Override
    public boolean isEditable() {
        final int id = fieldDefinition.getID();

        /*
         * Calculated fields area path and iteration path delegate editability
         * to the corresponding underlying id field.
         */
        if (id == WorkItemFieldIDs.AREA_PATH) {
            return fieldCollection.getFieldInternal(WorkItemFieldIDs.AREA_ID).isEditable();
        }
        if (id == WorkItemFieldIDs.ITERATION_PATH) {
            return fieldCollection.getFieldInternal(WorkItemFieldIDs.ITERATION_ID).isEditable();
        }

        /*
         * A few fields are hardcoded to not be editable.
         */
        if (id == WorkItemFieldIDs.WORK_ITEM_TYPE || id == WorkItemFieldIDs.CREATED_BY) {
            return false;
        }

        /*
         * Field definitions marked readonly or ignore are not editable
         * (excepting for the hardcoded special cases above).
         */
        if (fieldDefinition.isIgnored() || fieldDefinition.isReadonly()) {
            return false;
        }

        /*
         * a field is editable if it is not marked as readonly by a rule, and
         * has not been set to be server computed by a rule
         */
        return !ruleReadOnly && (serverComputedValueType == null);
    }

    @Override
    public FieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    @Override
    public String getHelpText() {
        return helpText;
    }

    /*
     * END of implementation of Field interface
     */

    /*
     * START of implementation of internal (FieldImpl) methods
     */

    public String getNewValueAsString() {
        if (fieldStatus == FieldStatus.INVALID_TYPE) {
            /*
             * Avoid calling into the type converter with a type that is almost
             * certainly not expected by that type converter.
             *
             * getNewValueAsString() is only called by the update code, which
             * under normal circumstances should never be called when any field
             * has a field status that is not FieldStatus.VALID.
             *
             * However, we can get here in one special case. That's when the UI
             * performs the hidden "Show Update XML" function and the field
             * happens to be invalid with FieldStatus.INVALID_TYPE. "Show Update
             * XML" calls the same update code as we would call when performing
             * a real update to build the XML that we would send to the server.
             * However "Show Update XML" doesn't care whether or not the work
             * item is valid.
             *
             * In this special case we will just return a recognizable string
             * that shows we're in this special case.
             */

            return MessageFormat.format(
                Messages.getString("FieldImpl.WarningFieldIsInvalidWithStatusInvalidTypeFormat"), //$NON-NLS-1$
                Integer.toString(getID()));
        }

        return fieldDefinition.getTypeConverter().toString(newValue);
    }

    public boolean isComputed() {
        return fieldDefinition.isComputed();
    }

    /**
     * Called to set the original value of this Field from character data sent
     * by the server. The proper WITypeConverter corresponding to this Field's
     * FieldDefinition is used to convert the input String into the proper type
     * for this Field. Important: this method should only be called with data
     * obtained from the server. If the data is coming from internal WIT code,
     * call setOriginalValueLocal() instead.
     *
     * @param value
     *        the character data to use as the original value of this Field
     */
    public void setOriginalValue(final String value) {
        try {
            originalValue = fieldDefinition.getTypeConverter().translate(value, WIValueSource.SERVER);
        } catch (final WITypeConverterException e) {
            /*
             * An exception here would indicate either totally unexpected data
             * from the server or a bug in one of the type converters.
             */
            log.warn(MessageFormat.format(
                "type conversion error setting original value \"{0}\" for field [{1}]", //$NON-NLS-1$
                value,
                fieldDefinition.getReferenceName()), e);
        }

        originalValueSet = true;
    }

    /**
     * Called to set the original value of this Field from internal WIT code. No
     * type conversion is done - the type of the input value is expected to be
     * the proper type for this Field. Important: this method should not be
     * called if the input value is character data obtained from the server. In
     * this case call setOriginalValue(). Only call this method when internal
     * work item code needs to set original Field values (which is rare).
     *
     * @param value
     *        the strongly typed value to use as the original value of this
     *        Field
     */
    public void setOriginalValueLocal(final Object value) {
        if (value != null) {
            if (!fieldDefinition.isValidType(value)) {
                throw new IllegalArgumentException(
                    MessageFormat.format(
                        "the input value is of type [{0}], which is invalid for field [{1}] (System type [{2}])", //$NON-NLS-1$
                        value.getClass().getName(),
                        Integer.toString(getID()),
                        fieldDefinition.getSystemType().getName()));
            }
        }

        originalValue = value;
        originalValueSet = true;
    }

    public FieldModificationType getModificationType() {
        return fieldModificationType;
    }

    public void setValue(final Object value, final FieldModificationType modificationType) {
        setValue(null, value, modificationType);
    }

    public void setValue(final Object source, Object value, final FieldModificationType modificationType) {
        boolean invalidType = false;
        FieldStatus invalidTypeStatus = null;

        /*
         * translate the value into this field's type
         */
        try {
            value = fieldDefinition.getTypeConverter().translate(
                value,
                (modificationType == FieldModificationType.SERVER ? WIValueSource.SERVER : WIValueSource.LOCAL));
        } catch (final WITypeConverterException ex) {
            invalidType = true;
            invalidTypeStatus = ex.getInvalidStatus();
            if (ex.containsInvalidValue()) {
                value = ex.getInvalidValue();
            }
        }

        boolean updated = false;

        if (newValueSet) {
            if (fieldDefinition.getID() != WorkItemFieldIDs.HISTORY && valuesEqual(originalValue, value)) {
                newValueSet = false;
                updated = true;
            } else if (!valuesEqual(newValue, value)) {
                newValue = value;
                updated = true;
            }
        } else {
            if (!valuesEqual(originalValue, value)) {
                newValueSet = true;
                newValue = value;
                updated = true;
            }
        }

        if (updated) {
            fieldModificationType = modificationType;
            fireFieldChangeListeners(source);
        }

        if (updated && (modificationType == FieldModificationType.USER) && !invalidType) {
            final boolean updatedByRules = fireRules();
            if (!updatedByRules
                && (fieldStatus == FieldStatus.INVALID_TYPE
                    || fieldStatus == FieldStatus.INVALID_DATE
                    || fieldStatus == FieldStatus.INVALID_CHARACTERS)) {
                log.trace(
                    MessageFormat.format(
                        "(non-rule) setting field [{0}] valid since it was previously invalid due to a type conversion", //$NON-NLS-1$
                        Integer.toString(fieldDefinition.getID())));
                setStatus(FieldStatus.VALID);
                fireFieldChangeListeners(source);
            }
        } else if (invalidType) {
            if (fieldStatus != invalidTypeStatus) {
                log.trace(MessageFormat.format(
                    "(non-rule) setting field [{0}] invalid because of type conversion: {1}", //$NON-NLS-1$
                    Integer.toString(fieldDefinition.getID()),
                    invalidTypeStatus.toString()));
                setStatus(invalidTypeStatus);
                fireFieldChangeListeners(source);
            }
        }

        return;
    }

    private boolean fireRules() {
        final RuleEngine ruleEngine = new RuleEngine(workItem, witContext);
        return ruleEngine.fieldChanged(fieldDefinition.getID());
    }

    private boolean valuesEqual(final Object val1, final Object val2) {
        return (val1 == null ? val2 == null : val1.equals(val2));
    }

    @Override
    public void setStatus(final FieldStatus status) {
        fieldStatus = status;
    }

    @Override
    public void overrideStatus(final FieldStatus status) {
        setStatus(status);
        fireFieldChangeListeners();
        workItem.fireStateListenersIfNeeded();
    }

    public FieldDefinitionImpl getFieldDefinitionInternal() {
        return fieldDefinition;
    }

    public void resetAfterUpdate() {
        if (fieldDefinition.getID() == WorkItemFieldIDs.HISTORY) {
            newValue = null;
        } else {
            if (newValueSet) {
                /*
                 * if the server computed type is set, we clear that
                 */
                serverComputedValueType = null;

                /*
                 * and always copy the new value into the original value
                 */
                originalValue = newValue;

                /*
                 * finally turn off the new value set flag, making this field no
                 * longer dirty
                 */
                newValueSet = false;
            }
        }
    }

    /**
     * Resets this Field. Any new value that has been set will be cleared. Any
     * original value that has been set is unaffected. If setServerComputed()
     * has been called (which is considered a new value) that state is also
     * cleared. In addition, the FieldStatus of this Field is reset to
     * FieldStatus.VALID.
     */
    public void reset() {
        if (fieldDefinition.getID() == WorkItemFieldIDs.HISTORY) {
            newValue = null;
        }

        if (newValueSet) {
            serverComputedValueType = null;
            newValueSet = false;
            fieldStatus = FieldStatus.VALID;
        }
    }

    private boolean isLinkCountField() {
        final int id = fieldDefinition.getID();

        return (id == WorkItemFieldIDs.HYPERLINK_COUNT
            || id == WorkItemFieldIDs.EXTERNAL_LINK_COUNT
            || id == WorkItemFieldIDs.RELATED_LINK_COUNT);
    }

    private Integer getAttachmentCount(final boolean oldCount) {
        final int count = workItem.getAttachmentsInternal().getCount(null, oldCount);
        return new Integer(count);
    }

    private Integer getLinkCount(final boolean oldCount) {
        final int id = fieldDefinition.getID();

        Class type;
        if (id == WorkItemFieldIDs.HYPERLINK_COUNT) {
            type = Hyperlink.class;
        } else if (id == WorkItemFieldIDs.EXTERNAL_LINK_COUNT) {
            type = ExternalLink.class;
        } else {
            type = RelatedLink.class;
        }
        final int count = workItem.getLinksInternal().getCount(type, oldCount);
        return new Integer(count);
    }

    public void fireFieldChangeListeners() {
        fireFieldChangeListeners(null);
    }

    public void fireFieldChangeListeners(final Object source) {
        changeListeners.fireListeners(source);
    }

    /*
     * END of implementation of internal (FieldImpl) methods
     */

    /*
     * START of implementation of IRuleTargetField methods note: some
     * IRuleTarget methods are satisfied by methods above
     */

    @Override
    public boolean isNewValueSet() {
        return newValueSet;
    }

    @Override
    public void setReadOnly(final boolean readOnly) {
        ruleReadOnly = readOnly;
    }

    @Override
    public void setServerComputed(final ServerComputedFieldType serverComputedType) {
        serverComputedValueType = serverComputedType;
        newValueSet = true;
    }

    @Override
    public void setValueFromRule(final Object value) {
        setValue(value, FieldModificationType.RULE);
    }

    @Override
    public IFieldPickListSupport getPickListSupport() {
        return pickListSupport;
    }

    @Override
    public ServerComputedFieldType getServerComputedType() {
        return serverComputedValueType;
    }

    @Override
    public void postProcessAfterRuleRun() {
        fireFieldChangeListeners();
    }

    @Override
    public void unsetNewValue() {
        newValueSet = false;
    }

    @Override
    public void setHelpText(final String helpText) {
        this.helpText = helpText;
    }

    /*
     * END of implementation of IRuleTargetField methods
     */
}
