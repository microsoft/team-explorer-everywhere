// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.util;

import java.lang.reflect.Array;

public class ArrayUtils {
    public static Object[] convert(final Object[] a, final Class componentType) {
        Check.notNull(componentType, "componentType"); //$NON-NLS-1$

        if (a == null) {
            return null;
        }

        if (componentType.isPrimitive()) {
            throw new IllegalArgumentException("component type is primitive"); //$NON-NLS-1$
        }

        final Object[] x = (Object[]) Array.newInstance(componentType, a.length);

        System.arraycopy(a, 0, x, 0, a.length);
        return x;
    }

    public static String byteArrayToHexString(final byte[] bytes) {
        return byteArrayToHexString(bytes, true);
    }

    public static String byteArrayToHexStringUpperCase(final byte[] bytes) {
        return byteArrayToHexString(bytes, true);
    }

    public static String byteArrayToHexStringLowerCase(final byte[] bytes) {
        return byteArrayToHexString(bytes, false);
    }

    /**
     * @equivalence When b != null: concat(a, new T[] { b })
     * @equivalence When b == null: concat(a, null)
     */
    public static <T> T[] concat(final T[] a, final T b) {
        if (b == null) {
            return concat(a, null);
        }

        @SuppressWarnings("unchecked")
        final T[] bArray = (T[]) Array.newInstance((a != null ? a.getClass().getComponentType() : b.getClass()), 1);
        bArray[0] = b;

        return concat(a, bArray);
    }

    /**
     * Returns a new array with the items in b appended in their original order
     * to the items in a. Tolerates <code>null</code> inputs well:
     * <ul>
     * <li>If both inputs are <code>null</code>, <code>null</code> is returned
     * </li>
     * <li>If a is <code>null</code> and b is not <code>null</code>, an array
     * containing the items in b is returned</li>
     * <li>If b is <code>null</code> and a is not <code>null</code>, an array
     * containing the items in a is returned</li></li>
     *
     * @param a
     *        an array of strings (may be <code>null</code>)
     * @param b
     *        an array of strings to append to a (may be <code>null</code>)
     *        <code>null</code>)
     * @return a new array containing the elements from a and b (never
     *         <code>null</code>)
     */
    public static <T> T[] concat(final T[] a, final T[] b) {
        if (a == null && b == null) {
            return null;
        } else if (a == null) {
            return b.clone();
        } else if (b == null) {
            return a.clone();
        }

        @SuppressWarnings("unchecked")
        final T[] ret = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + b.length);
        System.arraycopy(a, 0, ret, 0, a.length);
        System.arraycopy(b, 0, ret, a.length, b.length);
        return ret;
    }

    private static String byteArrayToHexString(final byte[] bytes, final boolean upperCase) {
        Check.notNull(bytes, "bytes"); //$NON-NLS-1$

        final char[] chars = upperCase ? UCASE_HEX_CHARS : LCASE_HEX_CHARS;

        final StringBuffer buffer = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; i++) {
            buffer.append(chars[(bytes[i] & 0xF0) >> 4]);
            buffer.append(chars[bytes[i] & 0x0F]);
        }

        return buffer.toString();
    }

    private static final char[] LCASE_HEX_CHARS = "0123456789abcdef".toCharArray(); //$NON-NLS-1$
    private static final char[] UCASE_HEX_CHARS = "0123456789ABCDEF".toCharArray(); //$NON-NLS-1$
}
