// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.eclipse.ui.propertysources;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

/**
 * <p>
 * {@link ReadonlyPropertySource} is an abstract base class that can be used to
 * help implement the {@link IPropertySource} interface for integration with the
 * properties view in the Eclipse IDE. {@link IPropertySource} implementations
 * based on {@link ReadonlyPropertySource} have no support for writable
 * properties.
 * </p>
 *
 * <p>
 * Writing an {@link IPropertySource} from scratch is tedious since there are
 * many pieces of boilerplate that the implementor must have. For instance:
 * <ul>
 * <li>Creating and caching {@link IPropertyDescriptor} objects</li>
 * <li>Coming up with unique IDs for each property</li>
 * <li>Dealing with callbacks in two separate methods (
 * {@link #getPropertyDescriptors()} and {@link #getPropertyValue(Object)})</li>
 * </ul>
 * By using {@link ReadonlyPropertySource}, none of this boilerplate is needed.
 * </p>
 *
 * <p>
 * Subclasses must provide an implementation of the abstract
 * {@link #populate(PropertyHolder)} method. This method is passed an instance
 * of {@link PropertyHolder}, which subclasses add properties to.
 * </p>
 *
 * @see IPropertySource
 * @see PropertyHolder
 */
public abstract class ReadonlyPropertySource implements IPropertySource {
    /**
     * The {@link PropertyHolder} used by this {@link ReadonlyPropertySource} to
     * hold properties and property values.
     */
    private PropertyHolder propertyHolder;

    /**
     * Subclasses must implement this base class method to add properties to the
     * specified {@link PropertyHolder} object. This method will be called only
     * once, and is not called until the {@link IPropertyDescriptor}s are
     * requested from this {@link IPropertySource}. Subclasses should call the
     * {@link PropertyHolder#addProperty(String, String, Object)} method on the
     * {@link PropertyHolder} for each property.
     *
     * @param propertyHolder
     *        a {@link PropertyHolder} to add properties to (never
     *        <code>null</code>)
     */
    protected abstract void populate(PropertyHolder propertyHolder);

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.views.properties.IPropertySource#getEditableValue()
     */
    @Override
    public Object getEditableValue() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.views.properties.IPropertySource#getPropertyDescriptors()
     */
    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        if (propertyHolder == null) {
            propertyHolder = new PropertyHolder();
            populate(propertyHolder);
        }

        return propertyHolder.getPropertyDescriptors();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.views.properties.IPropertySource#getPropertyValue(java
     * .lang.Object)
     */
    @Override
    public Object getPropertyValue(final Object id) {
        return propertyHolder.getPropertyValue(id);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.views.properties.IPropertySource#isPropertySet(java.lang
     * .Object)
     */
    @Override
    public boolean isPropertySet(final Object id) {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.views.properties.IPropertySource#resetPropertyValue(java
     * .lang.Object)
     */
    @Override
    public void resetPropertyValue(final Object id) {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.ui.views.properties.IPropertySource#setPropertyValue(java
     * .lang.Object, java.lang.Object)
     */
    @Override
    public void setPropertyValue(final Object id, final Object value) {
    }
}
