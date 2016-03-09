// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.fields;

import java.text.MessageFormat;
import java.util.Date;

import com.microsoft.tfs.core.clients.workitem.fields.AllowedValuesCollection;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinition;
import com.microsoft.tfs.core.clients.workitem.fields.FieldType;
import com.microsoft.tfs.core.clients.workitem.fields.FieldUsages;
import com.microsoft.tfs.core.clients.workitem.internal.WITContext;
import com.microsoft.tfs.core.clients.workitem.internal.WorkItemFieldIDs;
import com.microsoft.tfs.core.clients.workitem.internal.metadata.dao.FieldDefinitionMetadata;
import com.microsoft.tfs.core.clients.workitem.internal.type.WITypeConverter;
import com.microsoft.tfs.core.clients.workitem.internal.type.WITypeConverterFactory;
import com.microsoft.tfs.core.clients.workitem.wittype.WorkItemType;
import com.microsoft.tfs.util.GUID;

public class FieldDefinitionImpl implements FieldDefinition {
    /*
     * The metadata that defines this field definition
     */
    private final FieldDefinitionMetadata fieldDefinitionMetadata;

    /*
     * A reference to the appropriate system type converter
     */
    private final WITypeConverter typeConverter;

    /*
     * Cached global allowed values collection for this field definition
     */
    private AllowedValuesCollectionImpl allowedValues;

    /*
     * Reference to the WIT context
     */
    private final WITContext context;

    /*
     * Cached, lazily instantiated field usages collection
     */
    private DatastoreItemFieldUsagesCollection fieldUsagesMetadata;

    private FieldUsages usage;

    private final int id;
    private final String referenceName;
    private final String name;
    private final FieldType fieldType;
    private final Class systemType;

    public FieldDefinitionImpl(
        final WITContext context,
        final FieldDefinitionMetadata fieldDefinitionMetadata,
        final WorkItemType type,
        final FieldUsages usage) {
        this.fieldDefinitionMetadata = fieldDefinitionMetadata;
        typeConverter = WITypeConverterFactory.getTypeConverter(getPSType());
        this.context = context;
        // Cache common values
        id = fieldDefinitionMetadata.getID();
        referenceName = fieldDefinitionMetadata.getReferenceName();
        name = fieldDefinitionMetadata.getName();

        // Usage
        this.usage = usage;

        // Get The WIT Field type
        fieldType = getTypeInternal();

        // Cache system type
        systemType = witFieldTypeToSystemType(getTypeInternal());
    }

    @Override
    public int compareTo(final Object o) {
        final FieldDefinitionImpl other = (FieldDefinitionImpl) o;
        return fieldDefinitionMetadata.getName().compareToIgnoreCase(other.fieldDefinitionMetadata.getName());
    }

    @Override
    public String toString() {
        return MessageFormat.format(
            "{0}({1})", //$NON-NLS-1$
            fieldDefinitionMetadata.getReferenceName(),
            Integer.toString(fieldDefinitionMetadata.getID()));
    }

    /*
     * ************************************************************************
     * START of implementation of FieldDefinition interface
     * ***********************************************************************
     */

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getReferenceName() {
        return referenceName;
    }

    @Override
    public int getID() {
        return id;
    }

    @Override
    public FieldType getFieldType() {
        if (fieldType == null) {
            throw new RuntimeException(MessageFormat.format(
                "unknown field type [{0}] for field {1} (id {2})", //$NON-NLS-1$
                fieldDefinitionMetadata.getType(),
                fieldDefinitionMetadata.getReferenceName(),
                Integer.toString(fieldDefinitionMetadata.getID())));
        }
        return fieldType;
    }

    @Override
    public boolean isQueryable() {
        if (isIgnored()) {
            return false;
        }

        final int dataType = fieldDefinitionMetadata.getType() & FieldTypeConstants.MASK_FIELD_TYPE_ONLY;

        return dataType == FieldTypeConstants.TYPE_STRING
            || dataType == FieldTypeConstants.TYPE_INTEGER
            || dataType == FieldTypeConstants.TYPE_DATETIME
            || dataType == FieldTypeConstants.TYPE_LONGTEXT
            || dataType == FieldTypeConstants.TYPE_TREENODE
            || dataType == FieldTypeConstants.TYPE_BIT
            || dataType == FieldTypeConstants.TYPE_DOUBLE
            || dataType == FieldTypeConstants.TYPE_GUID;
    }

    @Override
    public boolean isSortable() {
        /*
         * same as the definition for isQueryable, but long text fields are not
         * sortable
         */

        return isQueryable()
            && ((fieldDefinitionMetadata.getType()
                & FieldTypeConstants.MASK_FIELD_TYPE_ONLY) != FieldTypeConstants.TYPE_LONGTEXT);
    }

    @Override
    public AllowedValuesCollection getAllowedValues() {
        synchronized (this) {
            if (allowedValues == null) {
                /*
                 * this is expensive we only calculate it once, then cache the
                 * result
                 */

                final AllowedValuesHelper helper = new AllowedValuesHelper(
                    fieldDefinitionMetadata.getID(),
                    fieldDefinitionMetadata.getType(),
                    context);
                final String[] values = helper.compute();
                allowedValues = new AllowedValuesCollectionImpl(values, getPSType());
            }

            return allowedValues;
        }
    }

    @Override
    public Class getSystemType() {
        return witFieldTypeToSystemType(getTypeInternal());
    }

    @Override
    public boolean isIndexed() {
        final DatastoreItemFieldUsage metadata = context.getWorkItemFieldUsages().getUsageByFieldID(id);
        return metadata != null && metadata.isOftenQueried();
    }

    @Override
    public boolean isLongText() {
        final FieldType type = getFieldType();
        return type == FieldType.PLAINTEXT || type == FieldType.HTML || type == FieldType.HISTORY;
    }

    @Override
    public boolean supportsTextQuery() {
        // We support text query if either the field (e.g. Text field type) or
        // usage (marked for indexing as a text field) says so.

        final FieldDefinitionMetadata fieldMetadata = getFieldDefinitionMetadata();
        if (fieldMetadata.supportsTextQuery()) {
            return true;
        }

        final DatastoreItemFieldUsage usageMetadata = context.getWorkItemFieldUsages().getUsageByFieldID(id);
        return usageMetadata != null && usageMetadata.supportsTextQuery();
    }

    /*
     * ************************************************************************
     * END of implementation of FieldDefinition interface
     * ***********************************************************************
     */

    /*
     * ************************************************************************
     * START of internal (FieldDefinitionImpl) methods
     * ***********************************************************************
     */

    /**
     * Determines whether the given object is a valid type for values of Fields
     * that use this FieldDefinition. The input must not be null.
     */
    public boolean isValidType(final Object obj) {
        return getSystemType().isAssignableFrom(obj.getClass());
    }

    public boolean canCopy() {
        switch (fieldDefinitionMetadata.getID()) {
            case WorkItemFieldIDs.HISTORY:
            case WorkItemFieldIDs.CHANGED_DATE:
            case WorkItemFieldIDs.CHANGED_BY:
            case WorkItemFieldIDs.CREATED_DATE:
            case WorkItemFieldIDs.CREATED_BY:
            case WorkItemFieldIDs.STATE:
            case WorkItemFieldIDs.REASON:
            case WorkItemFieldIDs.WORK_ITEM_TYPE:
            case WorkItemFieldIDs.AUTHORIZED_DATE:
            case WorkItemFieldIDs.WATERMARK:
                return false;
        }

        if (isIgnored() || isReadonly()) {
            return false;
        }

        final int storageType = fieldDefinitionMetadata.getType() & FieldTypeConstants.MASK_FIELD_TYPE_ONLY;

        switch (storageType) {
            case FieldTypeConstants.TYPE_STRING:
            case FieldTypeConstants.TYPE_INTEGER:
            case FieldTypeConstants.TYPE_DATETIME:
            case FieldTypeConstants.TYPE_LONGTEXT:
            case FieldTypeConstants.TYPE_BIT:
            case FieldTypeConstants.TYPE_DOUBLE:
            case FieldTypeConstants.TYPE_GUID:
                return true;
        }

        return false;
    }

    public boolean isUsedInPhysicalType() {
        /*
         * This method is called to determine whether a field definition should
         * be included in a physical type (contained inside a FieldCollection).
         * This is called after a field has already been determined through
         * field usages to be part of a particular physical type.
         *
         * Apparently, field definitions of certain types are simply not
         * included as part of physical types. See CObjectSchema.HrBuildArray
         * for where I believe this determination is made in Microsoft code.
         */

        final int storageType = fieldDefinitionMetadata.getType() & FieldTypeConstants.MASK_FIELD_TYPE_ONLY;

        switch (storageType) {
            case FieldTypeConstants.TYPE_STRING:
            case FieldTypeConstants.TYPE_INTEGER:
            case FieldTypeConstants.TYPE_DATETIME:
            case FieldTypeConstants.TYPE_LONGTEXT:
            case FieldTypeConstants.TYPE_TREENODE:
            case FieldTypeConstants.TYPE_BIT:
            case FieldTypeConstants.TYPE_DOUBLE:
            case FieldTypeConstants.TYPE_GUID:
                return true;

            default:
                return false;
        }
    }

    public int getPSType() {
        return fieldDefinitionMetadata.getType() & FieldTypeConstants.MASK_FIELD_TYPE_AND_SUBTYPE;
    }

    public int getInternalType() {
        return fieldDefinitionMetadata.getType();
    }

    public WITypeConverter getTypeConverter() {
        return typeConverter;
    }

    public boolean isIgnored() {
        return (fieldDefinitionMetadata.getType() & FieldTypeConstants.FLAG_IGNORE_TYPE) > 0;
    }

    public boolean isReadonly() {
        return (fieldDefinitionMetadata.getType() & FieldTypeConstants.FLAG_READONLY_TYPE) > 0;
    }

    @Override
    public boolean isComputed() {
        return ((fieldDefinitionMetadata.getType() & FieldTypeConstants.FLAG_READONLY_TYPE) > 0)
            || ((fieldDefinitionMetadata.getType()
                & FieldTypeConstants.MASK_FIELD_TYPE_ONLY) == FieldTypeConstants.TYPE_TREENODE);
    }

    public boolean isLargeText() {
        return (fieldDefinitionMetadata.getType()
            & FieldTypeConstants.MASK_FIELD_TYPE_ONLY) == FieldTypeConstants.TYPE_LONGTEXT;
    }

    @Override
    public synchronized FieldUsages getUsage() {
        return usage;
    }

    public DatastoreItemFieldUsagesCollection getFieldUsageMetadata() {
        if (fieldUsagesMetadata == null) {
            fieldUsagesMetadata = new DatastoreItemFieldUsagesCollection(fieldDefinitionMetadata.getID(), context);
        }
        return fieldUsagesMetadata;
    }

    /**
     * <p>
     * Similar to
     * "Microsoft.TeamFoundation.WorkItemTracking.Client.FieldDefinition#PsFieldTypeToFieldType"
     * .
     * </p>
     * <p>
     * The difference between getTypeInternal() and getType() is that
     * getTypeInternal() returns null if there is no corresponding WITFieldType,
     * while the public OM method getType() throws an exception.
     * </p>
     */
    public FieldType getTypeInternal() {
        final int psType = getPSType();

        switch (psType) {
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD:
            case PSFieldDefinitionTypeEnum.TREE_NODE:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREE_NODE_NAME:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREE_NODE_TYPE:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_PERSON:
                return FieldType.STRING;

            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_KEYWORD_TREEPATH:
                return FieldType.TREEPATH;

            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_INTEGER:
            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_INTEGER_TREEID:
                return FieldType.INTEGER;

            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_DOUBLE:
                return FieldType.DOUBLE;

            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_DATE_TIME:
                return FieldType.DATETIME;

            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_PLAINTEXT:
                return FieldType.PLAINTEXT;

            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_HISTORY:
                return FieldType.HISTORY;

            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_LARGE_TEXT_HTML:
                return FieldType.HTML;

            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_GUID:
                return FieldType.GUID;

            case PSFieldDefinitionTypeEnum.SINGLE_VALUED_BOOLEAN:
                return FieldType.BOOLEAN;

            default:
                break;
        }

        return FieldType.STRING;
        /*
         * throw new ValidationException(
         * "TF26031: The Team Foundation Server returned a field type that was not recognized + ("
         * + psType + ", " + referenceName + ").");
         */
    }

    /**
     * Similar to
     * "Microsoft.TeamFoundation.WorkItemTracking.Client.FieldDefinition#FieldTypeToSystemType"
     * .
     */
    public static Class witFieldTypeToSystemType(final FieldType fieldType) {
        if (fieldType == FieldType.STRING
            || fieldType == FieldType.PLAINTEXT
            || fieldType == FieldType.HTML
            || fieldType == FieldType.TREEPATH
            || fieldType == FieldType.HISTORY) {
            return String.class;
        } else if (fieldType == FieldType.INTEGER) {
            return Integer.class;
        } else if (fieldType == FieldType.DATETIME) {
            return Date.class;
        } else if (fieldType == FieldType.DOUBLE) {
            return Double.class;
        } else if (fieldType == FieldType.GUID) {
            return GUID.class;
        } else if (fieldType == FieldType.BOOLEAN) {
            return Boolean.class;
        } else {
            return Object.class;
        }
    }

    public FieldDefinitionMetadata getFieldDefinitionMetadata() {
        return fieldDefinitionMetadata;
    }

    public void setUsage(final FieldUsages usage) {
        this.usage = usage;
    }

    /*
     * ************************************************************************
     * END of internal (FieldDefinitionImpl) methods
     * ***********************************************************************
     */
}
