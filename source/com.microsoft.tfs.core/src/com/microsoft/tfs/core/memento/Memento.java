// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.core.memento;

import java.util.Map;

/**
 * <p>
 * An in-memory hierarchichal data structure, intended primarily for persisting
 * a program component's settings. {@link Memento}s form trees, and each tree
 * node can have data attached (by string key) and can have children (more
 * {@link Memento}s).
 * </p>
 * <p>
 * In short, things you can do with a {@link Memento}:
 * </p>
 * <p>
 * <ul>
 * <li>Add data to it by a String key</li>
 * <li>Get data by String key</li>
 * <li>Add text to the special text node (no key required)</li>
 * <li>Get text from the special text node (no key required)</li>
 * <li>Create child Memento objects</li>
 * <li>Get its child Memento objects</li>
 * <li>Copy an existing Memento into a new child of another Memento</li>
 * </ul>
 * </p>
 * <p>
 * A {@link Memento} must accept multiple children with the same name. The child
 * collection's order is unspecified (this is important to remember when calling
 * {@link #getChild(String)}, which returns any child node with the matching
 * name). {@link Memento} names and attribute names are case-sensitive.
 * </p>
 * <p>
 * The String key names for all types of attributes (int, long, boolean, String,
 * etc.) share the same namespace. The different types of accessors are for
 * convenience, they may be stored as string representations inside.
 * </p>
 *
 * @since TEE-SDK-10.1
 * @threadsafety thread-safe
 */
public interface Memento {
    /**
     * <p>
     * Creates a new {@link Memento} with the given name and attaches it as a
     * child to this {@link Memento}. Child names do not have to be unique
     * within a memento; multiple children with the same name are allowed.
     * </p>
     * <p>
     * Use the {@link #getChild(String)} and {@link #getChildren(String)}
     * methods to retrieve children by name.
     * </p>
     *
     * @param name
     *        the name of the child {@link Memento} (must not be
     *        <code>null</code> or empty)
     * @return the new child {@link Memento} created in this memento
     */
    public Memento createChild(String name);

    /**
     * Returns a child with the given name. The order in which children are
     * stored is not specified, so this method may return a child with the given
     * name at any position in the child collection. To access multiple children
     * with the same name, use {@link #getChildren(String)}.
     *
     * @param name
     *        the name of the child {@link Memento} to get (must not be
     *        <code>null</code> or empty)
     * @return the first child with the given name
     */
    public Memento getChild(String name);

    /**
     * Returns all children with the given name.
     *
     * @param name
     *        the name of the child {@link Memento}s to get (must not be
     *        <code>null</code> or empty)
     * @return an array of children with the given name, never null but may be
     *         empty
     */
    public Memento[] getChildren(String name);

    /**
     * Gets all the children.
     *
     * @return an array of all children, never null but may be empty
     */
    public Memento[] getAllChildren();

    /**
     * Removes all children with the given name.
     *
     * @param name
     *        the name of the children to remove (must not be <code>null</code>
     *        or empty)
     * @return the children that were removed, never null but may be empty
     */
    public Memento[] removeChildren(String name);

    /**
     * Removes the given child. The memento is matched to this memento's
     * children by object equality.
     *
     * @param memento
     *        the child to remove (must not be <code>null</code>)
     * @return true if the memento was a child (and was removed), false if the
     *         memento was not a child
     */
    public boolean removeChild(Memento memento);

    /**
     * @return this {@link Memento}'s name (never <code>null</code> or empty)
     */
    public String getName();

    /**
     * Gets a map of attribute keys to values. All keys and values are
     * {@link String}s.
     *
     * @return the {@link Map} of attribute {@link String} keys to
     *         {@link String} values (never <code>null</code>)
     */
    public Map getAllAttributes();

    /**
     * Gets the double floating point value of the given key.
     *
     * @param key
     *        the key
     * @return the value, or <code>null</code> if the key was not found or was
     *         found but was not a floating point number
     */
    public Double getDouble(String key);

    /**
     * Gets the floating point value of the given key.
     *
     * @param key
     *        the key
     * @return the value, or <code>null</code> if the key was not found or was
     *         found but was not a floating point number
     */
    public Float getFloat(String key);

    /**
     * Gets the integer value of the given key.
     *
     * @param key
     *        the key
     * @return the value, or <code>null</code> if the key was not found or was
     *         found but was not an integer
     */
    public Integer getInteger(String key);

    /**
     * Gets the long integer value of the given key.
     *
     * @param key
     *        the key
     * @return the value, or <code>null</code> if the key was not found or was
     *         found but was not an integer
     */
    public Long getLong(String key);

    /**
     * Gets the string value of the given key.
     *
     * @param key
     *        the key
     * @return the value, or <code>null</code> if the key was not found
     */
    public String getString(String key);

    /**
     * Gets the Boolean value of the given key.
     *
     * @param key
     *        the key
     * @return the value, or null if the key was not found
     */
    public Boolean getBoolean(String key);

    /**
     * Gets the data from the special text area of the {@link Memento}. Each
     * {@link Memento} is allowed only one special text area.
     *
     * @return the contents of the special text area of the {@link Memento}, or
     *         <code>null</code> if the {@link Memento} has no text.
     */
    public String getTextData();

    /**
     * Sets the value of the given key to the given double floating point
     * number.
     *
     * @param key
     *        the key (must not be <code>null</code> or empty)
     * @param value
     *        the value
     */
    public void putDouble(String key, double value);

    /**
     * Sets the value of the given key to the given floating point number.
     *
     * @param key
     *        the key (must not be <code>null</code> or empty)
     * @param value
     *        the value
     */
    public void putFloat(String key, float value);

    /**
     * Sets the value of the given key to the given integer.
     *
     * @param key
     *        the key (must not be <code>null</code> or empty)
     * @param value
     *        the value (may be <code>null</code> or empty)
     */
    public void putInteger(String key, int value);

    /**
     * Sets the value of the given key to the given long integer.
     *
     * @param key
     *        the key (must not be <code>null</code> or empty)
     * @param value
     *        the value (may be <code>null</code> or empty)
     */
    public void putLong(String key, long value);

    /**
     * Sets the value of the given key to the given string.
     *
     * @param key
     *        the key (must not be <code>null</code> or empty)
     * @param value
     *        the value (may be <code>null</code> or empty)
     */
    public void putString(String key, String value);

    /**
     * Sets the value of the given key to the given boolean value.
     *
     * @param key
     *        the key (must not be <code>null</code> or empty)
     * @param value
     *        the value (may be <code>null</code> or empty)
     */
    public void putBoolean(String key, boolean value);

    /**
     * Sets the {@link Memento}'s special text area to contain the given data.
     * If a special text value was previously set, it is replaced with the given
     * text. Each memento is allowed only one special text value.
     *
     * @param text
     *        the text to be placed into the special text area (may be
     *        <code>null</code> or empty)
     */
    public void putTextData(String text);

    /**
     * Copy the special text, attributes, and children from the given
     * {@link Memento} into the receiver. The name of the receiver
     * {@link Memento} is not changed.
     *
     * @param memento
     *        the {@link Memento} to be copied (must not be <code>null</code>)
     */
    public void putMemento(Memento memento);
}
