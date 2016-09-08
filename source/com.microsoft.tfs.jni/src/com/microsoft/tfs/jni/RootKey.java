// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.jni;

/**
 * Define Java constants to represent Windows registry root handles. The Windows
 * native code maps the integers defined here to the WIN32 predefined root
 * hadles. WARNING: these values must stay in sync with the values in the
 * Windows native code.
 *
 *
 * @threadsafety unknown
 */
public class RootKey {
    // WARNING: These integer values are mapped in the native code to the
    // corresponding root registry key (e.g. HKEY_CURRENT_USER). The values
    // defined here must stay in sync with the mapping method in the natives.

    public static final RootKey HKEY_CURRENT_USER = new RootKey(1, "HKEY_CURRENT_USER"); //$NON-NLS-1$
    public static final RootKey HKEY_LOCAL_MACHINE = new RootKey(2, "HKEY_LOCAL_MACHINE"); //$NON-NLS-1$
    public static final RootKey HKEY_CLASSES_ROOT = new RootKey(3, "HKEY_CLASSES_ROOT"); //$NON-NLS-1$

    private final int value;
    private final String name;

    private RootKey(final int value, final String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
