// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the repository root.

package com.microsoft.tfs.client.common.ui.framework.helper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.microsoft.tfs.client.common.ui.framework.WindowSystem;
import com.microsoft.tfs.util.Check;

public class ColorUtils {
    private static final Log log = LogFactory.getLog(ColorUtils.class);

    /**
     * Returns a hexadecimal string ("html color") describing the given color.
     *
     * @param color
     *        The color to describe in hexadecimal notation (not null)
     * @return A hexadecimal string (including leading "#") that describes the
     *         color
     */
    public static String getColorString(final Color color) {
        Check.notNull(color, "color"); //$NON-NLS-1$

        final String red = Integer.toHexString(color.getRed());
        final String green = Integer.toHexString(color.getGreen());
        final String blue = Integer.toHexString(color.getBlue());

        final StringBuffer sb = new StringBuffer("#"); //$NON-NLS-1$

        if (red.length() == 1) {
            sb.append("0"); //$NON-NLS-1$
        }
        sb.append(red);

        if (green.length() == 1) {
            sb.append("0"); //$NON-NLS-1$
        }
        sb.append(green);

        if (blue.length() == 1) {
            sb.append("0"); //$NON-NLS-1$
        }
        sb.append(blue);

        return sb.toString();
    }

    /**
     * Convert hexadecimal string to RGB
     *
     * @param hexadecimal
     * @return
     */
    public static RGB hexadecimalToRGB(final String hexadecimal) {
        java.awt.Color col = null;
        try {
            col = java.awt.Color.decode(hexadecimal);
        } catch (final Exception e) {
            col = java.awt.Color.WHITE;
        }
        final int red = col.getRed();
        final int blue = col.getBlue();
        final int green = col.getGreen();

        return new RGB(red, green, blue);
    }

    /**
     * Convert hexademical to SWT color
     *
     * @param hexadecimal
     * @return
     */
    public static Color hexadecimalToColor(final Display display, final String hexadecimal) {
        return new Color(display, hexadecimalToRGB(hexadecimal));
    }

    /**
     * Returns a color created as the average of the two provided color values.
     * The returned color must be disposed properly.
     *
     * @param one
     *        A color to build the average from (not <code>null</code>)
     * @param two
     *        A color to build the average from (not <code>null</code>)
     * @return The average color created from the inputs (must be disposed)
     */
    public static Color getAverageColor(final Display display, final Color one, final Color two) {
        Check.notNull(display, "display"); //$NON-NLS-1$
        Check.notNull(one, "one"); //$NON-NLS-1$
        Check.notNull(two, "two"); //$NON-NLS-1$

        return new Color(
            display,
            (one.getRed() + two.getRed()) / 2,
            (one.getGreen() + two.getGreen()) / 2,
            (one.getBlue() + two.getBlue()) / 2);
    }

    /* WIN32 Specific Color Handling */

    /**
     * Gets the windows system color id identified by name, which will be
     * resolved to the SWT Win32 specific color names (mostly a mirror of the
     * constants used by GetSysColor, but not necessarily. See
     * org.eclipse.swt.internal.win32.OS for color names.)
     *
     * @param colorName
     *        The name of the color to resolve.
     * @throws IllegalArgumentException
     *         if the current platform is not win32
     * @return The color id identified by this name, or -1 if it could not be
     *         looked up.
     */
    @SuppressWarnings({
        "unchecked",
        "rawtypes"
    })
    public static int getWin32SystemColorID(final String colorName) {
        Check.notNull(colorName, "colorName"); //$NON-NLS-1$
        Check.isTrue(WindowSystem.isCurrentWindowSystem(WindowSystem.WIN32), "WindowSystem.WIN32"); //$NON-NLS-1$

        try {
            final Class osClass = Class.forName("org.eclipse.swt.internal.win32.OS"); //$NON-NLS-1$

            if (osClass == null) {
                log.warn("Could not load win32 constants class"); //$NON-NLS-1$
            } else {
                final Field swtColorIdField = osClass.getField(colorName);

                if (swtColorIdField == null) {
                    log.warn(MessageFormat.format("Could not load swt win32 color constant {0}", colorName)); //$NON-NLS-1$
                } else {
                    /*
                     * Get the SWT constant id for this color (this is not the
                     * windows color id)
                     */
                    final Integer swtColorIdValue = swtColorIdField.getInt(osClass);

                    if (swtColorIdValue == null) {
                        log.warn(MessageFormat.format("Could not load swt win32 color constant {0}", colorName)); //$NON-NLS-1$
                    } else {
                        /* Now look up the windows color ID */
                        final Method sysColorMethod = osClass.getMethod("GetSysColor", new Class[] { //$NON-NLS-1$
                            int.class
                        });

                        if (sysColorMethod == null) {
                            log.warn("Could not load win32 GetSysColor method"); //$NON-NLS-1$
                        } else {
                            final Object winColorId = sysColorMethod.invoke(osClass, new Object[] {
                                swtColorIdValue.intValue()
                            });

                            if (winColorId == null) {
                                log.warn(MessageFormat.format("Could not query win32 color constant {0}", colorName)); //$NON-NLS-1$
                            } else if (!(winColorId instanceof Integer)) {
                                log.warn(MessageFormat.format(
                                    "Received non-integer win32 color constant for {0}", //$NON-NLS-1$
                                    colorName));
                            } else {
                                return ((Integer) winColorId).intValue();
                            }
                        }
                    }
                }
            }
        } catch (final Throwable t) {
            log.warn("Could not load win32 constants", t); //$NON-NLS-1$
        }

        return -1;
    }

    /**
     * Gets the windows system color identified by name, which will be resolved
     * to the SWT Win32 specific color names (mostly a mirror of the constants
     * used by GetSysColor, but not necessarily. See
     * org.eclipse.swt.internal.win32.OS for color names.)
     *
     * This color MUST NOT be disposed.
     *
     * @param colorName
     *        The name of the color to resolve.
     * @throws IllegalArgumentException
     *         if the current platform is not win32
     * @return The color identified by this name, or null if it could not be
     *         looked up.
     */
    public static Color getWin32SystemColor(final Display display, final String colorName) {
        final int colorId = getWin32SystemColorID(colorName);

        if (colorId >= 0) {
            return getWin32SystemColor(display, colorId);
        }

        return null;
    }

    /**
     * Gets the windows system color identified by id (deferring to GetSysColor
     * system call.)
     *
     * This color MUST NOT be disposed.
     *
     * @param colorName
     *        The id of the color to be resolved.
     * @throws IllegalArgumentException
     *         if the current platform is not win32
     * @return The color identified by this id, or null if it could not be
     *         looked up.
     */
    @SuppressWarnings({
        "unchecked",
        "rawtypes"
    })
    public static Color getWin32SystemColor(final Display display, final int colorId) {
        Check.isTrue(WindowSystem.isCurrentWindowSystem(WindowSystem.WIN32), "WindowSystem.WIN32"); //$NON-NLS-1$

        try {
            final Class colorClass = Class.forName("org.eclipse.swt.graphics.Color"); //$NON-NLS-1$

            if (colorClass == null) {
                log.warn("Could not load win32 color class"); //$NON-NLS-1$
            } else {
                final Method createMethod = colorClass.getMethod("win32_new", new Class[] { //$NON-NLS-1$
                    Device.class,
                    int.class
                });

                if (createMethod == null) {
                    log.warn("Could not load win32 new color method"); //$NON-NLS-1$
                } else {
                    final Object color = createMethod.invoke(colorClass, new Object[] {
                        display,
                        colorId
                    });

                    if (color == null) {
                        log.warn(MessageFormat.format("Could not query win32 color id {0}", Integer.toString(colorId))); //$NON-NLS-1$
                    } else if (!(color instanceof Color)) {
                        log.warn("Received non-color from win32 color query"); //$NON-NLS-1$
                    } else {
                        return (Color) color;
                    }
                }
            }
        } catch (final Throwable t) {
            log.warn("Could not load win32 color", t); //$NON-NLS-1$
        }

        return null;
    }
}
