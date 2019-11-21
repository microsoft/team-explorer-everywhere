// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

import com.microsoft.tfs.jni.internal.winapi.Advapi32;
import com.microsoft.tfs.util.Check;
import com.microsoft.tfs.util.LocaleInvariantStringHelpers;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.ptr.IntByReference;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        final String[] subSubkeyNames = nativeGetSubkeys(root.getValue(), subkeyPath);

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
        return nativeGetValue(root.getValue(), path, name);
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
        final String[] subkeyNames = nativeGetSubkeys(root.getValue(), path);
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
        return nativeGetValues(root.getValue(), path);
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
    private static final int KEY_NAME_MAXSIZE = 256;
    private static final int VALUE_NAME_MAXSIZE = 16383;
    private static final int VALUE_MAXSIZE = 32768;

    private static WinReg.HKEY getRootKey(int rootKeyId) {
        switch (rootKeyId) {
            case 3:
                return WinReg.HKEY_CLASSES_ROOT;
            case 2:
                return WinReg.HKEY_LOCAL_MACHINE;
            default:
                return WinReg.HKEY_CURRENT_USER;
        }
    }

    private static Advapi32 advapi32 = Advapi32.INSTANCE;

    private static void nativeCreate(int rootID, String subkeyName) {
        WinReg.HKEY rootKey = getRootKey(rootID);
        WinReg.HKEYByReference subkey = new WinReg.HKEYByReference();
        int result = advapi32.RegCreateKeyEx(
            rootKey,
            subkeyName,
            0,
            null,
            0,
            WinNT.KEY_WRITE,
            null,
            subkey,
            null);
        if (result != 0) {
            throw new RegistryException("Registry error code: " + result);
        }

        advapi32.RegCloseKey(subkey.getValue());
    }

    private static WinReg.HKEY HKEY_ERROR = new WinReg.HKEY(0);
    private static WinReg.HKEY openKey(int rootKeyId, String path, int accessType) {
        WinReg.HKEY rootKey = getRootKey(rootKeyId);
        WinReg.HKEYByReference subkey = new WinReg.HKEYByReference();
        int result = advapi32.RegOpenKeyEx(rootKey, path, 0, accessType, subkey);
        if (result != 0)
            return HKEY_ERROR;

        return subkey.getValue();
    }

    private static boolean nativeExists(int rootID, String path) {
        WinReg.HKEY hkey = openKey(rootID, path, WinNT.KEY_READ);
        if (!hkey.equals(HKEY_ERROR)) {
            advapi32.RegCloseKey(hkey);
            return true;
        }

        return false;
    }

    private static boolean nativeCreateSubkey(int rootID, String path, String name) {
        WinReg.HKEY key = openKey(rootID, path, WinNT.KEY_WRITE);
        if (key.equals(HKEY_ERROR)) {
            return false;
        }

        try {
            WinReg.HKEYByReference subkey = new WinReg.HKEYByReference();
            int result = advapi32.RegCreateKeyEx(key, name, 0, null, 0, WinNT.KEY_WRITE, null, subkey, null);
            if (result == 0) {
                advapi32.RegCloseKey(subkey.getValue());
                return true;
            }

            return false;
        } finally {
            advapi32.RegCloseKey(key);
        }
    }

    private static String[] nativeGetSubkeys(int rootID, String path) {
        WinReg.HKEY key = openKey(rootID, path, WinNT.KEY_READ);
        if (key.equals(HKEY_ERROR)) {
            return null;
        }

        try {
            IntByReference subkeyCount = new IntByReference();
            int result = advapi32.RegQueryInfoKey(
                key,
                null,
                null,
                null,
                subkeyCount,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
            if (result != WinNT.ERROR_SUCCESS) {
                return null;
            }

            String[] subkeys = new String[subkeyCount.getValue()];
            for (int subkeyIndex = 0; subkeyIndex < subkeyCount.getValue(); ++subkeyIndex) {
                char[] buffer = new char[KEY_NAME_MAXSIZE];
                IntByReference size = new IntByReference(buffer.length);
                result = advapi32.RegEnumKeyEx(key, subkeyIndex, buffer, size, null, null, null, null);
                if (result == 0) {
                    String subkeyName = new String(buffer, 0, size.getValue());
                    subkeys[subkeyIndex] = subkeyName;
                }
            }

            return subkeys;
        } finally {
            advapi32.RegCloseKey(key);
        }
    }

    private static boolean nativeDeleteSubkey(int rootID, String path, String name) {
        WinReg.HKEY parentKey = openKey(rootID, path, WinNT.KEY_WRITE);
        if (parentKey.equals(HKEY_ERROR))
            return false;

        try {
            return advapi32.RegDeleteKey(parentKey, name) == WinNT.ERROR_SUCCESS;
        } finally {
            advapi32.RegCloseKey(parentKey);
        }
    }

    private static WinReg.HKEY getSubkey(WinReg.HKEY key, String subkeyName, int accessType) {
        WinReg.HKEYByReference subkey = new WinReg.HKEYByReference();
        if (advapi32.RegOpenKeyEx(key, subkeyName, 0, accessType, subkey) == 0) {
            return subkey.getValue();
        }

        return HKEY_ERROR;
    }

    private static RegistryValue createRegistryValue(String valueName, byte[] data, int actualType) {
        if (actualType == WinNT.REG_DWORD && data.length >= 4) {
            int value = (data[3] << 24) + (data[2] << 16) + (data[1] << 8) + data[0];
            return new RegistryValue(valueName, value);
        } else if (actualType == WinNT.REG_SZ) {
            try {
                String value = new String(data, "UTF-16");
                return new RegistryValue(valueName, value);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else
            return null;
    }

    private static RegistryValue getValue(WinReg.HKEY key, String subkeyName, String valueName, int[] types) {
        WinReg.HKEY subkey = getSubkey(key, subkeyName, WinNT.KEY_READ);
        if (subkey.equals(HKEY_ERROR))
            return null;

        try {
            IntByReference actualType = new IntByReference();
            byte[] buffer = new byte[VALUE_MAXSIZE];
            IntByReference size = new IntByReference(buffer.length);
            if (advapi32.RegQueryValueExW(subkey, valueName, null, actualType, buffer, size) != 0) {
                return null;
            }

            // Ensure the actual type matches what was desired:
            if (types != null) {
                boolean matchedType = false;
                for (int type : types) {
                    if (type == actualType.getValue()) {
                        matchedType = true;
                        break;
                    }
                }

                if (!matchedType)
                    return null;
            }

            return createRegistryValue(valueName, buffer, actualType.getValue());
        } finally {
            advapi32.RegCloseKey(subkey);
        }
    }

    private static RegistryValue nativeGetValue(int rootID, String path, String name) {
        WinReg.HKEY key = getRootKey(rootID);
        return getValue(key, path, name, new int[] { WinNT.REG_DWORD, WinNT.REG_SZ });
    }

    private static RegistryValue[] nativeGetValues(int rootID, String path) {
        WinReg.HKEY key = openKey(rootID, path, WinNT.KEY_READ);
        if (key.equals(HKEY_ERROR))
            return null;

        try {
            IntByReference valueCount = new IntByReference();
            if (advapi32.RegQueryInfoKey(key, null, null, null, null, null, null, valueCount, null, null, null, null) != WinNT.ERROR_SUCCESS) {
                return null;
            }

            List<RegistryValue> values = new ArrayList<RegistryValue>(valueCount.getValue());
            char[] nameBuffer = new char[VALUE_NAME_MAXSIZE];
            byte[] dataBuffer = new byte[VALUE_MAXSIZE];
            for (int valueIndex = 0; valueIndex < valueCount.getValue(); ++valueIndex) {
                IntByReference nameSize = new IntByReference(nameBuffer.length);
                IntByReference dataSize = new IntByReference(dataBuffer.length);
                IntByReference valueType = new IntByReference();
                if (advapi32.RegEnumValue(
                    key,
                    valueIndex,
                    nameBuffer,
                    nameSize,
                    null,
                    valueType,
                    dataBuffer,
                    dataSize) != WinNT.ERROR_SUCCESS) {
                    break;
                }

                String valueName = new String(nameBuffer, 0, nameSize.getValue());
                RegistryValue value = createRegistryValue(valueName, dataBuffer, valueType.getValue());
                values.add(value);
            }

            return values.toArray(new RegistryValue[0]);
        } finally {
            advapi32.RegCloseKey(key);
        }
    }

    private static boolean nativeHasSubkey(int rootID, String path, String name) {
        WinReg.HKEY key = openKey(rootID, path, WinNT.KEY_READ);
        if (key.equals(HKEY_ERROR))
            return false;

        try {
            WinReg.HKEY subkey = getSubkey(key, name, WinNT.KEY_READ);
            if (subkey.equals(HKEY_ERROR))
                return false;

            advapi32.RegCloseKey(subkey);
            return true;
        } finally {
            advapi32.RegCloseKey(key);
        }
    }

    private static boolean nativeHasSubkeys(int rootID, String path) {
        WinReg.HKEY key = openKey(rootID, path, WinNT.KEY_READ);
        if (key.equals(HKEY_ERROR))
            return false;

        try {
            IntByReference subkeyCount = new IntByReference();
            if (advapi32.RegQueryInfoKey(
                key,
                null,
                null,
                null,
                subkeyCount,
                null,
                null,
                null,
                null,
                null,
                null,
                null) != WinNT.ERROR_SUCCESS) {
                return false;
            }

            return subkeyCount.getValue() > 0;
        } finally {
            advapi32.RegCloseKey(key);
        }
    }

    private static boolean nativeHasValue(int rootID, String path, String name) {
        WinReg.HKEY key = getRootKey(rootID);
        RegistryValue value = getValue(key, path, name, null);
        return value != null;
    }

    private static boolean nativeHasValues(int rootID, String path) {
        WinReg.HKEY key = openKey(rootID, path, WinNT.KEY_READ);
        if (key.equals(HKEY_ERROR))
            return false;

        try {
            IntByReference valuesCount = new IntByReference();
            if (advapi32.RegQueryInfoKey(
                key,
                null,
                null,
                null,
                null,
                null,
                null,
                valuesCount,
                null,
                null,
                null,
                null) != WinNT.ERROR_SUCCESS) {
                return false;
            }

            return valuesCount.getValue() > 0;
        } finally {
            advapi32.RegCloseKey(key);
        }
    }

    private static boolean setValue(WinReg.HKEY key, String valueName, int type, byte[] data) {
        return advapi32.RegSetValueEx(key, valueName, 0, type, data, data.length) == WinNT.ERROR_SUCCESS;
    }

    // TODO: What if the existing value is a different type?
    private static void nativeSetDwordValue(int rootID, String path, String name, int data) {
        WinReg.HKEY key = openKey(rootID, path, WinNT.KEY_WRITE);
        if (key.equals(HKEY_ERROR))
            return;

        try {
            byte[] bytes = new byte[4];
            bytes[0] = (byte)(data & 0xff);
            data >>>= 8;
            bytes[1] = (byte)(data & 0xff);
            data >>>= 8;
            bytes[2] = (byte)(data & 0xff);
            data >>>= 8;
            bytes[3] = (byte)(data & 0xff);

            setValue(key, name, WinNT.REG_DWORD, bytes);
        } finally {
            advapi32.RegCloseKey(key);
        }
    }

    // TODO: What if the existing value is a different type?
    private static void nativeSetStringValue(int rootID, String path, String name, String data) {
        WinReg.HKEY key = openKey(rootID, path, WinNT.KEY_WRITE);
        if (key.equals(HKEY_ERROR))
            return;

        try {
            byte[] stringData = data.getBytes("UTF-16");
            byte[] stringDataWithTerminatingZero = Arrays.copyOf(stringData, stringData.length + 1);
            setValue(key, name, WinNT.REG_SZ, stringDataWithTerminatingZero);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } finally {
            advapi32.RegCloseKey(key);
        }
    }

    private static boolean nativeDeleteValue(int rootID, String path, String name) {
        WinReg.HKEY parentKey = openKey(rootID, path, WinNT.KEY_WRITE);
        if (parentKey.equals(HKEY_ERROR))
            return false;

        try {
            return advapi32.RegDeleteValue(parentKey, name) == WinNT.ERROR_SUCCESS;
        } finally {
            advapi32.RegCloseKey(parentKey);
        }
    }
}
