// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.internal.form;

import org.xml.sax.Attributes;

import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.fields.FieldCollection;
import com.microsoft.tfs.core.clients.workitem.form.WIFormParam;
import com.microsoft.tfs.core.clients.workitem.form.WIFormParamTypeEnum;

public class WIFormParamImpl extends WIFormElementImpl implements WIFormParam {
    private String parameterIndex;
    private String parameterValue;
    private WIFormParamTypeEnum parameterType = WIFormParamTypeEnum.CURRENT;
    private String substitutionToken;

    /**
     * Process the attributes of the "Param" element.
     *
     * Attributes: - Index: required - Value: required - Type: optional
     */
    @Override
    void startLoading(final Attributes attributes) {
        parameterIndex = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_INDEX);
        parameterValue = WIFormParseHandler.readStringValue(attributes, WIFormParseConstants.ATTRIBUTE_NAME_VALUE);
        parameterType =
            WIFormParamTypeEnumFactory.fromType(attributes.getValue(WIFormParseConstants.ATTRIBUTE_NAME_TYPE));
        setAttributes(attributes);
    }

    /**
     * Corresponds to the "Index" attribute of the "Param" element.
     */
    public String getIndex() {
        return parameterIndex;
    }

    /**
     * Corresponds to the "Value" attribute of the "Param" element.
     */
    public String getValue() {
        return parameterValue;
    }

    /**
     * Corresponds to the "Type" attribute of the "Param" element.
     */
    public WIFormParamTypeEnum getType() {
        return parameterType;
    }

    /**
     * Get the substitution token for the specified parameter index.
     */
    @Override
    public String getSubstitutionToken() {
        if (substitutionToken == null) {
            substitutionToken = "\\{" + parameterIndex + "\\}"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return substitutionToken;
    }

    /**
     * Get the substitution value for the specified value. The value represents
     * the name of a WIT field. A new substitution value must be generated on
     * each call of the method since the value may depend on the current value
     * of a WIT field.
     */
    @Override
    public String getSubstitutionValue(final WorkItem workItem) {
        // Locate the WIT field specified by the "Value" attribute.
        final FieldCollection fields = workItem.getFields();
        final Field field = fields.getField(parameterValue);

        if (field != null) {
            if (getType() == WIFormParamTypeEnum.ORIGINAL) {
                // Use the original value of the WIT field.
                final Object originalValue = field.getOriginalValue();
                if (originalValue != null) {
                    return originalValue.toString();
                }
            } else {
                // Use the current value of the WIT field.
                final Object currentValue = field.getValue();
                if (currentValue != null) {
                    return currentValue.toString();
                }
            }
        }

        return ""; //$NON-NLS-1$
    }
}
