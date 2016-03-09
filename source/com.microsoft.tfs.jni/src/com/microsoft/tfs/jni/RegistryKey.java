// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.tfs.jni.internal.LibraryNames;
import com.microsoft.tfs.jni.loader.NativeLoader;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleInvariantStringHelpers;

/**
 * The class is the main API which provides support for navigating, reading, and
 * writing to the Windows registry. The registry key is represented by a ROOT
 * and PATH both of which are immutable. The ROOT is a root key in the windows
 * registry (such has HKEY_CURRENT_USER), the PATH is the full key path beneath
 * the ROOT key (such as Software\Microsoft).
 *
 *
 * @threadsafety unknown
 */
public class RegistryKey {
    /**
     * This static initializer is a "best-effort" native code loader (no
     * exceptions thrown for normal load failures).
     *
     * Apps with multiple classloaders (like Eclipse) can run this initializer
     * more than once in a single JVM OS process, and on some platforms
     * (Windows) the native libraries will fail to load the second time, because
     * they're already loaded. This failure can be ignored because the native
     * code will execute fine.
     */
    static {
        NativeLoader.loadLibraryAndLogError(LibraryNames.WINDOWS_REGISTRY_LIBRARY_NAME);
    }

    /*
     * The root key such has HKEY_CURRENT_USER.
     */
    private final RootKey root;

    /*
     * The path beneath the root key such as Software\Microsoft.
     */
    private final String path;

    /**
     * Each instance must have a non-null root and path and is immutable.
     *
     * @param root
     *        A root key such as HKEY_CURRENT_USER (must not be
     *        <code>null</code>)
     * @param path
     *        The registry path such as Software\Microsoft (must not be
     *        <code>null</code>)
     */
    public RegistryKey(final RootKey root, final String path) {
        Check.notNull(root, "root"); //$NON-NLS-1$
        Check.notNull(path, "path"); //$NON-NLS-1$

        this.root = root;
        this.path = path;
    }

    /**
     * Returns the RootKey associated with this registry key.
     */
    public RootKey getRoot() {
        return root;
    }

    /**
     * Returns the Path associated with this registry key.
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the name of this registry key (ie, the last portion of the path.)
     */
    public String getName() {
        if (path == null || !path.contains("\\")) //$NON-NLS-1$
        {
            return path;
        }

        return path.substring(path.lastIndexOf("\\") + 1); //$NON-NLS-1$
    }

    /**
     * Creates this registry key in the system registry.
     *
     * @throws RegistryException
     *         if this registry key already exists in the registry.
     */
    public void create() {
        if (exists()) {
            final String format = Messages.getString("RegistryKey.KeyAlreadyExistsFormat"); //$NON-NLS-1$
            throw new RegistryException(MessageFormat.format(format, path));
        }

        nativeCreate(root.getValue(), path);
    }

    /**
     * Tests if this registry key currently exists.
     *
     *
     * @return Returns true if the key exists in the registry.
     */
    public boolean exists() {
        return nativeExists(root.getValue(), path);
    }

    /**
     * Creates a new RegistryKey for the specified subkey if it exists under
     * this registry key. Returns null if the subkey does not exist.
     *
     *
     * @param name
     *        The subkey name, must not be null.
     * @return
     */
    public RegistryKey getSubkey(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$

        if (nativeHasSubkey(root.getValue(), path, name)) {
            return new RegistryKey(root, createSubkeyPath(name));
        }

        return null;
    }

    /**
     * Creates the specified subkey.
     *
     *
     * @param name
     *        The name of the subkey to be created.
     * @return The created subkey.
     */
    public RegistryKey createSubkey(final String name) {
        final boolean success = nativeCreateSubkey(root.getValue(), path, name);
        return success ? new RegistryKey(root, createSubkeyPath(name)) : null;
    }

    /**
     * Deletes the specified subkey. Note that the subkey must be empty (may not
     * have its own subkeys.)
     *
     * @param name
     *        The name of the subkey to delete
     * @return <code>true</code> if the subkey was deleted, <code>false</code>
     *         otherwise
     */
    public boolean deleteSubkey(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$

        return nativeDeleteSubkey(root.getValue(), path, name);
    }

    /**
     * Convenience method to deletes the specified subkey and any child subkeys.
     *
     * @param name
     *        The name of the subkey to delete
     * @return <code>true</code> if the subkey was deleted, <code>false</code>
     *         otherwise
     */
    public boolean deleteSubkeyRecursive(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$

        return deleteSubkeyRecursive(root.getValue(), path, name);
    }

    private boolean deleteSubkeyRecursive(final int rootID, final String path, final String name) {
        Check.notNull(path, "path"); //$NON-NLS-1$
        Check.notNull(name, "name"); //$NON-NLS-1$

        final String subkeyPath = path + "\\" + name; //$NON-NLS-1$
        final String[] subSubkeyNames = (String[]) nativeGetSubkeys(root.getValue(), subkeyPath);

        if (subSubkeyNames != null) {
            for (final String subSubkeyName : subSubkeyNames) {
                deleteSubkeyRecursive(rootID, subkeyPath, subSubkeyName);
            }
        }

        return nativeDeleteSubkey(rootID, path, name);
    }

    /**
     * Convenience method to return the registry value as a string value. This
     * should only be called when it is known that the registry value is a
     * string. If the value does not exist in the registry the specified default
     * value is returned.
     *
     *
     * @param name
     *        The value name in this subkey.
     * @param defaultValue
     *        The value to return if the registry value does not exist.
     * @return The string value read from the registry or the default value
     *         supplied by the caller.
     */
    public String getStringValue(final String name, final String defaultValue) {
        final RegistryValue value = getValue(name);

        if (value == null || value.isStringType() == false) {
            return defaultValue;
        } else {
            return value.getStringValue();
        }
    }

    /**
     * Convenience method to return the registry value as an integer value. This
     * should only be called when it is known that the registry value is a
     * DWORD. If the value does not exist in the registry the specified default
     * value is returned.
     *
     *
     * @param name
     *        The value name in this subkey.
     * @param defaultValue
     *        The value to return if the registry value does not exist.
     * @return The integer value read from the registry or the default value
     *         supplied by the caller.
     */
    public int getIntegerValue(final String name, final int defaultValue) {
        final RegistryValue value = getValue(name);

        if (value == null || value.isIntegerType() == false) {
            return defaultValue;
        } else {
            return value.getIntegerValue();
        }
    }

    /**
     * Gets the value associated with the specified value name from this
     * registry key. Returns null if the value does not exist or is not a
     * supported type.
     *
     *
     * @param name
     *        The value name.
     * @return
     */
    public RegistryValue getValue(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$
        return (RegistryValue) nativeGetValue(root.getValue(), path, name);
    }

    /**
     * Get the Windows registry default value for this registry key.
     *
     *
     * @return
     */
    public RegistryValue getDefaultValue() {
        return getValue(""); //$NON-NLS-1$
    }

    /**
     * Deletes the value associated with the specified value name from this
     * registry key.
     *
     * @param name
     *        The value name
     * @return <code>true</code> if the value was deleted, <code>false</code>
     *         otherwise.
     */
    public boolean deleteValue(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$
        return nativeDeleteValue(root.getValue(), path, name);
    }

    /**
     * Returns true if this registry key contains the specified subkey.
     *
     *
     * @param name
     *        The subkey name to find. Must not be null.
     * @return
     */
    public boolean hasSubkey(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$
        return nativeHasSubkey(root.getValue(), path, name);
    }

    /**
     * Returns true if this registry key has any subkeys.
     *
     *
     * @return
     */
    public boolean hasSubkeys() {
        return nativeHasSubkeys(root.getValue(), path);
    }

    /**
     * Returns true if this registry key has a value with the specified name.
     *
     *
     * @param name
     *        The value name to find. Must not be null.
     * @return
     */
    public boolean hasValue(final String name) {
        Check.notNull(name, "name"); //$NON-NLS-1$
        return nativeHasValue(root.getValue(), path, name);
    }

    /**
     * Returns true if this registry key contains any values.
     *
     *
     * @return
     */
    public boolean hasValues() {
        return nativeHasValues(root.getValue(), path);
    }

    /**
     * Set the specified value in this registry key. If the value already exists
     * it is overwritten. It is created if it does not currently exist.
     *
     *
     * @param value
     *        The value to set. Must not be null.
     */
    public void setValue(final RegistryValue value) {
        Check.notNull(value, "value"); //$NON-NLS-1$

        if (value.getType() == ValueType.REG_DWORD) {
            nativeSetDwordValue(root.getValue(), path, value.getName(), ((Integer) value.getValue()).intValue());
        } else if (value.getType() == ValueType.REG_SZ) {
            nativeSetStringValue(root.getValue(), path, value.getName(), (String) value.getValue());
        }
    }

    /**
     * Returns the set of subkeys for this registry key.
     *
     *
     * @return
     */
    public RegistryKey[] subkeys() {
        final String[] subkeyNames = (String[]) nativeGetSubkeys(root.getValue(), path);
        if (subkeyNames == null) {
            return new RegistryKey[0];
        }

        final List<RegistryKey> subkeys = new ArrayList<RegistryKey>(subkeyNames.length);
        for (final String subkeyName : subkeyNames) {
            subkeys.add(new RegistryKey(root, createSubkeyPath(subkeyName)));
        }

        return subkeys.toArray(new RegistryKey[subkeys.size()]);
    }

    @Override
    public String toString() {
        return root.getName() + "\\" + path; //$NON-NLS-1$
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof RegistryKey == false) {
            return false;
        }

        final RegistryKey other = (RegistryKey) obj;

        return root == other.root && path.equalsIgnoreCase(other.path);
    }

    @Override
    public int hashCode() {
        int result = 17;

        result = result * 37 + root.hashCode();
        result = result * 37 + LocaleInvariantStringHelpers.caseInsensitiveHashCode(path);

        return result;
    }

    /**
     * Returns the set of values for this registry key.
     *
     *
     * @return
     */
    public RegistryValue[] values() {
        return (RegistryValue[]) nativeGetValues(root.getValue(), path);
    }

    /**
     * Create a subkey path by combining this path with a subkey name.
     *
     *
     * @param path
     * @param subkey
     * @return
     */
    private String createSubkeyPath(final String subkey) {
        return path + "\\" + subkey; //$NON-NLS-1$
    }

    /*
     * The NATIVE method definitions.
     */

    private static native void nativeCreate(int rootID, String path);

    private static native boolean nativeExists(int rootID, String path);

    private static native boolean nativeCreateSubkey(int rootID, String path, String name);

    private static native Object nativeGetSubkeys(int rootID, String path);

    private static native boolean nativeDeleteSubkey(int rootID, String path, String name);

    private static native Object nativeGetValue(int rootID, String path, String name);

    private static native Object nativeGetValues(int rootID, String path);

    private static native boolean nativeHasSubkey(int rootID, String path, String name);

    private static native boolean nativeHasSubkeys(int rootID, String path);

    private static native boolean nativeHasValue(int rootID, String path, String name);

    private static native boolean nativeHasValues(int rootID, String path);

    private static native void nativeSetDwordValue(int rootID, String path, String name, int data);

    private static native void nativeSetStringValue(int rootID, String path, String name, String data);

    private static native boolean nativeDeleteValue(int rootID, String path, String name);
}
