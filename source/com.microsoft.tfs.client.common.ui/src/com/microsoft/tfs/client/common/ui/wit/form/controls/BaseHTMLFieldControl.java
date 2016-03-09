// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.wit.form.controls;

import com.microsoft.tfs.core.clients.workitem.fields.Field;
import com.microsoft.tfs.core.clients.workitem.form.WIFormLabelPositionEnum;
import com.microsoft.tfs.util.htmlfilter.HTMLFilter;

/**
 * Base HTML field control. Handles data scrubbing and field access common to
 * all implementations.
 *
 * @threadsafety unknown
 */
public abstract class BaseHTMLFieldControl extends LabelableControl {
    public BaseHTMLFieldControl() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wantsVerticalFill() {
        final WIFormLabelPositionEnum labelPosition = getControlDescription().getLabelPosition();
        if ((WIFormLabelPositionEnum.LEFT == labelPosition) || (WIFormLabelPositionEnum.RIGHT == labelPosition)) {
            return false;
        }

        return isFormElementLastAmongSiblings();
    }

    /**
     * @return the {@link Field} this control is controlling (never
     *         <code>null</code>)
     */
    protected Field getField() {
        return getWorkItem().getFields().getField(getControlDescription().getFieldName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Field getFieldForToolTipSupport() {
        return getField();
    }

    /**
     * Gets the HTML text from the give {@link Field}. The text is processed via
     * {@link HTMLFilter} to strip unsafe tags, attributes, and link types
     * before it is returned.
     *
     * @return the HTML field text (processed by
     *         {@link HTMLFilter#strip(String)}), possibly empty
     */
    protected String getHTMLTextFromField() {
        final String fieldValue = (String) getField().getValue();

        if (fieldValue == null || fieldValue.length() == 0) {
            return ""; //$NON-NLS-1$
        }

        return HTMLFilter.strip(fieldValue);
    }

    @Override
    protected int getControlColumns() {
        return 1;
    }
}
