// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertysources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

/**
 * {@link PropertyHolder} is a helper class used by instances of
 * {@link ReadonlyPropertySource}. {@link PropertyHolder} holds a collection of
 * {@link IPropertyDescriptor}s and corresponding property values.
 */
public class PropertyHolder {
    /**
     * A list of {@link IPropertyDescriptor}s currently held by this
     * {@link PropertyHolder}.
     */
    private final List propertyDescriptors = new ArrayList();

    /**
     * A map from property ID ({@link IPropertyDescriptor#getId()}) to property
     * value.
     */
    private final Map propertyValues = new HashMap();

    /**
     * The next property ID that will be generated.
     */
    private int nextId = 0;

    /**
     * Adds a new property to this {@link PropertyHolder}. The generated
     * {@link IPropertyDescriptor} will not have a description.
     *
     * @param displayName
     *        the display name of the generated {@link IPropertyDescriptor}
     * @param value
     *        the property value
     */
    public void addProperty(final String displayName, final Object value) {
        addProperty(displayName, null, value);
    }

    /**
     * Adds a new property to this {@link PropertyHolder}.
     *
     * @param displayName
     *        the display name of the generated {@link IPropertyDescriptor}
     * @param description
     *        the description of the generated {@link IPropertyDescriptor}
     * @param value
     *        the property value
     */
    public void addProperty(final String displayName, final String description, final Object value) {
        final Object id = new Integer(nextId++);

        propertyValues.put(id, value);

        final PropertyDescriptor propertyDescriptor = new PropertyDescriptor(id, displayName);
        propertyDescriptor.setDescription(description);

        /*
         * Calling setAlwaysIncompatible(true) keeps this property from showing
         * up in the properties view when the selection contains more than one
         * element.
         */
        propertyDescriptor.setAlwaysIncompatible(true);

        propertyDescriptors.add(propertyDescriptor);
    }

    /**
     * @return the current {@link IPropertyDescriptor}s held by this
     *         {@link PropertyHolder}
     */
    public IPropertyDescriptor[] getPropertyDescriptors() {
        return (IPropertyDescriptor[]) propertyDescriptors.toArray(new IPropertyDescriptor[propertyDescriptors.size()]);
    }

    /**
     * Called to obtain a property value that is held by this
     * {@link PropertyHolder}. If the given ID is not an ID of one of the
     * {@link IPropertyDescriptor}s returned by
     * {@link #getPropertyDescriptors()}, <code>null</code> will be returned.
     *
     * @param id
     *        the property ID
     * @return the property value, possibly <code>null</code>
     */
    public Object getPropertyValue(final Object id) {
        return propertyValues.get(id);
    }
}
