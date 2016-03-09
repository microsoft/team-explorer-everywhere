// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.clients.workitem.fields;

import com.microsoft.tfs.core.clients.workitem.WorkItem;

/**
 * Represents a field in a {@link WorkItem}.
 *
 * @since TEE-SDK-10.1
 */
public interface Field {
    /**
     * @return the name of this field.
     */
    public String getName();

    /**
     * @return the reference name of this field.
     */
    public String getReferenceName();

    /**
     * @return the ID of this field.
     */
    public int getID();

    /**
     * @return the current value of this field.
     */
    public Object getValue();

    /**
     * @return the value of this field as it existed when the work item was
     *         saved most recently.
     */
    public Object getOriginalValue();

    /**
     * Sets he value of this field.
     *
     * @param value
     *        the new value
     */
    public void setValue(Object value);

    /**
     * Sets he value of this field.
     *
     * @param source
     *        the object which is the source of this modification (may be
     *        <code>null</code>)
     * @param value
     *        the new value
     */
    public void setValue(Object source, Object value);

    /**
     * @return <code>true</code> if this field has unsaved changes,
     *         <code>false</code> if it does not
     */
    public boolean isDirty();

    /**
     * @return a description of the current state of this field.
     */
    public FieldStatus getStatus();

    /**
     * This method allows custom controls to override the status that was set by
     * the WIT rules engine (e.g. to mark a field as invalid). This fires all
     * necessary field changed events. Note that this status can be later
     * changed by the RuleEngine, so it may be necessary for a custom control to
     * listen for field changes and reset the status after the value has
     * changed.
     *
     * @param status
     *        FieldStatus value to set (Invalid fields will be highlighted in
     *        the Work Item Editor)
     */
    public void overrideStatus(FieldStatus status);

    /**
     * @return <code>true</code> if this field can be edited in the current
     *         state of the work item, <code>false</code> otherwise
     */
    public boolean isEditable();

    /**
     * @return a collection of values that are valid for this field.
     */
    public AllowedValuesCollection getAllowedValues();

    /**
     * @return a collection of values that are not allowed for this field.
     */
    public ValuesCollection getProhibitedValues();

    /**
     * @return the {@link WorkItem} that contains this field.
     */
    public WorkItem getWorkItem();

    /**
     * Adds a listener for field change events.
     *
     * @param listener
     *        the listener to add (must not be <code>null</code>)
     */
    public void addFieldChangeListener(FieldChangeListener listener);

    /**
     * Remvoes a listener for field change events.
     *
     * @param listener
     *        the listener to remove (must not be <code>null</code>)
     */
    public void removeFieldChangeListener(FieldChangeListener listener);

    /**
     * @return the {@link FieldDefinition} that defines this field.
     */
    public FieldDefinition getFieldDefinition();

    /**
     * @return help text for this field.
     */
    public String getHelpText();
}
